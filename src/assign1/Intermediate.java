package assign1;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Intermediate extends Stoppable{
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket serverSideSocket, receiveSocket, replySocket;
	int replyPort, serverPort;
	InetAddress replyAddress;
	
	static String packetType;
	static int packetNumber;
	static int time=0;
	static String errorType = "";

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
		try {
			byte tempData[] = new byte[516];
			tempData = receivePacket.getData();						
			sendPacket = new DatagramPacket(tempData, receivePacket.getLength(),InetAddress.getLocalHost(),69);
			boolean specialRequest = false;
			
			System.out.println("in run");
			
			if(packetType.toUpperCase().equals("RRQ")||packetType.toUpperCase().equals("WRQ"))
			{				
				specialRequest = true;
				System.out.println("setting special");
			}
			
			if(errorType.toUpperCase().contains("DELAY") && specialRequest){
				System.out.println("about to delay");
				delay();
				System.out.println("delayed");
				specialRequest = false;
			}			
			else if(errorType.toUpperCase().contains("DUPLICATE") && specialRequest){
				duplicate(sendPacket);
				specialRequest = false;
			}
			
			if(!errorType.toUpperCase().contains("LOSE")){
				System.out.println("sending");
				serverSideSocket.send(sendPacket);
			}
			else{
				if(!specialRequest){
					serverSideSocket.send(sendPacket);
				}
			}
			
			while(true){				
				byte data[] = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				serverSideSocket.setSoTimeout(60000);//timeout if no data for over a minute
				serverSideSocket.receive(receivePacket);
				serverPort = receivePacket.getPort();
				Message.printIncoming(receivePacket, "Intermediate Host",verbose);								
				sendPacket = new DatagramPacket(receivePacket.getData(),receivePacket.getLength(),InetAddress.getLocalHost(),replyPort); 
				if(packetType.equals(getOpCode(receivePacket.getData())) && Message.parseBlock(receivePacket.getData()) == packetNumber){
					if(errorType.toUpperCase().contains("DELAY")){
						delay();
					}
					else if(errorType.toUpperCase().contains("DUPLICATE")){
						duplicate(sendPacket);
					}					
					if(!errorType.toUpperCase().contains("LOSE")){
						replySocket.send(sendPacket);
						Message.printOutgoing(sendPacket, "Intermediate Host",verbose);
					}					
				}
				else{
					replySocket.send(sendPacket);
					Message.printOutgoing(sendPacket, "Intermediate Host",verbose);
				}
				replySocket.setSoTimeout(60000); 
				replySocket.receive(receivePacket);
				Message.printIncoming(receivePacket, "Intermediate Host",verbose);				
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),InetAddress.getLocalHost(),serverPort);
				if(packetType.equals(getOpCode(receivePacket.getData())) && Message.parseBlock(receivePacket.getData()) == packetNumber){
					if(errorType.toUpperCase().contains("DELAY")){
						delay();
					}
					else if(errorType.toUpperCase().contains("DUPLICATE")){
						duplicate(sendPacket);
					}					
					if(!errorType.toUpperCase().contains("LOSE")){
						serverSideSocket.send(sendPacket);
						Message.printOutgoing(sendPacket, "Intermediate Host",verbose);
					}					
				}
				else{
					serverSideSocket.send(sendPacket);
				}

			}
		} catch (SocketTimeoutException e) {
			return; //this thread is done
		} catch (UnknownHostException e) {
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
	
	private void delay()
	{
		try {
	          Thread.sleep(time);
	      } catch (InterruptedException e ) {
	          e.printStackTrace();
	          System.exit(1);
	      }
	}
	
	private void duplicate(DatagramPacket send) throws IOException{
		serverSideSocket.send(send);
		try {
	          Thread.sleep(time);
	      } catch (InterruptedException e ) {
	          e.printStackTrace();
	          System.exit(1);
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
		Scanner sc = new Scanner(System.in);
		
		System.out.println("What error would you like to simulate? \n (de)layed packet, (l)ost packet, (du)plicated, or (n)one?");
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
			else if (x.contains("n")||x.contains("N")) {
				System.out.println("No errors will be simulated");				
			}
			else {
				sc.reset(); //clear scanner
			}
		}
		sc.close();		
		i.forward();
		i.receiveSocket.close(); //close the sockets, right now this will never happen
		i.serverSideSocket.close(); //but in iteration1...when there's a way to exit
	}
}
