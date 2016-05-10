package assign1;

import java.io.*;
import java.io.IOException;
import java.net.*;
import java.net.InetAddress;

public class Stoppable extends Thread {
	protected boolean shutdown;
	protected boolean timeout;
	DatagramPacket sendPacket,receivePacket;


	public void setShutdown() {
		shutdown = true;
	}
	
	public void write(BufferedOutputStream out, byte[] data) throws IOException {
				out.write(data,4,data.length-4);
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
				System.out.println(""+port+(Message.parseBlock(sendPacket.getData())==Message.parseBlock(message)));
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
