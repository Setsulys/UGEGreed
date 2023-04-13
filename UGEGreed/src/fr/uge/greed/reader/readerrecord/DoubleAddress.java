package fr.uge.greed.reader.readerrecord;

import java.net.InetSocketAddress;

public record DoubleAddress(InetSocketAddress addressSource, InetSocketAddress addressDestination) {

}