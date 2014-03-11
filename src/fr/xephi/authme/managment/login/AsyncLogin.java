package fr.xephi.authme.managment.login;

import java.util.Date;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class AsyncLogin extends Thread {

	private AuthMe plugin;
    private DataSource database;

	private Player player;
	private String name;
	private String password;
	private boolean forceLogin;

	private static RandomString rdm = new RandomString(Settings.captchaLength);
	private Messages m = Messages.getInstance();

	public AsyncLogin(AuthMe plugin, DataSource datasource, Player player, String password, boolean forceLogin) {
		this.plugin = plugin;
		this.database = datasource;
		this.player = player;
		this.password = password;
		this.name = player.getName().toLowerCase();
		this.forceLogin = forceLogin;
	}

	protected String getIP() {
		String ip = player.getAddress().getAddress().getHostAddress();
		return ip;
	}

	protected boolean needsCaptcha() {
		if (Settings.useCaptcha) {
			if (!plugin.captcha.containsKey(name)) {
				plugin.captcha.put(name, 1);
			} else {
				int i = plugin.captcha.get(name) + 1;
				plugin.captcha.remove(name);
				plugin.captcha.put(name, i);
			}
			if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) >= Settings.maxLoginTry) {
				plugin.cap.put(name, rdm.nextString());
				player.sendMessage(m._("need_captcha").replace("THE_CAPTCHA", plugin.cap.get(name)).replace("<theCaptcha>", plugin.cap.get(name)));
				return true;
			} else if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) >= Settings.maxLoginTry) {
				try {
					plugin.captcha.remove(name);
					plugin.cap.remove(name);
				} catch (NullPointerException npe) {
				}
			}
		}
		return false;
	}

	/**
	 * Checks the precondition for authentication (like user known) and returns the playerAuth-State
	 */
	protected PlayerAuth preAuth() {
		if (PlayerCache.getInstance().isAuthenticated(name)) {
			player.sendMessage(m._("logged_in"));
			return null;
		}
		if (!database.isAuthAvailable(name)) {
			player.sendMessage(m._("user_unknown"));
			return null;
		}
		PlayerAuth pAuth = database.getAuth(name);
		if (pAuth == null) {
			player.sendMessage(m._("user_unknown"));
			return null;
		}
		return pAuth;
	}

	@Override
	public void run() {
		PlayerAuth pAuth = preAuth();
		if (pAuth == null || needsCaptcha()) {
			return;
		}

		String hash = pAuth.getHash();
		boolean passwordVerified = true;
		if (!forceLogin) {
			try {
				passwordVerified = PasswordSecurity.comparePasswordWithHash(password, hash, name);
			} catch (Exception ex) {
				ConsoleLogger.showError(ex.getMessage());
				player.sendMessage(m._("error"));
				return;
			}
		}
		if (passwordVerified && player.isOnline()) {
			PlayerAuth auth = new PlayerAuth(name, hash, getIP(), new Date().getTime());
			database.updateSession(auth);

			if (Settings.useCaptcha) {
				if (plugin.captcha.containsKey(name)) {
					plugin.captcha.remove(name);
				}
				if (plugin.cap.containsKey(name)) {
					plugin.cap.remove(name);
				}
			}

			player.setNoDamageTicks(0);
			player.sendMessage(m._("login"));

			if (!Settings.noConsoleSpam) {
				ConsoleLogger.info(player.getName() + " logged in!");
			}

			if (plugin.notifications != null) {
				plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged in!"));
			}

			// makes player isLoggedin via API
			PlayerCache.getInstance().addPlayer(auth);

			// As the scheduling executes the Task most likely after the current task, we schedule it in the end
			// so that we can be sure, and have not to care if it might be processed in other order.
			SyncLogin syncronousPlayerLogin = new SyncLogin(plugin, player);
			if (syncronousPlayerLogin.getLimbo() != null) {
				player.getServer().getScheduler().cancelTask(syncronousPlayerLogin.getLimbo().getTimeoutTaskId());
				player.getServer().getScheduler().cancelTask(syncronousPlayerLogin.getLimbo().getMessageTaskId());
			}
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, syncronousPlayerLogin);
		} else if (player.isOnline()) {
			if (!Settings.noConsoleSpam) {
				ConsoleLogger.info(player.getName() + " used the wrong password");
			}
			if (Settings.isKickOnWrongPasswordEnabled) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
						player.kickPlayer(m._("wrong_pwd"));
					}
				});
			} else {
				player.sendMessage(m._("wrong_pwd"));
				return;
			}
		} else {
			ConsoleLogger.showError("Player " + name + " wasn't online during login process, aborted... ");
		}
	}

}
