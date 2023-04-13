package fr.uge.greed.data;

import java.net.InetSocketAddress;

public record DataResponse(int opCode, InetSocketAddress addressSrc, InetSocketAddress addressDst, boolean boolByte) {

}
