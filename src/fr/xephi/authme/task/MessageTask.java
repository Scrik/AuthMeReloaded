package fr.xephi.authme.task;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.login.LoginCache;

public class MessageTask implements Runnable {

	private AuthMe plugin;
	private Player player;
	private String name;
	private String msg;
	private int interval;

	public MessageTask(AuthMe plugin, Player player, String msg, int interval) {
		this.plugin = plugin;
		this.player = player;
		this.name = player.getName().toLowerCase();
		this.msg = msg;
		this.interval = interval;
	}

	@Override
	public void run() {
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}

		player.sendMessage(msg);
		BukkitScheduler sched = plugin.getServer().getScheduler();
		BukkitTask late = sched.runTaskLater(plugin, this, interval * 20);
		if (LoginCache.getInstance().hasPlayer(name)) {
			LoginCache.getInstance().getPlayer(name).setMessageTaskId(late.getTaskId());
		}
	}
}
