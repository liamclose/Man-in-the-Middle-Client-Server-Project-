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
	} //does not work for client rn

	public void write(BufferedOutputStream out, DatagramSocket sendReceiveSocket) throws IOException {
		byte[] resp = new byte[4];
		resp[0] = 0;
		resp[1] = 4;
		byte block1 = 0;
		byte block2 = 0;
		byte[] data = new byte[516];
		try {
			do {
				timeout = true;
				receivePacket = new DatagramPacket(data,516);
				//validate and save after we get it
				while (timeout) {
					timeout = false;
					try {
						sendReceiveSocket.setSoTimeout(300);
						sendReceiveSocket.receive(receivePacket);
						if (!Message.validate(receivePacket)) {
							System.out.print("Invalid packet.");
							Message.printIncoming(receivePacket, "ERROR", true);
							System.exit(0);
						}
					} catch (SocketTimeoutException e) {
						timeout = true;
						if (shutdown) {
							System.exit(0);
						}
					}
				}				
				port = receivePacket.getPort();
				Message.printIncoming(receivePacket, "Write",verbose);
				out.write(data,4,receivePacket.getLength()-4);
				System.arraycopy(receivePacket.getData(), 2, resp, 2, 2);
				sendPacket = new DatagramPacket(resp, resp.length,
						receivePacket.getAddress(), receivePacket.getPort());
				sendReceiveSocket.send(sendPacket);
				Message.printOutgoing(sendPacket, this.toString(),verbose);
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
			boolean empty = true;
			sendPacket = new DatagramPacket(resp,4);
			while (((n = in.read(data)) != -1)) {
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
				sendReceiveSocket.send(sendPacket);
				receivePacket = new DatagramPacket(resp,4);
				timeout = true;
				while (timeout) {
					timeout = false;
					try {
						sendReceiveSocket.setSoTimeout(300);
						sendReceiveSocket.receive(receivePacket);
						if (!Message.validate(receivePacket)) {
							System.out.print("Invalid packet.");
							Message.printIncoming(receivePacket, "ERROR", true);
							System.exit(0);
						}
					} catch (SocketTimeoutException e) {
						timeout = true;
						if (shutdown) {
							System.exit(0);
						}
					}
				}
				Message.printIncoming(receivePacket, "Read", verbose);
				if (!(Message.parseBlock(sendPacket.getData())==Message.parseBlock(message))) {
					System.out.println("ERROR: Acknowledge does not match block sent "+ Message.parseBlock(sendPacket.getData()) + "    "+ Message.parseBlock(message));
					return;
				}

			}
			System.out.println("" + n + sendPacket.getLength());
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
				sendReceiveSocket.receive(sendPacket);
				Message.printIncoming(sendPacket, "Read", verbose);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
