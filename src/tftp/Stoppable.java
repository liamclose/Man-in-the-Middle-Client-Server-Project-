package tftp;

import java.io.*;
import java.net.*;

public class Stoppable extends Thread {
	protected boolean shutdown;
	protected boolean timeout;
	DatagramPacket sendPacket,receivePacket;
	int port;
	String filename;
	boolean verbose = true;

	public void setShutdown() {
		shutdown = true;
	} 

	/* TODO
	 * 	-remove extra print statements
	 * 	-fix the client menu?
	 * 	-class descriptions in readme??
	 * 	-locations of files (specify vs default) etc et c e t   c 
	 * 		-i mean it kind of works, but.....
	 * 	-set name for printing in constructors
	 */


	/*
	 *	Situations
	 *		RRQ
	 *		Lose!, Delay! (probs also bad on timeout), Dup!
	 *		WRQ
	 *		Lose!, Delay! (bad on timeout), Dup! (file size 0 has probs)
	 * 		Data 1->n-1
	 * 		Lose!, Delay, Dup!
	 *		Data n
	 *		Lose!, Delay, Dup!
	 *		Ack0
	 *		Lose! (except for quit timeout), Delay!, Dup!
	 *		Ack 1->n-1
	 *		Lose!, Delay, Dup!
	 *		Ack n
	 *		Lose!, Delay, Dup!
	 */

	/*
	 * write takes a file outputstream and a communication socket as arguments
	 * it waits for data on the socket and writes it to the file
	 */
	public void write(BufferedOutputStream out, DatagramSocket sendReceiveSocket) throws IOException {
		byte[] resp = new byte[4];
		resp[0] = 0;
		resp[1] = 4;
		int timeoutCounter = 0;
		port = 0;
		int expected = 1;
		int actual;
		byte[] data = new byte[516];
		timeout = true;
		try {
			do {
				receivePacket = new DatagramPacket(data,516);
				while (timeout) {
					try {
						sendReceiveSocket.setSoTimeout(1500);
						sendReceiveSocket.receive(receivePacket); //receive from client
						timeout = false;
						timeoutCounter = 0; //client is still alive
						if (!Message.validate(receivePacket)) {
							System.out.print("Invalid packet.");
							Message.printIncoming(receivePacket, "ERROR", true);
							System.exit(0);
						}
					} catch (SocketTimeoutException e) {
						timeout = true;
						timeoutCounter++;
						if (shutdown||timeoutCounter==10) { //interrupt?
							return;
						}
						System.out.println("Timed out. Retransmitting.");
					}
				}				
				if ((port!=0)&&(receivePacket.getPort()!=port)) {
					System.out.println("ERROR, WRONG PORT");
					Message.printIncoming(receivePacket, "ERROR", verbose);
					System.exit(2);
				}
				port = receivePacket.getPort();
				Message.printIncoming(receivePacket, "Write",verbose);
				actual = Message.parseBlock(data);
				if (expected==actual) {
					System.out.println("Writing to file.");
					out.write(data,4,receivePacket.getLength()-4);
					expected++;
				}
				System.arraycopy(receivePacket.getData(), 2, resp, 2, 2);
				sendPacket = new DatagramPacket(resp, resp.length,
						receivePacket.getAddress(), receivePacket.getPort());
				sendReceiveSocket.send(sendPacket);
				Message.printOutgoing(sendPacket, this.toString(),verbose);
				if (receivePacket.getLength()==516) {
					timeout = true;
				}
			} while (receivePacket.getLength()==516);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Transfer Complete");
	}

	/*
	 * read takes an input stream, a socket and a port as arguments
	 * reads data from the file in 512 byte chunks and sends them over the socket to the port
	 * on localhost
	 */
	public void read(BufferedInputStream in, DatagramSocket sendReceiveSocket, int port) throws IOException {
		int n;
		int timeoutCounter = 0;
		byte block1 = 0;
		byte block2 = 0;
		int newPort = port;
		byte[] data = new byte[512];
		byte[] resp = new byte[4];
		try {
			boolean empty = true;
			sendPacket = new DatagramPacket(resp,4);
			while (((n = in.read(data)) != -1)) {
				timeout = true;
				if ((int) block2 ==-1)
					block1++;
				block2++;
				empty = false;
				byte[] message = new byte[n+4];
				message[0] = 0;
				message[1] = 3;
				message[2] = block1;
				message[3] = block2;
				for (int i = 0;i<n;i++) {
					message[i+4] = data[i];
				}
				sendPacket = new DatagramPacket(message,n+4,InetAddress.getLocalHost(),port);
				Message.printOutgoing(sendPacket, "Read", verbose);
				while (timeout) {
					sendReceiveSocket.send(sendPacket);
					receivePacket = new DatagramPacket(resp,4);
					try {
						sendReceiveSocket.setSoTimeout(1500);
						
						sendReceiveSocket.receive(receivePacket);
						timeout = false;
						timeoutCounter = 0;
						Message.printIncoming(receivePacket,"Read",verbose);

						if (receivePacket.getPort()!=port) {
							System.out.println("ERROR, WRONG PORT");
							System.exit(2);
						}
						if (!Message.validate(receivePacket)) {
							System.out.print("Invalid packet.");
							Message.printIncoming(receivePacket, "ERROR", true);
							System.exit(0);
						}
					} catch (SocketTimeoutException e) {
						timeout = true;
						timeoutCounter++;
						if (shutdown||timeoutCounter==10) {
							return;
						}
						else {
							System.out.println("Timed out. Retransmitting block.");
							sendReceiveSocket.send(sendPacket);
						} 
					}
				}
				while ((Message.parseBlock(sendPacket.getData())!=Message.parseBlock(receivePacket.getData()))||timeout) { //fuck? no retransmit here?
					try {
						sendReceiveSocket.receive(receivePacket);
						Message.printIncoming(receivePacket,"Final packet",verbose);
						timeout = false;
					}
					catch (SocketTimeoutException e) {
						timeout = true;
					}
				}

			}
			if ((n==-1&&sendPacket.getLength()==516)||empty) {
				if ((int) block2 ==-1)
					block1++;
				block2++;
				resp[0] = 0;
				resp[1] = 3;
				resp[2] = block1;
				resp[3] = block2;
				sendPacket = new DatagramPacket(resp,4,InetAddress.getLocalHost(),port);
				sendReceiveSocket.send(sendPacket);
				Message.printOutgoing(sendPacket, "Read", verbose);
				while ((Message.parseBlock(sendPacket.getData())!=Message.parseBlock(receivePacket.getData()))||timeout) {
					try {
						sendReceiveSocket.receive(receivePacket);
						Message.printIncoming(receivePacket, "Read", verbose);
						timeout = false;
					} catch (SocketTimeoutException e) {
						timeout = true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Transfer Complete");
	}
}
