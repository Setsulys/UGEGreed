package fr.uge.greed.reader;

import java.nio.ByteBuffer;
import java.sql.DriverPropertyInfo;

import fr.uge.greed.data.DataALotAddress;
import fr.uge.greed.data.DataDoubleAddress;
import fr.uge.greed.data.DataOneAddress;
import fr.uge.greed.data.DataResponse;
import fr.uge.greed.trame.Trame;
import fr.uge.greed.trame.TrameAnnonceIntentionDeco;
import fr.uge.greed.trame.TrameFirstLeaf;
import fr.uge.greed.trame.TrameFirstRoot;
import fr.uge.greed.trame.TrameFullTree;
import fr.uge.greed.trame.TrameNewLeaf;
import fr.uge.greed.trame.TramePingConfirmationChangementCo;
import fr.uge.greed.trame.TramePingEnvoi;
import fr.uge.greed.trame.TramePingReponse;
import fr.uge.greed.trame.TrameSuppression;

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
			intReader.reset();
			switch(op){
				case 0 ->{//Demande de connexion_________DUMP
						System.out.println("hellow");
					}
				case 1 ->{//Acceptation de connexion_______DUMP
						System.out.println("hellow");
					}
				case 2 ->{//Demande de reconnexion_______DUMP
						System.out.println("hellow");
					}
				case 3 ->{//Annonce intention de deconnexion
						var doubleAddReaderState = doubleAddReader.process(bb);
						if(doubleAddReaderState == ProcessStatus.DONE){
							var doubleadd = doubleAddReader.get();
							dataDoubleAddress = new DataDoubleAddress(op,doubleadd.addressSource(),doubleadd.addressDestination());
							
						}
						
						else{
							return doubleAddReaderState;
						}
					}
				case 4 ->{//Ping de confirmation de changement de connexion
						var doubleAddReaderStatePC = doubleAddReader.process(bb);
						if(doubleAddReaderStatePC == ProcessStatus.DONE){
							var doubleadd = doubleAddReader.get();
							dataDoubleAddress = new DataDoubleAddress(op,doubleadd.addressSource(),doubleadd.addressDestination());
							
						}
						else{
							return doubleAddReaderStatePC;
						}
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
					
					
				case 6 ->{
				}//Trame First ROOT
					//ONLY OP 
					
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
				case 9-> {//Trame New LEAF
					var addReaderStateNL = addReader.process(bb);
					if(addReaderStateNL == ProcessStatus.DONE){
						var address = addReader.get();
						dataOneAddress = new DataOneAddress(op,address);
						//reset addReader
					}
					else{
						return addReaderStateNL;
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
					
				case 14->{//envoi de donnee traitee
					System.out.println("hellow");
				}
					
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
		case 0->{
			return null; // ______DUMP
		}
		case 1 -> {
			return null; // ______DUMP
		}
		case 2 -> {
			return null; // ______DUMP
		}
		case 3 -> {
			return new TrameAnnonceIntentionDeco(dataDoubleAddress);
		}
		case 4 -> {
			return new TramePingConfirmationChangementCo(dataDoubleAddress);
		}
		case 5 -> {
			return new TrameSuppression(dataOneAddress);
		}
		case 6 -> {
			return new TrameFirstRoot(op);
		}
		case 7 -> {
			return new TrameFirstLeaf(dataALotAddress);
		}
		case 8->{
			return new TrameFullTree(dataALotAddress);
		}
		case 9 -> {
			return new TrameNewLeaf(dataOneAddress);
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
		case 14 -> {
			return null; // TODO
		}
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
