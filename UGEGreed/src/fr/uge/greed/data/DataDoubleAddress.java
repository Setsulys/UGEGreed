package fr.uge.greed.data;

import java.net.InetSocketAddress;

public record DataDoubleAddress(int opCode, InetSocketAddress AddressSrc,InetSocketAddress AddressDst) {

}
