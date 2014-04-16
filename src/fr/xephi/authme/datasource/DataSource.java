package fr.xephi.authme.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;


public class DataSource {

	private DataBackend source;

	private HashMap<String, PlayerAuth> authCache = new HashMap<String, PlayerAuth>();
	private HashMap<String, List<String>> ipCache = new HashMap<String, List<String>>();

	private BukkitTask autosavetask = null;

	public DataSource(DataBackend databackend) {
		this.source = databackend;
		cacheAllAuths();
		scheduleAutoSaveTask();
	}

	public synchronized void saveDatabase() {
		source.dumpAuths(authCache.values());
	}


	public synchronized boolean isAuthAvailable(String user) {
		return authCache.containsKey(user);
	}

	public synchronized PlayerAuth getAuth(String user) {
		return authCache.get(user);
	}

	public synchronized void saveAuth(PlayerAuth auth) {
		cacheAuth(auth);
	}

	public synchronized void removeAuth(String user) {
		clearAuth(user);
	}

	public synchronized void updatePassword(PlayerAuth auth) {
		authCache.get(auth.getNickname()).setHash(auth.getHash());
	}

	public synchronized void updateSession(PlayerAuth auth) {
		authCache.get(auth.getNickname()).setIp(auth.getIp());
		authCache.get(auth.getNickname()).setLastLogin(auth.getLastLogin());
	}

	public synchronized List<String> getAllAuthsByIp(String ip) {
		if (ipCache.containsKey(ip)) {
			return ipCache.get(ip);
		} else {
			return new ArrayList<String>();
		}
	}

	public synchronized int purgeDatabase(long until) {
		int cleared = 0;
		for (PlayerAuth auth : new ArrayList<PlayerAuth>(authCache.values())) {
			if (auth.getLastLogin() < until) {
				clearAuth(auth.getNickname());
				cleared++;
			}
		}
		return cleared;
	}

	public synchronized void reload() {
		if (autosavetask != null) {
			autosavetask.cancel();
		}
		scheduleAutoSaveTask();
		authCache.clear();
		ipCache.clear();
		cacheAllAuths();
	}

	public void cacheAllAuths() {
		List<PlayerAuth> auths = source.getAllAuths();
		for (PlayerAuth auth : auths) {
			cacheAuth(auth);
		}
		ConsoleLogger.info("Cached "+ auths.size()+ " player auths");
	}

	private void cacheAuth(PlayerAuth auth) {
		String nick = auth.getNickname();
		authCache.put(nick, auth);
		String ip = auth.getIp();
		if (!ipCache.containsKey(ip)) {
			ipCache.put(ip, new ArrayList<String>());
		}
		ipCache.get(ip).add(nick);
	}

	private void clearAuth(String nick) {
		authCache.remove(nick);
		for (List<String> ipauths : ipCache.values()) {
			ipauths.remove(nick);
		}
	}

	private void scheduleAutoSaveTask() {
		if (Settings.databaseAutoSaveEnabled) {
			autosavetask = Bukkit.getScheduler().runTaskTimerAsynchronously(
				AuthMe.getInstance(), 
				new Runnable() {
					public void run() {
						saveDatabase();
					}
				},
				20 * Settings.databaseAutoSaveInterval, 
				20 * Settings.databaseAutoSaveInterval
			);
		}
	}

}
