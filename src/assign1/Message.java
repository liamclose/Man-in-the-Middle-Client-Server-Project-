package assign1;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Pattern;


/*
 * TODO:
 * 	-File Transfer
 * 		-Reading from file - mostly done?
 * 		-Writing to file
 * 		-Formatting messages/updating byte numbers/etc
 *  -Read Me
 *  -Testing
 *  -Ask TA about verbose/quiet mode and UML collaboration diagrams
 *  -Closing intermediate/multithreading there?
 *  -Verbose/quiet
 *  -Test/normal
 */



public class Message extends Thread{
	Scanner sc;
	Stoppable s;

	public Message(Stoppable s) {
		this.s = s;
		sc = new Scanner(System.in);
	}
	
	public void run() {
		if (sc.hasNext()) {
			s.setShutdown();
		}
	}
	
	//this probs matches invalid strings but so does the sample...
	public static boolean validate(String data) {
		return Pattern.matches("^\0(((\001|\002).+\0(([oO][cC][tT][eE][tT])|([nN][eE][tT][aA][sS][cC][iI][iI]))\0)|(\004..)|(\003...*))$", data);
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
		System.out.println(""+x + y);
		return 256*x+y;
	}
	public static void main(String[] args) {
		byte[] x = {0,3,0,1};
		x = formatRequest("a.txt","octet",2);
		System.out.println(validate(new String(x,0,x.length)));
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
	public static void printIncoming(DatagramPacket p, String name) {
		System.out.println(name + ": packet received.");
		System.out.println("From host: " + p.getAddress());
		System.out.println("Host port: " + p.getPort());
		int len = p.getLength();
		System.out.println("Length: " + len);
		System.out.print("Byte form: " );
		for (int i = 0;i<len;i++) {
			System.out.print(p.getData()[i]);
			System.out.print(" ");
		}
		String received = new String(p.getData(),0,len);   
		System.out.println("\nString form: " + received + "\n");
	}

	//prints information about an outfoing packet
	public static void printOutgoing(DatagramPacket p, String name) {
		System.out.println(name + ": packet sent.");
		System.out.println("To host: " + p.getAddress());
		System.out.println("Host port: " + p.getPort());
		int len = p.getLength();
		System.out.println("Length: " + len);
		System.out.print("Byte form: " );
		for (int i = 0;i<len;i++) {
			System.out.print(p.getData()[i]);
			System.out.print(" ");
		}
		String sent = new String(p.getData(),0,len);   
		System.out.println("\nString form: " + sent + "\n");
	}
}
