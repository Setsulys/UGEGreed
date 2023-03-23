package fr.uge.greed;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class UrlReader implements Reader<String>{
	private enum State{
		DONE, WAITING, ERROR
	}

	private static int BUFFER_SIZE = 1024;
	private final ByteBuffer bufferSize = ByteBuffer.allocate(Long.BYTES);
	private final ByteBuffer bufferUrl = ByteBuffer.allocate(BUFFER_SIZE);
	private String url;
	private State state = State.WAITING;
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		bb.flip();
		try {
			while(bufferSize.hasRemaining() && bb.hasRemaining()) {
				bufferSize.put(bb.get());
			}
			
			if (bufferSize.hasRemaining()) {
				return ProcessStatus.REFILL;
			}
			long size = bufferSize.flip().getLong();
			if(size < 0 || size > BUFFER_SIZE) {
				return ProcessStatus.ERROR;
			}
			while(bb.hasRemaining() && bufferUrl.position() < size && bufferUrl.hasRemaining()) {
				bufferUrl.put(bb.get());
			}
			if(bufferUrl.position() < size) {
				return ProcessStatus.REFILL;
			}
		} finally {
			bb.compact();
		}
		
		url = StandardCharsets.US_ASCII.decode(bufferUrl.flip()).toString();
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public String get() {
		if(state == State.DONE) {
			return url;
		}
		throw new IllegalStateException();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		state = State.WAITING;
		bufferUrl.clear();
		bufferSize.clear();
	}
	
}
