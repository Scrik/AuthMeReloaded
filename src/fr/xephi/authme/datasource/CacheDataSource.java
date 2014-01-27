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
    private HashMap<String, List<String>> emailCache = new HashMap<String, List<String>>();
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
    public int getIps(String ip) {
        return source.getIps(ip);
    }

    @Override
    public List<String> autoPurgeDatabase(long until) {
        List<String> cleared = source.autoPurgeDatabase(until);
        if (cleared.size() > 0) {
            for (PlayerAuth auth : authCache.values()) {
                if(auth.getLastLogin() < until) {
                    authCache.remove(auth.getNickname());
                }
            }
        }
        return cleared;
    }

    @Override
    public synchronized boolean removeAuth(String user) {
        if (source.removeAuth(user)) {
            authCache.remove(user);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void close() {
        source.close();
    }

    @Override
    public void reload() {
    	authCache.clear();
    	cacheAllAuths();
    }

	@Override
	public synchronized boolean updateEmail(PlayerAuth auth) {
		if(source.updateEmail(auth)) {
			authCache.get(auth.getNickname()).setEmail(auth.getEmail());
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
	public synchronized List<String> getAllAuthsByEmail(String email) {
		if (emailCache.containsKey(email)) {
			return emailCache.get(email);
		} else {
			return new ArrayList<String>();
		}
	}

	@Override
	public synchronized void purgeBanned(List<String> banned) {
		source.purgeBanned(banned);
		for (PlayerAuth auth : authCache.values()) {
			if (banned.contains(auth.getNickname())) {
				authCache.remove(auth.getNickname());
			}
		}
	}
	
	public List<PlayerAuth> getAllAuths() {
		return source.getAllAuths();
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
		String email = auth.getEmail();
		if (!emailCache.containsKey(email)) {
			emailCache.put(email, new ArrayList<String>());
		}
		emailCache.get(email).add(nick);
	}

}
