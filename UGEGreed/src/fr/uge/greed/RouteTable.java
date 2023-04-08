package fr.uge.greed;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class RouteTable {

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
	
}
