package fr.uge.greed.trame;

import fr.uge.greed.data.*;
public record TramePingReponse(DataResponse dr) implements Trame {
	public int getOp() {
		return dr.opCode();
	}
}
