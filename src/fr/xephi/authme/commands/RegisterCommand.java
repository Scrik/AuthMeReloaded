package fr.xephi.authme.commands;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import me.muizers.Notifications.Notification;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RegisterTeleportEvent;
import fr.xephi.authme.security.PasswordSecurity;
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

		final Player player = (Player) sender;
		final String name = player.getName().toLowerCase();
		final String ip = player.getAddress().getAddress().getHostAddress();

		if (PlayerCache.getInstance().isAuthenticated(name)) {
			player.sendMessage(m._("logged_in"));
			return true;
		}

		if (!Settings.isRegistrationEnabled) {
			player.sendMessage(m._("reg_disabled"));
			return true;
		}

		if (database.isAuthAvailable(player.getName().toLowerCase())) {
			player.sendMessage(m._("user_regged"));
			return true;
		}

		if (Settings.getmaxRegPerIp > 0) {
			if (!plugin.authmePermissible(sender, "authme.allow2accounts") && database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp) {
				player.sendMessage(m._("max_reg"));
				return true;
			}
		}

		if (args.length == 0 || (Settings.getEnablePasswordVerifier && args.length < 2)) {
			player.sendMessage(m._("usage_reg"));
			return true;
		}

		if (args[0].length() < Settings.getPasswordMinLen || args[0].length() > Settings.passwordMaxLength) {
			player.sendMessage(m._("pass_len"));
			return true;
		}
		try {
			String hash;
			if (Settings.getEnablePasswordVerifier) {
				if (args[0].equals(args[1])) {
					hash = PasswordSecurity.getHash(Settings.getPasswordHash, args[0], name);
				} else {
					player.sendMessage(m._("password_error"));
					return true;
				}
			} else {
				hash = PasswordSecurity.getHash(Settings.getPasswordHash, args[0], name);
			}
			auth = new PlayerAuth(name, hash, ip, new Date().getTime());
			if (!database.saveAuth(auth)) {
				player.sendMessage(m._("error"));
				return true;
			}
			PlayerCache.getInstance().addPlayer(auth);
			LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
			if (limbo != null) {
				player.setGameMode(limbo.getGameMode());
				if (Settings.isTeleportToSpawnEnabled) {
					World world = player.getWorld();
					Location loca = plugin.getSpawnLocation(world);
					RegisterTeleportEvent tpEvent = new RegisterTeleportEvent( player, loca);
					plugin.getServer().getPluginManager().callEvent(tpEvent);
					if (!tpEvent.isCancelled()) {
						player.teleport(tpEvent.getTo());
					}
				}
				sender.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
				sender.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
				LimboCache.getInstance().deleteLimboPlayer(name);
			}

			player.sendMessage(m._("registered"));

			if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
				player.setAllowFlight(false);
				player.setFlying(false);
			}
			if (!Settings.noConsoleSpam) {
				ConsoleLogger.info(player.getName() + " registered " + player.getAddress().getAddress().getHostAddress());
			}
			if (plugin.notifications != null) {
				plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " has registered!"));
			}
		} catch (NoSuchAlgorithmException ex) {
			ConsoleLogger.showError(ex.getMessage());
			sender.sendMessage(m._("error"));
		}
		return true;
	}

}
