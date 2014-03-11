package fr.xephi.authme.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;


public class DataSource {

	private DataBackend source;

	private HashMap<String, PlayerAuth> authCache = new HashMap<String, PlayerAuth>();
	private HashMap<String, List<String>> ipCache = new HashMap<String, List<String>>();

	public DataSource(DataBackend databackend) {
		this.source = databackend;
		cacheAllAuths();
	}


	public synchronized boolean isAuthAvailable(String user) {
		return authCache.containsKey(user);
	}

	public synchronized PlayerAuth getAuth(String user) {
		return authCache.get(user);
	}

	public synchronized boolean saveAuth(PlayerAuth auth) {
		if (source.saveAuth(auth)) {
			cacheAuth(auth);
			return true;
		}
		return false;
	}

	public synchronized boolean removeAuth(String user) {
		if (source.removeAuth(user)) {
			clearAuth(user);
			return true;
		}
		return false;
	}

	public synchronized boolean updatePassword(PlayerAuth auth) {
		if (source.updatePassword(auth)) {
			authCache.get(auth.getNickname()).setHash(auth.getHash());
			return true;
		}
		return false;
	}

	public boolean updateSession(PlayerAuth auth) {
		if (source.updateSession(auth)) {
			authCache.get(auth.getNickname()).setIp(auth.getIp());
			authCache.get(auth.getNickname()).setLastLogin(auth.getLastLogin());
			return true;
		}
		return false;
	}

	public synchronized List<String> getAllAuthsByIp(String ip) {
		if (ipCache.containsKey(ip)) {
			return ipCache.get(ip);
		} else {
			return new ArrayList<String>();
		}
	}

	public List<String> autoPurgeDatabase(long until) {
		List<String> cleared = source.autoPurgeDatabase(until);
		for (String nickname : cleared) {
			clearAuth(nickname);
		}
		return cleared;
	}

	public void convertDatabase() {
		source.convertDatabase();
	}

	public List<PlayerAuth> getAllAuths() {
		return source.getAllAuths();
	}

	public void reload() {
		source.reload();
		authCache.clear();
		ipCache.clear();
		cacheAllAuths();
	}

	public void cacheAllAuths() {
		List<PlayerAuth> auths = getAllAuths();
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

}
