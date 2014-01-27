package fr.xephi.authme.datasource;

import java.util.List;

import fr.xephi.authme.cache.auth.PlayerAuth;


public interface DataSource {

    public enum DataSourceType {

        MYSQL, FILE, SQLITE
    }
    
    public boolean isAuthAvailable(String user);

    PlayerAuth getAuth(String user);

    boolean saveAuth(PlayerAuth auth);

    boolean updateSession(PlayerAuth auth);

    boolean updatePassword(PlayerAuth auth);
    
    List<String> autoPurgeDatabase(long until);

    boolean removeAuth(String user);

    List<String> getAllAuthsByIp(String ip);

    List<String> getAllAuthsByEmail(String email);

    boolean updateEmail(PlayerAuth auth);

    boolean updateSalt(PlayerAuth auth);

    void close();

    void reload();

    void purgeBanned(List<String> banned);
    
    List<PlayerAuth> getAllAuths();

}
