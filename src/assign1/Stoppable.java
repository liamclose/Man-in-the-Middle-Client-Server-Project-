package assign1;

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
	 * 	-print ack0
	 * 	-timeout/retransmit
	 * 	-timing diagram
	 * 	-update class diagram
	 * 	-fix ucm
	 * 	-cleanup intermediate
	 * 	-remove extra print statements
	 * 	-package name
	 * 	-fix the client menu?
	 * 	-indicate when verbosity changes
	 * 	-class descriptions in readme
	 * 	-locations of files (specify vs default) etc et c e t   c 
	 * 		-i mean it kind of works, but.....
	 * 	-ALSO, i forget
	 * 	-set name for printing in constructors
	 * 	-quit on too many timeouts
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
	//Working:
	//	-Lose WRQ
	//	-Lose RRQ
	//	-Duplicate D/A
	//	-Delay D on R
	//	-Normal Read
	//	-Normal Write (maybe duplicate ack?)

	//Not working:
	//	-Lose D1 acts like duplicate RRQ/WRQ
	//	-Losing last A
	//	-Losing A on RRQ may work?
	//	-On write request, losing data
	//	-
	//WRQ



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
						sendReceiveSocket.receive(receivePacket);
						timeout = false;
						timeoutCounter = 0;
						if (!Message.validate(receivePacket)) {
							System.out.print("Invalid packet.");
							Message.printIncoming(receivePacket, "ERROR", true);
							System.exit(0);
						}
					} catch (SocketTimeoutException e) {
						timeout = true;
						timeoutCounter++;
						System.out.println("Timed out.");

						if (shutdown||timeoutCounter==10) { //interrupt?
							return;
						}
						sendReceiveSocket.send(sendPacket);
					}
				}				
				System.out.println(timeoutCounter);

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
				System.out.println("write:  " + (expected-1) +"      " + actual + "           " + receivePacket.getLength());

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
		System.out.println("Done.");
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
				System.out.println(timeoutCounter);
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
					//retransmit here???
					timeout = false;
					timeoutCounter = 0;
					try {
						sendReceiveSocket.setSoTimeout(1500);
						sendReceiveSocket.receive(receivePacket);
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
							System.exit(0);
						}
						else {
							System.out.println("Timed out. Retransmitting block: " + Message.parseBlock(sendPacket.getData()));
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
						System.out.println("Timing out, no retransmit??????");
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
				System.out.println("" + Message.parseBlock(sendPacket.getData()));
				while ((Message.parseBlock(sendPacket.getData())!=Message.parseBlock(receivePacket.getData()))||timeout) {
					try {
						sendReceiveSocket.receive(receivePacket);
						Message.printIncoming(receivePacket, "Read123", verbose);
						timeout = false;
					} catch (SocketTimeoutException e) {
						System.out.println(":/");
						timeout = true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
