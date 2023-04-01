package fr.uge.greed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class Application {

	static private class Context {

		private final SelectionKey key;
		private final SocketChannel scContext;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final Application server;
		private boolean closed = false;

		private Context(SelectionKey key, Application server) {
			this.key = key;
			this.scContext = (SocketChannel) key.channel();
			this.server = server;

		}

		private SocketChannel getChannel() {
			return scContext;
		}

//	     private void updateInterestOps() {
////	            var ops = 0;
////	            if (!closed && bufferOut.hasRemaining()) {
////	                ops |= SelectionKey.OP_READ;
////	            }
////	            if (bufferOut.position() != 0){
////	                ops |= SelectionKey.OP_WRITE;
////	            }
////	            if (ops == 0 && closed) {
////	               	
////	                return;
////	            }
//	    	 	if(closed){
//					 
//				 }
//	            key.interestOps(ops);
//	        }

	     private void silentlyClose(SelectionKey key) {
	 		Channel sc = (Channel) key.channel();
	 		try {
	 			sc.close();
	 		} catch (IOException e) {
	 			// ignore exception
	 		}
	 	}

		public void doRead() throws IOException {
			if(scContext.read(bufferIn)==-1){
				System.out.println("Connexion closed");
				silentlyClose(key);
				return;	
			}
			//scContext.read(bufferIn);
			bufferIn.flip();
			System.out.println(StandardCharsets.UTF_8.decode(bufferIn));
			bufferIn.clear();
			
		}

	}
	
	public class RouteTable {

		private final LinkedHashMap<InetSocketAddress, InetSocketAddress> routeTable = new LinkedHashMap<>();
		
		/**
		 * Met A jour la Table de routage
		 * @param newAdress
		 * @param route
		 */
		public void updateRouteTable(InetSocketAddress newAdress,InetSocketAddress route) {
			//newAdress l'adresse de la node que l'on veut,route l'adresse de la node par laquelle on passe pour aller a newAdress
			Objects.requireNonNull(newAdress);
			Objects.requireNonNull(route);
			routeTable.put(newAdress, route);
		}
		
		
		/**
		 * Supprime une Application et son chemin (clé valeur) de la table de routage lors de la déconnexion de l'Application
		 * @param unlinked
		 */
		public void deleteRouteTable(InetSocketAddress unlinked) {
			Objects.requireNonNull(unlinked);
			routeTable.remove(unlinked);
		}
		
		
		/**
		 * Recuppere le chemin (voisin) par lequel une trame doit passer pour atteindre sa destination
		 * renvoi null si il n'y a pas de chemin
		 * @param destination
		 * 
		 * @return InetSocketAddress
		 */
		public InetSocketAddress get(InetSocketAddress destination) {
			Objects.requireNonNull(destination);
			return routeTable.get(destination);
		}
		
		/**
		 * Affiche Toute la route table
		 */
		@Override
		public String toString() {
			return routeTable.entrySet().stream().map(e -> e.getKey() + " : " + e.getValue()).collect(Collectors.joining("\n"));
		}
		
	}


	// private final SelectionKey key;

	private final ServerSocketChannel sc;
	private final SocketChannel scDaron;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private final Selector selector;
	private boolean isroot;
	private final HashSet<Context> connexions = new HashSet<>();
	private RouteTable table = new RouteTable();

	static private final int BUFFER_SIZE = 1024;

	public Application(String host, int port) throws IOException { // root
		isroot = true;
		sc = ServerSocketChannel.open();
		scDaron = null;
		sc.bind(new InetSocketAddress(host, port));
		selector = Selector.open();
	}

	public Application(String host, int port, InetSocketAddress fatherAddress) throws IOException { // Connecting to
																									// father
		isroot = false;
		sc = ServerSocketChannel.open();
		sc.bind(new InetSocketAddress(host, port));
		selector = Selector.open();
		scDaron = SocketChannel.open();
		scDaron.configureBlocking(false);
		scDaron.register(selector, SelectionKey.OP_CONNECT);
		scDaron.connect(fatherAddress);
		table.updateRouteTable(fatherAddress, fatherAddress);
	}

	public void launch() throws IOException {
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_ACCEPT);
		while (!Thread.interrupted()) {
			// Helpers.printKeys(selector); // for debug
			// System.out.println("Starting select");
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			// System.out.println("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
//        Helpers.printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isConnectable()) {
				doConnect(key);
			}
			if (key.isValid() && key.isWritable()) {
				// ((Context) key.attachment()).doWrite();
				System.out.println("A modi");
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
				removeIfClosed();
				printConnexions();
			}
		} catch (IOException e) {
			logger.info("Connection closed with client due to IOException");
			silentlyClose(key);
			removeIfClosed();
			printConnexions();
		}
	}


	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel nouvFils = sc.accept();

		if (sc == null) {
			logger.info("selector gave bad hint");
			return;
		}
		nouvFils.configureBlocking(false);
		var newKey = nouvFils.register(selector, SelectionKey.OP_READ);
		var context = new Application.Context(newKey, this);
		newKey.attach(context);
		connexions.add(context);
		table.updateRouteTable((InetSocketAddress) context.getChannel().getRemoteAddress(), (InetSocketAddress)context.getChannel().getRemoteAddress());
		printConnexions();
	}

	private void doConnect(SelectionKey key) {
		try {
			if (!scDaron.finishConnect()) {
				return;
			}

		} catch (IOException e) {
			e.getCause();
		}
		var context = new Application.Context(key, this);
		key.attach(context);
		connexions.add(context);
		consoleTest(key);
		var con = (Context) key.attachment();
		if(con.closed){
			System.out.println("interrrrr");
			Thread.currentThread().interrupt();
		}
		key.interestOps(SelectionKey.OP_READ);
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	@SuppressWarnings("preview")
	private void consoleTest(SelectionKey key) {
		Thread.ofPlatform().start(() -> {
			try {
				try (var scanner = new Scanner(System.in)) {
					while (scanner.hasNextLine()) {
						var msg = scanner.nextLine();
                       if(msg.equals("Disconnect")) {
                    	   System.out.println("Disconnecting the node");
                    	   var con = (Context) key.attachment();
                    	   con.closed = true;
                    	   silentlyClose(key);
                    	   Thread.currentThread().interrupt();
                    	   return;
                       }
						var buf = ByteBuffer.allocate(msg.length());
						buf.put(Charset.forName("UTF-8").encode(msg));
						var c = (Context) key.attachment();
						SocketChannel a = c.getChannel();
						a.write(buf.flip());
					}
				}
				logger.info("Console thread stopping ");
			} catch (IOException e) {
				logger.info("IOE");
			}
		});

	}
	
	private void removeIfClosed() {
		connexions.removeIf(e -> !e.scContext.isOpen());
	}
	
	private void printConnexions() {
    	System.out.println("-------------Table of connexions--------------");
		for (var e : connexions) {
			if (e.scContext.equals(scDaron)) {
				System.out.println("Connected To : " + e.scContext);
			} else {
				System.out.println("Conneted from : " + e.scContext);
			}
		}
//		System.out.println("-----RouteTable------");
//		System.out.println(table);
	}
	


	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Application host port adress");
		System.out.println(" - Root Mode - ");
		System.out.println("Application host port");
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 2 || args.length > 5) {
			usage();
			return;
		}

		if (args.length >= 2) {
			String host = args[0];
			int port = Integer.valueOf(args[1]);
			if (args.length == 4) {
				String fatherHost = args[2];
				int fatherPort = Integer.valueOf(args[3]);
				new Application(host, port, new InetSocketAddress(fatherHost, fatherPort)).launch(); // normal
			} else {
				new Application(host, port).launch();// root
			}
		}

	}
}
