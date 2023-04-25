package fr.uge.greed.trame;

public record TrameFirstRoot(int op) implements Trame{
	public int getOp() {
		return op;
	}
}
