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
	boolean waiting = false;

	public void setShutdown() {
		shutdown = true;
	} 



	public DatagramPacket createErrorPacket(String errorMessage,int errorCode,int port) throws UnknownHostException {
		byte[] errorBytes = new byte[errorMessage.length()+5];
		System.arraycopy(errorMessage.getBytes(), 0, errorBytes, 4, errorMessage.length());
		errorBytes[0] = 0;
		errorBytes[1] = 5;
		errorBytes[2] = 0;
		errorBytes[3] = (byte) errorCode;
		errorBytes[errorBytes.length-1] = 0;
		return new DatagramPacket(errorBytes,errorBytes.length,InetAddress.getLocalHost(),port);
	}
	
	public boolean waiting() {
		return waiting;
	}

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
		int actual= 0;
		byte[] data = new byte[516];
		timeout = true;
		boolean wrapped = false;
		try {
			do {
				receivePacket = new DatagramPacket(data,516);
				while (timeout) {
					try {
						sendReceiveSocket.setSoTimeout(1500);
						sendReceiveSocket.receive(receivePacket); //receive from other
						timeout = false;
						timeoutCounter = 0; //other is still alive
						if (!Message.validate(receivePacket,false)) {
							Message.printIncoming(receivePacket, "ERROR2", verbose);
							sendPacket = createErrorPacket("Malformed Packet.",4,receivePacket.getPort());
							sendReceiveSocket.send(sendPacket);
							Message.printOutgoing(sendPacket, "Error - invalid", verbose);
							return;
						}
					} catch (SocketTimeoutException e) {
						timeout = true;
						timeoutCounter++;
						if (shutdown||timeoutCounter==10) {
							return;
						}
						System.out.println("Timed out. Continuing to wait.");
					} catch (MalformedPacketException e) {
						sendPacket = createErrorPacket(e.getMessage(),4,receivePacket.getPort());
						try {
							sendReceiveSocket.send(sendPacket);
							Message.printOutgoing(sendPacket, "Error - caught", verbose);
							return;
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}				
				if ((port!=0)&&(receivePacket.getPort()!=port)) {
					System.out.println("ERROR, WRONG PORT");
					Message.printIncoming(receivePacket, "ERROR1", verbose);
					sendPacket = createErrorPacket("Unknown TID.",5,receivePacket.getPort());
				} 
				else if (data[1]==4) {
					System.out.println("ERROR, WRONG OPCODE");
					Message.printIncoming(receivePacket, "ERROR3", verbose);
					sendPacket = createErrorPacket("Unexpected opcode received.",4,receivePacket.getPort());
					sendReceiveSocket.send(sendPacket);
					return;
				}
				else {
					if (data[1]==5) {
						Message.printIncoming(receivePacket, "Error4", verbose);
						return;

					}
					actual = Message.parseBlock(data);
					if (expected<actual&&!wrapped) {
						sendPacket = createErrorPacket("Unexpected block received.",4,port);
						sendReceiveSocket.send(sendPacket);
						Message.printOutgoing(sendPacket, "Error", verbose);
						return;
					}
					if (expected==actual) {
						try {
							out.write(data,4,receivePacket.getLength()-4);
						} catch (IOException e) {
							DatagramPacket errorPacket = createErrorPacket("Disk full.",3,receivePacket.getPort());
							sendReceiveSocket.send(errorPacket);
							Message.printOutgoing(errorPacket, "ERROR", verbose);
							return;
						}
						expected++;
						if (expected==65536) {
							expected = 0;
							wrapped = true;
						}
					}
					port = receivePacket.getPort();
					Message.printIncoming(receivePacket, "Write",verbose);
					System.arraycopy(receivePacket.getData(), 2, resp, 2, 2);
					sendPacket = new DatagramPacket(resp, resp.length,
							receivePacket.getAddress(), receivePacket.getPort());
				}
				sendReceiveSocket.send(sendPacket);
				Message.printOutgoing(sendPacket, this.toString(),verbose);
				if (receivePacket.getLength()==516) {
					timeout = true;
				}
			} while (receivePacket.getLength()==516);
			try {
				out.close();
			} catch (IOException e){
				DatagramPacket errorPacket = createErrorPacket("Disk full.",3,receivePacket.getPort());
				sendReceiveSocket.send(errorPacket);
				Message.printOutgoing(errorPacket, "ERROR", verbose);
				return;
				
			}
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
		byte[] data = new byte[512];
		byte[] resp = new byte[400];
		boolean wrongPort = false;
		boolean wrapped = false;
		try {
			boolean empty = true;
			sendPacket = new DatagramPacket(resp,4);
			while (((n = in.read(data)) != -1)) {
				timeout = true;
				if ((int) block2 ==-1) {
					block1++;
					if (block1==0) {
						wrapped = true;
					}
				}
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
				while (timeout||wrongPort) {
					if (!wrongPort) {
						sendReceiveSocket.send(sendPacket);
					}
					else {
						wrongPort = false;
					}
					receivePacket = new DatagramPacket(resp,400);
					try {
						sendReceiveSocket.setSoTimeout(1500);
						sendReceiveSocket.receive(receivePacket);
						timeout = false;
						timeoutCounter = 0;

						if (receivePacket.getPort()!=port) {
							Message.printIncoming(receivePacket, "ERROR5", verbose);
							DatagramPacket errorPacket = createErrorPacket("Unknown TID.",5,receivePacket.getPort());
							sendReceiveSocket.send(errorPacket);
							Message.printOutgoing(errorPacket, "Error - port", verbose);
							wrongPort = true;
						}
						else if (!Message.validate(receivePacket,false)) {
							Message.printIncoming(receivePacket, "ERROR6", verbose);
							sendPacket = createErrorPacket("Malformed Packet.",4,receivePacket.getPort());
							sendReceiveSocket.send(sendPacket);
							Message.printOutgoing(sendPacket, "Error", verbose);
							return;
						}
						else if (receivePacket.getData()[1]==5) {
							Message.printIncoming(receivePacket,"ERROR",verbose);
							return;
						}
						else if (receivePacket.getData()[1]!=4) {
							Message.printIncoming(receivePacket, "ERROR7", verbose);
							sendPacket = createErrorPacket("Unexpected opcode.",4,receivePacket.getPort());
							sendReceiveSocket.send(sendPacket);
							Message.printOutgoing(sendPacket, "Error", verbose);
							return;
						}
						else if (Message.parseBlock(receivePacket.getData())>Message.parseBlock(sendPacket.getData())&&!wrapped) {
							Message.printIncoming(receivePacket, "ERROR8", verbose);
							sendPacket = createErrorPacket("Invalid block number.",4,receivePacket.getPort());
							sendReceiveSocket.send(sendPacket);
							Message.printOutgoing(sendPacket, "Error", verbose);
							return;
						}
						else {
							Message.printIncoming(receivePacket,"Read",verbose);
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
					} catch (MalformedPacketException e) {
						port = receivePacket.getPort();
						sendPacket = createErrorPacket(e.getMessage(),4,receivePacket.getPort());
						sendReceiveSocket.send(sendPacket);
						Message.printOutgoing(sendPacket, "Error", verbose);
						return;
					}
				}
				//look into this...
				while ((Message.parseBlock(sendPacket.getData())<Message.parseBlock(receivePacket.getData()))||timeout) { //fuck? no retransmit here?
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
