package tftp;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Intermediate extends Stoppable{
	DatagramPacket sendPacket, receivePacket, delayPacket;
	DatagramSocket serverSideSocket, receiveSocket, replySocket,delaySocket;
	int replyPort, serverPort;
	InetAddress replyAddress;

	static String packetType = "", errorType = "", packetError = "";
	static int packetNumber = 0, time=0;

	public Intermediate() {	
		try {
			//create the two sockets which always exist, one on port 23 to receive requests from the client
			receiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public Intermediate(DatagramPacket received,boolean verbose, int clientPort) {
		this.verbose = verbose;
		filename = Message.parseFilename(new String(received.getData(),0,received.getLength()));
		receivePacket = received;
		try {
			serverSideSocket = new DatagramSocket();
			replySocket = new DatagramSocket();
			replyPort = clientPort; 
			serverPort = received.getPort();
			replyAddress = received.getAddress();
			receivePacket = received;
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}	

	public void run() {		
		//server side printing on receive
		try {
			byte tempData[] = new byte[516];
			tempData = receivePacket.getData();						
			sendPacket = new DatagramPacket(tempData, receivePacket.getLength(),InetAddress.getLocalHost(),69);
			readWriteError();
			int x = 0;			
			int timeoutCount = 0;
			timeout = false;
			while(true) { //loop forever
				if (timeoutCount ==5) {
					return;
				}
				if (Thread.interrupted()) {
					actualDelay();
				}
				byte data[] = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				try {
					serverSideSocket.setSoTimeout(600); 
					serverSideSocket.receive(receivePacket); //receive from server
					timeout = false;
					Message.printIncoming(receivePacket, "Intermediate Host - ServerSide",verbose);
					sendPacket = new DatagramPacket(receivePacket.getData(),receivePacket.getLength(),InetAddress.getLocalHost(),replyPort); 
				} catch (SocketTimeoutException e) {
					timeoutCount++;
					timeout = true;
				}
				if (x==0) {
					serverPort = receivePacket.getPort();
					x++;
				}				
				if (!timeout) {
					errorCheck(replySocket, "Client");				
				}
				try {
					replySocket.setSoTimeout(600); 
					replySocket.receive(receivePacket); //receive from client
					timeout = false;
					Message.printIncoming(receivePacket, "Intermediate Host - ClientSide",verbose);
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),InetAddress.getLocalHost(),serverPort);
				} catch (SocketTimeoutException e) {
					timeout = true;
				}

				if (!timeout) {
					errorCheck(serverSideSocket, "Server");
				}
			}
		} catch (UnknownHostException e){
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	private String getOpCode(byte[] code){
		if(code[0] == 0 && code[1] == 3){
			return "DATA";
		}
		else if(code[0] == 0 && code[1] == 4){
			return "ACK";
		}		
		return "";
	}

	private void delay(DatagramSocket del) //new thread to delay?
	{
		//errorType = "";
		delayPacket = sendPacket;
		delaySocket = del;
		new Message(time,this).start();
	}
	private void actualDelay() {
		try {
			delaySocket.send(delayPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void duplicate(DatagramPacket send, DatagramSocket sender){
		errorType = "";
		try {
			sender.send(send);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Message.printOutgoing(send,"First Duplicate",verbose);
		try {
			Thread.sleep(time);
		} catch (InterruptedException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	private void readWriteError(){
		boolean specialRequest = false;
		if(packetType.toUpperCase().equals("RRQ")||packetType.toUpperCase().equals("WRQ"))
		{				
			specialRequest = true;
		}

		if(errorType.toUpperCase().contains("DELAY") && specialRequest){
			System.out.println("Delaying last received packet for" + time + "milliseconds.");
			delay(serverSideSocket);
			specialRequest = false;
		}			
		else if(errorType.toUpperCase().contains("DUPLICATE") && specialRequest){
			specialRequest = false;
			errorType = "";
			new Intermediate(receivePacket, verbose, replyPort).start();
		}
		else if(errorType.toUpperCase().contains("CORRUPT") && specialRequest){
			specialRequest = false;
			corruptPacket();				
		}

		if(!errorType.toUpperCase().contains("LOSE")&&!errorType.toUpperCase().contains("DELAY")){
			try {
				serverSideSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Message.printOutgoing(sendPacket, "Intermediate Host", verbose);
		}
		else{
			if(!specialRequest){
				try {
					serverSideSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Lost initial request.");
				errorType = "";
			}
		}
	}

	private void errorCheck(DatagramSocket socket, String host){
		if(host.toUpperCase().equals("SERVER")){
			host = "Server";
		}
		else{
			host = "Client";
		}

		if(packetType.equals(getOpCode(receivePacket.getData())) && Message.parseBlock(receivePacket.getData()) == packetNumber){
			if(errorType.toUpperCase().contains("DELAY")){
				delay(socket);

			}
			else if(errorType.toUpperCase().contains("DUPLICATE")){
				duplicate(sendPacket, socket);
			}
			else if(errorType.toUpperCase().contains("UNKNOWN")){
				try {
					DatagramSocket tempSendSocket = new DatagramSocket(47);   /////random packet
					tempSendSocket.send(sendPacket);
					tempSendSocket.close();
				} catch (SocketException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(errorType.toUpperCase().contains("CORRUPT")){
				corruptPacket();
			}

			if((!errorType.toUpperCase().contains("LOSE"))&&!(errorType.toUpperCase().contains("DELAY"))){				
				try {
					socket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				} 
				Message.printOutgoing(sendPacket, "Intermediate Host - " + host + "Side",verbose);
			}
			else {
				errorType = "";
				System.out.println("Lost packet going to " + host);
			}
		}
		else{
			try {
				socket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Message.printOutgoing(sendPacket, "Intermediate Host - " + host + "Side", verbose);
		}
	}



	private void corruptPacket(){
		byte[] data = new byte[sendPacket.getLength()];
				System.arraycopy(sendPacket.getData(), 0, data, 0, sendPacket.getLength());
		if(packetError.toUpperCase().contains("INVALID OPCODE")){
			data[1] = 7;
			sendPacket.setData(data);
		}
		else if(packetError.toUpperCase().contains("INVALID MODE")){
			data[data.length-2] = 'x';
			sendPacket.setData(data);
		}
		else if(packetError.toUpperCase().contains("BLOCK NUMBER TOO HIGH")){			
			data[2] = Message.toBlock(Message.parseBlock(receivePacket.getData()) + 10)[0];
			data[3] = Message.toBlock(Message.parseBlock(receivePacket.getData()) + 10)[1];
			sendPacket.setData(data);
		}
		else if(packetError.toUpperCase().contains("NO TERMINATOR")){
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength()-1,InetAddress.getLocalHost(),sendPacket.getPort());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		else if(packetError.toUpperCase().contains("UNEXPECTED OPCODE")){			
			if(data[1]==2){
				data[1] = 3;
			}
			else{
				data[1] = 2;
			}
			sendPacket.setData(data);
		}
		else if(packetError.toUpperCase().contains("TOO LONG")){
			try {
				sendPacket = new DatagramPacket(data, receivePacket.getLength()+10,InetAddress.getLocalHost(),sendPacket.getPort());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * forward takes all messages from the client and forwards them on to the server
	 * it also creates a new intermediate host thread with its own serverside port and 
	 * reply port to continue forwarding messages
	 */
	public void forward() {
		while (!shutdown) { //loop forever-ish
			byte data[] = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			Message.printIncoming(receivePacket, "Intermediate Host",verbose);
			replyPort = receivePacket.getPort();	

			new Intermediate(receivePacket, verbose, replyPort).start();

		}
	}

	public static void main (String[] args) {
		Intermediate i = new Intermediate();		
		String x;	
		//make this loop ideally
		while(true) {
			Scanner sc = new Scanner(System.in);
			System.out.println("What type of error would you like to simulate? \n (n)etwork error or (p)acket error or (no)ne?");
			if(sc.hasNext()) {
				x = sc.next();
				if(x.contains("no")||x.contains("NO")||x.contains("No")||x.contains("No")) {
					System.out.println("No errors will be simulated");				
				}
				else if(x.contains("P")||x.contains("p")) {
					System.out.println("What packet error would you like to simulate? \n (c)orrupted packet (Error Code 4) or (u)nknown source (Error Code 5)?");
					x = sc.next();
					if(x.contains("c")||x.contains("C")) {
						errorType = "Corrupted";
						System.out.println("What type of packet would you like to Corrupt? \n (R)RQ, (W)RQ, (D)ATA, (A)CK");
						packetType = sc.next();
						if(packetType.contains("D")||packetType.contains("d")){
							packetType ="DATA";	
							System.out.println("What DATA number would you like to Corrupt? (ex. 1, 2 etc.)");
							packetNumber = sc.nextInt();
							System.out.println("How would you like to Corrupt the Data packet? \n (i)nvalid opcode or (b)lock number too high or (u)nexpected opcode");
							x = sc.next();
							if(x.contains("i")||x.contains("I")){
								packetError = "Invalid Opcode";
							}
							else if(x.contains("b")||x.contains("B")){
								packetError = "Block Number Too High";
							}
							else if(x.contains("u")||x.contains("U")){
								packetError = "Unexpected Opcode";
							}
							else{
								sc.reset(); //clear scanner
							}
							System.out.println("Corrupting " + packetType + " " + packetNumber + ". Error: " + packetError);
						}
						else if(packetType.contains("A")||packetType.contains("a")){
							packetType ="ACK";
							System.out.println("What ACK number would you like to Corrupt? (ex. 1, 2 etc.)");
							packetNumber = sc.nextInt();
							System.out.println("How would you like to Corrupt the ACK packet? \n (i)nvalid opcode or (b)lock number too high or (u)nexpected opcode");
							x = sc.next();
							if(x.contains("i")||x.contains("I")){
								packetError = "Invalid Opcode";
							}
							else if(x.contains("b")||x.contains("B")){
								packetError = "Block Number Too High";
							}
							else if(x.contains("u")||x.contains("U")){
								packetError = "Unexpected Opcode";
							}
							else{
								sc.reset(); //clear scanner
							}
							System.out.println("Corrupting " + packetType + " " + packetNumber + ". Error: " + packetError);
						}
						else if(packetType.contains("W")||packetType.contains("w")){
							packetType = "WRQ";
							System.out.println("How would you like to Corrupt the WRQ? \n (i)nvalid opcode or (in)valid mode or (n)o null terminator or (u)nexpected opcode");
							x = sc.next();
							if(x.contains("i")||x.contains("I")){
								packetError = "Invalid Opcode";
							}
							else if(x.contains("n")||x.contains("N")){
								packetError = "No Terminator";
							}
							else if(x.contains("in")||x.contains("IN")||x.contains("In")||x.contains("iN")){
								packetError = "Invalid Mode";
							}
							else if(x.contains("u")||x.contains("U")){
								packetError = "Unexpected Opcode";
							}
							else{
								sc.reset(); //clear scanner
							}
							System.out.println("Corrupting " + packetType + ". Error: " + packetError);
						}
						else if(packetType.contains("R")||packetType.contains("r")){
							packetType = "RRQ";
							System.out.println("How would you like to Corrupt the RRQ? \n (i)nvalid opcode or (in)valid mode or (n)o null terminator or (u)nexpected opcode");
							x = sc.next();
							if(x.contains("i")||x.contains("I")){
								packetError = "Invalid Opcode";
							}
							else if(x.contains("n")||x.contains("N")){
								packetError = "No Terminator";
							}
							else if(x.contains("in")||x.contains("IN")||x.contains("In")||x.contains("iN")){
								packetError = "Invalid Mode";
							}
							else if(x.contains("u")||x.contains("U")){
								packetError = "Unexpected Opcode";
							}
							else{
								sc.reset(); //clear scanner
							}
							System.out.println("Corrupting " + packetType + ". Error: " + packetError);
						}
						else{
							sc.reset(); //clear scanner
						}						
					}
					else if(x.contains("u")||x.contains("U")) { 
						errorType = "Unknown";
						System.out.println("What type of packet would you like to send from an Unknown Source? \n (D)ATA or (A)CK");  
						packetType = sc.next();
						if(packetType.contains("D")||packetType.contains("d")){
							packetType ="DATA";
							System.out.println("What DATA number would you like to send the Unknown Source packet before? (ex. 1, 2 etc.)");
							packetNumber = sc.nextInt();
							System.out.println("Sending " + packetType + " " + packetNumber + " from an Unknown Source");
						}
						else if(packetType.contains("A")||packetType.contains("a")){
							packetType ="ACK";
							System.out.println("What ACK number would you like to send the Unknown Source packet before? (ex. 1, 2 etc.)");
							packetNumber = sc.nextInt();
							System.out.println("Sending " + packetType + " " + packetNumber + " from an Unknown Source");
						}
						else{
							sc.reset(); //clear scanner
						}
					}
					else {
						sc.reset();
					}
				}
				else if(x.contains("N")||x.contains("n")) {
					System.out.println("What network error would you like to simulate? \n (de)layed packet, (l)ost packet, (du)plicated packet?");
					if(sc.hasNext()) {
						x = sc.next();
						if (x.contains("de")||x.contains("De")||x.contains("DE")||x.contains("dE")) {
							errorType = "Delay";
							System.out.println("How long would you like to delay the packet for? (milliseconds)");
							time = sc.nextInt();	
							System.out.println("What type of packet would you like to delay? \n (R)RQ, (W)RQ, (D)ATA, (A)CK");
							packetType = sc.next();
							if(packetType.contains("D")||packetType.contains("d")){
								packetType ="DATA";
								System.out.println("What DATA number would you like to delay? (ex. 1, 2 etc.)");
								packetNumber = sc.nextInt();
								System.out.println("Delaying " + packetType + " " + packetNumber + " for " + time + " milliseconds");
							}
							else if(packetType.contains("A")||packetType.contains("a")){
								packetType ="ACK";
								System.out.println("What ACK number would you like to delay? (ex. 1, 2 etc.)");
								packetNumber = sc.nextInt();
								System.out.println("Delaying " + packetType + " " + packetNumber + " for " + time + " milliseconds");
							}
							else if(packetType.contains("W")||packetType.contains("w")){
								packetType = "WRQ";
								System.out.println("Delaying " + packetType + " for " + time + " milliseconds");
							}
							else if(packetType.contains("R")||packetType.contains("r")){
								packetType = "RRQ";
								System.out.println("Delaying " + packetType + " for " + time + " milliseconds");
							}
							else{
								sc.reset(); //clear scanner
							}
						}
						else if (x.contains("du")||x.contains("Du")||x.contains("DU")||x.contains("dU")) {
							errorType = "Duplicate";
							System.out.println("How long after the first packet would you like the duplicated packet to send? (milliseconds)");
							time = sc.nextInt();
							System.out.println("What type of packet would you like to duplicate? \n (R)RQ, (W)RQ, (D)ATA, (A)CK");
							packetType = sc.next();
							if(packetType.contains("D")||packetType.contains("d")){
								packetType ="DATA";
								System.out.println("What DATA number would you like to duplicate? (ex. 1, 2 etc.)");
								packetNumber = sc.nextInt();
								System.out.println("Duplicating " + packetType + " " + packetNumber + " after " + time + " milliseconds");
							}
							else if(packetType.contains("A")||packetType.contains("a")){
								packetType ="ACK";
								System.out.println("What ACK number would you like to duplicate? (ex. 1, 2 etc.)");
								packetNumber = sc.nextInt();
								System.out.println("Duplicating " + packetType + " " + packetNumber + " after " + time + " milliseconds");
							}
							else if(packetType.contains("W")||packetType.contains("w")){
								packetType = "WRQ";
								System.out.println("Duplicating " + packetType + " after " + time + " milliseconds");
							}
							else if(packetType.contains("R")||packetType.contains("r")){
								packetType = "RRQ";
								System.out.println("Duplicating " + packetType + " after " + time + " milliseconds");
							}
							else{
								sc.reset(); //clear scanner
							}
						}
						else if (x.contains("l")||x.contains("L")) {
							errorType = "Lose";
							System.out.println("What type of packet would you like to lose? \n (R)RQ, (W)RQ, (D)ATA, (A)CK");
							packetType = sc.next();
							if(packetType.contains("D")||packetType.contains("d")){
								packetType ="DATA";
								System.out.println("What DATA number would you like to lose? (ex. 1, 2 etc.)");
								packetNumber = sc.nextInt();
								System.out.println("Losing " + packetType + " " + packetNumber);
							}
							else if(packetType.contains("A")||packetType.contains("a")){
								packetType ="ACK";
								System.out.println("What ACK number would you like to lose? (ex. 1, 2 etc.)");
								packetNumber = sc.nextInt();
								System.out.println("Losing " + packetType + " " + packetNumber);
							}
							else if(packetType.contains("w")||packetType.contains("W")){
								packetType = "WRQ";
								System.out.println("Losing " + packetType);
							}
							else if(packetType.contains("r")||packetType.contains("R")){
								packetType = "RRQ";
								System.out.println("Losing " + packetType);
							}
							else{
								sc.reset(); //clear scanner
							}
						}						
						else {
							sc.reset(); //clear scanner
						}
					}
				}
				else {
					sc.reset(); //clear scanner
				}
			}

			sc.close();		
			i.forward();
		}
		//i.receiveSocket.close(); //close the sockets, right now this will never happen
		//	i.serverSideSocket.close(); //but in iteration1...when there's a way to exit

	}
}
