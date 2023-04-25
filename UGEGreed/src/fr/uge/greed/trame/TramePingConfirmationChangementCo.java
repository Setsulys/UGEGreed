package fr.uge.greed.trame;

import fr.uge.greed.data.*;

public record TramePingConfirmationChangementCo(DataDoubleAddress dda) implements Trame{
	public int getOp() {
		return dda.opCode();
	}
}
