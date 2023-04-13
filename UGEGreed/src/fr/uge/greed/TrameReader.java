package fr.uge.greed;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import fr.uge.greed.data.*;
import fr.uge.greed.reader.*;


public class TrameReader implements Reader<InetSocketAddress>{
	
	private enum State{
		DONE,WAITING,ERROR
	}
	

	private DataDoubleAddress dataDoubleAddress = null;
	private DataOneAddress dataOneAddress = null;
	private DataResponse dataResponse = null;
	private ArrayList<InetSocketAddress> list = null;
	private int ipType;
	private State state = State.WAITING;
		private LotAddressReader lotAddReader = new LotAddressReader();
	private DoubleAddressReader doubleAddReader = new DoubleAddressReader();
	private final IntReader intReader = new IntReader();
	private final AddressReader addReader = new AddressReader();
	private final ResponseReader responseReader = new ResponseReader();
	
	
	
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
						list = lotAddReader.get();
						
					}
					else{
						return lotAddReaderState;
					}
				case 8://Trame Full TREE
					var lotAddReaderStateFT = lotAddReader.process(bb);
					if(lotAddReaderStateFT == ProcessStatus.DONE){
						list = lotAddReader.get();
						
					}
					else{
						return lotAddReaderStateFT;
					}
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
					var responseReaderState = responseReader.process(bb);
					if(responseReaderState == ProcessStatus.DONE) {
						var resp = responseReader.get();
						dataResponse = new DataResponse(op,resp.addressSrc(),resp.addressDst(),resp.boolbyte());
					}
					else{
						return responseReaderState;
					}
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
