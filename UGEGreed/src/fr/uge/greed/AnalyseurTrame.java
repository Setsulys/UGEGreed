package fr.uge.greed;

import java.nio.ByteBuffer;
import java.util.Objects;

public class AnalyseurTrame {
	ByteBuffer buffer = ByteBuffer.allocate(2024);   //j'ai allocate a 2024 en attendant qu'on se mette ok sur la taille
	
	
	/*On lit l'op de la trame pour pouvoir savoir quelle trame on a reçu*/
	
	int getOp(ByteBuffer buf) {
		Objects.requireNonNull(buf);	//buffer altéré 
		var op = new IntReader();
		op.process(buf);
		return op.get();
	}
	
	void analyseur(int op, ByteBuffer buf) {
		switch(op) {
		/*Une fonction pour chaque trame*/
		//case 0:
			
		}
	}
	
	
}
