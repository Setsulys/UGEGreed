package fr.uge.greed.reader;

import java.nio.ByteBuffer;

public class BooleanReader implements Reader<Boolean>{

	private enum State{
		DONE,WAITING,ERROR
	}
	
	private State state = State.WAITING;
	private final ByteBuffer internalBuffer = ByteBuffer.allocate(Byte.BYTES);
	private boolean value;
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		bb.flip();
		try {
			if(bb.remaining() <= internalBuffer.remaining()) {
				internalBuffer.put(bb);
			}
			else {
				var oldLimit = bb.limit();
				bb.limit(internalBuffer.remaining());
				internalBuffer.put(bb);
				bb.limit(oldLimit);
			}
		}finally {
			bb.compact();
		}
		if(internalBuffer.hasRemaining()) {
			return ProcessStatus.REFILL;
		}
		internalBuffer.flip();
		if(internalBuffer.get() == 0) {
			value = false;
		}
		else {
			value = true;
		}
		
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public Boolean get() {
		if(state != State.DONE) {
			throw new IllegalStateException();
		}
		return value;
	}

	@Override
	public void reset() {
		state = State.WAITING;
		internalBuffer.clear();
		
	}
	
}
