package fr.xephi.authme;

import java.io.IOException;
import java.util.HashMap;

import net.citizensnpcs.Citizens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.RecodedAPI;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.login.LoginCache;
import fr.xephi.authme.cache.login.LoginPlayer;
import fr.xephi.authme.commands.AdminCommand;
import fr.xephi.authme.commands.CaptchaCommand;
import fr.xephi.authme.commands.ChangePasswordCommand;
import fr.xephi.authme.commands.LoginCommand;
import fr.xephi.authme.commands.PasspartuCommand;
import fr.xephi.authme.commands.RegisterCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FileDataBackend;
import fr.xephi.authme.listener.AuthMeRestrictListener;
import fr.xephi.authme.listener.AuthMeAuthListener;
import fr.xephi.authme.managment.Management;
import fr.xephi.authme.plugin.manager.CitizensCommunicator;
import fr.xephi.authme.plugin.manager.EssSpawn;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;

public class AuthMe extends JavaPlugin {

	public DataSource database = null;
	private Settings settings;
	private Messages m;
	private static AuthMe instance;
	public CitizensCommunicator citizens;
	public int CitizensVersion = 0;
	public int CombatTag = 0;
	public Essentials ess;
	public Management management;
	public HashMap<String, Integer> captcha = new HashMap<String, Integer>();
	public HashMap<String, String> cap = new HashMap<String, String>();
	public Location essentialsSpawn;

	@Override
	public void onEnable() {
		instance = this;

		citizens = new CitizensCommunicator(this);

		settings = new Settings(this);
		settings.loadConfigOptions();

		setMessages(Messages.getInstance());

		//Check Citizens Version
		citizensVersion();

		//Check Combat Tag Version
		combatTag();

		//Check Essentials
		checkEssentials();

		//Backend
		FileDataBackend databackend;
		try {
			databackend = new FileDataBackend();
		} catch (IOException e) {
			ConsoleLogger.showError(e.getMessage());
			if (Settings.isStopEnabled) {
				ConsoleLogger.showError("Can't use FLAT FILE... SHUTDOWN...");
				Bukkit.shutdown();
			}
			if (!Settings.isStopEnabled) {
				this.getServer().getPluginManager().disablePlugin(this);
			}
			return;
		}

		// DataSource
		database = new DataSource(databackend);

		// Setup API
		API.setupAPI(this, database);
		RecodedAPI.setupRecodedAPI(this, database);

		// Setup Management
		management = new Management(database, this);

		// Setup Listener
		getServer().getPluginManager().registerEvents(new AuthMeRestrictListener(this), this);
		getServer().getPluginManager().registerEvents(new AuthMeAuthListener(this, database), this);

		// Setup commands
		this.getCommand("authme").setExecutor(new AdminCommand(this, database));
		this.getCommand("register").setExecutor(new RegisterCommand(database, this));
		this.getCommand("login").setExecutor(new LoginCommand(this));
		this.getCommand("changepassword").setExecutor(new ChangePasswordCommand(database, this));
		this.getCommand("passpartu").setExecutor(new PasspartuCommand(this));
		this.getCommand("captcha").setExecutor(new CaptchaCommand(this));

		if(!Settings.isForceSingleSessionEnabled) {
			ConsoleLogger.showError("ATTENTION by disabling ForceSingleSession, your server protection is set to low");
		}
		ConsoleLogger.info("Authme " + this.getDescription().getVersion() + " enabled");
	}

	private void checkEssentials() {
		if (this.getServer().getPluginManager().getPlugin("Essentials") != null && this.getServer().getPluginManager().getPlugin("Essentials").isEnabled()) {
			try {
				ess  = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials");
				ConsoleLogger.info("Hook with Essentials plugin");
			} catch (NullPointerException npe) {
				ess = null;
			} catch (ClassCastException cce) {
				ess = null;
			} catch (NoClassDefFoundError ncdfe) {
				ess = null;
			}
		}
		if (this.getServer().getPluginManager().getPlugin("EssentialsSpawn") != null && this.getServer().getPluginManager().getPlugin("EssentialsSpawn").isEnabled()) {
			this.essentialsSpawn = EssSpawn.getInstance().getLocation();
			ConsoleLogger.info("Hook with EssentialsSpawn plugin");
		}
	}

	private void combatTag() {
		if (this.getServer().getPluginManager().getPlugin("CombatTag") != null && this.getServer().getPluginManager().getPlugin("CombatTag").isEnabled()) {
			this.CombatTag = 1;
		} else {
			this.CombatTag = 0;
		}
	}

	private void citizensVersion() {
		if (this.getServer().getPluginManager().getPlugin("Citizens") != null && this.getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
			Citizens cit = (Citizens) this.getServer().getPluginManager().getPlugin("Citizens");
			String ver = cit.getDescription().getVersion();
			String[] args = ver.split("\\.");
			if (args[0].contains("1")) {
				this.CitizensVersion = 1;
			} else {
				this.CitizensVersion = 2;
			}
		} else {
			this.CitizensVersion = 0;
		}
	}



	@Override
	public void onDisable() {
		if (Bukkit.getOnlinePlayers() != null) {
			for(Player player : Bukkit.getOnlinePlayers()) {
				this.savePlayer(player);
			}
		}
		database.saveDatabase();
		ConsoleLogger.info("Authme " + this.getDescription().getVersion() + " disabled");
	}

	public static AuthMe getInstance() {
		return instance;
	}

	public void savePlayer(Player player) {
		String name = player.getName().toLowerCase();
		if (LoginCache.getInstance().hasPlayer(name)) {
			LoginPlayer limbo = LoginCache.getInstance().getPlayer(name);
			if (Settings.protectInventoryBeforeLogInEnabled) {
				player.getInventory().setArmorContents(limbo.getArmour());
				player.getInventory().setContents(limbo.getInventory());
			}
			player.teleport(limbo.getLoc());
			Bukkit.getScheduler().cancelTask(limbo.getTimeoutTaskId());
			LoginCache.getInstance().deletePlayer(name);
		}
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			PlayerCache.getInstance().removePlayer(name);
		}
	}

	public CitizensCommunicator getCitizensCommunicator() {
		return citizens;
	}

	public void setMessages(Messages m) {
		this.m = m;
	}

	public Messages getMessages() {
		return m;
	}

	public boolean authmePermissible(Player player, String perm) {
		if (player.hasPermission(perm)) {
			return true;
		}
		return false;
	}

	public boolean authmePermissible(CommandSender sender, String perm) {
		if (sender.hasPermission(perm)) {
			return true;
		}
		return false;
	}


	public Location getSpawnLocation(World world) {
		Location spawnLoc = world.getSpawnLocation();
		if (essentialsSpawn != null) {
			spawnLoc = essentialsSpawn;
		}
		if (Spawn.getInstance().getLocation() != null) {
			spawnLoc = Spawn.getInstance().getLocation();
		}
		return spawnLoc;
	}

}
