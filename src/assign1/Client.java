package assign1;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Stoppable{

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;

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

	public static String parseFilename(String data) {
		return data;
	}
	
	/*
	 * sendAndReceive takes an opcode as an argument and sends a request of that type
	 * the hardcoded filename is test.txt and the format is specified to be octet.
	 * Once it has sent the request it waits for the server to respond.
	 */
	public void sendAndReceive(int opcode) {
		System.out.println(opcode);
		timeout = true;
		System.out.println(filename);
		String format = "ocTeT";
		byte msg[] = Message.formatRequest(filename, format, opcode);

		try {
			sendPacket = new DatagramPacket(msg, msg.length,
					InetAddress.getLocalHost(), 6000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Message.printOutgoing(sendPacket, "Client");

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
					sendReceiveSocket.receive(receivePacket);
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
				System.out.println("Now we read.");
				try {
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("outc.txt"));
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
		System.out.println("(R)ead, (w)rite, or (q)uit?");
		while(sc.hasNext()) {
			x = sc.next();
			System.out.println(x.contains("r"));
			if (x.contains("R")||x.contains("r")) {
				System.out.println("Please enter a filename.");
				c.filename = sc.next();
				c.sendAndReceive(READ);
			}
			else if (x.contains("w")||x.contains("W")) {
				System.out.println("Please enter a filename.");
				c.filename = sc.next();
				c.sendAndReceive(WRITE);
			}
			else if (x.contains("q")||x.contains("Q")) {
				c.sendReceiveSocket.close();
				System.exit(0);
			}
			else {
				sc.reset();
			}
		}
		
		//new Message(c).start();
		//c.sendAndReceive(WRITE);

	}
}
