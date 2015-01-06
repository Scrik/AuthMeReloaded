package fr.xephi.authme;

import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Scanner;

import org.bukkit.entity.Player;

import fr.xephi.authme.api.API;
import fr.xephi.authme.settings.Settings;

public class Utils {

	private static Utils singleton = new Utils();

	public static Utils getInstance() {
		return singleton;
	}

	public boolean isUnrestricted(Player player) {
		if (Settings.getUnrestrictedName == null || Settings.getUnrestrictedName.isEmpty()) {
			return false;
		}
		if (Settings.getUnrestrictedName.contains(player.getName())) {
			return true;
		}
		return false;
	}

	/*
	 * Random Token for passpartu
	 */
	public boolean obtainToken() {
		File file = new File("plugins/AuthMe/passpartu.token");
		if (file.exists()) {
			file.delete();
		}

		FileWriter writer = null;
		try {
			file.createNewFile();
			writer = new FileWriter(file);
			String token = generateToken();
			writer.write(token + ":" + System.currentTimeMillis() / 1000 + API.newline);
			writer.flush();
			ConsoleLogger.info("[AuthMe] Security passpartu token: " + token);
			writer.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * Read Token
	 */
	public boolean readToken(String inputToken) {
		File file = new File("plugins/AuthMe/passpartu.token");

		if (!file.exists()) {
			return false;
		}

		if (inputToken.isEmpty()) {
			return false;
		}
		Scanner reader = null;
		try {
			reader = new Scanner(file);
			while (reader.hasNextLine()) {
				final String line = reader.nextLine();
				if (line.contains(":")) {
					String[] tokenInfo = line.split(":");
					if (tokenInfo[0].equals(inputToken) && System.currentTimeMillis() / 1000 - 30 <= Integer.parseInt(tokenInfo[1])) {
						file.delete();
						reader.close();
						return true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		reader.close();
		return false;
	}

	/*
	 * Generate Random Token
	 */
	private String generateToken() {
		// obtain new random token
		Random rnd = new Random();
		char[] arr = new char[5];
		for (int i = 0; i < 5; i++) {
			int n = rnd.nextInt(36);
			arr[i] = (char) (n < 10 ? '0' + n : 'a' + n - 10);
		}
		return new String(arr);
	}

}
