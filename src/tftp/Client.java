package tftp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Stoppable{

	DatagramSocket sendReceiveSocket;
	Scanner sc;


	int serverPort = 69;
	InetAddress serverIP;

	public static final int READ = 1; 
	public static final int WRITE = 2;
	
	public int opcode = 0;

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
	public void sendAndReceive() {
		System.out.println(filename);
		timeout = true;
		String format = "ocTet";
		byte msg[] = Message.formatRequest(filename, format, opcode);
		super.sendPacket = new DatagramPacket(msg, msg.length, ip, serverPort); 

		int timeoutCounter = 0;
		byte data[] = new byte[516];
		super.receivePacket = new DatagramPacket(data, data.length);
		synchronized(this) {
			menu = false;
			this.notify();
		}
		if (!shutdown) {
			// Process the received datagram.
			//recursive print
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
					sendAndReceive();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (opcode==READ) {
				//resend on read?
				try {
					System.out.println("Creating file output.");
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
					try {
						sendReceiveSocket.send(super.sendPacket);
						Message.printOutgoing(sendPacket, "Client", verbose);
						first = true;
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
						sendAndReceive();
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
		if (!shutdown) {
		System.out.println("(R)ead, (w)rite, toggle (v)erbose, toggle (t)est, or (q)uit?");
		System.out.println("Default options are verbose mode on, test mode off.");
		}

	}

	//invalid ip as an error

	public static InetAddress getIP(Client c, String s) {
		String[] r;
		boolean error = true;
		InetAddress ret = null;
		while (error) {
			try {
				r = s.split("\\.");

				byte[] b = new byte[r.length];
				for (int i=0;i<r.length;i++) {
					b[i] = (byte) Integer.parseInt(r[i]);
				}

				ret = InetAddress.getByAddress(b);
				error = false;
			} catch (UnknownHostException | NumberFormatException e) {
				System.out.println("Invalid IP, please check the value: " + s);
				s = c.sc.next();
			}
		}
		return ret;
	}

	public void menu(String x) {
		System.out.println(x + menu);
		while(menu || sc.hasNext()) {
			if (menu) {
				menu = false;
			}
			else {
				x = sc.next();
				System.out.println("nope");
			}
			if (x.contains("R")||x.contains("r")) {
				opcode = READ;
				System.out.println("Please enter a filename.");
				filename = sc.next();
				sc.reset();
				System.out.println("Read");
				menu = false;
				return;
			}
			else if (x.contains("w")||x.contains("W")) {
				opcode = WRITE;
				System.out.println("Please enter a filename.");
				filename = sc.next();
				sc.reset();
				menu = false;
				return;
			}
			else if (x.contains("v")||x.contains("V")) {
				verbose = !verbose;
				System.out.println("Verbose = " + verbose);
			}
			else if (x.contains("t")||x.contains("T")) {
				if (serverPort==23) {
					serverPort = 69;
					ip = serverIP;
					System.out.println("Test mode off.");
				}
				else {
					serverPort = 23;
					try {
						DatagramPacket d = new DatagramPacket(ip.getAddress(),4,InetAddress.getLocalHost(),23);
						sendReceiveSocket.send(d);
						ip = InetAddress.getLocalHost();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("Test mode on.");
				}
			}
			else if (x.contains("q")||x.contains("Q")) {
				sendReceiveSocket.close();
				System.out.println("Closing connection.");
				sc.close();
				System.exit(0);
			}
			else {
				sc.reset(); //clear scanner
			}

		}
		
	}
	//client broken for quitting mid transfer
	public static void main(String args[]) {
		Client c = new Client();
		String x;
		System.out.println("Is the server running on this computer? Y/N");
		if (c.sc.hasNext()) {
			x = c.sc.next();
			if (x.contains("N")||x.contains("n")) {
				System.out.println("Please enter the server IP address.");
				x = c.sc.next();
				c.ip = getIP(c,x);
				c.serverIP = c.ip;
				System.out.println("ugh");
			}
			else {
				try {
					c.ip = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				c.serverIP = c.ip;
			}

		}
		System.out.println("(R)ead, (w)rite, toggle (v)erbose, toggle (t)est, or (q)uit?");
		System.out.println("Default options are verbose mode on, test mode off.");
		c.menu = true;
		new Message(c,c.sc).start();
		while (!c.shutdown) {
			c.menu = true;
			synchronized(c) {
				
				try {
					c.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("awoke! ! ! " + c.filename + c.opcode);
				c.sendAndReceive();
			}
		}
	}
}
