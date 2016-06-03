package tftp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Stoppable{

	DatagramSocket sendReceiveSocket;
	Scanner sc;
	

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
		sc = new Scanner(System.in);
	}

	/*
	 * sendAndReceive takes an opcode as an argument and sends a request of that type
	 * the filename is
	 * Once it has sent the request it waits for the server to respond.
	 */
	//SENDING B4 OPEN
	public void sendAndReceive(int opcode) {
		System.out.println(filename);
		timeout = true;
		String format = "ocTet";
		byte msg[] = Message.formatRequest(filename, format, opcode);
		super.sendPacket = new DatagramPacket(msg, msg.length, ip, serverPort); 

		int timeoutCounter = 0;
		byte data[] = new byte[516];
		super.receivePacket = new DatagramPacket(data, data.length);
		menu = false;
		if (!shutdown) {
			// Process the received datagram.
			if (opcode==WRITE) {
				try {
					byte[] resp = new byte[500];
					super.receivePacket = new DatagramPacket(resp,500);
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
					while (timeout) {
						try {
							sendReceiveSocket.send(super.sendPacket);
							Message.printOutgoing(super.sendPacket, "Initial Request", verbose);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						timeout = false;
						try {
							sendReceiveSocket.setSoTimeout(1500);
							sendReceiveSocket.receive(super.receivePacket);
							if (!Message.validate(super.receivePacket, false)) {
								in.close();
								return;
							}
							Message.printIncoming(super.receivePacket, "Client", verbose);
						} catch (SocketTimeoutException e) {

							timeoutCounter++;
							timeout = true;
							if (shutdown||timeoutCounter==5) {
								System.exit(0);
							}
							System.out.println("Timed out, retransmitting.  ");
							Message.printOutgoing(super.sendPacket,"Retransmit:",verbose);
						} catch (MalformedPacketException e) {
							Message.printIncoming(super.receivePacket, "ERROR", verbose);
							super.sendPacket = createErrorPacket(e.getMessage(),4,super.receivePacket.getPort());
							sendReceiveSocket.send(super.sendPacket);
							Message.printOutgoing(super.sendPacket, "Error", verbose);
							in.close();
							return;
						}
					}
					port = super.receivePacket.getPort();

					if (super.receivePacket.getData()[1]==4&&Message.parseBlock(super.receivePacket.getData())==0) {
						read(in,sendReceiveSocket,port);
					}
					else if (super.receivePacket.getData()[1]==4){
						super.sendPacket = createErrorPacket("Invalid block number.",4,super.receivePacket.getPort());
						sendReceiveSocket.send(super.sendPacket);
						Message.printOutgoing(super.sendPacket, "Error", verbose);
					}
					else if (super.receivePacket.getData()[1]==5) {

					}
					else{
						super.sendPacket = createErrorPacket("Invalid opcode.",4,super.receivePacket.getPort());
						sendReceiveSocket.send(super.sendPacket);
						Message.printOutgoing(super.sendPacket, "Error", verbose);
					}
					in.close();

				} catch (FileNotFoundException e) {
					System.out.println("File: " + filename + " does not exist.\nPlease enter a new file.");
					waiting = true;
					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					waiting = false;
					sendAndReceive(WRITE);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (opcode==READ) {

				try {
					System.out.println("Creating file output.");
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
					try {
						sendReceiveSocket.send(super.sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					write(out,sendReceiveSocket);
					out.close();
				} catch (IOException e) {
					if (e.getMessage().equals("There is not enough space on the disk")||e.getMessage().equals("Stream Closed")) {
					} 
					else if (e.getMessage().equals(filename + " (Access is denied)")) {
						System.out.println("Access denied: " + filename + "\nPlease enter a new file.");
						waiting = true;
						synchronized (this) {
							try {
								wait();
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
						waiting = false;
						sendAndReceive(READ);
					}
					else {
						e.printStackTrace();
						System.out.println(e.getMessage());
						try {
							sendPacket = createErrorPacket(e.getMessage(),0,super.receivePacket.getPort());
						} catch (UnknownHostException e1) {
							e1.printStackTrace();
						}
						try {
							sendReceiveSocket.send(sendPacket);
							Message.printOutgoing(super.sendPacket, "ERROR", verbose);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}

				}
			}
		}
		System.out.println("(R)ead, (w)rite, toggle (v)erbose, toggle (t)est, or (q)uit?");
		System.out.println("Default options are verbose mode on, test mode off.");

	}

	public static byte[] getIP(String s) {
		String[] r = s.split("\\.");
		byte[] b = new byte[r.length];
		for (int i=0;i<r.length;i++) {
			b[i] = (byte) Integer.parseInt(r[i]);
		}
		return b;
	}
	
	public static void main(String args[]) {
		Client c = new Client();
		String x;
		System.out.println("Is the server running on this computer? Y/N");
		if (c.sc.hasNext()) {
			x = c.sc.next();
			if (x.contains("y")||x.contains("Y")) {
				System.out.println("Please enter the server IP address.");
				x = c.sc.next();
				byte[] address = getIP(x);
				try {
					c.ip = InetAddress.getByAddress(address);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				System.out.println(x);
				System.out.println(address[0] + "  " +address[1] + "  " +address[2] + "  " +address[3]);
			}
				
		}
		System.out.println("(R)ead, (w)rite, toggle (v)erbose, toggle (t)est, or (q)uit?");
		System.out.println("Default options are verbose mode on, test mode off.");
		new Message(c,c.sc).start();
		while (!c.shutdown) {
			c.menu = true;
			while(c.sc.hasNext()) {
				x = c.sc.next();
				System.out.println(x);
				if (x.contains("R")||x.contains("r")) {
					System.out.println("Please enter a filename.");
					c.filename = c.sc.next();
					c.sc.reset();
					
					c.sc.reset();
					c.sendAndReceive(READ);
				}
				else if (x.contains("w")||x.contains("W")) {
					System.out.println("Please enter a filename.");
					c.filename = c.sc.next();
					c.sc.reset();
					c.sc.reset();
					c.sendAndReceive(WRITE);
				}
				else if (x.contains("v")||x.contains("V")) {
					c.verbose = !c.verbose;
					System.out.println("Verbose = " + c.verbose);
				}
				else if (x.contains("t")||x.contains("T")) {
					if (c.serverPort==23) {
						c.serverPort = 69;
						System.out.println("Test mode off.");
					}
					else {
						c.serverPort = 23;
						System.out.println("Test mode on.");
					}
				}
				else if (x.contains("q")||x.contains("Q")) {
					c.sendReceiveSocket.close();
					System.out.println("Closing connection.");
					c.sc.close();
					System.exit(0);
				}
				else {
					c.sc.reset(); //clear scanner
				}
			}
		}	
	}
}
