package fr.xephi.authme.managment.register;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.settings.Settings;

public class SyncRegister implements Runnable {

	private Player player;
	public SyncRegister(Player player) {
		this.player = player;
	}
	
	@Override
	public void run() {
		//restore flying state
		if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
			player.setAllowFlight(false);
			player.setFlying(false);
		}
		// The Loginevent now fires (as intended) after everything is processed
		Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
	}

}
