package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class CaptchaCommand implements CommandExecutor {

	private Messages m = Messages.getInstance();
	private RandomString rdm = new RandomString(Settings.captchaLength);
	private AuthMe plugin;

	public CaptchaCommand(AuthMe plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {

		if (!(sender instanceof Player)) {
			return true;
		}

		Player player = (Player) sender;
		String name = player.getName().toLowerCase();

		if (args.length == 0) {
			player.sendMessage(m.getMessage("usage_captcha"));
			return true;
		}

		if (PlayerCache.getInstance().isAuthenticated(name)) {
			player.sendMessage(m.getMessage("logged_in"));
			return true;
		}

		if (!plugin.authmePermissible(player, "authme." + label.toLowerCase())) {
			player.sendMessage(m.getMessage("no_perm"));
			return true;
		}

		if (!Settings.useCaptcha) {
			player.sendMessage(m.getMessage("usage_log"));
			return true;
		}

		if (!plugin.cap.containsKey(name)) {
			player.sendMessage(m.getMessage("usage_log"));
			return true;
		}

		if (Settings.useCaptcha && !args[0].equals(plugin.cap.get(name))) {
			plugin.cap.remove(name);
			plugin.cap.put(name, rdm.nextString());
			player.sendMessage(m.getMessage("wrong_captcha").replaceAll("THE_CAPTCHA", plugin.cap.get(name)));
			return true;
		}
		try {
			plugin.captcha.remove(name);
			plugin.cap.remove(name);
		} catch (NullPointerException npe) {
		}
		player.sendMessage(m.getMessage("valid_captcha"));
		player.sendMessage(m.getMessage("login_msg"));
		return true;
	}

}
