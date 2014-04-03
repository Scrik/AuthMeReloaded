package fr.xephi.authme.api;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;

public class RecodedAPI {

	private static AuthMe instance;
	private static DataSource database;

	public RecodedAPI(AuthMe instance, DataSource database) {
		RecodedAPI.instance = instance;
		RecodedAPI.database = database;
	}
	/**
	 * Hook into AuthMe
	 * @return AuthMe instance
	 */
	public static AuthMe hookAuthMe() {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("AuthMe");
		return (AuthMe) plugin;
	}

	/**
	 *
	 * @param player
	 * @return true if player is authenticate
	 */
	public static boolean isAuthenticated(Player player) {
		return PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase());
	}

	/**
	 *
	 * @param playerName
	 * @return true if player is registered
	 */
	public static boolean isRegistered(String playerName) {
		String player = playerName.toLowerCase();
		return database.isAuthAvailable(player);
	}

	/**
	 * @param String playerName, String passwordToCheck
	 * @return true if the password is correct , false else
	 */
	public static boolean checkPassword(String playerName, String passwordToCheck) {
		if (!isRegistered(playerName)) {
			return false;
		}
		String player = playerName.toLowerCase();
		PlayerAuth auth = database.getAuth(player);
		try {
			return PasswordSecurity.comparePasswordWithHash(passwordToCheck, auth.getHash(), player);
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
	}

	/**
	 * Register a player
	 * @param String playerName, String password
	 * @return true if the player is register correctly
	 */
	public static boolean registerPlayer(String playerName, String password) {
		try {
			String name = playerName.toLowerCase();
			String hash = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
			if (isRegistered(name)) {
				return false;
			}
			PlayerAuth auth = new PlayerAuth(name, hash, "198.18.0.1", 0);
			if (!database.saveAuth(auth)) {
				return false;
			}
			return true;
		} catch (NoSuchAlgorithmException ex) {
			return false;
		}
	}

	/**
	 * Force a player to login
	 * @param Player player
	 */
	public static void forceLogin(Player player) {
		instance.management.performLogin(player, "dontneed", true);
	}

	/**
	 * Check if can register from this ip
	 * @param Player player
	 */
	public static boolean canRegister(Player player) {
		String ip = player.getAddress().getAddress().getHostAddress();
		return database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp;
	}

}
