package fr.xephi.authme.managment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.managment.login.AsyncLogin;
import fr.xephi.authme.managment.register.AsyncRegister;

public class Management {

	private DataSource database;
	public AuthMe plugin;

	private ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

	public Management(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
	}

	public void performLogin(final Player player, final String password, final boolean forceLogin) {
		service.submit(new AsyncLogin(plugin, database, player, password, forceLogin));
	}

	public void performRegister(final Player player, final String password) {
		service.submit(new AsyncRegister(plugin, database, player, password));
	}

}
