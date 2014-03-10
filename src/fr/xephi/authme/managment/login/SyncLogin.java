package fr.xephi.authme.managment.login;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.settings.Settings;

public class SyncLogin implements Runnable {

	private AuthMe plugin;

	private LimboPlayer limbo;
	private Player player;
	private String name;
	public SyncLogin(AuthMe plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
		this.name = player.getName().toLowerCase();
		this.limbo = LimboCache.getInstance().getLimboPlayer(name);
	}

	public LimboPlayer getLimbo() {
		return limbo;
	}

	protected void restoreOpState() {
		player.setOp(limbo.getOperator());
		if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
			player.setAllowFlight(limbo.isFlying());
			player.setFlying(limbo.isFlying());
		}
	}

	protected void teleportBackFromSpawn() {
		AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
		Bukkit.getPluginManager().callEvent(tpEvent);
		if (!tpEvent.isCancelled()) {
			Location fLoc = tpEvent.getTo();
			player.teleport(fLoc);
		}
	}
	protected void teleportToSpawn() {
		Location spawnL = plugin.getSpawnLocation(player.getWorld());
		SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnL, true);
		Bukkit.getPluginManager().callEvent(tpEvent);
		if (!tpEvent.isCancelled()) {
			Location fLoc = tpEvent.getTo();
			if (!fLoc.getChunk().isLoaded()) {
				fLoc.getChunk().load();
			}
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
			// Op & Flying
			restoreOpState();

			/*
			 * Restore Inventories and GameMode
			 * We need to restore them before teleport the player
			 * Cause in AuthMePlayerListener, we call ProtectInventoryEvent after Teleporting
			 * Also it's the current world inventory !
			 */
			 player.setGameMode(limbo.getGameMode());
			// Inventory - Make it after restore GameMode , cause we need to restore the
			 // right inventory in the right gamemode
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

		// The Loginevent now fires (as intended) after everything is processed
		Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
	}

}