package fr.xephi.authme.settings;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.HashAlgorithm;


public final class Settings extends YamlConfiguration {

	public static final String PLUGIN_FOLDER = "./plugins/AuthMe";
	public static final String CACHE_FOLDER = Settings.PLUGIN_FOLDER + "/cache";
	public static final String AUTH_FILE = Settings.PLUGIN_FOLDER + "/auths.db";
	public static final String MESSAGE_FILE = Settings.PLUGIN_FOLDER + "/messages";
	public static final String SETTINGS_FILE = Settings.PLUGIN_FOLDER + "/config.yml";
	public static List<String> allowCommands = null;
	public static List<String> getJoinPermissions = null;
	public static List<String> getUnrestrictedName = null;
	private static List<String> getRestrictedIp;
	public static List<String> countries = null;
	public final Plugin plugin;
	private final File file;
	public static HashAlgorithm getPasswordHash;
	public static HashAlgorithm rakamakHash;
	public static Boolean useLogging = false;

	public static boolean isRegistrationEnabled, isForcedRegistrationEnabled,
	isTeleportToSpawnEnabled, isChatAllowed, isAllowRestrictedIp,
	isMovementAllowed, isKickNonRegisteredEnabled, isForceSingleSessionEnabled,
	isCachingEnabled, isKickOnWrongPasswordEnabled,
	getEnablePasswordVerifier, protectInventoryBeforeLogInEnabled,
	enablePasspartu, isStopEnabled, rakamakUseIp, noConsoleSpam, displayOtherAccounts,
	useCaptcha, multiverse, notifications, chestshop, banUnsafeIp,
	disableSocialSpy, useEssentialsMotd,
	supportOldPassword,
	enableProtection, enableAntiBot;

	public static String getNickRegex,
	getcUnrestrictedName, messagesLanguage, getMySQLlastlocX, getMySQLlastlocY, getMySQLlastlocZ,
	rakamakUsers, rakamakUsersIp, getMySQLColumnId,
	defaultWorld;

	public static int getWarnMessageInterval, getRegistrationTimeout, getMaxNickLength,
	getMinNickLength, getPasswordMinLen, getMovementRadius, getmaxRegPerIp,
	passwordMaxLength, maxLoginTry, captchaLength,
	antiBotSensibility, antiBotDuration;

	protected static YamlConfiguration configFile;

	public Settings(Plugin plugin) {
		this.file = new File(plugin.getDataFolder(),"config.yml");
		this.plugin = plugin;
		if(exists()) {
			load();
		}
		else {
			loadDefaults(file.getName());
			load();
		}
		configFile = (YamlConfiguration) plugin.getConfig();
	}

	@SuppressWarnings("unchecked")
	public void loadConfigOptions() {
		plugin.getLogger().info("Loading Configuration File...");

		messagesLanguage = checkLang(configFile.getString("settings.messagesLanguage","en"));
		isForcedRegistrationEnabled  = configFile.getBoolean("settings.registration.force", true);
		isRegistrationEnabled = configFile.getBoolean("settings.registration.enabled", true);
		isTeleportToSpawnEnabled = configFile.getBoolean("settings.restrictions.teleportUnAuthedToSpawn",false);
		getWarnMessageInterval = configFile.getInt("settings.registration.messageInterval",5);
		getRegistrationTimeout = configFile.getInt("settings.restrictions.timeout",30);
		isChatAllowed = configFile.getBoolean("settings.restrictions.allowChat",false);
		getMaxNickLength = configFile.getInt("settings.restrictions.maxNicknameLength",20);
		getMinNickLength = configFile.getInt("settings.restrictions.minNicknameLength",3);
		getPasswordMinLen = configFile.getInt("settings.security.minPasswordLength",4);
		getNickRegex = configFile.getString("settings.restrictions.allowedNicknameCharacters","[a-zA-Z0-9_?]*");
		isAllowRestrictedIp = configFile.getBoolean("settings.restrictions.AllowRestrictedUser",false);
		getRestrictedIp = configFile.getStringList("settings.restrictions.AllowedRestrictedUser");
		isMovementAllowed = configFile.getBoolean("settings.restrictions.allowMovement",false);
		getMovementRadius = configFile.getInt("settings.restrictions.allowedMovementRadius",100);
		getJoinPermissions = configFile.getStringList("GroupOptions.Permissions.PermissionsOnJoin");
		isKickOnWrongPasswordEnabled = configFile.getBoolean("settings.restrictions.kickOnWrongPassword",false);
		isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered",false);
		isForceSingleSessionEnabled = configFile.getBoolean("settings.restrictions.ForceSingleSession",true);
		getmaxRegPerIp = configFile.getInt("settings.restrictions.maxRegPerIp",1);
		getPasswordHash = getPasswordHash();
		isCachingEnabled = configFile.getBoolean("DataSource.caching",true);
		getUnrestrictedName = configFile.getStringList("settings.unrestrictions.UnrestrictedName");
		getEnablePasswordVerifier = configFile.getBoolean("settings.restrictions.enablePasswordVerifier" , true);
		protectInventoryBeforeLogInEnabled = configFile.getBoolean("settings.restrictions.ProtectInventoryBeforeLogIn", true);
		passwordMaxLength = configFile.getInt("settings.security.passwordMaxLength", 20);
		enablePasspartu = configFile.getBoolean("Passpartu.enablePasspartu",false);
		isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
		allowCommands = (List<String>) configFile.getList("settings.restrictions.allowCommands");
		if (configFile.contains("allowCommands")) {
			if (!allowCommands.contains("/login")) {
				allowCommands.add("/login");
			}
			if (!allowCommands.contains("/register")) {
				allowCommands.add("/register");
			}
			if (!allowCommands.contains("/l")) {
				allowCommands.add("/l");
			}
			if (!allowCommands.contains("/reg")) {
				allowCommands.add("/reg");
			}
			if (!allowCommands.contains("/passpartu")) {
				allowCommands.add("/passpartu");
			}
			if (!allowCommands.contains("/email")) {
				allowCommands.add("/email");
			}
			if(!allowCommands.contains("/captcha")) {
				allowCommands.add("/captcha");
			}
		}
		rakamakUsers = configFile.getString("Converter.Rakamak.fileName", "users.rak");
		rakamakUsersIp = configFile.getString("Converter.Rakamak.ipFileName", "UsersIp.rak");
		rakamakUseIp = configFile.getBoolean("Converter.Rakamak.useIp", false);
		rakamakHash = getRakamakHash();
		noConsoleSpam = configFile.getBoolean("Security.console.noConsoleSpam", false);
		displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
		getMySQLColumnId = configFile.getString("DataSource.mySQLColumnId", "id");
		useCaptcha = configFile.getBoolean("Security.captcha.useCaptcha", false);
		maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
		captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
		multiverse = configFile.getBoolean("Hooks.multiverse", true);
		chestshop = configFile.getBoolean("Hooks.chestshop", true);
		notifications = configFile.getBoolean("Hooks.notifications", true);
		banUnsafeIp = configFile.getBoolean("settings.restrictions.banUnsafedIP", false);
		useLogging = configFile.getBoolean("Security.console.logConsole", false);
		disableSocialSpy = configFile.getBoolean("Hooks.disableSocialSpy", true);
		useEssentialsMotd = configFile.getBoolean("Hooks.useEssentialsMotd", false);
		defaultWorld = configFile.getString("Purge.defaultWorld", "world");
		supportOldPassword = configFile.getBoolean("settings.security.supportOldPasswordHash", false);
		enableProtection = configFile.getBoolean("Protection.enableProtection", false);
		countries = (List<String>) configFile.getList("Protection.countries");
		enableAntiBot = configFile.getBoolean("Protection.enableAntiBot", false);
		antiBotSensibility = configFile.getInt("Protection.antiBotSensibility", 5);
		antiBotDuration = configFile.getInt("Protection.antiBotDuration", 10);

		saveDefaults();
	}

	@SuppressWarnings("unchecked")
	public static void reloadConfigOptions(YamlConfiguration newConfig) {
		configFile = newConfig;

		messagesLanguage = checkLang(configFile.getString("settings.messagesLanguage","en"));
		isForcedRegistrationEnabled = configFile.getBoolean("settings.registration.force", true);
		isRegistrationEnabled = configFile.getBoolean("settings.registration.enabled", true);
		isTeleportToSpawnEnabled = configFile.getBoolean("settings.restrictions.teleportUnAuthedToSpawn",false);
		getWarnMessageInterval = configFile.getInt("settings.registration.messageInterval",5);
		getRegistrationTimeout = configFile.getInt("settings.restrictions.timeout",30);
		isChatAllowed = configFile.getBoolean("settings.restrictions.allowChat",false);
		getMaxNickLength = configFile.getInt("settings.restrictions.maxNicknameLength",20);
		getMinNickLength = configFile.getInt("settings.restrictions.minNicknameLength",3);
		getPasswordMinLen = configFile.getInt("settings.security.minPasswordLength",4);
		getNickRegex = configFile.getString("settings.restrictions.allowedNicknameCharacters","[a-zA-Z0-9_?]*");
		isAllowRestrictedIp = configFile.getBoolean("settings.restrictions.AllowRestrictedUser",false);
		getRestrictedIp = configFile.getStringList("settings.restrictions.AllowedRestrictedUser");
		isMovementAllowed = configFile.getBoolean("settings.restrictions.allowMovement",false);
		getMovementRadius = configFile.getInt("settings.restrictions.allowedMovementRadius",100);
		getJoinPermissions = configFile.getStringList("GroupOptions.Permissions.PermissionsOnJoin");
		isKickOnWrongPasswordEnabled = configFile.getBoolean("settings.restrictions.kickOnWrongPassword",false);
		isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered",false);
		getmaxRegPerIp = configFile.getInt("settings.restrictions.maxRegPerIp",1);
		getPasswordHash = getPasswordHash();
		isCachingEnabled = configFile.getBoolean("DataSource.caching",true);
		getUnrestrictedName = configFile.getStringList("settings.unrestrictions.UnrestrictedName");
		getEnablePasswordVerifier = configFile.getBoolean("settings.restrictions.enablePasswordVerifier" , true);
		protectInventoryBeforeLogInEnabled = configFile.getBoolean("settings.restrictions.ProtectInventoryBeforeLogIn", true);
		passwordMaxLength = configFile.getInt("settings.security.passwordMaxLength", 20);
		enablePasspartu = configFile.getBoolean("Passpartu.enablePasspartu",false);
		isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
		allowCommands = (List<String>) configFile.getList("settings.restrictions.allowCommands");
		if (configFile.contains("allowCommands")) {
			if (!allowCommands.contains("/login")) {
				allowCommands.add("/login");
			}
			if (!allowCommands.contains("/register")) {
				allowCommands.add("/register");
			}
			if (!allowCommands.contains("/l")) {
				allowCommands.add("/l");
			}
			if (!allowCommands.contains("/reg")) {
				allowCommands.add("/reg");
			}
			if (!allowCommands.contains("/passpartu")) {
				allowCommands.add("/passpartu");
			}
			if (!allowCommands.contains("/email")) {
				allowCommands.add("/email");
			}
			if(!allowCommands.contains("/captcha")) {
				allowCommands.add("/captcha");
			}
		}
		rakamakUsers = configFile.getString("Converter.Rakamak.fileName", "users.rak");
		rakamakUsersIp = configFile.getString("Converter.Rakamak.ipFileName", "UsersIp.rak");
		rakamakUseIp = configFile.getBoolean("Converter.Rakamak.useIp", false);
		rakamakHash = getRakamakHash();
		noConsoleSpam = configFile.getBoolean("Security.console.noConsoleSpam", false);
		displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
		getMySQLColumnId = configFile.getString("DataSource.mySQLColumnId", "id");
		useCaptcha = configFile.getBoolean("Security.captcha.useCaptcha", false);
		maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
		captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
		multiverse = configFile.getBoolean("Hooks.multiverse", true);
		chestshop = configFile.getBoolean("Hooks.chestshop", true);
		notifications = configFile.getBoolean("Hooks.notifications", true);
		banUnsafeIp = configFile.getBoolean("settings.restrictions.banUnsafedIP", false);
		useLogging = configFile.getBoolean("Security.console.logConsole", false);
		disableSocialSpy = configFile.getBoolean("Hooks.disableSocialSpy", true);
		useEssentialsMotd = configFile.getBoolean("Hooks.useEssentialsMotd", false);
		defaultWorld = configFile.getString("Purge.defaultWorld", "world");
		supportOldPassword = configFile.getBoolean("settings.security.supportOldPasswordHash", false);
		enableProtection = configFile.getBoolean("Protection.enableProtection", false);
		countries = (List<String>) configFile.getList("Protection.countries");
		enableAntiBot = configFile.getBoolean("Protection.enableAntiBot", false);
		antiBotSensibility = configFile.getInt("Protection.antiBotSensibility", 5);
		antiBotDuration = configFile.getInt("Protection.antiBotDuration", 10);
	}

	private static HashAlgorithm getPasswordHash() {
		String key = "settings.security.passwordHash";
		try {
			return HashAlgorithm.valueOf(configFile.getString(key,"SHA256").toUpperCase());
		} catch (IllegalArgumentException ex) {
			ConsoleLogger.showError("Unknown Hash Algorithm; defaulting to SHA256");
			return HashAlgorithm.SHA256;
		}
	}

	private static HashAlgorithm getRakamakHash() {
		String key = "Converter.Rakamak.newPasswordHash";

		try {
			return HashAlgorithm.valueOf(configFile.getString(key,"SHA256").toUpperCase());
		} catch (IllegalArgumentException ex) {
			ConsoleLogger.showError("Unknown Hash Algorithm; defaulting to SHA256");
			return HashAlgorithm.SHA256;
		}
	}

	/**
	 * Config option for setting and check restricted user by
	 * username;ip , return false if ip and name doesnt amtch with
	 * player that join the server, so player has a restricted access
	 */
	public static Boolean getRestrictedIp(String name, String ip) {

		Iterator<String> iter = getRestrictedIp.iterator();
		Boolean trueonce = false;
		Boolean namefound = false;
		while (iter.hasNext()) {
			String[] args =  iter.next().split(";");
			String testname = args[0];
			String testip = args[1];
			if(testname.equalsIgnoreCase(name) ) {
				namefound = true;
				if(testip.equalsIgnoreCase(ip)) {
					trueonce = true;
				};
			}
		}
		if ( namefound == false){
			return true;
		}
		else {
			if ( trueonce == true ){
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Loads the configuration from disk
	 *
	 * @return True if loaded successfully
	 */
	public final boolean load() {
		try {
			load(file);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public final void reload() {
		load();
		loadDefaults(file.getName());
	}

	/**
	 * Saves the configuration to disk
	 *
	 * @return True if saved successfully
	 */
	public final boolean save() {
		try {
			save(file);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Simple function for if the Configuration file exists
	 *
	 * @return True if configuration exists on disk
	 */
	public final boolean exists() {
		return file.exists();
	}

	/**
	 * Loads a file from the plugin jar and sets as default
	 *
	 * @param filename The filename to open
	 */
	public final void loadDefaults(String filename) {
		InputStream stream = plugin.getResource(filename);
		if(stream == null) {
			return;
		}
		setDefaults(YamlConfiguration.loadConfiguration(stream));
	}

	/**
	 * Saves current configuration (plus defaults) to disk.
	 *
	 * If defaults and configuration are empty, saves blank file.
	 *
	 * @return True if saved successfully
	 */
	public final boolean saveDefaults() {
		options().copyDefaults(true);
		options().copyHeader(true);
		boolean success = save();
		options().copyDefaults(false);
		options().copyHeader(false);
		return success;
	}

	/**
	 * Clears current configuration defaults
	 */
	public final void clearDefaults() {
		setDefaults(new MemoryConfiguration());
	}

	/**
	 * Check loaded defaults against current configuration
	 *
	 * @return false When all defaults aren't present in config
	 */
	public boolean checkDefaults() {
		if (getDefaults() == null) {
			return true;
		}
		return getKeys(true).containsAll(getDefaults().getKeys(true));
	}

	public static String checkLang(String lang) {
		for(messagesLang language: messagesLang.values()) {
			if(lang.toLowerCase().contains(language.toString())) {
				ConsoleLogger.info("Set Language: "+lang);
				return lang;
			}
		}
		ConsoleLogger.info("Set Default Language: En ");
		return "en";
	}

	public static void switchAntiBotMod(boolean mode) {
		if (mode) {
			isKickNonRegisteredEnabled = true;
		} else {
			isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered",false);
		}
	}

	public enum messagesLang {
		en, de, br, cz, pl, fr, ru, hu, sk, es, zhtw, fi, zhcn, lt, it, ko, pt, nl
	}
}
