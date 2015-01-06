package fr.xephi.authme.task;

import org.bukkit.entity.Player;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.login.LoginCache;
import fr.xephi.authme.cache.login.LoginPlayer;
import fr.xephi.authme.settings.Messages;



public class TimeoutTask implements Runnable {

	private Player player;
	private String name;
	private Messages m = Messages.getInstance();

	public TimeoutTask(Player player) {
		this.player = player;
		this.name = player.getName().toLowerCase();
	}

	public String getName() {
		return name;
	}

	@Override
	public void run() {
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}

		if (LoginCache.getInstance().hasPlayer(name)) {
			LoginPlayer inv = LoginCache.getInstance().getPlayer(name);
			player.getServer().getScheduler().cancelTask(inv.getMessageTaskId());
			player.getServer().getScheduler().cancelTask(inv.getTimeoutTaskId());
		}
		player.kickPlayer(m.getMessage("timeout"));
	}
}
