package fr.uge.greed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

public class Application {

	static private class Context {

		private final SelectionKey key;
		private final SocketChannel scContext;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final Application server;
		private boolean closed = false;

		/**
		 * For every socket channel we link a context with a key
		 * 
		 * @param key
		 * @param server
		 */
		private Context(SelectionKey key, Application server) {
			this.key = key;
			this.scContext = (SocketChannel) key.channel();
			this.server = server;

		}

		private SocketChannel getChannel() {
			return scContext;
		}

		private void updateInterestOps() {
			var ops = 0;
//	            if (!closed && bufferOut.hasRemaining()) {
//	                ops |= SelectionKey.OP_READ;
//	            }
//	            if (bufferOut.position() != 0){
//	                ops |= SelectionKey.OP_WRITE;
//	            }
//	            if (ops == 0 && closed) {
//	               	
//	                return;
//	            }
			if (closed) {

			}
			key.interestOps(ops);
		}

		/**
		 * Close the channel between two applications
		 * 
		 * @param key
		 */
		private void silentlyClose(SelectionKey key) {
			Channel sc = (Channel) key.channel();
			try {
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Read the buffer that get datas if we close the connexion read return -1 and
		 * we shut down the connexion
		 * 
		 * @throws IOException
		 */
		public void doRead() throws IOException {
			if (scContext.read(bufferIn) == -1) {
				System.out.println("Connexion closed");
				silentlyClose(key);
				return;
			}
			// scContext.read(bufferIn);
			bufferIn.flip();
			System.out.println(StandardCharsets.UTF_8.decode(bufferIn));
			bufferIn.clear();

		}

	}

	// private final SelectionKey key;

	private final ServerSocketChannel sc;
	private final InetSocketAddress inet;
	private final SocketChannel scDaron;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private final Selector selector;
	private boolean isroot;
	private final HashSet<Context> connexions = new HashSet<>();
	private RouteTable table = new RouteTable();
	private ByteBuffer bufferDonnee = ByteBuffer.allocate(4048);
	private ByteBuffer bufferDonneeTraitee = ByteBuffer.allocate(4048);
	private ByteBuffer bufferDonneeDeco = ByteBuffer.allocate(4048);
	private ByteBuffer bufferEnvoie = ByteBuffer.allocate(4048);
	private InetSocketAddress dataFrom =null;

	static private final int BUFFER_SIZE = 1024;

	/**
	 * Start the application in mode Root
	 * 
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public Application(String host, int port) throws IOException { // root
		isroot = true;
		sc = ServerSocketChannel.open();
		scDaron = null;
		inet = new InetSocketAddress(host, port);
		sc.bind(inet);
		selector = Selector.open();

	}

	/**
	 * Start the application in normal mode it get it father inetsocketaddress and
	 * we need it inetsocketaddress to start If the father doesn't exist
	 * 
	 * @param host
	 * @param port
	 * @param fatherAddress
	 * @throws IOException
	 */
	public Application(String host, int port, InetSocketAddress fatherAddress) throws IOException { // Connecting to
																									// father
		isroot = false;
		sc = ServerSocketChannel.open();
		inet = new InetSocketAddress(host, port);
		sc.bind(inet);
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
				System.out.println("yesyes");
				printConnexions();
			}
		} catch (IOException e) {
			logger.info("Connection closed with client due to IOException");
			silentlyClose(key);
			removeIfClosed();
			printConnexions();
		}
	}

	/**
	 * Accept a connexion from a "Child node"
	 * 
	 * @param key
	 * @throws IOException
	 */
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
		table.updateRouteTable((InetSocketAddress) context.getChannel().getRemoteAddress(),
				(InetSocketAddress) context.getChannel().getRemoteAddress());
		printConnexions();
	}

	/**
	 * Connect to a "Parent node"
	 * 
	 * @param key
	 */
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
		if (con.closed) {
			Thread.currentThread().interrupt();
		}
		key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * closing a Channel between two applications
	 * 
	 * @param key
	 */
	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	/**
	 * Function that read the lines prompted in the bash if the lines is
	 * "Disconnect" it disconnect the application
	 * 
	 * @param key
	 */
	@SuppressWarnings("preview")
	private void consoleTest(SelectionKey key) {
		Thread.ofPlatform().start(() -> {
			try {
				try (var scanner = new Scanner(System.in)) {
					while (scanner.hasNextLine()) {
						var msg = scanner.nextLine();
						if (msg.equals("Disconnect")) {
							System.out.println("---------------------\nDisconnecting the node ...");
							var con = (Context) key.attachment();
							con.closed = true;
							silentlyClose(key);
							Thread.currentThread().interrupt();
							System.out.println("Disconnected Succesfully\n---------------------");
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
				System.exit(0);
			}
		});

	}

	/**
	 * Remove the closed connexion from the HashSet
	 * @throws IOException 
	 */
	private void removeIfClosed()  {
		try {
			for(var e : connexions) {
				if(!e.scContext.isOpen()) {
					System.out.println("lolololololololo"+e.scContext.getRemoteAddress());
					//table.deleteRouteTable((InetSocketAddress) e.scContext.getRemoteAddress());
				}
			}
		}catch (IOException e) {
			e.getCause();
		}
		connexions.removeIf(e -> !e.scContext.isOpen());
		
	}

	/**
	 * Print connexions of each nodes connected to the application
	 */
	private void printConnexions() {
		System.out.println("-------------Table of connexions--------------");
		for (var e : connexions) {
			if (e.scContext.equals(scDaron)) {
				System.out.println("Connected To : " + e.scContext);
			} else {
				System.out.println("Conneted from : " + e.scContext);
			}
		}
		System.out.println("-----RouteTable------");
		System.out.println(table);
	}

	/**
	 * No need to tell you what is it about
	 */
	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Application host port adress");
		System.out.println(" - Root Mode - ");
		System.out.println("Application host port");
	}

	// READ MODE
	/**
	 * Read the frame op to know what to do next
	 * @return
	 */
	int getOp() {
		Objects.requireNonNull(this.bufferDonnee);	//buffer altéré
		var op = new IntReader();
		op.process(this.bufferDonnee);
		return op.get();
	}

	/**
	 * Check and work on the frame from what op code we got
	 * @param op
	 * @param buf
	 */
	void analyseur(Trames op, ByteBuffer buf,InetSocketAddress address) {
		switch(op) {
		/*Une fonction pour chaque trame*/
		case PINGENVOI -> {
			//getAddressFromBuffer(buf);
			renvoiePingEnvoi(address);
			//TODO
			receivePingEnvoiAndSendPingReponse(buf);
		}
		case ACCEPTCO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case CONFIRMATIONCHANGEMENTCO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DEMANDECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DEMANDERECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DONNEEATRAITER -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DONNEEDECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DONNEETRAITEES -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case FIRSTLEAF -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case FIRSTROOT -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case FULLTREE -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case INTENTIONDECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case NEWLEAF -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case PINGREP -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case SUPPRESSION -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		default -> throw new IllegalArgumentException("Unexpected value: " + op);
		
		}
	}
	
	/**
	 * Add in a buffer the inetSocketAddress that was get in the parameters
	 * @param inet
	 * @return
	 */
	ByteBuffer addressTrame(InetSocketAddress inet) {
		ByteBuffer internBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		var ipByte = new byte[8];			//constructiontrame(truc)
		ipByte = inet.getAddress().getAddress();
		internBuffer.put(ipByte);
		internBuffer.putShort((short) inet.getPort());
		
		return internBuffer.flip();
	}
	
	
	///////////////////////////////////////////////////////////////////////////////REVOIR CETTE FONCTION aussi a ligne  375
	
	/**
	 * Get The address Of Source for the broadCast
	 * @param internBuffer
	 * @return
	 */
	void getAddressFromBuffer(ByteBuffer internBuffer) {
		try {
		internBuffer.flip();
		internBuffer.getInt();
		internBuffer.limit(8);
		byte[] ipByte = new byte[8];
		internBuffer.get(ipByte);
		var port = internBuffer.getShort();
		var ipAddress = InetAddress.getByAddress(ipByte);
		this.dataFrom =  new InetSocketAddress(ipAddress,port);
		}catch (IOException e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * Construct the frame for Opcode :11 Trame ping reponse (see here https://gitlab.com/Setsulys/ugegreed-debats-ly-ieng/-/blob/main/GreedRfc.md)
	 * @param buf
	 */
	void receivePingEnvoiAndSendPingReponse(ByteBuffer buf){
		//var address = table.get(/*addresss*/);
		
		//envoie paquet PINGENVOI a toute les autres connexions
		
		/*___________METHODE WAKE UP ENVOIE PAQUET_________*/
		
		
		if(buf.remaining()<10){
			return;
		}
		byte[] ipByte = new byte[8];
		buf.get(ipByte);
		short port = buf.getShort();
		try {
			InetAddress address = InetAddress.getByAddress(ipByte);
			InetSocketAddress socketAddress = new InetSocketAddress(address,port);
			// Partie envoi des donnée
			
			byte bit = 11;//opcode
			bufferEnvoie.put(bit);
			bufferEnvoie.put(addressTrame(inet)); //Addresse Source
			bufferEnvoie.put(addressTrame(socketAddress)); // Adresse ping
			
			//mise de la reponse
			if(bufferDonnee.hasRemaining()){
				bit = 0;//Not Avaliable
			}
			else{
				bit = 1;//Avaliable
			}
			bufferEnvoie.put(bit);
			bufferEnvoie.flip();
			/*___________METHODE WAKE UP ENVOIE PAQUET_________*/
			
			
		} catch (UnknownHostException e) {
			//e.getCause();
		}
		
	}
	
	void renvoiePingEnvoi(InetSocketAddress address) {
		bufferEnvoie.position(0);
		
	}
	void broadCast(InetSocketAddress address) throws IOException{
		for(var key : selector.keys()){
			var context = (Context) key.attachment();
			if(context != null && address!=(InetSocketAddress) context.scContext.getRemoteAddress()){
				
			}
		}
	}

	
	
	
	
	
	
	

	/**
	 * Main part of the application
	 * @param args
	 * @throws IOException
	 */
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
