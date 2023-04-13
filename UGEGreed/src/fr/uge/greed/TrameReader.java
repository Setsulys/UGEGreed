package fr.uge.greed;

import java.lang.Thread.State;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import fr.uge.greed.reader.*;
import fr.uge.greed.data.*;

public class TrameReader implements Reader<InetSocketAddress>{
	
	private enum State{
		DONE,WAITING,ERROR
	}
	
	private LotAddressReader lotAddReader = new LotAddressReader();
	private DoubleAddressReader doubleAddReader = new DoubleAddressReader();
	private DataDoubleAddress dataDoubleAddress = null;
	private DataOneAddress dataOneAddress = null;
	private int ipType;
	private State state = State.WAITING;
	private final IntReader intReader = new IntReader();
	private final AddressReader addReader = new AddressReader();
	
	
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		var readerState = intReader.process(bb);
		if(readerState == ProcessStatus.DONE){
			var op = intReader.get();
			intReader.reset();
			switch(op){
				case 0://Demande de connexion_________DUMP
					System.out.println("hellow");
				case 1://Acceptation de connexion_______DUMP
					System.out.println("hellow");
				case 2://Demande de reconnexion_______DUMP
					System.out.println("hellow");
				case 3://Annonce intention de deconnexion
					var doubleAddReaderState = doubleAddReader.process(bb);
					if(doubleAddReaderState == ProcessStatus.DONE){
						var doubleadd = doubleAddReader.get();
						dataDoubleAddress = new DataDoubleAddress(op.intValue(),doubleadd.addressSource(),doubleadd.addressDestination());
						
					}
					else{
						return doubleAddReaderState;
					}
				case 4://Ping de confirmation de changement de connexion
					var doubleAddReaderStatePC = doubleAddReader.process(bb);
					if(doubleAddReaderStatePC == ProcessStatus.DONE){
						var doubleadd = doubleAddReader.get();
						dataDoubleAddress = new DataDoubleAddress(op.intValue(),doubleadd.addressSource(),doubleadd.addressDestination());
						
					}
					else{
						return doubleAddReaderStatePC;
					}
				case 5://Trame suppression d'application'
					var addReaderState = addReader.process(bb);
					if(addReaderState == ProcessStatus.DONE){
						var address = addReader.get();
						dataOneAddress = new DataOneAddress(op,address);
						//reset addReader
					}
					else{
						return addReaderState;
					}
					
					
				case 6://Trame First ROOT
					//ONLY OP 
					
				case 7://Trame Firstt LEAF
					var lotAddReaderState = lotAddReader.process(bb);
					if(lotAddReaderState == ProcessStatus.DONE){
						var lot = lotAddReader.get();
						
					}
				case 8://Trame Full TREE
					System.out.println("hellow");
				case 9://Trame New LEAF
					var addReaderStateNL = addReader.process(bb);
					if(addReaderStateNL == ProcessStatus.DONE){
						var address = addReader.get();
						dataOneAddress = new DataOneAddress(op,address);
						//reset addReader
					}
					else{
						return addReaderStateNL;
					}
				

				case 10://Trame ping d'envoie'
					var addReaderStatePE = addReader.process(bb);
					if(addReaderStatePE == ProcessStatus.DONE){
						var address = addReader.get();
						dataOneAddress = new DataOneAddress(op,address);
						//reset addReader
					}
					else{
						return addReaderStatePE;
					}
					
				case 11://Trame ping reponse
					System.out.println("hellow");
				case 12://envoi de donnee à traiter
					System.out.println("hellow");
				case 13://envoi de donnee deco
					System.out.println("hellow");
				case 14://envoi de donnee traitee
					System.out.println("hellow");

					
			}
			
		} 
		else{
			return readerState;
		}
		state = state.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public InetSocketAddress get() {
		if(state == State.DONE) {
			return null;
		}
		throw new IllegalStateException();
	}

	@Override
	public void reset() {
		state = State.WAITING;
		
		intReader.reset();
		
	}
	

	
}
