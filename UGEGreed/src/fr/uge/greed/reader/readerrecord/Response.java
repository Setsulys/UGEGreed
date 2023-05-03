package fr.uge.greed.reader.readerrecord;

import java.net.InetSocketAddress;

public record Response(InetSocketAddress addressSrc, InetSocketAddress addressDst, boolean boolbyte) {

}
