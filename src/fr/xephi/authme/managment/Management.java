package fr.xephi.authme.managment;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.managment.login.AsyncLogin;
import fr.xephi.authme.managment.register.AsyncRegister;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;

/**
 *
 * @authors Xephi59, <a href="http://dev.bukkit.org/profiles/Possible/">Possible</a>
 *
 */
public class Management {

	private DataSource database;
	public AuthMe plugin;
	public static RandomString rdm = new RandomString(Settings.captchaLength);
	public PluginManager pm;

	public Management(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
		this.pm = plugin.getServer().getPluginManager();
	}

	public void performLogin(final Player player, final String password, final boolean forceLogin) {
		new AsyncLogin(plugin, database, player, password, forceLogin).start();
	}

	public void performRegister(final Player player, final String password) {
		new AsyncRegister(plugin, database, player, password).start();
	}

}
