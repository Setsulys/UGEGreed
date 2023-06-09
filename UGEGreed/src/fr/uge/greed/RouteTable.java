package fr.uge.greed;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;;

//public class RouteTable implements Iterable<Context> {
//
//	private final LinkedHashMap<Context, Context> routeTable = new LinkedHashMap<>();
//	
//	/**
//	 * Update The Route Table
//	 * @param newAdress
//	 * @param route
//	 */
//	public void updateRouteTable(Context newAdress,Context route) {
//		//newAdress l'adresse de la node que l'on veut,route l'adresse de la node par laquelle on passe pour aller a newAdress
//		routeTable.put(newAdress, route);
//	} 
//	
//	
//	/**
//	 * Suppress the application and it channel from the route table when an application is disconnecting
//	 * @param unlinked
//	 */
//	public void deleteRouteTable(Context unlinked) {
//		Objects.requireNonNull(unlinked);
//		routeTable.remove(unlinked);
//	}
//	
//	/**
//	 * Get the (neighbore) channel that the frame need to pass to the destination
//	 * Return null if there is no way to the destination
//	 * @param destination
//	 * 
//	 * @return InetSocketAddress
//	 */
//	public Context get(Context destination) {
//		Objects.requireNonNull(destination);
//		return routeTable.get(destination);
//	}
//	
//	
//	public ArrayList<Context> getAllAddress(){
//		return new ArrayList<>(routeTable.keySet().stream().collect(Collectors.toList()));
//	}
//	
//	/**
//	 * Print the route table
//	 */
//	@Override
//	public String toString(){
//		return routeTable.entrySet().stream().map(e -> {
//			try {
//				return e.getKey().getChannel().getRemoteAddress() + " : " + e.getValue().getChannel().getRemoteAddress();
//			} catch (IOException e1) {
//				return null;
//			}
//		}).collect(Collectors.joining(",\n"));
//	}
//	
//	public Iterator<Context> iterator(){
//		return new myIterator();
//	}
//	
//	private class myIterator implements Iterator<Context>{
//		private Iterator<Map.Entry<Context,Context>> iterator  = routeTable.entrySet().iterator();
//		
//		@Override 
//		public boolean hasNext(){
//			return iterator.hasNext();
//		}
//		
//		@Override
//		public Context next(){
//			return iterator.next().getKey();
//		}
//		
//		@Override
//		public void remove(){
//			iterator.remove();	
//		}
//	}
//}


public class RouteTable implements Iterable<InetSocketAddress> {

	private final LinkedHashMap<InetSocketAddress, InetSocketAddress> routeTable = new LinkedHashMap<>();
	private final InetSocketAddress me;
	
	public RouteTable(InetSocketAddress me){
		this.me = me;
	}
	
	/**
	 * Update The Route Table
	 * @param newAdress
	 * @param route
	 */
	public void addToRouteTable(InetSocketAddress newAdress,InetSocketAddress route) {
		//newAdress l'adresse de la node que l'on veut,route l'adresse de la node par laquelle on passe pour aller a newAdress
		//System.out.println(me +" lolololololol");
		if(newAdress != me){
			Objects.requireNonNull(newAdress);
			Objects.requireNonNull(route);
			routeTable.put(newAdress, route);
		}

		
	} 
	
	public void removeKeyFromValue(InetSocketAddress address){
		Iterator<InetSocketAddress> iterator = routeTable.keySet().iterator();
		while(iterator.hasNext()){
			InetSocketAddress key = iterator.next();
			if(routeTable.get(key).equals(address)){
				iterator.remove();
			}
		}
	}
	
	public void removeKeyIf(ArrayList<InetSocketAddress> addresses){
		for(var e : routeTable.entrySet()) {
			if(!addresses.contains(e.getKey())) {
				routeTable.remove(e.getKey());
			}
		}
	}
	/**
	 * Suppress the application Address and the other applications address that use this application address as a route 
	 * @param unlinked
	 */
	public void deleteRouteTable(InetSocketAddress unlinked) {
		Objects.requireNonNull(unlinked);
		routeTable.remove(unlinked);
		ArrayList<InetSocketAddress> toRemove = routeTable.entrySet().stream().filter(entry -> unlinked.equals(entry.getValue())).map(e->e.getKey()).collect(Collectors.toCollection(ArrayList::new));
		toRemove.removeIf(routeTable::containsKey);
		toRemove.forEach(routeTable::remove);
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
	 *  Get all the key of InetSocketAddres in the hashmap and put it in an ArrayList
	 * @return
	 */
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
