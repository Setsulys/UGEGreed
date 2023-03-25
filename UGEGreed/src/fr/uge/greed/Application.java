package fr.uge.greed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import javax.naming.Context;

public class Application {
	//private final SelectionKey key;
	private final ServerSocketChannel sc;
	private final SocketChannel sca;
	private final Logger logger = Logger.getLogger(Application.class.getName());
	private final Selector selector;
	
	public Application(String host,int port) throws IOException { //root
		sc = ServerSocketChannel.open();
		sca = null;
		sc.bind(new InetSocketAddress(host,port));
		selector = Selector.open();
	}
	
	public Application(String host,int port, InetSocketAddress fatherAddress) throws IOException { //root
		sc = ServerSocketChannel.open();
		sc.bind(new InetSocketAddress(host,port));
		selector = Selector.open();
		sca = SocketChannel.open();
		sca.configureBlocking(false);
		sca.register(selector, SelectionKey.OP_CONNECT);
		sca.connect(fatherAddress);
	}

	
	public void launch() throws IOException {
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_ACCEPT);
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
        try {
            if (key.isValid() && key.isWritable()) {
                ((Context) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Connection closed with client due to IOException", e);
            silentlyClose(key);
        }
    }
    
    private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}
    
    private void doAccept(SelectionKey key) throws IOException {
        // TODO
        SocketChannel sc = serverSocketChannel.accept();
        if(sc == null) {
            logger.info("selector gave bad hint");
            return;
        }
        sc.configureBlocking(false);
        var newKey = sc.register(selector, SelectionKey.OP_READ);
        newKey.attach(new Context(newKey));
    }
	
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
		
		//appli localhost 5555  localhost 6666
		
		
		
	} 
}
