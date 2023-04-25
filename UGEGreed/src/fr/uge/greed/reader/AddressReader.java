package fr.uge.greed.reader;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class AddressReader implements Reader<InetSocketAddress>{
	
	private enum State{
		DONE,WAITING_IP,WAITING_TYPE,WAITING_HOST,ERROR
	}

	private static int BUFFER_SIZE = 1024;
	private final ByteBuffer bufferType =  ByteBuffer.allocate(Byte.BYTES);
	private final ByteBuffer bufferHost = ByteBuffer.allocate(Short.BYTES);
	private final ByteBuffer bufferAddress = ByteBuffer.allocate(BUFFER_SIZE);
	private byte ipType;
	
	private InetSocketAddress address;
	private  Short host;
	private State state = State.WAITING_TYPE;
	
	private int IPV4 = 4 * Byte.BYTES;
	private int IPV6 = 16 * Byte.BYTES;
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		System.out.println("intreader of addressReader"+bb.remaining());
		bb.flip();
		System.out.println("intreader of addressReader"+bb.remaining());
		try {
			System.out.println("process AddressReader");
			if(state == State.WAITING_TYPE) {
				var oldLimit = bb.limit();
				if(bufferType.remaining() > oldLimit) {
					bufferType.put(bb);
				}
				else {
					bb.limit(bufferType.remaining());
					bufferType.put(bb);
					bb.limit(oldLimit);
				}
				System.out.println("ipv4"+ bb.remaining());
				//If not getting all the integer
				if(bufferType.remaining() != 0) {
					System.out.println("remain");
					return ProcessStatus.REFILL;
				}
				ipType = bufferType.flip().get();
				if(ipType != 4 && ipType != 6) {
					return ProcessStatus.ERROR;
				}
				state = State.WAITING_IP;
			}
			if(state == State.WAITING_IP) {
				if(ipType == 4) {
					while(bb.hasRemaining() && bufferAddress.position() < IPV4 && bufferAddress.hasRemaining()) {
						bufferAddress.put(bb.get());
					}
					System.out.println("ipv4 remain" + bb.remaining());
					if(bufferAddress.position() < IPV4) {
						System.out.println("ipv4refill");
						return ProcessStatus.REFILL;
					}
				}
				else {
					while(bb.hasRemaining() && bufferAddress.position()  < IPV6 && bufferAddress.hasRemaining()) {
						bufferAddress.put(bb.get());
					}
					if(bufferAddress.position() < IPV6) {
						System.out.println("ipv6refill");
						return ProcessStatus.REFILL;
					}
				}
				state = State.WAITING_HOST;
			}
			System.out.println("short buff" + bb.remaining() + " " + bb.limit());
			if(state == State.WAITING_HOST) {
				
				if(bb.remaining() <= bufferHost.remaining() ) {
					bufferHost.put(bb);
				}
				else {
					System.out.println("pass in else");
					var oldLimit = bb.limit();
					bb.limit(bufferHost.remaining());
					bufferHost.put(bb);
					bb.limit(oldLimit);
				}
				//If not getting All the Short
				System.out.println("short buff bis" + bb.remaining());
				if(bufferHost.remaining() != 0) {
					System.out.println("Refill short");
					return ProcessStatus.REFILL;
				}
				host = bufferHost.flip().getShort();
			}
			
			
		}finally {
			bb.compact();
		}
		try {
			InetAddress inetAddress = null;
			if(ipType == 4) {
				byte[] addressBytes = new byte[4];
				bufferAddress.flip();
				bufferAddress.get(addressBytes);
				inetAddress = Inet4Address.getByAddress(addressBytes);
				address = new InetSocketAddress(inetAddress,host);
			}
			else {
				byte[] addressBytes = new byte[16];
				bufferAddress.flip();
				bufferAddress.get(addressBytes);
				inetAddress = InetAddress.getByAddress(addressBytes);
				address = new InetSocketAddress(inetAddress,host);
			}
		}catch(IOException e){ //UnknownHostException
			
		}
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public InetSocketAddress get() {
		if(state == State.DONE) {
			return address;
		}
		throw new IllegalStateException();
	}

	@Override
	public void reset() {
		state = State.WAITING_TYPE;
		bufferAddress.clear();
		bufferType.clear();
		bufferHost.clear();
	}

	
}
