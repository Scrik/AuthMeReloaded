package fr.xephi.authme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import com.earth2me.essentials.Essentials;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


import me.muizers.Notifications.Notifications;
import net.citizensnpcs.Citizens;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import com.maxmind.geoip.LookupService;
import com.onarandombox.MultiverseCore.MultiverseCore;

import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.commands.AdminCommand;
import fr.xephi.authme.commands.CaptchaCommand;
import fr.xephi.authme.commands.ChangePasswordCommand;
import fr.xephi.authme.commands.EmailCommand;
import fr.xephi.authme.commands.LoginCommand;
import fr.xephi.authme.commands.PasspartuCommand;
import fr.xephi.authme.commands.RegisterCommand;
import fr.xephi.authme.commands.UnregisterCommand;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FileDataSource;
import fr.xephi.authme.datasource.MySQLDataSource;
import fr.xephi.authme.datasource.SqliteDataSource;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.listener.AuthMeBungeeCordListener;
import fr.xephi.authme.listener.AuthMeChestShopListener;
import fr.xephi.authme.listener.AuthMeEntityListener;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.listener.AuthMeSpoutListener;
import fr.xephi.authme.plugin.manager.BungeeCordMessage;
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
    public static Plugin authme;
    public static Permission permission;
	private static AuthMe instance;
    private JavaPlugin plugin;
	public CitizensCommunicator citizens;
	public SendMailSSL mail = null;
	public int CitizensVersion = 0;
	public int CombatTag = 0;
	public double ChestShop = 0;
	public boolean BungeeCord = false;
	public Essentials ess;
	public Notifications notifications;
	public API api;
	public Management management;
    public HashMap<String, Integer> captcha = new HashMap<String, Integer>();
    public HashMap<String, String> cap = new HashMap<String, String>();
    public HashMap<String, String> realIp = new HashMap<String, String>();
	public MultiverseCore multiverse = null;
	public Location essentialsSpawn;
	public LookupService ls = null;
	public boolean antibotMod = false;
	public boolean delayedAntiBot = true;

    @Override
    public void onEnable() {
    	instance = this;
    	authme = instance;
    	
    	citizens = new CitizensCommunicator(this);

        settings = new Settings(this);
        settings.loadConfigOptions();
        
        if (Settings.enableAntiBot) {
        	Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					delayedAntiBot = false;
				}
        	}, 2400);
        }

        setMessages(Messages.getInstance());

        server = getServer();

        //Load MailApi
        if(!Settings.getmailAccount.isEmpty() && !Settings.getmailPassword.isEmpty())
        	mail = new SendMailSSL(this);

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

        /*
         *  Back style on start if avaible
         */
        if(Settings.isBackupActivated && Settings.isBackupOnStart) {
        Boolean Backup = new PerformBackup(this).DoBackup();
        if(Backup) ConsoleLogger.info("Backup Complete");
            else ConsoleLogger.showError("Error while making Backup");
        }

        /*
         * Backend MYSQL - FILE - SQLITE
         */
        switch (Settings.getDataSource) {
            case FILE:
                try {
                    database = new FileDataSource();
                } catch (Exception ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use FLAT FILE... SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            case MYSQL:
                try {
                    database = new MySQLDataSource();
                } catch (Exception ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            case SQLITE:
                try {
                     database = new SqliteDataSource();
                } catch (Exception ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use SQLITE... ! SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
        }

        if (Settings.isCachingEnabled) {
            database = new CacheDataSource(this, database);
            CacheDataSource.class.cast(database).preload(Settings.authcachepreload);
        }

        // Setup API
    	api = new API(this, database);

		// Setup Management
		management = new Management(database, this);

        PluginManager pm = getServer().getPluginManager();
        if (pm.isPluginEnabled("Spout")) {
        	pm.registerEvents(new AuthMeSpoutListener(database), this);
        	ConsoleLogger.info("Successfully hook with Spout!");
        }
        pm.registerEvents(new AuthMePlayerListener(this,database),this);
        pm.registerEvents(new AuthMeBlockListener(database, this),this);
        pm.registerEvents(new AuthMeEntityListener(database, this),this);
        if (ChestShop != 0) {
        	pm.registerEvents(new AuthMeChestShopListener(database, this), this);
        	ConsoleLogger.info("Successfully hook with ChestShop!");
        }
        if (Settings.bungee) {
        	Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        	Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeCordMessage(this));
        	pm.registerEvents(new AuthMeBungeeCordListener(database), this);
        	ConsoleLogger.info("Successfully hook with BungeeCord!");
        }

        //Find Permissions
        if (pm.getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null) {
            	permission = permissionProvider.getProvider();
            	ConsoleLogger.info("Vault plugin detected, hook with " + permission.getName() + " system");
            }
            else {
            	ConsoleLogger.showError("Vault plugin is detected but not the permissions plugin!");
            }
        }

        this.getCommand("authme").setExecutor(new AdminCommand(this, database));
        this.getCommand("register").setExecutor(new RegisterCommand(database, this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("changepassword").setExecutor(new ChangePasswordCommand(database, this));
        this.getCommand("unregister").setExecutor(new UnregisterCommand(this, database));
        this.getCommand("passpartu").setExecutor(new PasspartuCommand(this));
        this.getCommand("email").setExecutor(new EmailCommand(this, database));
        this.getCommand("captcha").setExecutor(new CaptchaCommand(this));

        if(!Settings.isForceSingleSessionEnabled) {
            ConsoleLogger.showError("ATTENTION by disabling ForceSingleSession, your server protection is set to low");
        }
        if (Settings.enableProtection) {
        	enableProtection();
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
        if (Bukkit.getOnlinePlayers() != null)
        for(Player player : Bukkit.getOnlinePlayers()) {
        		this.savePlayer(player);
        }

        if (database != null) {
            database.close();
        }

        if(Settings.isBackupActivated && Settings.isBackupOnStop) {
        Boolean Backup = new PerformBackup(this).DoBackup();
        if(Backup) ConsoleLogger.info("Backup Complete");
            else ConsoleLogger.showError("Error while making Backup");
        }
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
	          this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
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
		if (player.hasPermission(perm))
			return true;
		else if (permission != null) {
			return permission.playerHas(player, perm);
		}
		return false;
	}

	public boolean authmePermissible(CommandSender sender, String perm) {
		if (sender.hasPermission(perm)) return true;
		else if (permission != null) {
			return permission.has(sender, perm);
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
        if (Spawn.getInstance().getLocation() != null)
            spawnLoc = Spawn.getInstance().getLocation();
        return spawnLoc;
    }

    private void enableProtection() {
    	ConsoleLogger.info(" This product includes GeoLite data created by MaxMind, available from http://www.maxmind.com");
    	File file = new File(getDataFolder(), "GeoIP.dat");
    	if (!file.exists()) {
        	try {
        		String url = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";
                URL downloadUrl = new URL(url);
                URLConnection conn = downloadUrl.openConnection();
                conn.setConnectTimeout(10000);
                conn.connect();
                InputStream input = conn.getInputStream();
                if (url.endsWith(".gz"))
                	input = new GZIPInputStream(input);
                OutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[2048];
                int length = input.read(buffer);
                while (length >= 0) {
                	output.write(buffer, 0, length);
                	length = input.read(buffer);
                }
                output.close();
                input.close();
        	} catch (Exception e) {}
    	}
    }
    
    public String getCountryCode(InetAddress ip) {
    	try {
    		if (ls == null)
    			ls = new LookupService(new File(getDataFolder(), "GeoIP.dat"));
        	String code = ls.getCountry(ip).getCode();
        	if (code != null && !code.isEmpty())
        		return code;
    	} catch (Exception e) {}
    	return null;
    }
    
    public void switchAntiBotMod(boolean mode) {
    	this.antibotMod = mode;
    	Settings.switchAntiBotMod(mode);
    }
}
