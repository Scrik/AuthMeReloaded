package fr.xephi.authme.managment.register;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.settings.Settings;

public class SyncRegister implements Runnable {

	private PlayerAuth auth;
	private Player player;
	public SyncRegister(PlayerAuth auth, Player player) {
		this.auth = auth;
		this.player = player;
	}
	
	@Override
	public void run() {
		//add to cache (logged in state)
		PlayerCache.getInstance().addPlayer(auth);
		//cancel limbo tasks
		LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(auth.getNickname());
		if (limbo != null) {
			player.setGameMode(limbo.getGameMode());
			Bukkit.getScheduler().cancelTask(limbo.getTimeoutTaskId());
			Bukkit.getScheduler().cancelTask(limbo.getMessageTaskId());
			LimboCache.getInstance().deleteLimboPlayer(auth.getNickname());
		}
		//restore flying state
		if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
			player.setAllowFlight(false);
			player.setFlying(false);
		}
		// The Loginevent now fires (as intended) after everything is processed
		Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
	}

}
