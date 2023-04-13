package fr.uge.greed;

import java.nio.ByteBuffer;

import fr.uge.greed.reader.*;
import fr.uge.greed.reader.readerrecord.*;
public class LotAddressReader implements Reader<LotAddress>{
	private enum State{
		DONE,WAITING,ERROR
	}
	private LotAddress lotAddress;
	private State state = State.WAITING;
	private final AddressReader reader = new AddressReader();
	private final IntReader intReader = new IntReader();
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
	}
	@Override
	public LotAddress get() {
		if(state == State.DONE) {
			return lotAddress;
		}
		throw new IllegalStateException();
	}
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		state = State.WAITING;
		reader.reset();
	}
	
}
