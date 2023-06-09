package fr.uge.greed.reader;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class LotAddressReader implements Reader<ArrayList<InetSocketAddress>>{
	private enum State{
		DONE,WAITING,ERROR
	}
	private State state = State.WAITING;
	private final AddressReader reader = new AddressReader();
	private final IntReader intReader = new IntReader();
	private  int nbAddress;
	private ArrayList<InetSocketAddress> list = new ArrayList<>();
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		bb.flip();
		var readerState = intReader.process(bb);
		if(readerState == ProcessStatus.DONE) { // On recupere le nombre d'addresse que la trame possede
			nbAddress = intReader.get();
			intReader.reset();

			var nb = 0;
			while(nb < nbAddress) {//On recupere chaque addresse qu'on met dans une liste
				readerState = reader.process(bb);
				if(readerState == ProcessStatus.DONE) {
					list.add(reader.get());
					reader.reset();
				}
				else {
					return readerState;
				}
				nb++;
				
			}
			
		}
		else {
			return readerState;
		}
		list = list.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
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
		list = new ArrayList<>();
		reader.reset();
		intReader.reset();
	}
	
}
