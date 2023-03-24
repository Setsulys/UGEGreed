package fr.uge.greed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class ApplicationClient {
	
	static private final int BUFFER_SIZE = 1024;
	static private Logger logger = Logger.getLogger(ApplicationClient.class.getName());
	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAdress;
	
	
	static private class Context{
		private final SelectionKey key;
		private final SocketChannel sc;
		private boolean closed = false;
		
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
	
		
		private Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
		}
		
		
		private void updateInterestOps(){
			var ops = 0;
			if(!closed && bufferOut.hasRemaining()) {
				ops |= SelectionKey.OP_READ;
			}
			if(bufferOut.position() != 0) {
				ops |= SelectionKey.OP_WRITE;
			}
			if(ops ==0) {
				silentlyClose();
				return;
			}
			key.interestOps();
		}
		
		private void doRead() {
			try {
				if(sc.read(bufferIn) == -1) {
					closed =true;
				}
			}catch(IOException e ) {
				e.getCause();
			}
		}
		
		private void doWrite() {
			try {
				bufferOut.flip();
				sc.write(bufferOut);
				bufferOut.compact();
				updateInterestOps();
			}catch(IOException e) {
				e.getCause();
			}
		}
		
		private void doConnect() {
			try {
				if(sc.finishConnect()) {
					return ; //The selector lied
				}
			} catch (IOException e) {
				e.getCause()
			}
			key.interestOps(SelectionKey.OP_READ);
		}
		
		private void silentlyClose() {
			try {
				sc.close();
			} catch(IOException e) {
				//Ignore Exception
			}
		}
	}
}
