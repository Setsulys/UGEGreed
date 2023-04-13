package fr.uge.greed.reader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.net.InetSocketAddress;

import fr.uge.greed.reader.*;

public class LotAddressReader implements Reader<ArrayList<InetSocketAddress>>{
	private enum State{
		DONE,WAITING,ERROR
	}
	private State state = State.WAITING;
	private final AddressReader reader = new AddressReader();
	private final IntReader intReader = new IntReader();
	private  int nbAddress;
	private final ArrayList<InetSocketAddress> list = new ArrayList<>();
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		//On flip pas car on flip deja dans AddressReader et intReader
		var readerState = intReader.process(bb);
		if(readerState == ProcessStatus.DONE) {
			nbAddress = intReader.get();
			intReader.reset();
			var nb = 0;
			while(nb < nbAddress) {
				readerState = reader.process(bb);
				if(readerState == ProcessStatus.DONE) {
					list.add(reader.get());
				}
				nb++;
			}
		}
		
		state = State.DONE;
		return ProcessStatus.DONE;
	}
	@Override
	public ArrayList<InetSocketAddress> get() {
		if(state == State.DONE) {
			return list;
		}
		throw new IllegalStateException();
	}
	@Override
	public void reset() {
		state = State.WAITING;
		reader.reset();
		intReader.reset();
	}
	
}
