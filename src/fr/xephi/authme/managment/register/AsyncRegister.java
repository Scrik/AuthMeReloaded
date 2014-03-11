package fr.xephi.authme.managment.register;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class AsyncRegister extends Thread {

	private AuthMe plugin;
    private DataSource database;

	private Player player;
	private String name;
	private String password;

	private Messages m = Messages.getInstance();

	public AsyncRegister(AuthMe plugin, DataSource datasource, Player player, String password) {
		this.plugin = plugin;
		this.database = datasource;
		this.player = player;
		this.password = password;
		this.name = player.getName().toLowerCase();
	}

	public boolean preRegister() {
    	if (PlayerCache.getInstance().isAuthenticated(name)) {
            player.sendMessage(m._("logged_in"));
            return false;
        }

        if (database.isAuthAvailable(player.getName().toLowerCase())) {
        	player.sendMessage(m._("user_regged"));
        	return false;
        }

		final String ip = player.getAddress().getAddress().getHostAddress();
		if (Settings.getmaxRegPerIp > 0) {
			if (!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp) {
				player.sendMessage(m._("max_reg"));
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
			player.sendMessage(m._("error"));
			return;
		}
		PlayerAuth auth = new PlayerAuth(name, hash, player.getAddress().getAddress().getHostAddress(), new Date().getTime());
		if (!database.saveAuth(auth)) {
			player.sendMessage(m._("error"));
			return;
		}

		player.sendMessage(m._("registered"));
	}

}
