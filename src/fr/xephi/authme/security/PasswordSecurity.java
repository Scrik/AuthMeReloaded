package fr.xephi.authme.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.bukkit.Bukkit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.settings.Settings;


public class PasswordSecurity {

	private static SecureRandom rnd = new SecureRandom();

	private static String createSalt(int length) throws NoSuchAlgorithmException {
		byte[] msg = new byte[40];
		rnd.nextBytes(msg);
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		sha1.reset();
		byte[] digest = sha1.digest(msg);
		return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1,digest)).substring(0, length);
	}

	public static String getHash(HashAlgorithm alg, String password, String playerName) throws NoSuchAlgorithmException {
		EncryptionMethod method;
		try {
			if (alg != HashAlgorithm.CUSTOM) {
				method = (EncryptionMethod) alg.getclass().newInstance();
			} else {
				method = null;
			}
		} catch (InstantiationException e) {
			throw new NoSuchAlgorithmException("Problem with this hash algorithm");
		} catch (IllegalAccessException e) {
			throw new NoSuchAlgorithmException("Problem with this hash algorithm");
		}
		String salt = "";
		switch (alg) {
		case SHA256:
			salt = createSalt(16);
			break;
		case MD5VB:
			salt = createSalt(16);
			break;
		case XAUTH:
			salt = createSalt(12);
			break;
		case SMF:
			return method.getHash(password, playerName.toLowerCase());
		case MD5:
		case SHA1:
		case WHIRLPOOL:
		case PLAINTEXT:
		case SHA512:
		case DOUBLEMD5:
		case CUSTOM:
			break;
		default:
			throw new NoSuchAlgorithmException("Unknown hash algorithm");
		}
		PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
		Bukkit.getPluginManager().callEvent(event);
		method = event.getMethod();
		if (method == null) {
			throw new NoSuchAlgorithmException("Unknown hash algorithm");
		}
		return method.getHash(password, salt);
	}

	public static boolean comparePasswordWithHash(String password, String hash, String playerName) throws NoSuchAlgorithmException {
		HashAlgorithm algo = Settings.getPasswordHash;
		EncryptionMethod method;
		try {
			if (algo != HashAlgorithm.CUSTOM) {
				method = (EncryptionMethod) algo.getclass().newInstance();
			} else {
				method = null;
			}
		} catch (InstantiationException e) {
			throw new NoSuchAlgorithmException("Problem with this hash algorithm");
		} catch (IllegalAccessException e) {
			throw new NoSuchAlgorithmException("Problem with this hash algorithm");
		}
		PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
		Bukkit.getPluginManager().callEvent(event);
		method = event.getMethod();
		if (method == null) {
			throw new NoSuchAlgorithmException("Unknown hash algorithm");
		}

		if (method.comparePassword(hash, password, playerName)) {
			return true;
		}
		if (Settings.supportOldPassword) {
			try {
				if (compareWithAllEncryptionMethod(password, hash, playerName)) {
					return true;
				}
			} catch (Exception e) {}
		}
		return false;
	}

	private static boolean compareWithAllEncryptionMethod(String password, String hash, String playerName) throws NoSuchAlgorithmException {
		for (HashAlgorithm algo : HashAlgorithm.values()) {
			try {
				EncryptionMethod method = (EncryptionMethod) algo.getclass().newInstance();
				if (algo != HashAlgorithm.CUSTOM) {
					if (method.comparePassword(hash, password, playerName)) {
						PlayerAuth nAuth = AuthMe.getInstance().database.getAuth(playerName);
						if (nAuth != null) {
							nAuth.setHash(getHash(Settings.getPasswordHash, password, playerName));
							AuthMe.getInstance().database.updatePassword(nAuth);
						}
						return true;
					}
				}
			} catch (Exception e) {}
		}
		return false;
	}
}
