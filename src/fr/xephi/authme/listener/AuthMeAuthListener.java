package fr.xephi.authme.listener;

import java.util.regex.PatternSyntaxException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class AuthMeAuthListener implements Listener {

	private Messages m = Messages.getInstance();
	public AuthMe plugin;
	private DataSource data;

	public AuthMeAuthListener(AuthMe plugin, DataSource data) {
		this.plugin = plugin;
		this.data = data;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerLogin(PlayerLoginEvent event) {

		final Player player = event.getPlayer();
		final String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
			return;
		}

		int min = Settings.getMinNickLength;
		int max = Settings.getMaxNickLength;
		String regex = Settings.getNickRegex;

		if (name.length() > max || name.length() < min) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("name_len"));
			return;
		}

		try {
			if (!player.getName().matches(regex) || name.equals("Player")) {
				try {
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex").replaceAll("REG_EX", regex));
				} catch (StringIndexOutOfBoundsException exc) {
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
				}
				return;
			}
		} catch (PatternSyntaxException pse) {
			if (regex == null || regex.isEmpty()) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Your nickname do not match");
				return;
			}
			try {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex").replaceAll("REG_EX", regex));
			} catch (StringIndexOutOfBoundsException exc) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
			}
			return;
		}

		if (player.isOnline() && Settings.isForceSingleSessionEnabled) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("same_nick"));
			return;
		}

		if (data.isAuthAvailable(name)) {
			PlayerAuth auth = data.getAuth(name);
			String realnickname = auth.getRealNickname();
			if (!realnickname.isEmpty() && !player.getName().equals(realnickname)) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Залогиньтесь используя ваш оригинальный ник: " + realnickname);
				return;
			}
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerJoin(PlayerJoinEvent event) {

		Player player = event.getPlayer();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		World world = player.getWorld();
		Location spawnLoc = plugin.getSpawnLocation(world);
		final String name = player.getName().toLowerCase();
		BukkitScheduler sched = plugin.getServer().getScheduler();

		String ip = player.getAddress().getAddress().getHostAddress();

		if (Settings.isAllowRestrictedIp && !Settings.getRestrictedIp(name, ip)) {
			player.kickPlayer("You are not the Owner of this account, please try another name!");
			if (Settings.banUnsafeIp) {
				plugin.getServer().banIP(ip);
			}
			return;
		}

		if (data.isAuthAvailable(name)) {
			LimboCache.getInstance().addLimboPlayer(player);
			if (Settings.isTeleportToSpawnEnabled) {
				SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
				plugin.getServer().getPluginManager().callEvent(tpEvent);
				if (!tpEvent.isCancelled()) {
					player.teleport(tpEvent.getTo());
				}
			}
			if (Settings.protectInventoryBeforeLogInEnabled) {
				LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
				ProtectInventoryEvent ev = new ProtectInventoryEvent(player,limbo.getInventory(), limbo.getArmour());
				plugin.getServer().getPluginManager().callEvent(ev);
				if (ev.isCancelled()) {
					if (!Settings.noConsoleSpam) {
						ConsoleLogger.info("ProtectInventoryEvent has been cancelled for "+ player.getName() + " ...");
					}
				} else {
					API.setPlayerInventory(player, ev.getEmptyInventory(), ev.getEmptyArmor());
				}
			}
		}
		String msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_msg");
		int time = Settings.getRegistrationTimeout * 20;
		int msgInterval = Settings.getWarnMessageInterval;
		if (time != 0) {
			BukkitTask id = sched.runTaskLater(plugin, new TimeoutTask(plugin, name), time);
			if (!LimboCache.getInstance().hasLimboPlayer(name)) {
				LimboCache.getInstance().addLimboPlayer(player);
			}
			LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id.getTaskId());
		}
		if (!LimboCache.getInstance().hasLimboPlayer(name)) {
			LimboCache.getInstance().addLimboPlayer(player);
		}
		BukkitTask msgT = sched.runTask(plugin, new MessageTask(plugin, name, msg, msgInterval));
		LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT.getTaskId());
		player.setNoDamageTicks(Settings.getRegistrationTimeout * 20);
		if (Settings.useEssentialsMotd) {
			player.performCommand("motd");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled=true)
	public void onPlayerQuit(PlayerQuitEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (LimboCache.getInstance().hasLimboPlayer(name)) {
			LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
			if (data.isAuthAvailable(name)) {
				if (Settings.protectInventoryBeforeLogInEnabled) {
					RestoreInventoryEvent ev = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
					plugin.getServer().getPluginManager().callEvent(ev);
					if (!ev.isCancelled()) {
						API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
					}
				}
				if (Settings.isTeleportToSpawnEnabled) {
					AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
					plugin.getServer().getPluginManager().callEvent(tpEvent);
					if (!tpEvent.isCancelled()) {
						Location fLoc = tpEvent.getTo();
						player.teleport(fLoc);
					}
				}
			}
			this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
			LimboCache.getInstance().deleteLimboPlayer(name);
		}
		PlayerCache.getInstance().removePlayer(name);
	}

}
