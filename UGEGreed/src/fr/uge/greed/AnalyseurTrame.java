//package fr.uge.greed;
//
//import java.nio.ByteBuffer;
//import java.util.Objects;
//import fr.uge.greed.Trames;
//import fr.uge.greed.Application;
//public class AnalyseurTrame {
//	ByteBuffer buffer = ByteBuffer.allocate(2024);   //j'ai allocate a 2024 en attendant qu'on se mette ok sur la taille
//	
//	
//	/*On lit l'op de la trame pour pouvoir savoir quelle trame on a reçu*/
//	
//	int getOp(ByteBuffer buf, Application app) {
//		Objects.requireNonNull(buf);	//buffer altéré 
//		var op = new IntReader();
//		op.process(buf);
//		return op.get();
//	}
//	
////	void analyseur(Trames op, ByteBuffer buf, Application app) {
////		switch(op) {
////		/*Une fonction pour chaque trame*/
////		case PINGENVOI -> {
////			traitementPingEnvoi(buf);
////		}
////		
////		}
////	}
//	
//	void traitementPingEnvoi(ByteBuffer buf, Application app){
//		if(app.GetbufferDonnee().hasRemaining()){
//			//Pas dispo, envoie trame pas dispo
//		}
//	}
//	
//	
//	
//	
//	
//	
//}
