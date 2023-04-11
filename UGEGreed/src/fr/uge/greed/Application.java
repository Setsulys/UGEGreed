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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

public class Application {

	static private class Context {

		private final SelectionKey key;
		private final SocketChannel scContext;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final OneAddressReader oaread= new OneAddressReader();
		private final Application server;
		private boolean closed = false;
		private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();
		private final IntReader intread = new IntReader();
		
		private InetSocketAddress disconnectedAddress;

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
	            if (!closed && bufferOut.hasRemaining()) {
	                ops |= SelectionKey.OP_READ;
	            }
	            if (bufferOut.position() != 0){
	                ops |= SelectionKey.OP_WRITE;
	            }
	            if (ops == 0 && closed) {
	               	
	                return;
	            }
			if (closed) {

			}
			key.interestOps(ops);
		}
		
		private void processIn() throws IOException {
            for (;;) {
                Reader.ProcessStatus status = intread.process(bufferIn);
                switch (status) {
                    case DONE:
                    	int op = intread.get();
                        Reader.ProcessStatus status2 = oaread.process(bufferIn);
                        	switch(status2) {
                        	case DONE:
                        		server.analyseur(Trames.values()[op], bufferIn, disconnectedAddress);
                        		bufferIn.clear();
                        	case REFILL:
                        		return;
                        	case ERROR:
                        		silentlyClose(key);
                        	}
                        break;
                    	
                    case REFILL:
                        return;
                    case ERROR:
                        silentlyClose(key);
                        return;
                }
            }
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
				
				disconnectedAddress = (InetSocketAddress) scContext.getRemoteAddress();
				System.out.println( "Connexion closed >>>>>>>>>>>>>>>>>>>>>>>>>>> " + disconnectedAddress +"\n");
				silentlyClose(key);
				return;
			}
			// scContext.read(bufferIn);
			bufferIn.flip();
			processIn();
			System.out.println(StandardCharsets.UTF_8.decode(bufferIn));
			bufferIn.clear();

		}
		
		public InetSocketAddress disconnectedAddress(){
			return disconnectedAddress;
		}
		
		public void doWrite() throws IOException{
			bufferOut.flip();
			scContext.write(bufferOut);
			bufferOut.compact();
			updateInterestOps();
		}
		
		
		public void queueMessage(ByteBuffer buffer) {
			buffer.flip();
			queue.add(buffer);
			buffer.flip();
			if(bufferOut.hasRemaining()) {
				//processOut();
			}
			updateInterestOps();
		}
		
		public void processOutTest(InetSocketAddress address) {
			if(bufferOut.remaining() < BUFFER_SIZE) {
				return;
			}
			//ceci est un test
			bufferOut.put(envoiFirstLEAF());
		}

	}

	// private final SelectionKey key;

	private final ServerSocketChannel sc;
	private static InetSocketAddress localInet;
	private final SocketChannel scDaron;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private final Selector selector;
	private boolean isroot;
	private final HashSet<Context> connexions = new HashSet<>();
	private RouteTable table = new RouteTable();
	private ByteBuffer bufferDonnee = ByteBuffer.allocate(BUFFER_SIZE);
	private ByteBuffer bufferDonneeTraitee = ByteBuffer.allocate(BUFFER_SIZE);
	private ByteBuffer bufferDonneeDeco = ByteBuffer.allocate(BUFFER_SIZE);
	private ByteBuffer bufferEnvoie = ByteBuffer.allocate(BUFFER_SIZE);
	private InetSocketAddress dataFrom =null;
	private ArrayList<InetSocketAddress> workers = new ArrayList<>();
	private InetSocketAddress beauDaron = null;

	static private final int BUFFER_SIZE = 4096;

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
		localInet = new InetSocketAddress(host, port);
		sc.bind(localInet);
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
		localInet = new InetSocketAddress(host, port);
		sc.bind(localInet);
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
				var c = (Context) key.attachment();
				if(c.disconnectedAddress != null){
					removeIfClosedTable(c.disconnectedAddress);
				}
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
		Thread.ofPlatform().daemon().start(() -> {
			try {
				try (var scanner = new Scanner(System.in)) {
					while (scanner.hasNextLine()) {
						var msg = scanner.nextLine();
						if (msg.equals("DISCONNECT")) {
							System.out.println("---------------------\nDisconnecting the node ...");
							var con = (Context) key.attachment();
							con.closed = true;
							silentlyClose(key);
							Thread.currentThread().interrupt();
						}
						if (msg.equals("Test")) {
							System.out.println("---------------------\nWhich One to send test ?");
							msg = scanner.nextLine();
							var who = Integer.parseInt(msg);
							Context element = null;
							Iterator<Context> it = connexions.iterator();
							while(it.hasNext() && who != 0){
								element = it.next();
								System.out.println("trux");
								who--;
							}
							element.processOutTest((InetSocketAddress) element.scContext.getRemoteAddress());
							
						}
						
						
						
						
//						for (var e : connexions) {
//							if (e.scContext.equals(scDaron)) {
//								System.out.println("Connected To : " + e.scContext);
//							} else {
//								System.out.println("Conneted from : " + e.scContext);
//							}
//						}
						
						var buf = ByteBuffer.allocate(msg.length());
						buf.put(Charset.forName("UTF-8").encode(msg));
						var c = (Context) key.attachment();
						SocketChannel a = c.getChannel();
						a.write(buf.flip());
					}
				}
				logger.info("Console thread stopping ");
			} catch (IOException e) {
				logger.info("Disconnected Succesfully\n---------------------");
				System.exit(0);
			}
		});

	}
	
	

	/**
	* Remove the closed connexion from the hashTable that is the routeTable
	*/
	private void removeIfClosedTable(InetSocketAddress address){
			table.deleteRouteTable(address);
	}
	/**
	 * Remove the closed connexion from the HashSet
	 */
	private void removeIfClosed()  {
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
		System.out.println(table+"\n\n\n\n");
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
		bufferDonnee.flip();
		var op = new IntReader();
		op.process(this.bufferDonnee);
		return op.get();
	}

	/**
	 * Check and work on the frame from what op code we got
	 * @param op
	 * @param buf
	 * @throws IOException 
	 */
	void analyseur(Trames op, ByteBuffer buf,InetSocketAddress address) throws IOException {
		switch(op) {
		/*Une fonction pour chaque trame*/
		case PINGENVOI -> {
			//getAddressFromBuffer(buf);
			renvoiePingEnvoi(address);
			
			
			receivePingEnvoiAndSendPingReponse(buf);
		}
		case ACCEPTCO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case CONFIRMATIONCHANGEMENTCO -> {
			recoitConfirmationChangementConnexion(buf);
		}
		case DEMANDECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DEMANDERECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DONNEEATRAITER -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DONNEEDECO -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case DONNEETRAITEES -> throw new UnsupportedOperationException("Unimplemented case: " + op);
		case FIRSTLEAF -> {
			if(isroot) { //on est la root, on update notre table de rootage
				decomposeFirstLEAF(buf);
				
			}
			else { //on est un pion dans la matrix
				renvoiFirstLEAF(buf);
			}
		}
		case FIRSTROOT -> {
			if(connexions.size()>1) { //Pas une feuille, on renvoit
				envoiFirstROOT();
			}
			else { //Une feuille, on envoit La trame First LEAF à son père
				envoiFirstLEAF();
			}
		}
		case FULLTREE -> {
			recoiFullTREE(buf);
		}
		case INTENTIONDECO -> {
			recoitIntentionDeco(buf);
		}
		case NEWLEAF -> {
			recoitNewLEAF(buf);
		}
		case PINGREP -> {
			recoitPingReponse(buf);
		}
		case SUPPRESSION -> {
			recoitSuppression(buf);
		}
		default -> throw new IllegalArgumentException("Unexpected value: " + op);
		
		}
	}
	
	/**
	 * Add in a buffer the inetSocketAddress that was get in the parameters
	 * @param inet
	 * @return
	 */
	static ByteBuffer addressTrame(InetSocketAddress inet) {
		ByteBuffer internBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		var ipByte = new byte[8];			//constructiontrame(truc)
		ipByte = inet.getAddress().getAddress();
		internBuffer.put(ipByte);
		internBuffer.putShort((short) inet.getPort());
		
		return internBuffer.flip();
	}
	
	ByteBuffer recoitDonneeDeco(ByteBuffer buf) {
		//TODO
		return null;
	}
	/**
	 * Receive the buffer that say a proximity connexion is starting to deconnect
	 * After the frame received it check if it is connected to the to deconnect application and send a frame to the connexion saying that it accept the deconnexion
	 * @param buf
	 * @throws IOException
	 */
	void recoitIntentionDeco(ByteBuffer buf) throws IOException {
		var address = getAddressFromBuffer(buf);
		if(address == (InetSocketAddress) scDaron.getLocalAddress()) {
			return;
		}
		address = getAddressFromBuffer(buf);
		beauDaron = (InetSocketAddress) scDaron.getLocalAddress();
		scDaron.connect(address);
		for (var e : table) {
			if(table.get(e) == beauDaron){
				table.updateRouteTable((InetSocketAddress) scDaron.getRemoteAddress(),address);
			}
		}
		
		envoiConfirmationChangementConnexion();
	}
	
	void envoiConfirmationChangementConnexion() {
		var buf = ByteBuffer.allocate(BUFFER_SIZE);
		buf.putInt(4);
		buf.put(addressTrame(localInet));
		buf.put(addressTrame(beauDaron));
		buf.flip();
		//TODO ENVOI DARON
	}
	
	void recoitConfirmationChangementConnexion(ByteBuffer buf) throws IOException {
		var address = getAddressFromBuffer(buf);
		var address2 = getAddressFromBuffer(buf);
		if(address2 != localInet) {
			//TODO ENVOI ADDRESS2
			return;
		}
		for(var e: connexions) {
			if(e.scContext.getRemoteAddress() == address) {
				connexions.remove(e);
			}
		}
		if(connexions.size() == 1) {
			envoiSuppression();
			deconnexion();
		}
		
	}
	
	void envoiSuppression() {
		var buf = ByteBuffer.allocate(BUFFER_SIZE);
		buf.putInt(5);
		buf.put(addressTrame(localInet));
		//TODO ENVOI DARON
	}
	
	void deconnexion() {
		System.out.println("---------------------\nDisconnecting the node ...");
		try {
			sc.close();
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			logger.info("Disconnected Succesfully\n---------------------");
			System.exit(0);
		}
		
	}
	
	void recoitSuppression(ByteBuffer buf) throws IOException {
		
		var address = getAddressFromBuffer(buf);
		table.deleteRouteTable(address);
		buf.position(0);
		broadCast(dataFrom,buf);
	}
	
	
	
	
	/**
	 * Create a frame that contain only an integer
	 * @param op
	 * @return
	 */
	ByteBuffer TrameOp(int op) {
		var buf = ByteBuffer.allocate(BUFFER_SIZE);
		buf.putInt(op);
		return buf;
	}
	
	/**
	 * Redirect the frame to every node that are not his connexion father
	 */
	void envoiFirstROOT() {
		for (var e : connexions) {
			if(e.scContext == scDaron) {
				continue;
			}
			var buf = TrameOp(6);
			bufferEnvoie.put(buf);
			//ENVOYER WAKEUP
		}
	}
	/**
	 * When we are on a leaf of the application it create a new Buffer with op code 7 and put 
	 */
	static ByteBuffer envoiFirstLEAF(){
		ByteBuffer connex = ByteBuffer.allocate(BUFFER_SIZE);
		connex.putInt(7);
		connex.putInt(1);
		connex.put(addressTrame(localInet));
		return connex;
	}
	
	
	/**
	 * Deconstruct the FirstLEAF frame to put all address in the RouteTable
	 * @param buf
	 */
	void decomposeFirstLEAF(ByteBuffer buf) {
		int nb = buf.getInt();
		int oldPosition = buf.position();
		int lastAddress = nb-1 * (8+Short.BYTES);
		buf.position(lastAddress);
		var routeAddress = getAddressFromBuffer(buf);
		buf.position(oldPosition);
		for(int i = 1 ;i < nb;i++) {
			var address = getAddressFromBuffer(buf);
			table.updateRouteTable(address,routeAddress);
		}
		
	}
	
	/**
	 * Fill a buffer with all the keys from the root RouteTable and send to every 
	 */
	void envoiFullTREE(){
		ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
		var ipByte = new byte[8];	
		var allAddress = table.getAllAddress();
		int nbAddress = allAddress.size();
		
		buf.putInt(8);
		buf.putInt(nbAddress);
		
		for(var address : allAddress){
			ipByte = address.getAddress().getAddress();
			buf.put(ipByte);
			buf.putShort((short) address.getPort());
		}
	}
	/**
	 * 
	 * @param buf
	 * @throws IOException
	 */
	void recoiFullTREE(ByteBuffer buf) throws IOException {
		int nb = buf.getInt();
		for(int i = 1 ;i < nb;i++) {
			var address = getAddressFromBuffer(buf);
			if(table.get(address)!=null) {
				continue;
			}
			table.updateRouteTable(address,(InetSocketAddress) scDaron.getLocalAddress());	//A voir
		}
	}
	
	/**
	 * flip the buffer received to get the number of nodes and increment it 
	 * at the end we flip the buffer again
	 * @param buf
	 * @return ByteBuffer
	 */
	ByteBuffer incrementNbNodes(ByteBuffer buf){
		buf.flip(); 
		buf.getInt();
		var nb =buf.getInt() +1;
		buf.position(Integer.BYTES);
		buf.putInt(nb);
		buf.position(0);
		buf.flip();
		return buf;
	}
	
	/**
	 * 
	 * @param buf
	 */
	void renvoiFirstLEAF(ByteBuffer buf) {
		var nouvBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		nouvBuffer.put(buf);
		decomposeFirstLEAF(buf);
		buf = incrementNbNodes(buf);

		//ENVOYER nouvBuffer DARON
		
	}
	/**
	 * Le caca a julien
	 * @param buf
	 * @throws IOException 
	 */
	void recoitNewLEAF(ByteBuffer buf) throws IOException {
		buf.position(0);
		broadCast(dataFrom,buf);
		buf.getInt();
		var address = getAddressFromBuffer(buf);
		table.updateRouteTable(address,dataFrom);
	}
	
	void recoitPingReponse(ByteBuffer buf) throws IOException {
		var address1 = getAddressFromBuffer(buf);
		var address = getAddressFromBuffer(buf);
		if(address != localInet){
			buf.position(0);
			broadCast(dataFrom,buf);
			return;
		}
		workers.add(address1);
	}
	
	
	
	
	
	///////////////////////////////////////////////////////////////////////////////REVOIR CETTE FONCTION aussi a ligne  382
	void dataFromGetAddress(ByteBuffer internBuffer){
		this.dataFrom = getAddressFromBuffer(internBuffer);
	}
	
	
	/**
	 * Get The address Of Source for the broadCast 
	 * ATTENTION THIS METHOD HAVE THE BUFFER IN READMODE
	 * @param internBuffer
	 * @return
	 */
	InetSocketAddress getAddressFromBuffer(ByteBuffer internBuffer) {
		
		try {
			//internBuffer.flip();
			if(internBuffer.remaining()<8 * Byte.BYTES + Short.BYTES){
				logger.info("Not enought remaining");
				return null;
			}
			byte[] ipByte = new byte[8]; //et le reste je suis pas sur de ce que ca fait mais ca a l'air de passer dans ma tete faudrait check
			internBuffer.get(ipByte); //recupere l'op dans le vide
			var ipAddress = InetAddress.getByAddress(ipByte); // 
			var port = internBuffer.getShort();
			return new InetSocketAddress(ipAddress,port);
		}catch (IOException e) {
			return null;
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
			bufferEnvoie.clear();
			bufferEnvoie.put(bit);
			bufferEnvoie.put(addressTrame(localInet)); //Addresse Source
			bufferEnvoie.put(addressTrame(socketAddress)); // Adresse ping
			
			//mise de la reponse
			if(bufferDonnee.hasRemaining()){
				bit = 0;//Not Avaliable
			}
			else{
				bit = 1;//Avaliable
			}
			bufferEnvoie.put(bit);
			
			/*___________METHODE WAKE UP ENVOIE PAQUET_________*/
			
			
		} catch (UnknownHostException e) {
			//e.getCause();
		}
		
	}
	
	
	void renvoiePingEnvoi(InetSocketAddress address) {
		bufferEnvoie.position(0);
		
	}
	
	/**
	 * BroadCast the buffer to all connexions apart the address that the frame come from
	 * @param address the guy who send it to us
	 * @param buf
	 * @throws IOException
	 */
	void broadCast(InetSocketAddress address,ByteBuffer buf) throws IOException{
		for(var key : selector.keys()){
			var context = (Context) key.attachment();
			if(context != null && address!=(InetSocketAddress) context.scContext.getRemoteAddress()){
			//TODO	
				
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
