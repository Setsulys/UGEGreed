package fr.uge.greed.reader.readerrecord;

import java.net.InetSocketAddress;

public record LotAddress(InetSocketAddress ... addresses) {

}