package fr.uge.greed;

import java.net.InetSocketAddress;

public class Application {
		
	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Application host port adress");
		System.out.println(" - Root Mode - ");
		System.out.println("Application host port");
	}
	
	
	public static void main(String[] args) {
		if(args.length > 3) {
			usage();
			return;
		}
		String host = args[0];
		int port = Integer.valueOf(args[1]);	
		if(args.length == 3) {
			int adress = Integer.valueOf(args[2]);
		}
		InetSocketAddress serverAddress = new InetSocketAddress(host,port);
		
	} 
}
