package fr.xephi.authme.cache.login;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.events.StoreInventoryEvent;

public class LoginCache {

	private static LoginCache singleton = new LoginCache(AuthMe.getInstance());

	public static LoginCache getInstance() {
		return singleton;
	}

	public HashMap<String, LoginPlayer> cache;
	public AuthMe plugin;

	private LoginCache(AuthMe plugin) {
		this.plugin = plugin;
		this.cache = new HashMap<String, LoginPlayer>();
	}

	public void addPlayer(Player player) {
		String name = player.getName().toLowerCase();
		Location loc = player.getLocation();
		ItemStack[] arm = null;
		ItemStack[] inv = null;

		StoreInventoryEvent storeevent = new StoreInventoryEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(storeevent);
		if (!storeevent.isCancelled()) {
			inv = storeevent.getInventory();
			arm = storeevent.getArmor();
		}

		if (player.isDead()) {
			loc = plugin.getSpawnLocation(player.getWorld());
		}
		cache.put(player.getName().toLowerCase(), new LoginPlayer(name, loc, inv, arm));
	}

	public void deletePlayer(String name) {
		cache.remove(name);
	}

	public LoginPlayer getPlayer(String name) {
		return cache.get(name);
	}

	public boolean hasPlayer(String name) {
		return cache.containsKey(name);
	}

}
