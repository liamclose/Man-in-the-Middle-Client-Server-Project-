package assign1;

//SimpleEchoServer.java
//This class is the server side of a simple echo server based on
//UDP/IP. The server receives from a client a packet containing a character
//string, then echoes the string back to the client.
//Last edited January 9th, 2016

import java.io.*;
import java.net.*;

public class Server {

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendSocket, receiveSocket;

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
	}

	

	public void receiveAndReply()
	{
		while (true) {
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);

			// Block until a datagram packet is received from receiveSocket.
			try {        
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
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
				try {
					sendSocket = new DatagramSocket(); //create new socket to send the response
				} catch (SocketException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
				if (data[1]==1) {
					System.out.println("Read request received.");
					sendPacket = new DatagramPacket(readAck, readAck.length,
							receivePacket.getAddress(), receivePacket.getPort());
				}
				else if (data[1]==2) {
					System.out.println("Write request received.");
					sendPacket = new DatagramPacket(writeAck, readAck.length,
							receivePacket.getAddress(), receivePacket.getPort());
				}
				else {
					System.out.println("Invalid opcode.");
					System.exit(1);
				}

				Message.printOutgoing(sendPacket, "Server");
				// Send the datagram packet to the client via the send socket. 
				try {
					sendSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				sendSocket.close();

			}
			else {
				System.out.println("Invalid Datagram. Exiting now.");
				throw new IllegalArgumentException();
			}
		}
	}

	public static void main( String args[] )
	{
		Server c = new Server();
		c.receiveAndReply();
		c.receiveSocket.close(); //close the receiving socket
	}
}
