package tftp;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
//ack 0 not printed
public class Server extends Stoppable{

	DatagramSocket sendSocket, receiveSocket;

	public static final byte[] dataOne = {0, 3, 0, 1};
	public static final byte[] ackZero = {0, 4, 0, 0};
	boolean readTransfer;
	

	public Server() {
		try {
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		shutdown = false;
		filesInProgress = new ArrayList<String>();

	}
	/*
	 * This constructer takes a datagrampacket as an argument, in order to save the socket of the client it is
	 * communicating with. Verbose is used to preserve the verbosity state of the parent.
	 */
	public Server(DatagramPacket received,boolean verbose) {
		this.verbose = verbose;
		filename = Message.parseFilename(new String(received.getData(),0,received.getLength()));
		receivePacket = received;
		ip = received.getAddress();
		int opcode = received.getData()[1];
		if (opcode==1) {
			readTransfer = true;
			sendPacket = new DatagramPacket(dataOne, dataOne.length,
					received.getAddress(), received.getPort());
		}
		else if (opcode==2) {
			readTransfer = false;
			sendPacket = new DatagramPacket(ackZero, ackZero.length,
					received.getAddress(), received.getPort());
		}
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}		

	public void read() { 
		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream ("server/".concat(filename)));
			Message.printOutgoing(sendPacket, "Server", verbose);
			super.read(in, sendSocket, receivePacket.getPort());
		} catch (FileNotFoundException e) {
			
			filename = "File " + filename + " does not exist.";
			byte[] errorBytes = new byte[filename.length()+4];
			errorBytes[0] = 0;
			errorBytes[1] = 5;
			errorBytes[2] = 0;
			errorBytes[3] = 1;
			System.arraycopy(filename.getBytes(), 0, errorBytes, 4, filename.length());
			try {
				sendPacket = new DatagramPacket(errorBytes,errorBytes.length,ip,receivePacket.getPort());
				sendSocket.send(sendPacket);
				Message.printOutgoing(sendPacket, "Error", verbose);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write() {
		BufferedOutputStream out;
		try {
			out = new BufferedOutputStream(new FileOutputStream("server/".concat(filename)));
			sendSocket.send(sendPacket);
			super.write(out, sendSocket);
		} catch (FileNotFoundException e) {
			try {
				DatagramPacket errorPacket = super.createErrorPacket("Access denied on: " + filename, 2, receivePacket.getPort());
				sendSocket.send(errorPacket);
				Message.printOutgoing(errorPacket, "Error", verbose);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		if (filesInProgress.contains(filename)) {
			try {
				sendSocket.send(super.createErrorPacket("Concurrent access on: " + filename, 2, receivePacket.getPort()));
				return;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		filesInProgress.add(filename);
		if (!readTransfer) {
			write();
		}
		else {
			read();
		}
		sendSocket.close();		
		filesInProgress.remove(filename);
	}
	/*
	 * waits for a new client connection and creates a new thread to deal with it
	 */
	public void receiveAndReply()
	{
		while (!shutdown) {
			timeout = false;
			byte data[] = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.setSoTimeout(600); //timeout for quit purposes
				receiveSocket.receive(receivePacket);
			} catch (SocketTimeoutException e) {
				timeout = true;
			}
			catch (IOException e) {
				System.out.print("IO Exception: likely:");
				e.printStackTrace();
				System.exit(1);
			}
			//if it passes the validation the Datagram is correctly formed, otherwise something went wrong
			try {
				if (timeout) {
				}
				else if (Message.validate(receivePacket,true)) {
					Message.printIncoming(receivePacket, "Server",verbose); 
					new Server(receivePacket, verbose).start();
				}
				else {
					try {
						DatagramPacket errorPacket = super.createErrorPacket("Invalid packet.", 4, receivePacket.getPort());
						sendSocket = new DatagramSocket();
						sendSocket.send(errorPacket);
						Message.printOutgoing(errorPacket, "Server - Error", verbose);
					} catch (IOException e) {
						e.printStackTrace();
					}
					sendSocket.close();
				}
			} catch (MalformedPacketException e){
				
				byte[] errorMessage = new byte[e.getMessage().length()+5];
				errorMessage[0] = 0;
				errorMessage[1] = 5;
				errorMessage[2] = 0;
				errorMessage[3] = 4;
				System.arraycopy(e.getMessage().getBytes(), 0, errorMessage, 4, e.getMessage().length());
				DatagramPacket errorPacket = new DatagramPacket(errorMessage,errorMessage.length,receivePacket.getAddress(),receivePacket.getPort());
				try {
					sendSocket = new DatagramSocket();
					sendSocket.send(errorPacket);
					Message.printOutgoing(errorPacket, "Server - Error", verbose);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		System.out.println("Shutting down. " + (Thread.activeCount()-1) + " transfers still in progress.");
		receiveSocket.close();
	}

	public static void main( String args[] )
	{
		System.out.println("Press q to quit or v to toggle verbose mode.\nVerbose mode is on by default.");
			try {
				System.out.println("Server IP address: " + InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		Server c = new Server();
		new Message(c,new Scanner(System.in)).start();
		c.receiveAndReply();
		c.receiveSocket.close(); //close the receiving socket
	}
}
