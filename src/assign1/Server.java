package assign1;

//SimpleEchoServer.java
//This class is the server side of a simple echo server based on
//UDP/IP. The server receives from a client a packet containing a character
//string, then echoes the string back to the client.
//Last edited January 9th, 2016

import java.io.*;
import java.net.*;

public class Server extends Stoppable{

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendSocket, receiveSocket;

	public static final byte[] dataOne = {0, 3, 0, 1};
	public static final byte[] ackZero = {0, 4, 0, 0};
	boolean readTransfer;

	public Server()
	{
		try {
			//make socket to receive requests on port 69
			receiveSocket = new DatagramSocket(69);  //CHANGED
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		shutdown = false;

	}

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
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}		

	public void read() { //test this!
		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream (filename));
			System.out.println(receivePacket.getPort());
			super.read(in, sendSocket, receivePacket.getPort());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write() {
		filename = "copy".concat(filename);
		BufferedOutputStream out;
		try {
			System.out.println(filename);
			out = new BufferedOutputStream(new FileOutputStream(filename));
			sendSocket.send(sendPacket);
			super.write(out, sendSocket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

	public void receiveAndReply()
	{
		while (!shutdown) {
			timeout = false;
			byte data[] = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.setSoTimeout(300); //timeout for quit purposes
				receiveSocket.receive(receivePacket);
			} catch (SocketTimeoutException e) {
				timeout = true;
			}
			catch (IOException e) {
				System.out.print("IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			//if it passes the validation the Datagram is correctly formed, otherwise something went wrong
			if (Message.validate(receivePacket)) {
				Message.printIncoming(receivePacket, "Server",verbose); //string not working for print
				new Server(receivePacket, verbose).start();

			}
			else if (timeout) {

			}
			else {
				System.out.println("Invalid Datagram. Exiting now.");
				throw new IllegalArgumentException("Invalid Packet");
			}

		}
		receiveSocket.close();
	}

	public static void main( String args[] )
	{
		System.out.println("Press q to quit or v to toggle verbose mode.");
		Server c = new Server();
		new Message(c).start();
		c.receiveAndReply();
		c.receiveSocket.close(); //close the receiving socket
	}
}
