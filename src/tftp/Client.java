package tftp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Stoppable{

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;

	int serverPort = 69;

	public static final int READ = 1; 
	public static final int WRITE = 2;

	// validation client side
	public Client()
	{
		try {
			sendReceiveSocket = new DatagramSocket();
		} 
		catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * sendAndReceive takes an opcode as an argument and sends a request of that type
	 * the hardcoded filename is test.txt and the format is specified to be octet.
	 * Once it has sent the request it waits for the server to respond.
	 */
	public void sendAndReceive(int opcode) {
		timeout = true;
		String format = "ocTeR";
		byte msg[] = Message.formatRequest(filename, format, opcode);
		try {
			super.sendPacket = new DatagramPacket(msg, msg.length,
					InetAddress.getLocalHost(), serverPort); //SERVERPORT TO SUBMIT  CHANGED
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Message.printOutgoing(super.sendPacket, "Client",verbose);

		// Send the datagram packet to the server via the send/receive socket. 
		
		int timeoutCounter = 0;
		byte data[] = new byte[516];
		super.receivePacket = new DatagramPacket(data, data.length);
		if (!shutdown) {
			// Process the received datagram.
			if (opcode==WRITE) {
				try {
					byte[] resp = new byte[4];
					super.receivePacket = new DatagramPacket(resp,4);
					while (timeout) {
						try {
							sendReceiveSocket.send(super.sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						timeout = false;
						try {
							sendReceiveSocket.setSoTimeout(1500);
							sendReceiveSocket.receive(super.receivePacket);
							Message.printIncoming(super.receivePacket, "Client", verbose);
						} catch (SocketTimeoutException e) {
							
							timeoutCounter++;
							timeout = true;
							if (shutdown||timeoutCounter==5) {
								System.exit(0);
							}
							System.out.println("Timed out, retransmitting.  ");
							Message.printOutgoing(super.sendPacket,"Retransmit:",verbose);
						}
					}
					port = super.receivePacket.getPort();
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
					read(in,sendReceiveSocket,port);
					in.close();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (opcode==READ) {
				try {
					sendReceiveSocket.send(super.sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				try {
					System.out.println("Creating file output.");
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
					write(out,sendReceiveSocket);
					out.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static void main(String args[])
	{
		
		Client c = new Client();
		String x;
		Scanner sc = new Scanner(System.in);
		System.out.println("(R)ead, (w)rite, (o)ptions, or (q)uit?");
		while(sc.hasNext()) {
			x = sc.next();
			if (x.contains("R")||x.contains("r")) {
				System.out.println("Please enter a filename.");
				c.filename = sc.next();
				sc.reset();
				new Message(c).start();
				c.sendAndReceive(READ);
				System.exit(0);
			}
			else if (x.contains("w")||x.contains("W")) {
				System.out.println("Please enter a filename.");
				c.filename = sc.next();
				sc.reset();
				new Message(c).start();
				c.sendAndReceive(WRITE);
				System.exit(0);
			}
			else if (x.contains("q")||x.contains("Q")) {
				c.sendReceiveSocket.close();
				System.exit(0);
			}
			else if (x.contains("o")||x.contains("O")) {
				System.out.println("Would you like to turn off verbose mode? Y/N");
				x = sc.next();
				sc.reset();
				if (x.contains("y")||x.contains("Y")) {
					c.verbose = false;
				}
				System.out.println("Would you like to turn on test mode?");
				x = sc.next();
				sc.reset();
				if (x.contains("y")||x.contains("Y")) {
					c.serverPort = 23;
				}
				System.out.println("(R)ead or (w)rite?");

			}
			else {
				sc.reset(); //clear scanner
			}
		}
		sc.close();
		
	}
}
