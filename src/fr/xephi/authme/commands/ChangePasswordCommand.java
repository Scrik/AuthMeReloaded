package fr.xephi.authme.commands;

import java.security.NoSuchAlgorithmException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class ChangePasswordCommand implements CommandExecutor {

	private Messages messages = Messages.getInstance();
	private DataSource database;
	private AuthMe plugin;

	public ChangePasswordCommand(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return true;
		}

		if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
			sender.sendMessage(messages.getMessage("no_perm"));
			return true;
		}

		Player player = (Player) sender;
		String name = player.getName().toLowerCase();
		if (!PlayerCache.getInstance().isAuthenticated(name)) {
			player.sendMessage(messages.getMessage("not_logged_in"));
			return true;
		}

		if (args.length != 2) {
			player.sendMessage(messages.getMessage("usage_changepassword"));
			return true;
		}

		try {
			String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, args[1], name);

			if (PasswordSecurity.comparePasswordWithHash(args[0], PlayerCache.getInstance().getAuth(name).getHash(), name)) {
				PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
				auth.setHash(hashnew);
				database.updatePassword(auth);
				PlayerCache.getInstance().updatePlayer(auth);
				player.sendMessage(messages.getMessage("pwd_changed"));
				ConsoleLogger.info(player.getName() + " changed his password");
			} else {
				player.sendMessage(messages.getMessage("wrong_pwd"));
			}
		} catch (NoSuchAlgorithmException ex) {
			ConsoleLogger.showError(ex.getMessage());
			sender.sendMessage(messages.getMessage("error"));
		}
		return true;
	}
}
