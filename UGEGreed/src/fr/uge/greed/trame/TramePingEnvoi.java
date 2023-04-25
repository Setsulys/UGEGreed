package fr.uge.greed.trame;

import fr.uge.greed.data.DataOneAddress;

public record TramePingEnvoi(DataOneAddress doa) implements Trame{
	public int getOp() {
		return doa.opCode();
	}
}
