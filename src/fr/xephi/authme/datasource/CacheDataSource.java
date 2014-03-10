package fr.xephi.authme.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;


public class CacheDataSource implements DataSource {

	private DataSource source;
	public AuthMe plugin;
	private HashMap<String, PlayerAuth> authCache = new HashMap<String, PlayerAuth>();
	private HashMap<String, List<String>> ipCache = new HashMap<String, List<String>>();

	public CacheDataSource(AuthMe plugin, DataSource source) {
		this.plugin = plugin;
		this.source = source;
		cacheAllAuths();
	}


	@Override
	public synchronized boolean isAuthAvailable(String user) {
		return authCache.containsKey(user);
	}

	@Override
	public synchronized PlayerAuth getAuth(String user) {
		return authCache.get(user);
	}

	@Override
	public synchronized boolean saveAuth(PlayerAuth auth) {
		if (source.saveAuth(auth)) {
			cacheAuth(auth);
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean removeAuth(String user) {
		if (source.removeAuth(user)) {
			clearAuth(user);
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean updatePassword(PlayerAuth auth) {
		if (source.updatePassword(auth)) {
			authCache.get(auth.getNickname()).setHash(auth.getHash());
			return true;
		}
		return false;
	}

	@Override
	public boolean updateSession(PlayerAuth auth) {
		if (source.updateSession(auth)) {
			authCache.get(auth.getNickname()).setIp(auth.getIp());
			authCache.get(auth.getNickname()).setLastLogin(auth.getLastLogin());
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean updateSalt(PlayerAuth auth) {
		if(source.updateSalt(auth)) {
			authCache.get(auth.getNickname()).setSalt(auth.getSalt());
			return true;
		}
		return false;
	}

	@Override
	public synchronized List<String> getAllAuthsByIp(String ip) {
		if (ipCache.containsKey(ip)) {
			return ipCache.get(ip);
		} else {
			return new ArrayList<String>();
		}
	}

	@Override
	public List<String> autoPurgeDatabase(long until) {
		List<String> cleared = source.autoPurgeDatabase(until);
		if (cleared.size() > 0) {
			for (PlayerAuth auth : authCache.values()) {
				if(auth.getLastLogin() < until) {
					clearAuth(auth.getNickname());
				}
			}
		}
		return cleared;
	}

	@Override
	public synchronized void purgeBanned(List<String> banned) {
		source.purgeBanned(banned);
		for (PlayerAuth auth : authCache.values()) {
			if (banned.contains(auth.getNickname())) {
				clearAuth(auth.getNickname());
			}
		}
	}

	@Override
	public List<PlayerAuth> getAllAuths() {
		return source.getAllAuths();
	}

	@Override
	public void reload() {
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

	@Override
	public synchronized void close() {
		source.close();
	}

}
