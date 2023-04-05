package fr.uge.greed;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

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
	
	/**
	 * Print the route table
	 */
	@Override
	public String toString() {
		return routeTable.entrySet().stream().map(e -> e.getKey() + " : " + e.getValue()).collect(Collectors.joining(",","[","]"));
	}
	
}
