package fr.xephi.authme.task;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.settings.Messages;



public class TimeoutTask implements Runnable {

	private JavaPlugin plugin;
	private String name;
	private Messages m = Messages.getInstance();

	public TimeoutTask(JavaPlugin plugin, String name) {
		this.plugin = plugin;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public void run() {
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (player.getName().toLowerCase().equals(name)) {
				if (LimboCache.getInstance().hasLimboPlayer(name)) {
					LimboPlayer inv = LimboCache.getInstance().getLimboPlayer(name);
					player.getServer().getScheduler().cancelTask(inv.getMessageTaskId());
					player.getServer().getScheduler().cancelTask(inv.getTimeoutTaskId());
				}
				GameMode gm = AuthMePlayerListener.gameMode.get(name);
				player.setGameMode(gm);
				ConsoleLogger.info("Set " + player.getName() + " to gamemode: " + gm.name());
				player.kickPlayer(m._("timeout"));
				break;
			}
		}
	}
}
