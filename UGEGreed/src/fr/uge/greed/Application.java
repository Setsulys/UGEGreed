package fr.uge.greed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
	     
	     private void updateInterestOps() {
	            var ops = 0;
	            if (!closed && bufferOut.hasRemaining()) {
	                ops |= SelectionKey.OP_READ;
	            }
	            if (bufferOut.position() != 0){
	                ops |= SelectionKey.OP_WRITE;
	            }
	            if (ops == 0 && closed) {
	                silentlyClose(key);
	                return;
	            }
	            key.interestOps(ops);
	        }
	     
	     private void silentlyClose(SelectionKey key) {
	 		Channel sc = (Channel) key.channel();
	 		try {
	 			sc.close();
	 		} catch (IOException e) {
	 			// ignore exception
	 		}
	 	}
	     
	     
//	     private void doConnect(){
//	 		try {
//	 			if(!sc.finishConnect()){
//	 				return;
//	 			}
//	 		} catch (IOException e) {
//	 			e.getCause();
//	 		}
//	 		//key.interestOps(SelectionKey.OP_READ);
//	 	} 
	 }
	
	
	//private final SelectionKey key;
	
	private final ServerSocketChannel sc;
	private final SocketChannel sca;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private final Selector selector;
	private boolean isroot;
	
	static private final int BUFFER_SIZE = 1024;
	
	public Application(String host,int port) throws IOException { //root
		isroot = true;
		sc = ServerSocketChannel.open();
		sca = null;
		sc.bind(new InetSocketAddress(host,port));
		sc.configureBlocking(false);
		selector = Selector.open();
		sc.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	public Application(String host,int port, InetSocketAddress fatherAddress) throws IOException { // Connecting to father
		isroot = false;
		sc = ServerSocketChannel.open();
		sc.bind(new InetSocketAddress(host,port));
		selector = Selector.open();
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_ACCEPT);
		
		sca = SocketChannel.open();
		sca.configureBlocking(false);
		sca.register(selector, SelectionKey.OP_CONNECT);
		sca.connect(fatherAddress);
	}

	
	public void launch() throws IOException {
//        sc.configureBlocking(false);
//        sc.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            Helpers.printKeys(selector); // for debug
            System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
            System.out.println("Select finished");
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
       // try {
            if (key.isValid() && key.isWritable()) {
            //    ((Context) key.attachment()).doWrite();
            System.out.println("A modi");
            }
            if (key.isValid() && key.isReadable()) {
            //    ((Context) key.attachment()).doRead();
            	System.out.println("ca");
            }
//        } catch (IOException e) {
//            logger.info("Connection closed with client due to IOException");
//            silentlyClose(key);
//        }
    }
    
    private void doAccept(SelectionKey key) throws IOException {
        SocketChannel nouvFils = sc.accept();
        if(sc == null) {
            logger.info("selector gave bad hint");
            return;
        }
        nouvFils.configureBlocking(false);
        var newKey = sc.register(selector, SelectionKey.OP_READ);
        newKey.attach(new Context(newKey,this));
    }
    
    private void silentlyClose(SelectionKey key) {
 		Channel sc = (Channel) key.channel();
 		try {
 			sc.close();
 		} catch (IOException e) {
 			// ignore exception
 		}
 	}
	
	private static void usage() {
		System.out.println("Usage :");
		System.out.println("Application host port adress");
		System.out.println(" - Root Mode - ");
		System.out.println("Application host port");
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length < 3 || args.length > 5) {
			usage();
			return;
		}

		if(args.length >= 3){
			String host = args[0];
			int port = Integer.valueOf(args[1]);
			if(args.length == 5){
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

