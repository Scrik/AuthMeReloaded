package fr.xephi.authme.listener;

import java.util.regex.PatternSyntaxException;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.api.API;
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

public class AuthMePlayerListener implements Listener {

	private Messages m = Messages.getInstance();
	public AuthMe plugin;
	private DataSource data;

	public AuthMePlayerListener(AuthMe plugin, DataSource data) {
		this.plugin = plugin;
		this.data = data;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}

		String msg = event.getMessage();
		// WorldEdit GUI Shit
		if (msg.equalsIgnoreCase("/worldedit cui")) {
			return;
		}

		String cmd = msg.split(" ")[0];
		if (
				cmd.equalsIgnoreCase("/login")
				|| cmd.equalsIgnoreCase("/register")
				|| cmd.equalsIgnoreCase("/passpartu")
				|| cmd.equalsIgnoreCase("/l")
				|| cmd.equalsIgnoreCase("/reg")
				|| cmd.equalsIgnoreCase("/email")
				|| cmd.equalsIgnoreCase("/captcha")
				) {
			return;
		}
		if (Settings.useEssentialsMotd && cmd.equalsIgnoreCase("/motd")) {
			return;
		}
		if (Settings.allowCommands.contains(cmd)) {
			return;
		}

		event.setMessage("/notloggedin");
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {

		final Player player = event.getPlayer();
		final String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}

		String cmd = event.getMessage().split(" ")[0];

		if (data.isAuthAvailable(name)) {
			player.sendMessage(m._("login_msg"));
		} else {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
			player.sendMessage(m._("reg_msg"));
			return;
		}

		if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerMove(PlayerMoveEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}

		if (!Settings.isForcedRegistrationEnabled) {
			return;
		}

		if (!Settings.isMovementAllowed) {
			event.setTo(event.getFrom());
			return;
		}

		if (Settings.getMovementRadius == 0) {
			return;
		}

		int radius = Settings.getMovementRadius;
		Location spawn = plugin.getSpawnLocation(player.getWorld());

		if (!event.getPlayer().getWorld().equals(spawn.getWorld())) {
			event.getPlayer().teleport(spawn);
			return;
		}
		if ((spawn.distance(player.getLocation()) > radius)) {
			event.getPlayer().teleport(spawn);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerLogin(PlayerLoginEvent event) {

		final Player player = event.getPlayer();
		final String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (Settings.isKickNonRegisteredEnabled) {
			if (!data.isAuthAvailable(name)) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("reg_only"));
				return;
			}
		}

		if (player.isOnline() && Settings.isForceSingleSessionEnabled) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("same_nick"));
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
		} else {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
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
		if (player.isOp()) {
			player.setOp(false);
		}
		if (!Settings.isMovementAllowed) {
			player.setAllowFlight(true);
			player.setFlying(true);
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
			player.setOp(limbo.getOperator());
			if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
				player.setAllowFlight(limbo.isFlying());
				player.setFlying(limbo.isFlying());
			}
			this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
			LimboCache.getInstance().deleteLimboPlayer(name);
		}
		PlayerCache.getInstance().removePlayer(name);
	}

	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled=true)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(
				player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerInventoryOpen(InventoryOpenEvent event) {

		Player player = (Player) event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled=true)
	public void onPlayerInventoryClick(InventoryClickEvent event) {

		Player player = (Player) event.getWhoClicked();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(
				player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		event.setResult(org.bukkit.event.Event.Result.DENY);
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(
				player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(
				player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled=true)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (PlayerCache.getInstance().isAuthenticated(
				player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onSignChange(SignChangeEvent event) {

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();
		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			return;
		}
		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		event.setCancelled(true);
	}

}
