package fr.uge.greed;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
	/**
	 * No need to tell you what is it about
	 */
	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Application host port adress");
		System.out.println(" - Root Mode - ");
		System.out.println("Application host port");
	}
	
	/**
	 * Main part of the application
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 2 || args.length > 5) {
			usage();
			return;
		}
		Application appli;
		if (args.length >= 2) {
			String host = args[0];
			int port = Integer.valueOf(args[1]);
			if (args.length == 4) {
				String fatherHost = args[2];
				int fatherPort = Integer.valueOf(args[3]);
				appli = new Application(host, port, new InetSocketAddress(fatherHost, fatherPort));// normal
			} else {
				appli = new Application(host, port);// root
			}
			appli.launch();
		}		
	}
}
