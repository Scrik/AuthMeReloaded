package fr.xephi.authme.events;

import org.bukkit.Server;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Xephi59
 */
public class CustomEvent extends Event implements Cancellable {

	private boolean isCancelled;
	private static final HandlerList handlers = new HandlerList();
	private static Server s;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return this.isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public static Server getServer() {
		return s;
	}

}
