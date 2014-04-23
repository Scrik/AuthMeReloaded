package fr.xephi.authme.managment.login;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.settings.Settings;

public class SyncLogin implements Runnable {

	private LimboPlayer limbo;
	private Player player;
	private String name;
	public SyncLogin(Player player) {
		this.player = player;
		this.name = player.getName().toLowerCase();
		this.limbo = LimboCache.getInstance().getLimboPlayer(name);
	}

	public LimboPlayer getLimbo() {
		return limbo;
	}

	protected void teleportBackFromSpawn() {
		AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
		Bukkit.getPluginManager().callEvent(tpEvent);
		if (!tpEvent.isCancelled()) {
			Location fLoc = tpEvent.getTo();
			player.teleport(fLoc);
		}
	}

	protected void restoreInventory() {
		RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			API.setPlayerInventory(player, event.getInventory(), event.getArmor());
		}
	}

	@Override
	public void run() {
		// Limbo contains the State of the Player before /login
		if (limbo != null) {
			// Restore inventory
			if (Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
				restoreInventory();
			}
			// Teleport
			if (Settings.isTeleportToSpawnEnabled) {
				teleportBackFromSpawn();
			}
			// Cleanup no longer used temporary data
			LimboCache.getInstance().deleteLimboPlayer(name);
		}
		// Call login event
		Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
	}

}
