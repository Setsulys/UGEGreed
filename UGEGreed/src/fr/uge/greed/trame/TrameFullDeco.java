package fr.uge.greed.trame;

public record TrameFullDeco(int op) implements Trame{
	public int getOp() {
		return op;
	}
}
