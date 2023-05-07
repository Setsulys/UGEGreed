	package fr.uge.greed;

import java.io.IOException;
import java.util.logging.Logger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
	private final static Logger log = Logger.getLogger(Main.class.getName());
	
	
	/**
	 * No need to tell you what is it about
	 */
	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Main <Host> <Port> <Remote Host> <Remote Port>");
		System.out.println(" - Root Mode - ");
		System.out.println("Main <Host> <Port>");
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
