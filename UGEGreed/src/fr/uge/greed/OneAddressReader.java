package fr.uge.greed;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class OneAddressReader implements Reader<InetSocketAddress> {
	private enum State{
		DONE, WAITING, ERROR
	}
	
	
	private InetSocketAddress address;
	//private int op;
	private State state = State.WAITING;
	private int SIZE = 8 * Byte.BYTES + Short.BYTES;
	//private int INT_SIZE = Integer.BYTES;
	private final ByteBuffer bufferAddress = ByteBuffer.allocate(SIZE);
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		bb.flip();
		try{
			if(bb.remaining() != SIZE /*+ INT_SIZE*/){
				state = State.ERROR;
			}
			//op = bb.getInt();
			while(bb.hasRemaining() && bufferAddress.position() < SIZE && bufferAddress.hasRemaining()){
				bufferAddress.put(bb.get());
			}
			if(bufferAddress.position() < SIZE){
				return ProcessStatus.REFILL;
			}

		}finally{
			bb.compact();
		}
		
		try{
			byte[] ipByte = new byte[8];
			bufferAddress.get(ipByte);
			var ipAddress = InetAddress.getByAddress(ipByte);
			var port = bufferAddress.getShort();
			address = new InetSocketAddress(ipAddress,port);
		}catch(IOException e){
			
		}
		
		state = State.DONE;
		return ProcessStatus.DONE;
	}
	
	@Override
	public InetSocketAddress get(){
		if(state ==State.DONE){
			//return new DataOneAddress(op,address);
			return address;
		}
		throw new IllegalStateException();
	}
		
		
	@Override
	public void reset(){
		state = State.WAITING;
		bufferAddress.clear();
	}
}
