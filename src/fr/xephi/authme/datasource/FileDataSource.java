package fr.xephi.authme.datasource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
	 * PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS:LASTPOSX:LASTPOSY:LASTPOSZ:LASTPOSWORLD
	 *
	 * Old but compatible:
	 * PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS
	 * PLAYERNAME:HASHSUM:IP
	 * PLAYERNAME:HASHSUM
	 *
	 */
	private File source;

	public FileDataSource() throws IOException {
		source = new File(Settings.AUTH_FILE);
		source.createNewFile();
	}

	@Override
	public synchronized boolean isAuthAvailable(String user) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				if (args.length > 1 && args[0].equals(user)) {
					return true;
				}
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
		}
		return false;
	}

	@Override
	public synchronized boolean saveAuth(PlayerAuth auth) {
		if (isAuthAvailable(auth.getNickname())) {
			return false;
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(source, true));
			bw.write(auth.getNickname() + ":" + auth.getHash() + ":" + auth.getIp() + ":" + auth.getLastLogin() + ":" + auth.getQuitLocX() + ":" + auth.getQuitLocY() + ":" + auth.getQuitLocZ() + ":" + auth.getWorld() + "\n");
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException ex) {
				}
			}
		}
		return true;
	}

	@Override
	public synchronized boolean updatePassword(PlayerAuth auth) {
		if (!isAuthAvailable(auth.getNickname())) {
			return false;
		}
		PlayerAuth newAuth = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(source));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				if (args[0].equals(auth.getNickname())) {
					switch (args.length) {
						case 9: case 8: {
							newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), args[7]);
							break;
						}
						case 7: {
							newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), "world");
							break;
						}
						case 4: {
							newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), 0, 0, 0, "world");
							break;
						}
						default: {
							newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], 0, 0, 0, 0, "world");
							break;
						}
					}
					break;
				}
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
		}
		removeAuth(auth.getNickname());
		saveAuth(newAuth);
		return true;
	}

	@Override
	public boolean updateSession(PlayerAuth auth) {
		if (!isAuthAvailable(auth.getNickname())) {
			return false;
		}
		PlayerAuth newAuth = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(source));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				if (args[0].equals(auth.getNickname())) {
					switch (args.length) {
						case 8: case 9: {
							newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), args[7]);
							break;
						}
						case 7: {
							newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), "world");
							break;
						}
						default: {
							newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), 0, 0, 0, "world");
							break;
						}
					}
					break;
				}
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
		}
		removeAuth(auth.getNickname());
		saveAuth(newAuth);
		return true;
	}

	@Override
	public List<String> autoPurgeDatabase(long until) {
		BufferedReader br = null;
		BufferedWriter bw = null;
		ArrayList<String> lines = new ArrayList<String>();
		List<String> cleared = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				if (args.length >= 4) {
					if (Long.parseLong(args[3]) >= until) {
						lines.add(line);
						continue;
					} else {
						cleared.add(args[0]);
					}
				}

			}
			bw = new BufferedWriter(new FileWriter(source));
			for (String l : lines) {
				bw.write(l + "\n");
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return cleared;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return cleared;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException ex) {
				}
			}
		}
		return cleared;
	}

	@Override
	public synchronized boolean removeAuth(String user) {
		if (!isAuthAvailable(user)) {
			return false;
		}
		BufferedReader br = null;
		BufferedWriter bw = null;
		ArrayList<String> lines = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				if (args.length > 1 && !args[0].equals(user)) {
					lines.add(line);
				}
			}
			bw = new BufferedWriter(new FileWriter(source));
			for (String l : lines) {
				bw.write(l + "\n");
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return false;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException ex) {
				}
			}
		}
		return true;
	}

	@Override
	public synchronized PlayerAuth getAuth(String user) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = parseAuth(line);
				if (auth.getNickname().equals(user)) {
					return auth;
				}
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return null;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return null;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
		}
		return null;
	}

	@Override
	public void reload() {
	}

	@Override
	public List<String> getAllAuthsByIp(String ip) {
		BufferedReader br = null;
		List<String> countIp = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				if (args.length > 3 && args[2].equals(ip)) {
					countIp.add(args[0]);
				}
			}
			return countIp;
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return new ArrayList<String>();
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return new ArrayList<String>();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	@Override
	public void purgeBanned(List<String> banned) {
		BufferedReader br = null;
		BufferedWriter bw = null;
		ArrayList<String> lines = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split(":");
				try {
					if (banned.contains(args[0])) {
						lines.add(line);
					}
				} catch (NullPointerException npe) {}
				catch (ArrayIndexOutOfBoundsException aioobe) {}
			}
			bw = new BufferedWriter(new FileWriter(source));
			for (String l : lines) {
				bw.write(l + "\n");
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return;
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
			return;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException ex) {
				}
			}
		}
		return;
	}

	@Override
	public List<PlayerAuth> getAllAuths() {
		List<PlayerAuth> auths = new ArrayList<PlayerAuth>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(source));
			String line;
			while ((line = br.readLine()) != null) {
				PlayerAuth auth = parseAuth(line);
				if (auth != null) {
					auths.add(auth);
				}
			}
		} catch (FileNotFoundException ex) {
			ConsoleLogger.showError(ex.getMessage());
		} catch (IOException ex) {
			ConsoleLogger.showError(ex.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
				}
			}
		}
		return auths;
	}
	
	@Override
	public void convertDatabase() {
		
	}

	private PlayerAuth parseAuth(String line) {
		PlayerAuth auth = null;
		String[] args = line.split(":");
		switch (args.length) {
			case 9: case 8: {
				auth = new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), args[7]);
				break;
			}
			case 7: {
				auth = new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), "unavailableworld");
				break;
			}
			case 4: {
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
