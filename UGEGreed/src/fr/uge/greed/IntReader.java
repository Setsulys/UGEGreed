package fr.uge.greed;

import java.nio.ByteBuffer;

public class IntReader implements Reader<Integer>{
	
	private enum State{
		DONE , WAITING , ERROR
	}
	
	private State state = State.WAITING;
	private final ByteBuffer internalBuffer = ByteBuffer.allocate(Integer.BYTES);
	private int value;
	private boolean flag = false;
	
	
	@Override
	public ProcessStatus process(ByteBuffer buffer){
		if(flag) {
			return ProcessStatus.DONE;
		}
		if(state == State.DONE || state == State.ERROR){
			throw new IllegalStateException();
		}
		buffer.flip();
		
		try{
			if(buffer.remaining() <= internalBuffer.remaining()){
				internalBuffer.put(buffer);
				
			}else{
				var oldLimit = buffer.limit();
				buffer.limit(internalBuffer.remaining());
				internalBuffer.put(buffer);
				buffer.limit(oldLimit);
			}
		} finally {
			buffer.compact();
		}
		
		if(internalBuffer.hasRemaining()){
			return ProcessStatus.REFILL;
		}
		
		state = State.DONE;
		internalBuffer.flip();
		value = internalBuffer.getInt();
		flag = true;
		return ProcessStatus.DONE;
	}

	@Override
	public Integer get(){
		if(state != State.DONE){
			throw new IllegalStateException();
		}
		return value;
	}
	
	@Override
	public void reset(){
		state = State.WAITING;
		internalBuffer.clear();
	}
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
}
