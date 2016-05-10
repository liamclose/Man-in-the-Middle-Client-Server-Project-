package assign1;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Stoppable{

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;
	
	int serverPort = 69;

	public static final int READ= 1; //caps
	public static final int WRITE = 2;

	// validation client side?
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
		String format = "ocTeT";
		byte msg[] = Message.formatRequest(filename, format, opcode);
		try {
			sendPacket = new DatagramPacket(msg, msg.length,
					InetAddress.getLocalHost(), 6000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Message.printOutgoing(sendPacket, "Client",verbose);

		// Send the datagram packet to the server via the send/receive socket. 
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		byte data[] = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		if (!shutdown) {
			// Process the received datagram.
			if (opcode==WRITE) {
				try {
					byte[] resp = new byte[4];
					receivePacket = new DatagramPacket(resp,4);
					while (timeout) {
						timeout = false;
						try {
							sendReceiveSocket.setSoTimeout(300);
							sendReceiveSocket.receive(receivePacket);
						} catch (SocketTimeoutException e) {
							timeout = true;
							if (shutdown) {
								System.exit(0);
							}
						}
					}
					port = receivePacket.getPort();
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
					read(in,sendReceiveSocket,port);
					in.close();

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (opcode==READ) {
				filename = "copy".concat(filename);
				try {
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
				
			}
			else {
				sc.reset();
			}
		}
		
		
		//c.sendAndReceive(WRITE);

	}
}
