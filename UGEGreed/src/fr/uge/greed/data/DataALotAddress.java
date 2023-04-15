package fr.uge.greed.data;

import java.util.ArrayList;
import java.net.InetSocketAddress;

public record DataALotAddress(int opCode, ArrayList<InetSocketAddress> list) {
}
