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
import java.util.Scanner;
import java.util.logging.Logger;

public class Application {
	
	
	 static private class Context {
		 
		 private final SelectionKey key;
	     private final SocketChannel scContext;
		 private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
	     private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
	     private final Application server;
	     private final boolean closed = false;
	     
	     private Context(SelectionKey key,Application server){
			 this.key = key;
			 this.scContext = (SocketChannel) key.channel();
			 this.server = server;
			 
		 }
	     
	     private SocketChannel getChannel() {
	    	 return scContext;
	     }
	     
//	     private void updateInterestOps() {
//	            var ops = 0;
//	            if (!closed && bufferOut.hasRemaining()) {
//	                ops |= SelectionKey.OP_READ;
//	            }
//	            if (bufferOut.position() != 0){
//	                ops |= SelectionKey.OP_WRITE;
//	            }
//	            if (ops == 0 && closed) {
//	                silentlyClose(key);
//	                return;
//	            }
//	            key.interestOps(ops);
//	        }
	     
//	     private void silentlyClose(SelectionKey key) {
//	 		Channel sc = (Channel) key.channel();
//	 		try {
//	 			sc.close();
//	 		} catch (IOException e) {
//	 			// ignore exception
//	 		}
//	 	}
	     
	     public void doRead() throws IOException {
	 		scContext.read(bufferIn);
	 		bufferIn.flip();
	 		System.out.println(StandardCharsets.UTF_8.decode(bufferIn));
	 	}
	     
	 }
	
	
	//private final SelectionKey key;
	
	private final ServerSocketChannel sc;
	private final SocketChannel scDaron;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private final Selector selector;
	private boolean isroot;
	
	static private final int BUFFER_SIZE = 1024;
	
	public Application(String host,int port) throws IOException { //root
		isroot = true;
		sc = ServerSocketChannel.open();
		scDaron = null;
		sc.bind(new InetSocketAddress(host,port));
		selector = Selector.open();
	}
	
	public Application(String host,int port, InetSocketAddress fatherAddress) throws IOException { // Connecting to father
		isroot = false;
		sc = ServerSocketChannel.open();
		sc.bind(new InetSocketAddress(host,port));
		selector = Selector.open();
		scDaron = SocketChannel.open();
		scDaron.configureBlocking(false);
		scDaron.register(selector, SelectionKey.OP_CONNECT);
		scDaron.connect(fatherAddress);
	}

	
	public void launch() throws IOException {
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            //Helpers.printKeys(selector); // for debug
            //System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
            //System.out.println("Select finished");
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
            //    ((Context) key.attachment()).doWrite();
            System.out.println("A modi");
            }
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.info("Connection closed with client due to IOException");
            silentlyClose(key);
        }
    }
    
    private void doAccept(SelectionKey key) throws IOException {
      	System.out.println("enter do accept");
        SocketChannel nouvFils = sc.accept();
        if(sc == null) {
            logger.info("selector gave bad hint");
            return;
        }
        System.out.println("caca1");
        nouvFils.configureBlocking(false);
        var newKey = nouvFils.register(selector, SelectionKey.OP_READ);
        System.out.println("caca2");
        var context = new Application.Context(newKey,this);
        System.out.println("caca3");
        newKey.attach(context);
        System.out.println(context.getChannel());
    }
    
    private void doConnect(SelectionKey key){
		try {
			if(!scDaron.finishConnect()){
				return;
			}

		} catch (IOException e) {
			e.getCause();
		}
		System.out.println("CONNECTED");
		System.out.println(scDaron);
        key.attach(new Context(key,this));	//A verif
		consoleTest(key);
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
    	Thread.ofPlatform().start(()->{
    		try {
                try (var scanner = new Scanner(System.in)) {
                    while (scanner.hasNextLine()) {
                       var msg = scanner.nextLine();
//                       if(msg.equals("Disconnect")) {
//                    	   System.out.println("Disconnecting the node");
//                    	   return;
//                       }
                       var buf = ByteBuffer.allocate(msg.length());
                       buf.put(Charset.forName("UTF-8").encode(msg));
                       var c = (Context) key.attachment();
                       SocketChannel a = c.getChannel();
                       a.write(buf.flip());
                    }
                }
                logger.info("Console thread stopping");
            } catch (IOException e) {
            	logger.info("IOE");
            }
      });
        
    }
	
	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Application host port adress");
		System.out.println(" - Root Mode - ");
		System.out.println("Application host port");
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length < 2 || args.length > 5) {
			usage();
			return;
		}

		if(args.length >=2){
			String host = args[0];
			int port = Integer.valueOf(args[1]);
			if(args.length == 4){
				String fatherHost = args[2];
				int fatherPort = Integer.valueOf(args[3]);	
				new Application(host,port,new InetSocketAddress(fatherHost,fatherPort)).launch(); //normal
			}
			else{
				new Application(host, port).launch();// root
			}
		}
		//appli localhost 5555  localhost 6666
		
		
		
	} 
}

