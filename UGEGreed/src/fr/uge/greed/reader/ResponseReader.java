package fr.uge.greed.reader;

import java.nio.ByteBuffer;
import fr.uge.greed.reader.readerrecord.*;

public class ResponseReader implements Reader<Response>{

	private enum State{
		DONE,WAITING,ERROR
	}
	
	private State state = State.WAITING;
	private final DoubleAddressReader dAddressReader = new DoubleAddressReader();
	private final BooleanReader boolReader = new BooleanReader();
	private Response response;
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		// On flip pas car on flip deja dans addressReader qui est dans DoubleAddressReader
		var readerState = dAddressReader.process(bb);
		if(readerState == ProcessStatus.DONE) {
			var doubleAddress = dAddressReader.get();
			dAddressReader.reset();
			var boolReadState = boolReader.process(bb);
			if(boolReadState == ProcessStatus.DONE) {
				var bool = boolReader.get();
				response = new Response(doubleAddress.addressSource(), doubleAddress.addressDestination(), bool);
			}
			else {
				return boolReadState;
			}
		}
		else {
			return readerState;
		}
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public Response get() {
		if(state == State.DONE) {
			return response;
		}
		throw new IllegalStateException();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		state = State.DONE;
		dAddressReader.reset();
		boolReader.reset();
	}
	
}
