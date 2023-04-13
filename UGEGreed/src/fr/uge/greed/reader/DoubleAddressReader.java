package fr.uge.greed.reader;

import java.nio.ByteBuffer;
import fr.uge.greed.*;
import fr.uge.greed.reader.readerrecord.*;

public class DoubleAddressReader implements Reader<DoubleAddress>{
	private enum State{
		DONE,WAITING, ERROR
	}
	
	private DoubleAddress doubleAddress;
	private State state = State.WAITING;
	private final AddressReader reader = new AddressReader();
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		var readerState = reader.process(bb); //On process la premiere addresse
		if(readerState == ProcessStatus.DONE) {
			var address1 = reader.get();
			reader.reset(); // On reset
			readerState = reader.process(bb); // On process la deuxieme addresse
			if(readerState == ProcessStatus.DONE) {
				var address2 = reader.get();
				doubleAddress = new DoubleAddress(address1, address2);
			}
			else {
				return readerState;
			}
		}
		else {
			return readerState;
		}
		state = State.DONE;
		return ProcessStatus.DONE;
	}
	@Override
	public DoubleAddress get() {
		if(state == State.DONE) {
			return doubleAddress;
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
