package fr.xephi.authme.datasource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;


public class FileDataSource implements DataSource {

	/* file layout:
	 *
	 * DBVER$1:PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS
	 *
	 * Old but compatible:
	 * PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS:LASTPOSX:LASTPOSY:LASTPOSZ:LASTPOSWORLD
	 * PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS
	 * PLAYERNAME:HASHSUM:IP
	 * PLAYERNAME:HASHSUM
	 *
	 */
	private File source;

	private int dbvers = 1;

	public FileDataSource() throws IOException {
		source = new File(Settings.AUTH_FILE);
		source.createNewFile();
		convertDatabase();
	}

	@Override
	public synchronized boolean isAuthAvailable(String user) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = convertDBStringToAuth(line);
				if (auth.getNickname().equals(user)) {
					br.close();
					return true;
				}
			}
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return false;
	}

	@Override
	public synchronized boolean saveAuth(PlayerAuth auth) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(source, true));
			bw.write(convertAuthToDBString(auth));
			bw.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized boolean updatePassword(PlayerAuth auth) {
		PlayerAuth newAuth = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth oldauth = convertDBStringToAuth(line);
				if (oldauth.getNickname().equals(auth.getNickname())) {
					newAuth = new PlayerAuth(oldauth.getNickname(), auth.getHash(), oldauth.getIp(), oldauth.getLastLogin());
				}
			}
			br.close();
		} catch (Exception ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		}
		removeAuth(auth.getNickname());
		saveAuth(newAuth);
		return true;
	}

	@Override
	public synchronized boolean updateSession(PlayerAuth auth) {
		PlayerAuth newAuth = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth oldauth = convertDBStringToAuth(line);
				if (oldauth.getNickname().equals(auth.getNickname())) {
					newAuth = new PlayerAuth(oldauth.getNickname(), oldauth.getHash(), auth.getIp(), auth.getLastLogin());
				}
			}
			br.close();
		} catch (Exception ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		}
		removeAuth(auth.getNickname());
		saveAuth(newAuth);
		return true;
	}

	@Override
	public List<String> autoPurgeDatabase(long until) {
		List<String> lines = new ArrayList<String>();
		List<String> cleared = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = convertDBStringToAuth(line);
				if (auth.getLastLogin() >= until) {
					lines.add(line);
				} else {
					cleared.add(auth.getNickname());
				}
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(source));
			for (String l : lines) {
				bw.write(l + "\n");
			}
			br.close();
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			return cleared;
		}
		return cleared;
	}

	@Override
	public synchronized boolean removeAuth(String user) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = convertDBStringToAuth(line);
				if (!auth.getNickname().equals(user)) {
					lines.add(line);
				}
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(source));
			for (String l : lines) {
				bw.write(l + "\n");
			}
			br.close();
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized PlayerAuth getAuth(String user) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = convertDBStringToAuth(line);
				if (auth.getNickname().equals(user)) {
					br.close();
					return auth;
				}
			}
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return null;
	}

	@Override
	public void reload() {
		convertDatabase();
	}

	@Override
	public List<String> getAllAuthsByIp(String ip) {
		List<String> countIp = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = convertDBStringToAuth(line);
				if (auth.getIp().equals(ip)) {
					countIp.add(auth.getIp());
				}
			}
			br.close();
			return countIp;
		} catch (Exception ex) {
			ConsoleLogger.showError(ex.getMessage());
			return new ArrayList<String>();
		}
	}

	@Override
	public List<PlayerAuth> getAllAuths() {
		List<PlayerAuth> auths = new ArrayList<PlayerAuth>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				auths.add(convertDBStringToAuth(line));
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return auths;
	}

	private String convertAuthToDBString(PlayerAuth auth) {
		StringBuilder sb = new StringBuilder();
		sb.append("DBVER$");
		sb.append(dbvers);
		sb.append(":");
		sb.append(auth.getNickname());
		sb.append(":");
		sb.append(auth.getHash());
		sb.append(":");
		sb.append(auth.getIp());
		sb.append(":");
		sb.append(auth.getLastLogin());
		sb.append("\n");
		return sb.toString();
	}

	private PlayerAuth convertDBStringToAuth(String dbstring) {
		String[] args = dbstring.split(":");
		return new PlayerAuth(args[1], args[2], args[3], Long.parseLong(args[4]));
	}

	@Override
	public void convertDatabase() {
		List<PlayerAuth> auths = new ArrayList<PlayerAuth>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("DBVER$"+dbvers)) {
					auths.add(parseOldAuth(line));
				} else {
					auths.add(convertDBStringToAuth(line));
				}
			}
			br.close();
			BufferedWriter writer = new BufferedWriter(new FileWriter(source, false));
			for (PlayerAuth auth : auths) {
				try {
					writer.write(convertAuthToDBString(auth));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private PlayerAuth parseOldAuth(String line) {
		PlayerAuth auth = null;
		String[] args = line.split(":");
		switch (args.length) {
			case 9: case 8: case 7: case 4: {
				auth = new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]));
				break;
			}
			case 3: {
				auth = new PlayerAuth(args[0], args[1], args[2], 0);
				break;
			}
			case 2: {
				auth = new PlayerAuth(args[0], args[1], "198.18.0.1", 0);
				break;
			}
		}
		return auth;
	}

}
