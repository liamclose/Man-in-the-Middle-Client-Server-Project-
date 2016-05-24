package tftp;

import java.io.*;
import java.net.*;
//ack 0 not printed
public class Server extends Stoppable{

	DatagramSocket sendSocket, receiveSocket;

	public static final byte[] dataOne = {0, 3, 0, 1};
	public static final byte[] ackZero = {0, 4, 0, 0};
	boolean readTransfer;

	public Server() {
		try {
			//make socket to receive requests on port 69
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		shutdown = false;

	}
	/*
	 * This constructer takes a datagrampacket as an argument, in order to save the socket of the client it is
	 * communicating with. Verbose is used to preserve the verbosity state of the parent.
	 */
	public Server(DatagramPacket received,boolean verbose) {
		this.verbose = verbose;
		filename = Message.parseFilename(new String(received.getData(),0,received.getLength()));
		receivePacket = received;
		int opcode = received.getData()[1];
		if (opcode==1) {
			readTransfer = true;
			sendPacket = new DatagramPacket(dataOne, dataOne.length,
					received.getAddress(), received.getPort());
		}
		else if (opcode==2) {
			readTransfer = false;
			sendPacket = new DatagramPacket(ackZero, ackZero.length,
					received.getAddress(), received.getPort());
		}
		try {
			sendSocket = new DatagramSocket();
			Message.printOutgoing(sendPacket, "Server", verbose);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}		

	public void read() { 
		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream ("server/".concat(filename)));
			super.read(in, sendSocket, receivePacket.getPort());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write() {
		filename = "server/".concat(filename);
		BufferedOutputStream out;
		try {
			out = new BufferedOutputStream(new FileOutputStream(filename));
			sendSocket.send(sendPacket);
			super.write(out, sendSocket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		if (!readTransfer) {
			write();
		}
		else {
			read();
		}
		sendSocket.close();
	}
	/*
	 * waits for a new client connection and creates a new thread to deal with it
	 */
	public void receiveAndReply()
	{
		while (!shutdown) {
			timeout = false;
			byte data[] = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.setSoTimeout(600); //timeout for quit purposes
				receiveSocket.receive(receivePacket);
			} catch (SocketTimeoutException e) {
				timeout = true;
			}
			catch (IOException e) {
				System.out.print("IO Exception: likely:");
				e.printStackTrace();
				System.exit(1);
			}
			//if it passes the validation the Datagram is correctly formed, otherwise something went wrong
			try {
				if (timeout) {

				}
				else if (Message.validate(receivePacket,true)) {
					Message.printIncoming(receivePacket, "Server",verbose); 
					new Server(receivePacket, verbose).start();

				}
				else {
					Message.printIncoming(receivePacket, "Why", verbose);
				}
			} catch (MalformedPacketException e){
				
				byte[] errorMessage = new byte[e.getMessage().length()+5];
				errorMessage[0] = 0;
				errorMessage[1] = 5;
				errorMessage[2] = 0;
				errorMessage[3] = 4;
				System.arraycopy(e.getMessage().getBytes(), 0, errorMessage, 4, e.getMessage().length());
				DatagramPacket errorPacket = new DatagramPacket(errorMessage,errorMessage.length,receivePacket.getAddress(),receivePacket.getPort());
				try {
					Message.printIncoming(receivePacket, "Why", verbose);
					sendSocket = new DatagramSocket();
					sendSocket.send(errorPacket);
					Message.printOutgoing(errorPacket, "Server - Error", verbose);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

		}
		receiveSocket.close();
	}

	public static void main( String args[] )
	{
		System.out.println("Press q to quit or v to toggle verbose mode.\nVerbose mode is on by default.");
		Server c = new Server();
		new Message(c).start();
		c.receiveAndReply();
		c.receiveSocket.close(); //close the receiving socket
	}
}
