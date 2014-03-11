package fr.xephi.authme.datasource;

import java.util.List;

import fr.xephi.authme.cache.auth.PlayerAuth;

public interface DataBackend {

	boolean saveAuth(PlayerAuth auth);

	boolean updateSession(PlayerAuth auth);

	boolean updatePassword(PlayerAuth auth);

	boolean removeAuth(String user);

	void convertDatabase();

	public List<PlayerAuth> getAllAuths();

}
