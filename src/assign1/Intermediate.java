package assign1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Intermediate extends Stoppable{
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket serverSideSocket, receiveSocket, replySocket;
	int replyPort;
	InetAddress replyAddress;
	private boolean shutdown = false;

	public Intermediate() {	
		try {
			//create the two sockets which always exist, one on port 23 to receive requests from the client
			//and one on a random port to communicate with the server
			serverSideSocket = new DatagramSocket();
			receiveSocket = new DatagramSocket(6001);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public Intermediate(DatagramPacket received) {
		replyPort = received.getPort();
		replyAddress = received.getAddress();
		receivePacket = received;
	}

	public void run() {
		try {
			sendPacket = new DatagramPacket(receivePacket.getData(),receivePacket.getLength(),InetAddress.getLocalHost(),6000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		

	}
	/*
	 * forward takes all messages from the client and forwards them on to the server
	 * it then waits for a response, which it forwards back to the client
	 */
	public void forward() {
		while (!shutdown) { //loop forever-ish
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			Message.printIncoming(receivePacket, "Intermediate Host");
			int len = receivePacket.getLength();

			try {
				sendPacket = new DatagramPacket(data, len, //create the packet that will be sent to the server
						InetAddress.getLocalHost(), 6000);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			try {
				serverSideSocket.send(sendPacket); //send the packet to the server
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			Message.printOutgoing(sendPacket, "Intermediate Host");
			try {
				serverSideSocket.receive(receivePacket);
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			Message.printIncoming(receivePacket, "Intermediate Host");
			len = receivePacket.getLength();
			sendPacket = new DatagramPacket(data, len, replyAddress, replyPort); //new packet to send back to client
			try {
				replySocket = new DatagramSocket(); //create a new socket to reply to client
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
			try {
				replySocket.send(sendPacket); //send the reply on to the client
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			Message.printOutgoing(sendPacket, "Intermediate Host");
			replySocket.close(); //close socket once message sent to client
		}
	}

	public static void main (String[] args) {
		Intermediate i = new Intermediate();
		i.forward();
		i.receiveSocket.close(); //close the sockets, right now this will never happen
		i.serverSideSocket.close(); //but in iteration1...when there's a way to exit
	}
}
