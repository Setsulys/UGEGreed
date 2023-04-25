package fr.uge.greed.trame;

import fr.uge.greed.data.*;

public record TrameSuppression (DataOneAddress doa)implements Trame{//op 5
	public int getOp() {
		return doa.opCode();
	}
}
