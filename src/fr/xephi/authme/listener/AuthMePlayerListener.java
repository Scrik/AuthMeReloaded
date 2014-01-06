package fr.xephi.authme.listener;

import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class AuthMePlayerListener implements Listener {

	public static HashMap<String, GameMode> gameMode = new HashMap<String, GameMode>();
	public static HashMap<String, String> joinMessage = new HashMap<String, String>();
	private Messages m = Messages.getInstance();
	public AuthMe plugin;
	private DataSource data;
	public boolean causeByAuthMe = false;
	private HashMap<String, PlayerLoginEvent> antibot = new HashMap<String, PlayerLoginEvent>();

	public AuthMePlayerListener(AuthMe plugin, DataSource data) {
		this.plugin = plugin;
		this.data = data;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled() || event.getPlayer() == null)
			return;

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player))
			return;

		if (PlayerCache.getInstance().isAuthenticated(name))
			return;

		if (!data.isAuthAvailable(name))
			if (!Settings.isForcedRegistrationEnabled)
				return;

		String msg = event.getMessage();
		// WorldEdit GUI Shit
		if (msg.equalsIgnoreCase("/worldedit cui"))
			return;

		String cmd = msg.split(" ")[0];
		if (cmd.equalsIgnoreCase("/login") || cmd.equalsIgnoreCase("/register")
				|| cmd.equalsIgnoreCase("/passpartu")
				|| cmd.equalsIgnoreCase("/l") || cmd.equalsIgnoreCase("/reg")
				|| cmd.equalsIgnoreCase("/email")
				|| cmd.equalsIgnoreCase("/captcha"))
			return;
		if (Settings.useEssentialsMotd && cmd.equalsIgnoreCase("/motd"))
			return;
		if (Settings.allowCommands.contains(cmd))
			return;

		event.setMessage("/notloggedin");
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled() || event.getPlayer() == null)
			return;

		final Player player = event.getPlayer();
		final String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player))
			return;

		if (PlayerCache.getInstance().isAuthenticated(name))
			return;

		String cmd = event.getMessage().split(" ")[0];

		if (data.isAuthAvailable(name)) {
			player.sendMessage(m._("login_msg"));
		} else {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
			if (Settings.emailRegistration) {
				player.sendMessage(m._("reg_email_msg"));
				return;
			} else {
				player.sendMessage(m._("reg_msg"));
				return;
			}
		}

		if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled() || event.getPlayer() == null) {
			return;
		}

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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent event) {

		final Player player = event.getPlayer();
		final String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (Settings.enableProtection && !Settings.countries.isEmpty()) {
			String code = plugin.getCountryCode(event.getAddress());
			if ((code == null)
					|| (!Settings.countries.contains(code) && !API
							.isRegistered(name))) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
						"Your country is banned from this server");
				return;
			}
		}

		if (Settings.isKickNonRegisteredEnabled) {
			if (!data.isAuthAvailable(name)) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
						m._("reg_only"));
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
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
							m._("regex").replaceAll("REG_EX", regex));
				} catch (StringIndexOutOfBoundsException exc) {
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
							"allowed char : " + regex);
				}
				return;
			}
		} catch (PatternSyntaxException pse) {
			if (regex == null || regex.isEmpty()) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
						"Your nickname do not match");
				return;
			}
			try {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex")
						.replaceAll("REG_EX", regex));
			} catch (StringIndexOutOfBoundsException exc) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
						"allowed char : " + regex);
			}
			return;
		}

		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
			checkAntiBotMod(event);
			return;
		}
	}

	private void checkAntiBotMod(final PlayerLoginEvent event) {
		if (plugin.delayedAntiBot || plugin.antibotMod)
			return;
		if (antibot.keySet().size() > Settings.antiBotSensibility) {
			plugin.switchAntiBotMod(true);
			Bukkit.broadcastMessage("[AuthMe] AntiBotMod automatically enabled due to massive connections! ");
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
					new Runnable() {
						@Override
						public void run() {
							if (plugin.antibotMod) {
								plugin.switchAntiBotMod(false);
								antibot.clear();
								Bukkit.broadcastMessage("[AuthMe] AntiBotMod automatically disabled after "
										+ Settings.antiBotDuration
										+ " Minutes, hope invasion stopped ");
							}
						}
					}, Settings.antiBotDuration * 1200);
			return;
		}
		antibot.put(event.getPlayer().getName().toLowerCase(), event);
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				antibot.remove(event.getPlayer().getName().toLowerCase());
			}
		}, 300);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (event.getPlayer() == null) {
			return;
		}

		Player player = event.getPlayer();
		
		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}
		
		World world = player.getWorld();
		Location spawnLoc = plugin.getSpawnLocation(world);
		final String name = player.getName().toLowerCase();
		gameMode.put(name, player.getGameMode());
		BukkitScheduler sched = plugin.getServer().getScheduler();

		String ip = player.getAddress().getAddress().getHostAddress();
		if (Settings.bungee) {
			if (plugin.realIp.containsKey(name))
				ip = plugin.realIp.get(name);
		}

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
		} else {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		if (Settings.protectInventoryBeforeLogInEnabled) {
			try {
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
			} catch (NullPointerException ex) {
			}
		}
		String msg = "";
		if (Settings.emailRegistration) {
			msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_email_msg");
		} else {
			msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_msg");
		}
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
		// Remove the join message while the player isn't logging in
		joinMessage.put(name, event.getJoinMessage());
		event.setJoinMessage(null);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (event.getPlayer() == null) {
			return;
		}

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (plugin.getCitizensCommunicator().isNPC(player, plugin)
				|| Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (LimboCache.getInstance().hasLimboPlayer(name)) {
			LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
			if (Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
				RestoreInventoryEvent ev = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
				plugin.getServer().getPluginManager().callEvent(ev);
				if (!ev.isCancelled()) {
					API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
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
		try {
			PlayerCache.getInstance().removePlayer(name);
			player.getVehicle().eject();
		} catch (NullPointerException ex) {
		}
		gameMode.remove(name);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (event.isCancelled() || event.getPlayer() == null) {
			return;
		}

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled() || event.getPlayer() == null)
			return;

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

		if (PlayerCache.getInstance().isAuthenticated(
				player.getName().toLowerCase())) {
			return;
		}

		if (!data.isAuthAvailable(name)) {
			if (!Settings.isForcedRegistrationEnabled) {
				return;
			}
		}
		if (event.getClickedBlock() != null
				&& event.getClickedBlock().getType() != Material.AIR)
			event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
		event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInventoryOpen(InventoryOpenEvent event) {
		if (event.isCancelled() || event.getPlayer() == null)
			return;
		if (!(event.getPlayer() instanceof Player))
			return;
		Player player = (Player) event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInventoryClick(InventoryClickEvent event) {
		if (event.isCancelled() || event.getWhoClicked() == null)
			return;
		if (!(event.getWhoClicked() instanceof Player))
			return;
		Player player = (Player) event.getWhoClicked();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.isCancelled() || event.getPlayer() == null) {
			return;
		}

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player)) {
			return;
		}

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		if (event.isCancelled() || event.getPlayer() == null) {
			return;
		}
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onSignChange(SignChangeEvent event) {
		if (event.isCancelled() || event.getPlayer() == null) {
			return;
		}
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event.getPlayer() == null || event == null) {
			return;
		}

		Player player = event.getPlayer();
		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player))
			return;

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

		if (PlayerCache.getInstance().isAuthenticated(name))
			return;

		if (!data.isAuthAvailable(name))
			if (!Settings.isForcedRegistrationEnabled)
				return;

		if (!Settings.isTeleportToSpawnEnabled)
			return;

		Location spawn = plugin.getSpawnLocation(player.getWorld());
		event.setRespawnLocation(spawn);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (event.isCancelled())
			return;
		if (event.getPlayer() == null || event == null)
			return;

		Player player = event.getPlayer();

		if (plugin.authmePermissible(player, "authme.bypassforcesurvival"))
			return;

		String name = player.getName().toLowerCase();

		if (Utils.getInstance().isUnrestricted(player)
				|| CombatTagComunicator.isNPC(player))
			return;

		if (plugin.getCitizensCommunicator().isNPC(player, plugin))
			return;

		if (PlayerCache.getInstance().isAuthenticated(name))
			return;

		if (!data.isAuthAvailable(name))
			if (!Settings.isForcedRegistrationEnabled)
				return;

		if (this.causeByAuthMe)
			return;
		event.setCancelled(true);
	}
}
