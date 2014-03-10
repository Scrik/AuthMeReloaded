package fr.xephi.authme.datasource;

import java.util.List;

import fr.xephi.authme.cache.auth.PlayerAuth;


public interface DataSource {

	public boolean isAuthAvailable(String user);

	PlayerAuth getAuth(String user);

	boolean saveAuth(PlayerAuth auth);

	boolean updateSession(PlayerAuth auth);

	boolean updatePassword(PlayerAuth auth);

	List<String> autoPurgeDatabase(long until);

	boolean removeAuth(String user);

	List<String> getAllAuthsByIp(String ip);

	void reload();

	void purgeBanned(List<String> banned);
	
	void convertDatabase();

	List<PlayerAuth> getAllAuths();

}
