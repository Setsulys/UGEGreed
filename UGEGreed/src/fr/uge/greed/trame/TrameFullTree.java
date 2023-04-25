package fr.uge.greed.trame;

import fr.uge.greed.data.*;

public record TrameFullTree(DataALotAddress dla) implements Trame{
	public int getOp() {
		return dla.opCode();
	}
}
