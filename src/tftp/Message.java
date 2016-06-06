package tftp;

import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;



/*TODO
 * ITERATION 3
Invalid filename?????????????
Too long?????????????????????????????

ITERATION 4
Concurrent access

ELSE
Relative paths?
 */

public class Message extends Thread{
	Scanner sc;
	Stoppable s;
	int sleep;
	boolean inter = false;
	public static final String[] ops = {"","RRQ","WRQ","DATA","ACK","ERROR"};

	public Message(Stoppable s,Scanner sc) {
		this.s = s;
		this.sc = sc;
	}
	public Message(int n,Stoppable s) {
		sleep =n;
		inter = true;
		this.s = s;
	}

	public void run() {
		if (inter) {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			s.interrupt();
		}
		else {
			while (true) {
					if (sc.hasNext()) {
						if (!s.menu) {
						String x = sc.next();
						if (s.waiting()) {
							s.filename = x;
							synchronized(s) {
								s.notify();
							}
						}
						//else if (x.equals("r")||x.equals("R")){
					//		s.menu = true;
					//	}
						else if (x.equals("q")||x.equals("Q")){
							s.setShutdown();
							return;
						}
						else if (x.equals("v")||x.equals("V")) {
							if (s.verbose) {
								System.out.println("Verbose mode turned off.");
							}
							else {
								System.out.println("Verbose mode turned on.");
							}
							s.verbose = !s.verbose;
						}
						//else {
						//	sc.reset();
						//}
					}
				
				else {
					synchronized(s) {
						 ((Client)s).menu(sc.next());
						s.notify();
					}
				}
					}
			}
		}
	}

	//this probs matches invalid strings but so does the sample...
	public static boolean validate(DatagramPacket receivePacket, boolean initial) throws MalformedPacketException{
		String data = new String(receivePacket.getData(),0,receivePacket.getLength());
		if (data.length()<4) {
			throw new MalformedPacketException("Malformed packet: Not enough data.");
		}
		if (Pattern.matches("^\0\005[\000-\007]{2}(.|\012|\015|\0)*$",data)) {
			return true;
		}
		if (Pattern.matches("^\0\003(.|\012|\015){2,}$",data)) {
			if (!initial) {
				return true;
			}
			throw new MalformedPacketException("Unexpected opcode on request.");
		}
		if (Pattern.matches("^\0\004(.|\012|\015)*$", data)) {
			if (data.length()==4) {
				if (!initial) {
					return true;
				}
				throw new MalformedPacketException("Unexpected opcode on request.");
			}
			throw new MalformedPacketException("Unexpected opcode.");
		}
		if (Pattern.matches("^\0(\001|\002).+\0(([oO][cC][tT][eE][tT])|([nN][eE][tT][aA][sS][cC][iI][iI]))\0$", data)) {
			if (initial) {
				return true;
			}
			throw new MalformedPacketException("Invalid opcode.");
		}
		if (Pattern.matches("^\0(\001|\002).+\0.+\0$", data)) {
			throw new MalformedPacketException("Invalid mode.");
		}
		if (data.charAt(0)!=0||data.charAt(1)>5) {
			throw new MalformedPacketException("Invalid opcode.");
		}
		if (Pattern.matches("^\0(\001|\002).+\0.+$", data)||Pattern.matches("^\0(\001|\002).+.+\0$", data)) {
			throw new MalformedPacketException("Missing null terminator.");
		}
		return false;
	}

	public static int parseBlock(byte[] data) {
		int x = (int) data[2];
		int y = (int) data[3];
		if (x<0) {
			x = 256+x;
		}
		if (y<0) {
			y = 256+y;
		}
		return 256*x+y;
	}

	public static byte[] toBlock(int n){
		byte[] b = {(byte) (n/256), (byte) (n%256)};
		return b;
	}

	public static String parseFilename(String data) {
		return data.split("\0")[1].substring(1);
	}

	/*
	 * formatRequest takes a filename and a format and an opcode (which corresponds to read or write)
	 * and formats them into a correctly formatted request
	 */
	public static byte[] formatRequest(String filename, String format, int opcode) {
		int l = filename.length();
		byte[] msg = filename.getBytes();
		byte [] result;
		result = new byte[l+4+format.length()];
		result[0] = 0;
		result[1] = (byte) opcode;
		for (int i = 0;i<l;i++) {
			result[i+2] = msg[i];
		}
		result[l+2] = 0;
		for (int i = 0;i<format.length();i++) {
			result[l+3+i] = format.getBytes()[i];
		}
		result[l+3+format.length()] = 0;
		return result;
	}

	//prints relevent information about an incoming packet
	public static void printIncoming(DatagramPacket p, String name, boolean verbose) {
		if (verbose) {
			int opcode = p.getData()[1];
			System.out.println(name + ": packet received.");
			System.out.println("From host: " + p.getAddress());
			System.out.println("Host port: " + p.getPort());
			int len = p.getLength();
			System.out.println("Length: " + len);
			if (opcode<ops.length) {
				System.out.println("Packet type: "+ ops[opcode]);
			}
			if (opcode<3) {
				System.out.println("Filename: "+ parseFilename(new String (p.getData(), 0, len)));
			}
			else if(opcode==5) {
			}
			else {
				System.out.println("Block number " + parseBlock(p.getData()));

			}
			if (opcode==3) {
				System.out.println("Number of bytes: "+ (len-4));
			}
			if (opcode==5) {
				System.out.println("Error code: " + p.getData()[3]);
				System.out.println("Error message: " + new String(p.getData(),4,(len-4)));
			}
			System.out.println();
		}
	}

	//prints information about an outgoing packet
	public static void printOutgoing(DatagramPacket p, String name, boolean verbose) {
		if (verbose) {
			int opcode = p.getData()[1];
			System.out.println(name + ": packet sent.");
			System.out.println("To host: " + p.getAddress());
			System.out.println("Host port: " + p.getPort());
			int len = p.getLength();
			System.out.println("Length: " + len);
			if (opcode<ops.length) {
				System.out.println("Packet type: "+ ops[opcode]);
			}
			if (opcode<3) {
				System.out.println("Filename: "+ parseFilename(new String (p.getData(), 0, len)));
			}
			else if(opcode==5) {

			}
			else {
				System.out.println("Block number " + parseBlock(p.getData()));

			}
			if (opcode==3) {
				System.out.println("Number of bytes: "+ (len-4));
			}
			if (opcode==5) {
				System.out.println("Error message: " + new String(p.getData(),4,(len-4)));
			}
			System.out.println();
		}
	}
}
