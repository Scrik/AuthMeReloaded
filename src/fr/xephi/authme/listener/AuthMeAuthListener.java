package fr.xephi.authme.listener;

import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.login.LoginCache;
import fr.xephi.authme.cache.login.LoginPlayer;
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

	private Messages messages = Messages.getInstance();

	private AuthMe plugin;
	private DataSource data;

	public AuthMeAuthListener(AuthMe plugin, DataSource data) {
		this.plugin = plugin;
		this.data = data;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {

		String name = event.getName();
		String lcname = name.toLowerCase();

		int min = Settings.getMinNickLength;
		int max = Settings.getMaxNickLength;
		String regex = Settings.getNickRegex;

		// check length
		if (lcname.length() > max || lcname.length() < min) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.getMessage("name_len"));
			return;
		}

		// check regex
		try {
			if (!name.matches(regex) || name.equals("Player")) {
				try {
					event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.getMessage("regex").replace("REG_EX", regex));
				} catch (StringIndexOutOfBoundsException exc) {
					event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
				}
				return;
			}
		} catch (PatternSyntaxException pse) {
			if (regex == null || regex.isEmpty()) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Your nickname do not match");
				return;
			}
			try {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.getMessage("regex").replace("REG_EX", regex));
			} catch (StringIndexOutOfBoundsException exc) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
			}
			return;
		}

		// check name case
		if (data.isAuthAvailable(lcname)) {
			PlayerAuth auth = data.getAuth(lcname);
			String realnickname = auth.getRealNickname();
			if (!realnickname.isEmpty() && !name.equals(realnickname)) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.getMessage("invalid_case").replace("REALNAME", realnickname));
				return;
			}
		}

		// check other single session
		Player oplayer = null;
		try {
			oplayer = Bukkit.getPlayerExact(name);
		} catch (Throwable t) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Error while logging in, please try again");
		}
		if (Settings.isForceSingleSessionEnabled && oplayer != null) {
			if (PlayerCache.getInstance().isAuthenticated(lcname) || !oplayer.getAddress().getAddress().getHostAddress().equals(event.getAddress().getHostAddress())) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.getMessage("same_nick"));
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
			return;
		}

		// Add login cache
		LoginCache.getInstance().addPlayer(player);

		// Teleport to spawn and protect inventory
		if (data.isAuthAvailable(name)) {
			LoginCache.getInstance().addPlayer(player);
			if (Settings.isTeleportToSpawnEnabled) {
				SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), plugin.getSpawnLocation(player.getWorld()), PlayerCache.getInstance().isAuthenticated(name));
				plugin.getServer().getPluginManager().callEvent(tpEvent);
				if (!tpEvent.isCancelled()) {
					player.teleport(tpEvent.getTo());
				}
			}
			if (Settings.protectInventoryBeforeLogInEnabled) {
				LoginPlayer limbo = LoginCache.getInstance().getPlayer(player.getName().toLowerCase());
				ProtectInventoryEvent ev = new ProtectInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
				plugin.getServer().getPluginManager().callEvent(ev);
				if (!ev.isCancelled()) {
					API.setPlayerInventory(player, ev.getEmptyInventory(), ev.getEmptyArmor());
				}
			}
		}

		// schedule timeout task
		String msg = data.isAuthAvailable(name) ? messages.getMessage("login_msg") : messages.getMessage("reg_msg");
		int time = Settings.getRegistrationTimeout * 20;
		if (time != 0) {
			BukkitTask id = Bukkit.getScheduler().runTaskLater(plugin, new TimeoutTask(player), time);
			LoginCache.getInstance().getPlayer(name).setTimeoutTaskId(id.getTaskId());
		}

		// schedule message task
		BukkitTask msgT = Bukkit.getScheduler().runTask(plugin, new MessageTask(plugin, player, msg, Settings.getWarnMessageInterval));
		LoginCache.getInstance().getPlayer(name).setMessageTaskId(msgT.getTaskId());
		player.setNoDamageTicks(Settings.getRegistrationTimeout * 20);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
			return;
		}

		// restore player position and inventory if he didin't log in before quitting
		if (LoginCache.getInstance().hasPlayer(name)) {
			LoginPlayer limbo = LoginCache.getInstance().getPlayer(name);
			if (data.isAuthAvailable(name)) {
				if (Settings.protectInventoryBeforeLogInEnabled) {
					RestoreInventoryEvent restoreInventoryEvent = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
					plugin.getServer().getPluginManager().callEvent(restoreInventoryEvent);
					if (!restoreInventoryEvent.isCancelled()) {
						API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
					}
				}
				if (Settings.isTeleportToSpawnEnabled) {
					AuthMeTeleportEvent teleportEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
					plugin.getServer().getPluginManager().callEvent(teleportEvent);
					if (!teleportEvent.isCancelled()) {
						Location fLoc = teleportEvent.getTo();
						player.teleport(fLoc);
					}
				}
			}
			this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
			LoginCache.getInstance().deletePlayer(name);
		}

		// remove from logged in
		PlayerCache.getInstance().removePlayer(name);
	}

}
