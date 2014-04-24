package fr.xephi.authme.managment;

import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.managment.login.AsyncLogin;
import fr.xephi.authme.managment.register.AsyncRegister;

public class Management {

	private DataSource database;
	public AuthMe plugin;

	public Management(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
	}

	public void performLogin(final Player player, final String password, final boolean forceLogin) {
		new AsyncLogin(plugin, database, player, password, forceLogin).start();
	}

	public void performRegister(final Player player, final String password) {
		new AsyncRegister(plugin, database, player, password).start();
	}

}
