package assign1;

import java.io.*;
import java.net.*;

public class Stoppable extends Thread {
	protected boolean shutdown;
	protected boolean timeout;
	DatagramPacket sendPacket,receivePacket;
	int port;
	String filename;

	public void setShutdown() {
		shutdown = true;
	} //does not work for client rn
	
	public void write(BufferedOutputStream out, DatagramSocket sendReceiveSocket) throws IOException {
		System.out.println("inside write");
		byte[] resp = new byte[4];
		resp[0] = 0;
		resp[1] = 4;
		byte block1 = 0;
		byte block2 = 0;
		byte[] data = new byte[516];
		System.out.println(data);
		try {
			do {
				receivePacket = new DatagramPacket(data,516);
				//validate and save after we get it
				sendReceiveSocket.receive(receivePacket);
				port = receivePacket.getPort();
				Message.printIncoming(receivePacket, "Server"); //fix
				System.out.println("got some data?");
				out.write(data,4,receivePacket.getLength()-4);
				//do that better
				System.arraycopy(receivePacket.getData(), 2, resp, 2, 2);
				sendPacket = new DatagramPacket(resp, resp.length,
						receivePacket.getAddress(), receivePacket.getPort());
				System.out.println("Ok.");
				sendReceiveSocket.send(sendPacket);
				Message.printOutgoing(sendPacket, this.toString());
				System.out.println(receivePacket.getLength());
			} while (receivePacket.getLength()==516);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void read(BufferedInputStream in, DatagramSocket sendReceiveSocket, int port) throws IOException {
		int n;
		byte block1 = 0;
		byte block2 = 0;
		byte[] data = new byte[512];
		byte[] resp = new byte[4];
		
		try {
			while (((n = in.read(data)) != -1)) {
				if ((int) block2 ==-1)
					block1++;
				block2++;
				System.out.println("heyyy" + n);
				byte[] message = new byte[n+4];
				message[0] = 0;
				message[1] = 3;
				message[2] = block1;
				message[3] = block2;
				for (int i = 0;i<n;i++) {
					//System.out.println(new Character((char) data[i]));
					message[i+4] = data[i];
				}
				sendPacket = new DatagramPacket(message,n+4,InetAddress.getLocalHost(),port);
				
				Message.printOutgoing(sendPacket, "Client");
				sendReceiveSocket.send(sendPacket);
				System.out.println("And here we are");
				receivePacket = new DatagramPacket(resp,4);
				sendReceiveSocket.receive(receivePacket);
				Message.printIncoming(receivePacket, "Client");
				//clients above should change
				//make the == a test rather than a print
				if (!(Message.parseBlock(sendPacket.getData())==Message.parseBlock(message))) {
					System.out.println("ERROR"+ Message.parseBlock(sendPacket.getData()) + "    "+ Message.parseBlock(message));
					return;
				}
				System.out.println(""+port);
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
