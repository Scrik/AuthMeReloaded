package fr.xephi.authme;

import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.settings.Settings;

public class Utils {
	private static Utils singleton;
	int id;

	public boolean isUnrestricted(Player player) {
		if(Settings.getUnrestrictedName == null || Settings.getUnrestrictedName.isEmpty()) {
			return false;
		}
		if(Settings.getUnrestrictedName.contains(player.getName())) {
			return true;
		}
		return false;
	}

	public static Utils getInstance() {
		singleton = new Utils();
		return singleton;
	}

	public void packCoords(int x, int y, int z, String w, final Player pl)
	{
		World theWorld;
		if (w.equals("unavailableworld")) {
			theWorld = pl.getWorld();
		} else {
			theWorld = Bukkit.getWorld(w);
		}
		if (theWorld == null) {
			theWorld = pl.getWorld();
		}
		final World world = theWorld;
		final int fY = y;
		final Location locat = new Location(world, x, y + 0.4D, z);
		final Location loc = locat.getBlock().getLocation();

		Bukkit.getScheduler().scheduleSyncDelayedTask(AuthMe.getInstance(), new Runnable() {
			@Override
			public void run() {
				AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(pl, loc);
				AuthMe.getInstance().getServer().getPluginManager().callEvent(tpEvent);
				if(!tpEvent.isCancelled()) {
					if (!tpEvent.getTo().getChunk().isLoaded()) {
						tpEvent.getTo().getChunk().load();
					}
					pl.teleport(tpEvent.getTo());
				}
			}
		});

		if (!PlayerCache.getInstance().isAuthenticated(pl.getName().toLowerCase())) {
			id = Bukkit.getScheduler().scheduleSyncRepeatingTask(AuthMe.getInstance(), new Runnable()
			{
				@Override
				public void run() {
					if (!PlayerCache.getInstance().isAuthenticated(pl.getName().toLowerCase())) {
						int current = (int)pl.getLocation().getY();
						World currentWorld = pl.getWorld();
						if (current != fY && world.getName().equals(currentWorld.getName())) {
							pl.teleport(loc);
						}
					}
				}
			}, 1L, 20L);
			Bukkit.getScheduler().scheduleSyncDelayedTask(AuthMe.getInstance(), new Runnable()
			{
				@Override
				public void run() {
					Bukkit.getScheduler().cancelTask(id);
				}
			}, 60L);
		}
	}

	/*
	 * Random Token for passpartu
	 * 
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
			ConsoleLogger.info("[AuthMe] Security passpartu token: "+ token);
			writer.close();
			return true;
		} catch(Exception e) {
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
					if(tokenInfo[0].equals(inputToken) && System.currentTimeMillis()/1000-30 <= Integer.parseInt(tokenInfo[1]) ) {
						file.delete();
						reader.close();
						return true;
					}
				}
			}
		} catch(Exception e) {
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
		Random rnd = new Random ();
		char[] arr = new char[5];
		for (int i=0; i<5; i++) {
			int n = rnd.nextInt (36);
			arr[i] = (char) (n < 10 ? '0'+n : 'a'+n-10);
		}
		return new String(arr);
	}

	/*
	 * Used for force player GameMode
	 */
	public static void forceGM(Player player) {
		if (!AuthMe.getInstance().authmePermissible(player, "authme.bypassforcesurvival")) {
			player.setGameMode(GameMode.SURVIVAL);
		}
	}

	public enum groupType {
		UNREGISTERED, REGISTERED, NOTLOGGEDIN, LOGGEDIN
	}

}
