package fr.xephi.authme.managment.register;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.login.LoginCache;
import fr.xephi.authme.cache.login.LoginPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class AsyncRegister implements Runnable {

	private AuthMe plugin;
	private DataSource database;

	private Player player;
	private String realname;
	private String name;
	private String password;

	private Messages m = Messages.getInstance();

	public AsyncRegister(AuthMe plugin, DataSource datasource, Player player, String password) {
		this.plugin = plugin;
		this.database = datasource;
		this.player = player;
		this.password = password;
		this.realname = player.getName();
		this.name = player.getName().toLowerCase();
	}

	public boolean preRegister() {
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			player.sendMessage(m.getMessage("logged_in"));
			return false;
		}

		if (database.isAuthAvailable(player.getName().toLowerCase())) {
			player.sendMessage(m.getMessage("user_regged"));
			return false;
		}

		final String ip = player.getAddress().getAddress().getHostAddress();
		if (Settings.getmaxRegPerIp > 0) {
			if (!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp) {
				player.sendMessage(m.getMessage("max_reg"));
				return false;
			}
		}

		return true;
	}

	@Override
	public void run() {
		if (!preRegister()) {
			return;
		}

		String hash = "";
		try {
			hash = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
		} catch (NoSuchAlgorithmException e) {
			ConsoleLogger.showError(e.getMessage());
			player.sendMessage(m.getMessage("error"));
			return;
		}
		PlayerAuth auth = new PlayerAuth(name, realname, hash, player.getAddress().getAddress().getHostAddress(), new Date().getTime());
		database.saveAuth(auth);

		player.sendMessage(m.getMessage("registered"));

		PlayerCache.getInstance().addPlayer(auth);

		LoginPlayer limbo = LoginCache.getInstance().getPlayer(auth.getNickname());
		if (limbo != null) {
			Bukkit.getScheduler().cancelTask(limbo.getTimeoutTaskId());
			Bukkit.getScheduler().cancelTask(limbo.getMessageTaskId());
			LoginCache.getInstance().deletePlayer(auth.getNickname());
		}
		
		// Schedule login event call
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
			}
		});
	}

}
