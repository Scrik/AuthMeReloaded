package fr.xephi.authme.datasource;

import java.util.Collection;
import java.util.List;

import fr.xephi.authme.cache.auth.PlayerAuth;

public interface DataBackend {

	public List<PlayerAuth> getAllAuths();

	public void dumpAuths(Collection<PlayerAuth> collection);

}
