package assign1;

import java.io.*;
import java.net.*;

public class Client extends Stoppable{

	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;

	public static final int read = 1;
	public static final int write = 2;

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
			String s = "test.txt";
			String format = "ocTeT";
			byte msg[] = Message.formatRequest(s, format, opcode);

			try {
				sendPacket = new DatagramPacket(msg, msg.length,
						InetAddress.getLocalHost(), 6001);
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
			// Construct a DatagramPacket for receiving packets up 
			// to 100 bytes long (the length of the byte array).
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			while (timeout&&!shutdown) {
				timeout = false;
				try {
					// Block until a datagram is received via sendReceiveSocket. 
					sendReceiveSocket.setSoTimeout(3000);
					sendReceiveSocket.receive(receivePacket);
				} 
				catch (SocketTimeoutException e) {
					timeout = true;
				}
				catch(IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		if (!shutdown) {
			// Process the received datagram.
			Message.printIncoming(receivePacket, "Client");
		}

	}
	public void read (BufferedInputStream in, int block) throws IOException {
		int n;
		 byte[] data = new byte[512];
	try {
		while (((n = in.read(data)) != -1)) {
		    System.out.println("heyyy" + n);
		    byte[] message = new byte[n+4];
	        message[0] = 0;
	        message[1] = 1;
	        message[2] = 0;
	        message[3] = 1;
	        for (int i = 0;i<n;i++) {
	        	//System.out.println(new Character((char) data[i]));
	        	message[i+4] = data[i];
	        }
	        sendPacket = new DatagramPacket(message,n+4,InetAddress.getLocalHost(),6000);
	        sendReceiveSocket.send(sendPacket);
	        
		}
		System.out.println(n);
	
    System.out.println("Hello world");
    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}

	public static void main(String args[])
	{
		Client c = new Client();
		
		//new Message(c).start();
		//c.sendAndReceive(read); 
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream("C:/Users/Megan/workspace/test.txt"));
			c.read(in, 0);
			c.read(in, 1);
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}
}
