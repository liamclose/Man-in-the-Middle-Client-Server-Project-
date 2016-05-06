package assign1;

//SimpleEchoServer.java
//This class is the server side of a simple echo server based on
//UDP/IP. The server receives from a client a packet containing a character
//string, then echoes the string back to the client.
//Last edited January 9th, 2016

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server extends Thread{

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendSocket, receiveSocket;
	Scanner s;
	boolean shutdown;

	public static final byte[] readAck = {0, 3, 0, 1};
	public static final byte[] writeAck = {0, 4, 0, 0};

	public Server()
	{
		try {
			//make socket to receive requests on port 69
			receiveSocket = new DatagramSocket(6000);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		s = new Scanner(System.in);
		shutdown = false;
		System.out.println("testing");

	}

	public Server(DatagramPacket received) {
		int opcode = received.getData()[1];
		if (opcode==1) {
			sendPacket = new DatagramPacket(readAck, readAck.length,
					received.getAddress(), received.getPort());
		}
		else if (opcode==2) {
			System.out.println("Write request received.");
			sendPacket = new DatagramPacket(writeAck, writeAck.length,
					received.getAddress(), received.getPort());
		}
	}

	public void run() {
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			sendSocket.send(sendPacket);
			Message.printOutgoing(sendPacket, this.toString());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		sendSocket.close();
	}
	
	public void setShutdown() {
		shutdown = true;
		receiveSocket.close();
	}

	public void receiveAndReply()
	{
		while (!shutdown) {
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println(activeCount());
			// Block until a datagram packet is received from receiveSocket.
			try {        
				receiveSocket.receive(receivePacket);
			} catch (SocketException e) {
				
			}
			catch (IOException e) {
				System.out.print("IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			//if it passes the validation the Datagram is correctly formed, otherwise something went wrong
			if (Message.validate(new String(receivePacket.getData(),0,receivePacket.getLength()))) {
				Message.printIncoming(receivePacket, "Server");

				//wait 1 second
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e ) {
					e.printStackTrace();
					System.exit(1);
				}
				new Server(receivePacket).start();

			}
			else if (shutdown) {
				return;
			}
			else {
				System.out.println("Invalid Datagram. Exiting now.");
				throw new IllegalArgumentException();
			}
		}
	}

	public static void main( String args[] )
	{
		System.out.println("Press any character to quit.");
		Server c = new Server();
		new Message(c).start();
		c.receiveAndReply();
		c.receiveSocket.close(); //close the receiving socket
	}
}
