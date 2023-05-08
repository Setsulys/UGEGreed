package fr.uge.greed.reader;

import java.nio.ByteBuffer;

import fr.uge.greed.data.*;
import fr.uge.greed.trame.*;

public class TrameReader implements Reader<Trame> {

	private enum State {
		DONE, WAITING, ERROR
	}

	private DataDoubleAddress dataDoubleAddress = null;
	private DataOneAddress dataOneAddress = null;
	private DataResponse dataResponse = null;
	private DataALotAddress dataALotAddress = null;
	private State state = State.WAITING;
	private LotAddressReader lotAddReader = new LotAddressReader();
	private DoubleAddressReader doubleAddReader = new DoubleAddressReader();
	private final IntReader intReader = new IntReader();
	private final AddressReader addReader = new AddressReader();
	private final ResponseReader responseReader = new ResponseReader();
	private int op = -1;

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		var readerState = intReader.process(bb);
		if(readerState == ProcessStatus.DONE){
			op = intReader.get();
			System.out.println("op reader : "+ op);
			intReader.reset();
			switch(op){

				case 3 ->{//Annonce intention de deconnexion
					System.out.println("JE PASSE PAR LA");
						var doubleAddReaderState = doubleAddReader.process(bb);
						if(doubleAddReaderState == ProcessStatus.DONE){
							var doubleadd = doubleAddReader.get();
							dataDoubleAddress = new DataDoubleAddress(op,doubleadd.addressSource(),doubleadd.addressDestination());
							
						}
						
						else{
							return doubleAddReaderState;
						}
						doubleAddReader.reset();
					}
				case 4 ->{//Ping de confirmation de changement de connexion
						System.out.println("on passe ici");
						var doubleAddReaderStatePC = doubleAddReader.process(bb);
						System.out.println(doubleAddReaderStatePC+"la state");
						if(doubleAddReaderStatePC == ProcessStatus.DONE){
							var doubleadd = doubleAddReader.get();
							dataDoubleAddress = new DataDoubleAddress(op,doubleadd.addressSource(),doubleadd.addressDestination());
							System.out.println(op +" " + dataDoubleAddress.AddressSrc() + " " + dataDoubleAddress.AddressDst());
						}
						else{
							return doubleAddReaderStatePC;
						}
						doubleAddReader.reset();
					}
				case 5 ->{//Trame suppression d'application'
						var addReaderState = addReader.process(bb);
						if(addReaderState == ProcessStatus.DONE){
							var address = addReader.get();
							dataOneAddress = new DataOneAddress(op,address);
							//reset addReader
						}
						else{
							return addReaderState;
						}
					}					
				case 7 ->{//Trame Firstt LEAF
						var lotAddReaderState = lotAddReader.process(bb);
						if(lotAddReaderState == ProcessStatus.DONE){
							dataALotAddress = new DataALotAddress(op,lotAddReader.get());
							
						}
						else{
							return lotAddReaderState;
						}
					}
				case 8-> { //Trame Full TREE
					var lotAddReaderStateFT = lotAddReader.process(bb);
					if(lotAddReaderStateFT == ProcessStatus.DONE){
						dataALotAddress = new DataALotAddress(op,lotAddReader.get());
						
					}
					else{
						return lotAddReaderStateFT;
					}
				}
				case 10 -> {//Trame ping d'envoie'
						var addReaderStatePE = addReader.process(bb);
						if(addReaderStatePE == ProcessStatus.DONE){
							var address = addReader.get();
							dataOneAddress = new DataOneAddress(op,address);
						}
						else{
							System.out.println("ERROR");
							return addReaderStatePE;
						}
					}
					
				case 11->{
					//Trame ping reponse
					var responseReaderState = responseReader.process(bb);
					if(responseReaderState == ProcessStatus.DONE) {
						var resp = responseReader.get();
						dataResponse = new DataResponse(op,resp.addressSrc(),resp.addressDst(),resp.boolbyte());
					}
					else{
						return responseReaderState;
					}
				}
					
				case 12->{
					//envoi de donnee à traiter
				
					System.out.println("hellow");
				}
				case 13->{
					System.out.println("hellow");//envoi de donnee deco
				}
				
//				case 0 ->{//Demande de connexion_________DUMP
//				System.out.println("hellow");
//			}
//		case 1 ->{//Acceptation de connexion_______DUMP
//				System.out.println("hellow");
//			}
//		case 2 ->{//Demande de reconnexion_______DUMP
//				System.out.println("hellow");
//			}
//				case 6 ->{
//				}//Trame First ROOT
//					//ONLY OP 
//				case 9-> {//Trame New LEAF
//					var addReaderStateNL = addReader.process(bb);
//					if(addReaderStateNL == ProcessStatus.DONE){
//						var address = addReader.get();
//						dataOneAddress = new DataOneAddress(op,address);
//						//reset addReader
//					}
//					else{
//						return addReaderStateNL;
//					}
//				}
//				case 77 ->{
//					
//				}
					
			}
			
		} 
		else{
			return readerState;
		}
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public Trame get() {
		if (state != State.DONE) {
			return null;
		}
		switch (op) {

			case 3 -> {
				return new TrameAnnonceIntentionDeco(dataDoubleAddress);
			}
			case 4 -> {
				return new TramePingConfirmationChangementCo(dataDoubleAddress);
			}
			case 5 -> {
				return new TrameSuppression(dataOneAddress);
			}
			case 7 -> {
				return new TrameFirstLeaf(dataALotAddress);
			}
			case 8->{
				return new TrameFullTree(dataALotAddress);
			}
			case 10 -> {
				return new TramePingEnvoi(dataOneAddress);
			}
			case 11 -> {
				return new TramePingReponse(dataResponse);
			}
			case 12 -> {
				return null; // TODO
			}	
			case 13 -> {
				return null; // TODO
			}
		//		case 0->{
		//		return null; // ______DUMP
		//	}
		//	case 1 -> {
		//		return null; // ______DUMP
		//	}
		//	case 2 -> {
		//		return null; // ______DUMP
		//	}
		//		case 6 -> {
		//		return new TrameFirstRoot(op);
		//	}
		//		case 9 -> {
		//		return new TrameNewLeaf(dataOneAddress);
		//	}
		//		case 77 ->{
		//			
		//		}
			}

		return null;
	}

	public int getOp() {
		return op;
	}

	@Override
	public void reset() {
		state = State.WAITING;

		intReader.reset();
		addReader.reset();
		responseReader.reset();
		lotAddReader.reset();
		doubleAddReader.reset();
	}

}
