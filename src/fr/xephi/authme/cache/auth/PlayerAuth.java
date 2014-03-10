package fr.xephi.authme.cache.auth;

import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

public class PlayerAuth {

	private String nickname = "";
	private String hash = "";
	private String ip = "198.18.0.1";
	private long lastLogin = 0;
	private int x = 0;
	private int y = 0;
	private int z = 0;
	private String world = "world";
	private String salt = "";
	private String vBhash = null;

	public PlayerAuth(String nickname, String hash, String ip, long lastLogin, String email) {
		this.nickname = nickname;
		this.hash = hash;
		this.ip = ip;
		this.lastLogin = lastLogin;
	}

	public PlayerAuth(String nickname, int x, int y, int z, String world) {
		this.nickname = nickname;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
	}

	public PlayerAuth(String nickname, String hash, String ip, long lastLogin, int x, int y, int z, String world, String email) {
		this.nickname = nickname;
		this.hash = hash;
		this.ip = ip;
		this.lastLogin = lastLogin;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
	}

	public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin) {
		this.nickname = nickname;
		this.hash = hash;
		this.ip = ip;
		this.lastLogin = lastLogin;
		this.salt = salt;
	}

	public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin, int x, int y, int z, String world, String email) {
		this.nickname = nickname;
		this.hash = hash;
		this.ip = ip;
		this.lastLogin = lastLogin;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.salt = salt;
	}

	public String getIp() {
		return ip;
	}

	public String getNickname() {
		return nickname;
	}

	public String getHash() {
		if(salt != null && !salt.isEmpty() && Settings.getPasswordHash == HashAlgorithm.MD5VB) {
			vBhash = "$MD5vb$"+salt+"$"+hash;
			return vBhash;
		}
		else {
			return hash;
		}
	}

	public String getSalt() {
		return this.salt;
	}

	public int getQuitLocX() {
		return x;
	}
	public int getQuitLocY() {
		return y;
	}
	public int getQuitLocZ() {
		return z;
	}
	public void setQuitLocX(int x) {
		this.x = x;
	}
	public void setQuitLocY(int y) {
		this.y = y;
	}
	public void setQuitLocZ(int z) {
		this.z = z;
	}
	public long getLastLogin() {
		return lastLogin;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PlayerAuth)) {
			return false;
		}
		PlayerAuth other = (PlayerAuth) obj;
		return other.getIp().equals(this.ip) && other.getNickname().equals(this.nickname);
	}

	@Override
	public int hashCode() {
		int hashCode = 7;
		hashCode = 71 * hashCode + (this.nickname != null ? this.nickname.hashCode() : 0);
		hashCode = 71 * hashCode + (this.ip != null ? this.ip.hashCode() : 0);
		return hashCode;
	}

	public void setWorld(String world) {
		this.world = world;
	}

	public String getWorld() {
		return world;
	}

	@Override
	public String toString() {
		String s = "Player : " + nickname + " ! IP : " + ip + " ! LastLogin : " + lastLogin + " ! LastPosition : " + x + "," + y + "," + z + "," + world
				+ " ! Hash : " + hash + " ! Salt : " + salt;
		return s;

	}

}
