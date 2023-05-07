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
	
	private static void allUsage(){
		System.out.println("Disconnect Usage :");
		System.out.println("DISCONNECT");
		launchUsage();
	}
	
	private static void launchUsage(){
		System.out.println("Start Usage :");
		System.out.println("START <url-jar> <fully-qualified-name> <start-range> <end-range> <filename>");	
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
			
//			@SuppressWarnings("preview")
			Thread.ofPlatform().daemon().start(()->{
				try(var scanner = new Scanner(System.in)){
					while(scanner.hasNextLine()){
						var msg = scanner.nextLine();
						if(msg.equals("DISCONNECT")){
							System.out.println("---------------------\nDisconnecting the node ...");
						}
						else{
							String str[] = msg.split(" ");
							var lst = Arrays.asList(str);
							if(lst.size() == 6){
								if(lst.get(0).equals("START")){
									System.out.println(lst);
									try{
										String jar = lst.get(1);
										String qualifiedName = lst.get(2);
										long start = Long.parseLong(lst.get(3));
										long end = Long.parseLong(lst.get(4));
										String fileName = lst.get(5);
									}catch(NumberFormatException e){
										log.info(" WRONG START");
										launchUsage();
									}
								
								}
								
							}
							else{
								log.info("WRONG COMMAND");
								allUsage();
							}
						}
					}
				}
			});	
		}	
	}
}

//		Thread.ofPlatform().start(()->{
//try (var scanner = new Scanner(System.in)) {
//	while (scanner.hasNextLine()) {
//		var msg = scanner.nextLine();
//		if (msg.equals("DISCONNECT")) {
//			System.out.println("---------------------\nDisconnecting the node ...");
//			appli.k
//			var con = (Context) key.attachment();
//			con.closed = true;
//			silentlyClose(key);
//			Thread.currentThread().interrupt();
//			System.exit(0);
//		}
//	}
//}
//});
