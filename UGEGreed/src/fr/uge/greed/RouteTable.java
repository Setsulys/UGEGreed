package fr.uge.greed;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Iterator;

public class RouteTable implements Iterable<InetSocketAddress> {

	private final LinkedHashMap<InetSocketAddress, InetSocketAddress> routeTable = new LinkedHashMap<>();
	
	/**
	 * Update The Route Table
	 * @param newAdress
	 * @param route
	 */
	public void updateRouteTable(InetSocketAddress newAdress,InetSocketAddress route) {
		//newAdress l'adresse de la node que l'on veut,route l'adresse de la node par laquelle on passe pour aller a newAdress
		Objects.requireNonNull(newAdress);
		Objects.requireNonNull(route);
		routeTable.put(newAdress, route);
	} 
	
	
	/**
	 * Suppress the application and it channel from the route table when an application is disconnecting
	 * @param unlinked
	 */
	public void deleteRouteTable(InetSocketAddress unlinked) {
		System.out.println("this need to be removed"+unlinked);
		Objects.requireNonNull(unlinked);
		routeTable.remove(unlinked);
	}
	
	/**
	 * Get the (neighbore) channel that the frame need to pass to the destination
	 * Return null if there is no way to the destination
	 * @param destination
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress get(InetSocketAddress destination) {
		Objects.requireNonNull(destination);
		return routeTable.get(destination);
	}
	
	
	public ArrayList<InetSocketAddress> getAllAddress(){
		return new ArrayList<>(routeTable.keySet().stream().collect(Collectors.toList()));
	}
	
	/**
	 * Print the route table
	 */
	@Override
	public String toString() {
		return routeTable.entrySet().stream().map(e -> e.getKey() + " : " + e.getValue()).collect(Collectors.joining(",\n"));
	}
	
	public Iterator<InetSocketAddress> iterator(){
		return new myIterator();
	}
	
	private class myIterator implements Iterator<InetSocketAddress>{
		private Iterator<Map.Entry<InetSocketAddress,InetSocketAddress>> iterator  = routeTable.entrySet().iterator();
		
		@Override 
		public boolean hasNext(){
			return iterator.hasNext();
		}
		
		@Override
		public InetSocketAddress next(){
			return iterator.next().getKey();
		}
		
		@Override
		public void remove(){
			iterator.remove();	
		}
	}
}
