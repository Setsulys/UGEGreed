package fr.uge.greed.trame;

import fr.uge.greed.data.*;
public record TrameFirstLeaf(DataALotAddress dla) implements Trame {
	public int getOp() {
		return dla.opCode();
	}
	
	public int getSize() {
		return dla.list().size();
	}
}
