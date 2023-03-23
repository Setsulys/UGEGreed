package fr.uge.greed;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class RouteTable {

	private LinkedHashMap<InetSocketAddress, InetSocketAddress> routeTable = new LinkedHashMap<>();
	

	
	/**
	 * Met A jour la Table de routage
	 * @param newAdress
	 * @param route
	 */
	public void UpdateRouteTable(InetSocketAddress newAdress,InetSocketAddress route) {
		//newAdress l'adresse de la node que l'on veut,route l'adresse de la node par laquelle on passe pour aller a newAdress
		Objects.requireNonNull(newAdress);
		Objects.requireNonNull(route);
		routeTable.put(newAdress, route);
	}
	
	
	/**
	 * Supprime une Application et son chemin (clé valeur) de la table de routage lors de la déconnexion de l'Application
	 * @param unlinked
	 */
	public void DeleteRouteTable(InetSocketAddress unlinked) {
		Objects.requireNonNull(unlinked);
		routeTable.remove(unlinked);
	}

	
	/**
	 * Recuppere le chemin (voisin) par lequel une trame doit passer pour atteindre sa destination
	 * renvoi null si il n'y a pas de chemin
	 * @param destination
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress get(InetSocketAddress destination) {
		Objects.requireNonNull(destination);
		return routeTable.get(destination);
	}
	
	/**
	 * Affiche Toute la route table
	 */
	@Override
	public String toString() {
		return routeTable.entrySet().stream().map(key -> key +" : " + routeTable.get(key)).collect(Collectors.joining(",","[","]"));
	}
	
}
