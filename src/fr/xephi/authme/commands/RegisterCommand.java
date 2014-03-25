package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class RegisterCommand implements CommandExecutor {

	private Messages m = Messages.getInstance();
	private DataSource database;
	public PlayerAuth auth;
	public AuthMe plugin;

	public RegisterCommand(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return true;
		}

		if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
			sender.sendMessage(m._("no_perm"));
			return true;
		}

		if (!Settings.isRegistrationEnabled) {
			sender.sendMessage(m._("reg_disabled"));
			return true;
		}

		if (args.length == 0 || (Settings.getEnablePasswordVerifier && args.length < 2)) {
			sender.sendMessage(m._("usage_reg"));
			return true;
		}

		if (args[0].length() < Settings.getPasswordMinLen || args[0].length() > Settings.passwordMaxLength) {
			sender.sendMessage(m._("pass_len"));
			return true;
		}

		if (Settings.getEnablePasswordVerifier) {
			if (!args[0].equals(args[1])) {
				sender.sendMessage(m._("password_error"));
				return true;
			}
		}

		final Player player = (Player) sender;
		final String password = args[0];
		final String name = player.getName().toLowerCase();
		final String ip = player.getAddress().getAddress().getHostAddress();

		if (PlayerCache.getInstance().isAuthenticated(name)) {
			player.sendMessage(m._("logged_in"));
			return true;
		}

		if (database.isAuthAvailable(name)) {
			player.sendMessage(m._("user_regged"));
			return true;
		}

		if (Settings.getmaxRegPerIp > 0) {
			if (!plugin.authmePermissible(sender, "authme.allow2accounts") && database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp) {
				player.sendMessage(m._("max_reg"));
				return true;
			}
		}

		plugin.management.performRegister(player, password);

		return true;
	}

}
