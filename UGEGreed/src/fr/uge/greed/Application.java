package fr.uge.greed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import fr.uge.greed.data.DataALotAddress;
import fr.uge.greed.data.DataDoubleAddress;
import fr.uge.greed.data.DataOneAddress;
import fr.uge.greed.data.DataResponse;
import fr.uge.greed.reader.Reader;
import fr.uge.greed.reader.TrameReader;
import fr.uge.greed.trame.Trame;
import fr.uge.greed.trame.TrameAnnonceIntentionDeco;
import fr.uge.greed.trame.TrameFirstLeaf;
import fr.uge.greed.trame.TrameFullTree;
import fr.uge.greed.trame.TramePingConfirmationChangementCo;
import fr.uge.greed.trame.TramePingEnvoi;
import fr.uge.greed.trame.TramePingReponse;
import fr.uge.greed.trame.TrameSuppression;

public class Application {

	static public class Context {

		private final SelectionKey key;
		private final SocketChannel scContext;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final Application server;
		private boolean closed = false;
		private final ArrayDeque<Trame> queue = new ArrayDeque<>();
		private final TrameReader trameReader = new TrameReader();
		private final Logger loggerC = Logger.getLogger(Application.class.getName());
		private InetSocketAddress disconnectedAddress;
		private int op = -1;

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

		
		/**
		 *  Get the socketChannel from of the context
		 * @return
		 */
		public SocketChannel getChannel() {
			return scContext;
		}

		private void updateInterestOps() {
			System.out.println("update");
			try {
				var ops = 0;
				if (!closed && bufferIn.hasRemaining()) {
					ops |= SelectionKey.OP_READ;
				}
				if (bufferOut.position() != 0) {
					ops |= SelectionKey.OP_WRITE;
				}
				if (ops == 0 && closed) {
					silentlyClose();
					return;
				} 
				System.out.println("update ICI "+ops);
				key.interestOps(ops);
			} catch (CancelledKeyException e) {
				System.out.println("CATCHED");
			}
			
		}

		/**
		 * Process the buffer in the Application
		 */
		private void processIn() {
			for (;;) {
//				if(server.isroot) {
//					System.out.println("is root" + bufferIn);
//				}
				Reader.ProcessStatus status = trameReader.process(bufferIn);
//				System.out.println("apres processtatus"+bufferIn);
				switch (status) {
				case DONE -> {
					trameReader.getOp();
					var tramez = trameReader.get();
					//System.out.println("OP PROCESSIN  " + tramez.getOp());
					trameReader.reset();
					
					try {
						server.recu = this.scContext;
						server.analyseur(tramez);
						break;
					} catch (IOException e) {
						loggerC.info("ProcessIn IOE");
					}
					trameReader.reset();
					break;
				}
				case REFILL -> {
					return;
				}
				case ERROR -> {
					silentlyClose();
					return;
				}
				default -> {
					return;
				}
				}
				return;
			}
		}
		
		/**
		 * Prepare BufferOut with the good frame
		 */
		private void processOut() {
			// System.out.println("PROCESSOUT " + tramez.getOp());
			var tramez = queue.poll();
			
			if(tramez == null){
				loggerC.info("nothing to poll");
				return;
			}
			op = tramez.getOp();
			switch (tramez.getOp()) {
			case 3 -> {// dataDoubleAddress
				if (bufferOut.remaining() < Integer.BYTES + (34 * Byte.BYTES)) {
					loggerC.warning("Buffer doesn't have  enough room");
					return;
				}
				var tmp = (TrameAnnonceIntentionDeco) tramez;
				bufferOut.putInt(tramez.getOp());
				if (tmp.dda().AddressSrc().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				if (tmp.dda().AddressSrc().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				bufferOut.put(addressTrame(tmp.dda().AddressSrc()));
				
				if (tmp.dda().AddressDst().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				if (tmp.dda().AddressDst().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);		
				}
				bufferOut.put(addressTrame(tmp.dda().AddressDst()));
				
			}

			case 4 -> {// dataDoubleAddress
				if (bufferOut.remaining() < Integer.BYTES + (34 * Byte.BYTES)) {
					loggerC.warning("Buffer doesn't have  enough room");
					return;
				}
				
				var tmp2 = (TramePingConfirmationChangementCo) tramez;
				bufferOut.putInt(tmp2.getOp());
				if (tmp2.dda().AddressSrc().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				else if (tmp2.dda().AddressSrc().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				bufferOut.put(addressTrame(tmp2.dda().AddressSrc()));
				if (tmp2.dda().AddressDst().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				else if (tmp2.dda().AddressDst().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				bufferOut.put(addressTrame(tmp2.dda().AddressDst()));
			}
			case 5 -> {// dataOneAddress
				if (bufferOut.remaining() < Integer.BYTES + (17 * Byte.BYTES)) {
					loggerC.warning("Buffer doesn't have  enough room");
					return;
				}
				var tmp3 = (TrameSuppression) tramez;
				bufferOut.putInt(tmp3.getOp());
				if (tmp3.doa().Address().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				if (tmp3.doa().Address().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				
				bufferOut.put(addressTrame(tmp3.doa().Address()));
			}

			case 7 -> {
				// dataALotAddress
				var tmp99 = (TrameFirstLeaf) tramez;
				if (bufferOut.remaining() < Integer.BYTES * 2 + (17 * tmp99.getSize() * Byte.BYTES)) {
					loggerC.warning("Buffer doesn't have  enough room");
					return;
				}
//				System.out.println("processout 1" + bufferOut);
				bufferOut.putInt(tramez.getOp());
				bufferOut.putInt(tmp99.getSize());
				for(int i = 0;i<tmp99.getSize();i++) {
					if (tmp99.dla().list().get(i).getAddress().getClass() == Inet6Address.class) {
						byte aaa = 6;
						bufferOut.put(aaa);
					}
					else {
						byte aaa = 4;
						bufferOut.put(aaa);
					}
					bufferOut.put(addressTrame(tmp99.dla().list().get(i)));
					
				}
//				System.out.println("processout " + bufferOut);
			}

			case 8 -> {
				// dataALotAddress
				var tmp999 = (TrameFullTree) tramez;
				if (bufferOut.remaining() < Integer.BYTES * 2 +  (17 * tmp999.getSize() * Byte.BYTES)) {
					loggerC.warning("Buffer doesn't have  enough room");
					return;
				}
				bufferOut.putInt(tramez.getOp());
				bufferOut.putInt(tmp999.getSize());
				for(int i = 0;i<tmp999.getSize();i++) {
					
					if (tmp999.dla().list().get(i).getAddress().getClass() == Inet6Address.class) {
						byte aaa = 6;
						bufferOut.put(aaa);
					}
					else {
						byte aaa = 4;
						bufferOut.put(aaa);
					}
					bufferOut.put(addressTrame(tmp999.dla().list().get(i)));
				}
			}

			case 10 -> { // dataOneAddress
				if (bufferOut.remaining() < Integer.BYTES + (17 * Byte.BYTES)) {
					return;
				}
				var tmp10 = (TramePingEnvoi) tramez;
				bufferOut.putInt(tramez.getOp());
				if (tmp10.doa().Address().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				if (tmp10.doa().Address().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				bufferOut.put(addressTrame(tmp10.doa().Address()));
			}
			case 11 -> {
				if(bufferOut.remaining() < Integer.BYTES + (35 * Byte.BYTES)) {
					System.out.println("WTF PLACE IN BUFFER ?");
					return;
				}
				var tmp11 = (TramePingReponse) tramez;
				bufferOut.putInt(tramez.getOp());
				if (tmp11.dr().addressSrc().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				if (tmp11.dr().addressSrc().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				bufferOut.put(addressTrame(tmp11.dr().addressSrc()));
				
				if (tmp11.dr().addressDst().getAddress().getClass() == Inet4Address.class) {
					byte aaa = 4;
					bufferOut.put(aaa);
				}
				if (tmp11.dr().addressDst().getAddress().getClass() == Inet6Address.class) {
					byte aaa = 6;
					bufferOut.put(aaa);
				}
				bufferOut.put(addressTrame(tmp11.dr().addressDst()));
				if(tmp11.dr().boolByte() == false) {
					bufferOut.put((byte)0);
				}
				else{
					bufferOut.put((byte)1);
				}
					
			}

			case 12 -> {
				return;
			} // TODO
			case 13 -> {
				return;
			} // TODO
//			case 0 -> {
//				return;
//			} // ______DUMP
//			case 1 -> {
//				return;
//			} // ______DUMP
//			case 2 -> {
//				return;
//			} // ______DUMP
//			case 6 -> {// op
//				if (bufferOut.remaining() < Integer.BYTES ) {	//first root
//					loggerC.warning("Buffer doesn't have  enough room");
//					return;
//				}
//				bufferOut.putInt(tramez.getOp());
//				
//			}
//			case 9 -> {// dataOneAddress
//				if (bufferOut.remaining() < Integer.BYTES + (16 * Byte.BYTES)) {
//					loggerC.warning("Buffer doesn't have  enough room");
//					return;
//				}
//				var tmp4 = (TrameNewLeaf) tramez;
//				bufferOut.putInt(tramez.getOp());
//				if (tmp4.doa().Address().getAddress().getClass() == Inet4Address.class) {
//					byte aaa = 4;
//					bufferOut.put(aaa);
//				}
//				if (tmp4.doa().Address().getAddress().getClass() == Inet6Address.class) {
//					byte aaa = 6;
//					bufferOut.put(aaa);
//				}
//				
//				bufferOut.put(addressTrame(tmp4.doa().Address()));
//			}
//			case 77 ->{
//				System.out.println("im here");
//				if (bufferOut.remaining() < Integer.BYTES ) {	//first root
//					loggerC.warning("Buffer doesn't have  enough room");
//					return;
//				}
//				bufferOut.putInt(tramez.getOp());
//			}
			}

		}

		/**
		 * Close the channel between two applications
		 * 
		 * 
		 */
		private void silentlyClose() {
			try {
				scContext.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Read the buffer that get Data if we close the connection read return -1 and
		 * we shut down the connection
		 * 
		 * @throws IOException
		 */
		public void doRead() throws IOException {
			var bytes = scContext.read(bufferIn);
			if (bytes == -1) {
				disconnectedAddress = (InetSocketAddress) scContext.getRemoteAddress();
				System.out.println("Connexion closed >>>>>>>>>>>>>>>>>>>>>>>>>>> " + disconnectedAddress + "\n");
				silentlyClose();
				return;
			}

			// scContext.read(bufferIn);
			System.out.println("RECIEVED BYTES" + bytes  );
			bufferIn.flip();
			processIn();
			bufferIn.flip();
			bufferIn.compact();
			updateInterestOps();

		}

		/**
		 * Return the disconnected Application Address;
		 * @return
		 */
		public InetSocketAddress disconnectedAddress() {
			return disconnectedAddress;
		}

		public void doWrite() throws IOException {
			bufferOut.flip();
			var olp= bufferOut.position();
			System.out.println("OPCODE AVANT ENVOI : "+bufferOut.getInt());
			bufferOut.position(olp);
//			bufferOut.flip();
			
			System.out.println("BYTES SEND : " + scContext.write(bufferOut));
			bufferOut.compact();
			
			updateInterestOps();
		}

		/**
		 * Queue All the messages that will be send by bufferOut
		 * @param tramez
		 */
		public void queueTrame(Trame tramez) {
			queue.add(tramez);
			if (bufferOut.hasRemaining()) {
				processOut();
				System.out.println("jespere"+bufferOut);
			}
			updateInterestOps();
		}

		

	}

	// private final SelectionKey key;

	private ServerSocketChannel ssc;
	private static InetSocketAddress localInet;
	private SocketChannel recu = null;
	private SocketChannel scDaron;
	private Context daronContext = null;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private Selector selector;
	private boolean isroot;
	private final HashSet<Context> connexions = new HashSet<>();
	private final ArrayList<InetSocketAddress> listChangementCo = new ArrayList<InetSocketAddress>();
	private RouteTable table;
	private ByteBuffer bufferDonnee = ByteBuffer.allocate(BUFFER_SIZE);
	private ByteBuffer bufferDonneeDeco = ByteBuffer.allocate(BUFFER_SIZE);
	private ByteBuffer bufferEnvoie = ByteBuffer.allocate(BUFFER_SIZE);
	private HashSet<InetSocketAddress> reseau = new HashSet<>();
	private InetSocketAddress dispo = null;
	private LinkedHashMap<InetSocketAddress,Boolean> commande = new LinkedHashMap<>();
	static private final int BUFFER_SIZE = 4096;
	private final BlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private final Thread console;
	
	
	private String urlJar =null;
	private String fullQualifiedName =null;
	private String fileName =null;
	private long startValue;
	private long endValue;
	
	
	/**
	 * Start the application in mode Root
	 * 
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public Application(String host, int port) throws IOException { // root
		isroot = true;
		ssc = ServerSocketChannel.open();
		scDaron = null;
		localInet = new InetSocketAddress(host, port);
		ssc.bind(localInet);
		selector = Selector.open();
		table = new RouteTable((InetSocketAddress)ssc.getLocalAddress());
		this.console = new Thread(this::consoleRun);
		
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
		ssc = ServerSocketChannel.open();
		localInet = new InetSocketAddress(host, port);
		ssc.bind(localInet);
		selector = Selector.open();
		scDaron = SocketChannel.open();
		scDaron.configureBlocking(false);
		scDaron.register(selector, SelectionKey.OP_CONNECT);
		scDaron.connect(fatherAddress);
		table = new RouteTable((InetSocketAddress)ssc.getLocalAddress());
//		for (var key : selector.keys()) {
//			var sc = (SocketChannel) key.channel();
//			if (sc.getRemoteAddress() == fatherAddress) {
//				table.updateRouteTable((Context) key.attachment(), (Context) key.attachment());
//			}
//		}
		table.addToRouteTable(fatherAddress, fatherAddress);
		this.console = new Thread(this::consoleRun);
	}

	/**
	 * Launch the application
	 * @throws IOException
	 */
	public void launch() throws IOException {
		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		console.start();
		while (!Thread.interrupted()) {
			 Helpers.printKeys(selector); // for debug
			// System.out.println("Starting select");
			try {
				processCommands();
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			// System.out.println("Select finished");
		}
	}

	/**
	 * The selector check what to do by looking at the SelectionKey
	 * @param key
	 */
	private void treatKey(SelectionKey key) {
		//Helpers.printSelectedKey(key); // for debug
		System.out.println("keys");
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
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
				var c = (Context) key.attachment();
				if (c.disconnectedAddress != null) {
					removeIfClosedTable(c.disconnectedAddress);
					// removeIfClosedTable(c);
				}
				removeIfClosed();
				printConnexions();
			}
		} catch (IOException e) {
			logger.info("Connection closed with client due to IOException");
			silentlyClose(key);
			removeIfClosed();
			try {
				removeIfClosedTable((InetSocketAddress) ((Context) key.attachment()).getChannel().getRemoteAddress());
			} catch (IOException e1) {

			}
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
		SocketChannel nouvFils = ssc.accept();

		if (ssc == null) {
			logger.info("selector gave bad hint");
			return;
		}
		nouvFils.configureBlocking(false);
		var newKey = nouvFils.register(selector, SelectionKey.OP_READ);
		var context = new Application.Context(newKey, this);
		newKey.attach(context);

		connexions.add(context);
		// table.updateRouteTable(context, context);
		consoleTest(key);
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
		daronContext = new Application.Context(key, this);
		key.attach(daronContext);
		connexions.add(daronContext);
		var con = (Context) key.attachment();
		if (con.closed) {
			Thread.currentThread().interrupt();
		}
		key.interestOps(SelectionKey.OP_READ);
		consoleTest(key);
		// daronContext.updateInterestOps();
		try {
			Thread.sleep(100);
			daronContext.updateInterestOps();
		} catch (InterruptedException e) {
			logger.info("Interrupted at connexion");
		}
		// daronContext.updateInterestOps();
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
	@SuppressWarnings({ "preview", "static-access" })
	private void consoleTest(SelectionKey key) {
		Thread.ofPlatform().daemon().start(() -> {
//			try (var scanner = new Scanner(System.in)) {
//				while (scanner.hasNextLine()) {
//					var msg = scanner.nextLine();
//					if (msg.equals("DISCONNECT")) {
//						InetSocketAddress truc = new InetSocketAddress("localhost",7777);
//						
//						
//						daronContext.closed=true;
//						silentlyClose(daronContext.key);
//						System.out.println("Silently close");
//						
//					
//						try {
//							scDaron = SocketChannel.open();
//							scDaron.configureBlocking(false);
//							scDaron.register(selector, SelectionKey.OP_CONNECT);
//							scDaron.connect(truc);
//							System.out.println("connexion");
//						} catch (IOException e) {
//							logger.info("Cannot connect to The new Father" + e);
//						}
//						
//					}
//
//					if (msg.equals("TEST")) {
//						System.out.println("---------------------\nTesting Trame RT");
						/*
						 * msg = scanner.nextLine(); var who = Integer.parseInt(msg); Context element =
						 * null; Iterator<Context> it = connexions.iterator(); while(it.hasNext() && who
						 * != 0){ element = it.next(); System.out.println("trux"); who--; }
						 */
//						if(isroot){
////							try {
//								
//								//Thread.currentThread().sleep(5000);
//								var truc = new TrameFirstRoot(6);
//								for(var e : connexions){
//									//System.out.println(e.scContext);
//									e.queueTrame(truc);
//								}
//								
//								selector.wakeup();
//								
//								
//								
////								System.out.println("root -> " + table);
////								Thread.currentThread().sleep(5000);
////								var doa = new DataOneAddress(10,localInet);
////								var trme = new TramePingEnvoi(doa);
////								for(var e : connexions){
////									System.out.println(e.scContext);
////									e.queueTrame(trme);
////								}
////								selector.wakeup();
////								Thread.currentThread().sleep(10000);
////								selector.wakeup();
//								
////							} catch (InterruptedException e) {
////								logger.info("Interruption at Console Test");
////							}
//							
//						}
//						else {
						
							
							
									var doa = new DataALotAddress(7,new ArrayList<InetSocketAddress>(Arrays.asList(localInet)));
									var trm = new TrameFirstLeaf(doa);
									if(!isroot) {
										daronContext.queueTrame(trm);
										selector.wakeup();
										System.out.println("ENVOI");
									}
									
//							try{
//							Thread.currentThread().sleep(15000);
//							System.out.println("fu -> " + table);
//						}catch(InterruptedException e){
//							e.printStackTrace();
//						}
//						}
//						if(localInet.equals(truc3)) {
//							try {
//								Thread.sleep(15000);
//								var dr = new DataResponse(11,localInet,localInet,false);
//								var trm = new TramePingReponse(dr);
//								daronContext.queueTrame(trm);
//								selector.wakeup();
//								
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//							
//							}
//						
//						var truc = new TrameFirstRoot(6);
//						
						
//						
//						DataOneAddress machin = new DataOneAddress(10, localInet);
//						TramePingEnvoi truc = new TramePingEnvoi(machin);
//						daronContext.queueTrame(truc);
						 
//						
						
						//var tmp = (Context) key.attachment();
						//tmp.processOut(truc);
						//tmp.updateInterestOps();

//						logger.info("Console thread stopping ");
//			}
//			}
		});			
	}

	/**
	 * 
	 * Remove the closed connexion from the hashTable that is the routeTable
	 */
	private void removeIfClosedTable(InetSocketAddress address) {
//		System.out.println(address);
		table.deleteRouteTable(address);
	}

	/**
	 * Remove the closed connexion from the HashSet
	 */
	private void removeIfClosed() {
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
		System.out.println("\n-----RouteTable------");
		System.out.println(table + "\n\n\n\n");
	}




	/**
	 * Check and work on the frame from what op code we got
	 * 
	 * @param op
	 * @param buf
	 * @throws IOException
	 */
	void analyseur(Trame tramez) throws IOException {
		Objects.requireNonNull(tramez);
		System.out.println(tramez.getOp());
		switch (tramez.getOp()) {

		case 3 -> {
			var tmp3 = (TrameAnnonceIntentionDeco) tramez;
			var appDeco = tmp3.dda().AddressSrc();
			var daronApp = tmp3.dda().AddressDst();
			
			if(daronApp.equals(localInet)) {
				System.out.println("JE SUIS TON PERE");
				
				System.out.println(localInet +" "+ appDeco);
					System.out.println("aefaefaefaefaef" + appDeco);
					//table.deleteRouteTable(appDeco);
					
					
					var doa = new DataOneAddress(5,appDeco);
//					TrameSuppression supp = new TrameSuppression(doa);
//					broadCastWithoutFrom((InetSocketAddress) recu.getRemoteAddress(), supp);
					
					var dda = new DataDoubleAddress(4,localInet,appDeco);
					var trameConfirmation  = new TramePingConfirmationChangementCo(dda);
					var context = getContextFromSocket(recu);
					//context.queueTrame(trameConfirmation);
			}
			else {
				System.out.println("C'est mon pere"+scDaron.getRemoteAddress());
				if(table.get(appDeco).equals(scDaron.getRemoteAddress())) {
					System.out.println("NONNN C4EST IMPOSSIBLE");
					
					scDaron.close();
					System.out.println("ole");
					scDaron = SocketChannel.open();
					System.out.println("open");


					System.out.println("LETSGO");
					selector = Selector.open();
					System.out.println("LETSGO 2");
//					scDaron = SocketChannel.open();
					System.out.println("LETSGO 3");
					scDaron.configureBlocking(false);
					System.out.println("LETSGO 4");
					scDaron.register(selector, SelectionKey.OP_CONNECT);
					System.out.println("LETSGO 5");
					
					scDaron.connect(daronApp);
					System.out.println("LETSGO 6");
					System.out.println("MON DARON: " + scDaron.getRemoteAddress());
					
//					System.out.println("YO");
//					table = new RouteTable((InetSocketAddress)ssc.getLocalAddress());
//					
//					var doa = new DataOneAddress(5,appDeco);
//					TrameSuppression supp = new TrameSuppression(doa);
//					broadCastWithoutFrom((InetSocketAddress) recu.getRemoteAddress(), supp);
//					
//					var dda = new DataDoubleAddress(4,localInet,appDeco);
//					var trameConfirmation  = new TramePingConfirmationChangementCo(dda);
//					var context = getContextFromSocket(recu);
//					context.queueTrame(trameConfirmation);
				}
			}
			
		}
		case 4 -> {
			var tmp4 = (TramePingConfirmationChangementCo) tramez;
			var addressDeco = tmp4.dda().AddressDst();
			var addressChangement = tmp4.dda().AddressSrc();
			if(addressDeco.equals(localInet)) { //c'est nous qui se barrons
				if(connexions.size() == 1) {
					
//					var doa = new DataOneAddress(5,localInet);
//					TrameSuppression supp = new TrameSuppression(doa);
//					daronContext.queueTrame(supp);
//					selector.wakeup();
//					try {
//						Thread.currentThread().sleep(1000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					silentlyClose(daronContext.key);
					Thread.currentThread().interrupt();
					logger.info("Disconnected Succesfully\n---------------------");
					System.exit(0);
				}
			}
			//verif si tout les confirmation sont la
			//deco
		}
		case 5 -> {
			var tmp5 = (TrameSuppression) tramez;
			var addressDeco = tmp5.doa().Address();
			
			table.deleteRouteTable(addressDeco);
			if(isroot) {
				//var list = new ArrayList<>(reseau);
				var list = table.getAllAddress();
				list.add(localInet);
//				System.out.println("J4ENVOI CETTE LISTE ET JE SUIS LA " + list);
				var ndla = new DataALotAddress(8, list);
				var caca = new TrameFullTree(ndla);
				broadCast(caca);
			}
			//enlever l'app dans la table de routage
		}

		case 7 -> {
//			System.out.println("Celui qui m'a envoye le 7 est : " + recu.getRemoteAddress());
			var tmp7 = (TrameFirstLeaf) tramez;
			var listo = tmp7.dla().list();
//			System.out.println("TAILLE DU PAQUET 7 :" + listo.size());
			var route = listo.get(listo.size()-1);
			for(int i = 0; i != listo.size();i++) {
				if(!listo.get(i).equals(localInet)) {
					table.addToRouteTable(listo.get(i), route);
					if(!reseau.contains(listo.get(i))) {
						reseau.add(listo.get(i));
					}
				}
				
			}
//			System.out.println("LES VALEURS DES ADDRESSES\n" + listo);
			if(!isroot) {//pas root
				if(connexions.size()!=1){
					System.out.println( listo.removeIf(e -> e.equals(localInet)));
					listo.add(localInet);
//					System.out.println("JE VAIS ENVOYER CETTE LISTE BATARD" + listo);
					var ndla = new DataALotAddress(7,listo);
					var trm = new TrameFirstLeaf(ndla);
					daronContext.queueTrame(trm);
				}
				
			}
			else {//root
				var list = new ArrayList<>(reseau);
//				System.out.println(list+"     mon réseau");
				list.add(localInet);
				var ndla = new DataALotAddress(8, list);
				var caca = new TrameFullTree(ndla);
				broadCast(caca);
			}
			selector.wakeup();
		}
		case 8 -> {
			var tmp8 = (TrameFullTree) tramez;
			var listz = tmp8.dla().list();
			table.removeKeyIf(listz);
			for(int i = 0; i != listz.size(); i++){
				if(!listz.get(i).equals(localInet)) {
					table.addToRouteTable(listz.get(i),(InetSocketAddress) recu.getRemoteAddress());
					
					if(reseau.contains(listz.get(i))) {
						reseau.add(listz.get(i));
					}
				}
				
			}
			
			if(!isroot && connexions.size() > 1){
				listz.removeIf(e -> e.equals(localInet));
				listz.add(localInet);
				var ndla = new DataALotAddress(8, listz);
				var trm = new TrameFullTree(ndla);
				broadCastWithoutFrom((InetSocketAddress)scDaron.getRemoteAddress(),trm); //A verifier avec le broadcast
			}

		}

		case 10 -> {
			TramePingEnvoi tmp = (TramePingEnvoi) tramez; // A verif
			var address = tmp.doa().Address();
			if(connexions.size() > 1) {
				broadCastWithoutFrom((InetSocketAddress) recu.getRemoteAddress(),tramez);
				selector.wakeup();
			}
			DataResponse dr;
			
			if(bufferDonnee.position() != 0 || dispo!=null) {
				dispo = address;
				dr = new DataResponse(11,localInet,address,false);
			} else {
				dr = new DataResponse(11,localInet,address,true);
			}
			
			var trm = new TramePingReponse(dr);
			var con = getContextFromSocket(recu);
			con.queueTrame(trm);
			selector.wakeup();
			
			
		}
		case 11 -> {
			var tmp11 = (TramePingReponse) tramez;
			var addressSrc = tmp11.dr().addressSrc();
			var addressDest = tmp11.dr().addressDst();
			var resp = tmp11.dr().boolByte();
			
			if(!addressDest.toString().equals(localInet.toString().replace("localhost", ""))) {
				broadCastWithoutFrom((InetSocketAddress) recu.getRemoteAddress(),tramez);
				
			}else {
				if(resp ==false) {
					if(commande.computeIfPresent(addressSrc, (k,v) -> v = false)==null){
						commande.put(addressSrc,false);
					}
				}
				if(resp == true) {
					if(commande.computeIfPresent(addressSrc, (k,v) -> v = true)==null){
						commande.put(addressSrc,true);
					}
				}
				System.out.println("ICI -------------------------------> \n"+commande);
			}
			
			
		}
//		case 0 -> {
//		
//		}
//		case 1 -> {
//		//DUMP
//		}
//		case 2 -> {
//		//DUMP
//		}
//		case 6 -> {
//			if(connexions.size()!=1) {
//				broadCastWithoutFrom((InetSocketAddress) scDaron.getRemoteAddress(),tramez);
//			}
//			else {
//				var listo = new ArrayList<InetSocketAddress>();
//				listo.add(localInet);
//				var truc = new DataALotAddress(7,listo);
//				TrameFirstLeaf fL = new TrameFirstLeaf(truc);
//				daronContext.queueTrame(fL);
//			}
//			//faire passer la FR à ses gosses ou l'envoyer au daron si feuille
//		}
//		case 9 -> {
//			var tmp9 = (TrameNewLeaf) tramez;
//			var nouvAddress = tmp9.doa().Address();
//			
//			table.addToRouteTable(nouvAddress,(InetSocketAddress) recu.getRemoteAddress());
//			broadCastWithoutFrom((InetSocketAddress) recu.getRemoteAddress(),tramez);
//			System.out.println("MA TABLE" + table);
//			System.out.println("JE BROADCAST LA NEWLEAF à tout le monde sauf " + (InetSocketAddress) recu.getRemoteAddress());
//			sendFullTreeTo(recu);
//		}
//		case 77 ->{
//			try {
//				var tmp77 = (TrameFullDeco) tramez;
//				broadCastWithoutFrom((InetSocketAddress) recu.getRemoteAddress(), tmp77);
//				selector.wakeup();
//			}finally {
//				silentlyClose(daronContext.key);
//				Thread.currentThread().interrupt();
//				logger.info("Disconnected Succesfully\n---------------------");
//				System.exit(0);
//			}
//		}
		default -> {
			return;
		}

		}
	}
	
	
		/**
	 * Get the context of a socketchannel
	 * @param sc
	 * @return
	 */
	public Context getContextFromSocket(SocketChannel sc) {
		for (SelectionKey key : selector.keys()) {
			Context context = (Context) key.attachment();
			if (context != null && context.scContext.equals(sc)) {
				return context;
			}
		}
		return null;
	}
	
	public void sendFullTreeTo(SocketChannel address) {
		var list = new ArrayList<InetSocketAddress>(reseau);
		var ndla = new DataALotAddress(8, list);
		var trm = new TrameFullTree(ndla);
		var fils = getContextFromSocket(address);
		fils.queueTrame(trm);
		selector.wakeup();
	}

	/**
	 * Add in a buffer the inetSocketAddress that was get in the parameters
	 * 
	 * @param inet
	 * @return
	 */
	static ByteBuffer addressTrame(InetSocketAddress inet) {
		ByteBuffer internBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		var ipByte = new byte[8]; // constructiontrame(truc)
		ipByte = inet.getAddress().getAddress();
		internBuffer.put(ipByte);
		internBuffer.putShort((short) inet.getPort());

		return internBuffer.flip();
	}

//	/**
//	 * Deconnect the application
//	 */
//	void deconnexion() {
//		logger.info("---------------------\nDisconnecting the node ...");
//		try {
//			ssc.close();
//			Thread.currentThread().interrupt();
//		} catch (IOException e) {
//			logger.info("Disconnected Succesfully\n---------------------");
//			System.exit(0);
//		}
//		logger.info("Disconnected Succesfully\n---------------------");
//
//	}


	/**
	 * BroadCast the buffer to all connections apart the address that the frame come
	 * from
	 * 
	 * @param address the guy who send it to us
	 * @param buf
	 * @throws IOException
	 */
	void broadCastWithoutFrom(InetSocketAddress address, Trame tramez) throws IOException {
		for (var key : selector.keys()) {
			var context = (Context) key.attachment();
			if (context != null && address != (InetSocketAddress) context.scContext.getRemoteAddress()) {
				System.out.println("J'envoi a " +(InetSocketAddress) context.scContext.getRemoteAddress() );
				context.queueTrame(tramez);
				selector.wakeup();
			}
		}
		
	}
	
	/**
	 * BroadCast the buffer to all connections apart the address that the frame come
	 * from
	 * 
	 * @param address the guy who send it to us
	 * @param buf
	 * @throws IOException
	 */
	void broadCast(Trame tramez) throws IOException {
		for (var key : selector.keys()) {
			var context = (Context) key.attachment();
			if (context != null) {
				context.queueTrame(tramez);
				selector.wakeup();
			}
		}
	}
	
	
	/*
	 * Show the usage of DISCONNECT and START
	 */
	private static void allUsage(){
		System.out.println("Disconnect Usage :");
		System.out.println("DISCONNECT");
		launchUsage();
	}
	
	/**
	 * Show the usage of START
	 */
	private static void launchUsage(){
		System.out.println("Start Usage :");
		System.out.println("START <url-jar> <fully-qualified-name> <start-range> <end-range> <filename>");	
	}
	
	
	/**
	 * Scan prompt in terminal and put it in a queue
	 */
	private void consoleRun() {
		try {
			try(var scanner = new Scanner(System.in)){
				while(scanner.hasNextLine()){
					var msg = scanner.nextLine();
					sendCommands(msg);
				}
			}
			logger.info("Console thread has stopped");
		} catch(InterruptedException e){
			logger.info("Console thread interrupted");
		}
	}
	
	/**
	 * Put the message in a queue
	 * @param msg
	 * @throws InterruptedException
	 */
	private void sendCommands(String msg) throws InterruptedException{
		if(msg == null) {
			return;
		}
		commandQueue.add(msg);
		selector.wakeup();
	}
	
	/**
	 * Process if the message is right or not
	 */
	private void processCommands() {
		if(commandQueue.isEmpty()) {
			return;
		}
		var commands = commandQueue.poll();
		if(commands.equals("DISCONNECT")) {
			if(isroot) {
				logger.info("---------------------\nDisconnecting the node ...");
				
//				var trameFullDeco = new TrameFullDeco(77);
//				try {
//					broadCast(trameFullDeco);
//					System.out.println("DISCONNECTING ALL NODES");
//				}catch (IOException e) {
//					logger.info("Deco IOException");
//				}finally {
				Thread.currentThread().interrupt();
				logger.info("Disconnected Succesfully\n---------------------");
				System.exit(0);
//				}
			}
			else {
				try {
					var ddl = new DataDoubleAddress(3,localInet,(InetSocketAddress) scDaron.getRemoteAddress());
					var tame = new TrameAnnonceIntentionDeco(ddl);
					logger.info("---------------------\nDisconnecting the node ...");
					if(connexions.size()==1) { //LEAF
						System.out.println("Deco SOLO");
						daronContext.queueTrame(tame);
						selector.wakeup();
					}
					else {
						broadCast(tame);
						selector.wakeup();
					}
				} catch (IOException e) {
					logger.info("ProcessCommand IOException");
				}
			}
				
		}
		else if(commands.startsWith("START")) {
			var lst = Arrays.asList(commands.split(" ")) ;
			if(lst.size()!=6) {
				launchUsage();
				return;
			}
			System.out.println(lst);
			try{
				if(urlJar==null && fullQualifiedName==null && fileName == null){
					urlJar = lst.get(1);
					fullQualifiedName = lst.get(2);
					fileName = lst.get(5);
					long start = Long.parseLong(lst.get(3));
					long end = Long.parseLong(lst.get(4));
					if(end < start){
						logger.warning("End Value must be Greater than start Value");
						return;
					}
					startValue = start;
					endValue = end;
				}

				
			}catch(NumberFormatException e){
				logger.info(" WRONG START");
				launchUsage();
			}
		}
		else {
			allUsage();
		}
	}
	
	
	
	
	void splitData(String jar,String qualifiedName,long start,long end,String fileName,int divide){
		var list = new ArrayList<ArrayList<Long>>();
		long divided = (end - start)/divide;
		long i = start;
		
		long subStart=0;
		long subEnd=0;
		while(i < end){
			subStart = i;
			i+= divided;
			if(i >= end){
				i = end;
				subEnd = i;
			}
			else{
				subEnd = i-1;
			}
			list.add(new ArrayList<Long>(Arrays.asList(subStart,subEnd)));
		}
	}
//	
//	/**
//	 * If all the argument of START is good put all the element in a buffer
//	 * @param jar
//	 * @param qualifiedName
//	 * @param start
//	 * @param end
//	 * @param fileName
//	 */
//	void putInData(String jar,String qualifiedName,long start,long end,String fileName) {
//		var charset = StandardCharsets.UTF_8;
//		
//		var bJar = charset.encode(jar);
//		bufferDonnee.put(bJar);
//		
//		var bQN = charset.encode(qualifiedName);
//		bufferDonnee.put(bQN);
//		
//		bufferDonnee.putLong(start);
//		bufferDonnee.putLong(end);
//		
//		var bFN = charset.encode(fileName);
//		bufferDonnee.put(bFN);
//		
//	}
 
}
