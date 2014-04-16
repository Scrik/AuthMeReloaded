package fr.xephi.authme;

import java.util.HashMap;
import me.muizers.Notifications.Notifications;
import net.citizensnpcs.Citizens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.onarandombox.MultiverseCore.MultiverseCore;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.RecodedAPI;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.commands.AdminCommand;
import fr.xephi.authme.commands.CaptchaCommand;
import fr.xephi.authme.commands.ChangePasswordCommand;
import fr.xephi.authme.commands.LoginCommand;
import fr.xephi.authme.commands.PasspartuCommand;
import fr.xephi.authme.commands.RegisterCommand;
import fr.xephi.authme.datasource.DataBackend;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FileDataBackend;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.listener.AuthMeChestShopListener;
import fr.xephi.authme.listener.AuthMeEntityListener;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.managment.Management;
import fr.xephi.authme.plugin.manager.CitizensCommunicator;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.plugin.manager.EssSpawn;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;

public class AuthMe extends JavaPlugin {

	public DataSource database = null;
	private Settings settings;
	private Messages m;
	public static Server server;
	private static AuthMe instance;
	public CitizensCommunicator citizens;
	public int CitizensVersion = 0;
	public int CombatTag = 0;
	public double ChestShop = 0;
	public Essentials ess;
	public Notifications notifications;
	public Management management;
	public HashMap<String, Integer> captcha = new HashMap<String, Integer>();
	public HashMap<String, String> cap = new HashMap<String, String>();
	public MultiverseCore multiverse = null;
	public Location essentialsSpawn;
	public boolean antibotMod = false;
	public boolean delayedAntiBot = true;

	@Override
	public void onEnable() {
		instance = this;

		citizens = new CitizensCommunicator(this);

		settings = new Settings(this);
		settings.loadConfigOptions();

		setMessages(Messages.getInstance());

		server = getServer();

		//Check Citizens Version
		citizensVersion();

		//Check Combat Tag Version
		combatTag();

		//Check Notifications
		checkNotifications();

		//Check Multiverse
		checkMultiverse();

		//Check ChestShop
		checkChestShop();

		//Check Essentials
		checkEssentials();

		//Backend
		DataBackend databackend = null;
		try {
			databackend = new FileDataBackend();
		} catch (Exception ex) {
			ConsoleLogger.showError(ex.getMessage());
			if (Settings.isStopEnabled) {
				ConsoleLogger.showError("Can't use FLAT FILE... SHUTDOWN...");
				server.shutdown();
			}
			if (!Settings.isStopEnabled) {
				this.getServer().getPluginManager().disablePlugin(this);
			}
			return;
		}

		// DataSource
		database = new DataSource(databackend);

		// Setup API
		new API(this, database);
		new RecodedAPI(this, database);

		// Setup Management
		management = new Management(database, this);

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new AuthMePlayerListener(this, database), this);
		pm.registerEvents(new AuthMeBlockListener(this), this);
		pm.registerEvents(new AuthMeEntityListener(this), this);
		if (ChestShop != 0) {
			pm.registerEvents(new AuthMeChestShopListener(database, this), this);
			ConsoleLogger.info("Successfully hook with ChestShop!");
		}

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

	private void checkChestShop() {
		if (!Settings.chestshop) {
			this.ChestShop = 0;
			return;
		}
		if (this.getServer().getPluginManager().isPluginEnabled("ChestShop")) {
			try {
				String ver = com.Acrobot.ChestShop.ChestShop.getVersion();
				try {
					double version = Double.valueOf(ver.split(" ")[0]);
					if (version >= 3.50) {
						this.ChestShop = version;
					} else {
						ConsoleLogger.showError("Please Update your ChestShop version!");
					}
				} catch (NumberFormatException nfe) {
					try {
						double version = Double.valueOf(ver.split("t")[0]);
						if (version >= 3.50) {
							this.ChestShop = version;
						} else {
							ConsoleLogger.showError("Please Update your ChestShop version!");
						}
					} catch (NumberFormatException nfee) {
					}
				}
			} catch (NullPointerException npe) {}
			catch (NoClassDefFoundError ncdfe) {}
			catch (ClassCastException cce) {}
		}
	}

	private void checkMultiverse() {
		if(!Settings.multiverse) {
			multiverse = null;
			return;
		}
		if (this.getServer().getPluginManager().getPlugin("Multiverse-Core") != null && this.getServer().getPluginManager().getPlugin("Multiverse-Core").isEnabled()) {
			try {
				multiverse  = (MultiverseCore) this.getServer().getPluginManager().getPlugin("Multiverse-Core");
				ConsoleLogger.info("Hook with Multiverse-Core for SpawnLocations");
			} catch (NullPointerException npe) {
				multiverse = null;
			} catch (ClassCastException cce) {
				multiverse = null;
			} catch (NoClassDefFoundError ncdfe) {
				multiverse = null;
			}
		}
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
			this.essentialsSpawn = new EssSpawn().getLocation();
			ConsoleLogger.info("Hook with EssentialsSpawn plugin");
		}
	}

	private void checkNotifications() {
		if (!Settings.notifications) {
			this.notifications = null;
			return;
		}
		if (this.getServer().getPluginManager().getPlugin("Notifications") != null && this.getServer().getPluginManager().getPlugin("Notifications").isEnabled()) {
			this.notifications = (Notifications) this.getServer().getPluginManager().getPlugin("Notifications");
			ConsoleLogger.info("Successfully hook with Notifications");
		} else {
			this.notifications = null;
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

	public void savePlayer(Player player) throws IllegalStateException, NullPointerException {
		try {
			if ((citizens.isNPC(player, this)) || (Utils.getInstance().isUnrestricted(player)) || (CombatTagComunicator.isNPC(player))) {
				return;
			}
		} catch (Exception e) { }
		try {
			String name = player.getName().toLowerCase();
			if (LimboCache.getInstance().hasLimboPlayer(name))
			{
				LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
				if (Settings.protectInventoryBeforeLogInEnabled) {
					player.getInventory().setArmorContents(limbo.getArmour());
					player.getInventory().setContents(limbo.getInventory());
				}
				player.teleport(limbo.getLoc());
				player.setOp(limbo.getOperator());
				Bukkit.getScheduler().cancelTask(limbo.getTimeoutTaskId());
				LimboCache.getInstance().deleteLimboPlayer(name);
			}
			PlayerCache.getInstance().removePlayer(name);
		} catch (Exception ex) {
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
		if (multiverse != null && Settings.multiverse) {
			try {
				spawnLoc = multiverse.getMVWorldManager().getMVWorld(world).getSpawnLocation();
			} catch (NullPointerException npe) {
			} catch (ClassCastException cce) {
			} catch (NoClassDefFoundError ncdfe) {
			}
		}
		if (essentialsSpawn != null) {
			spawnLoc = essentialsSpawn;
		}
		if (Spawn.getInstance().getLocation() != null) {
			spawnLoc = Spawn.getInstance().getLocation();
		}
		return spawnLoc;
	}

}
