package fr.xephi.authme.datasource;

import java.util.List;

import fr.xephi.authme.cache.auth.PlayerAuth;

public interface DataBackend {

	public boolean isAuthAvailable(String user);

	public PlayerAuth getAuth(String user);

	boolean saveAuth(PlayerAuth auth);

	boolean updateSession(PlayerAuth auth);

	boolean updatePassword(PlayerAuth auth);

	public List<String> autoPurgeDatabase(long until);

	boolean removeAuth(String user);

	public List<String> getAllAuthsByIp(String ip);

	void reload();

	void convertDatabase();

	public List<PlayerAuth> getAllAuths();

}
