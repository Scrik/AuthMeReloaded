package fr.xephi.authme.plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.trc202.CombatTag.CombatTag;
import com.trc202.CombatTagApi.CombatTagApi;

public class CombatTagComunicator {

	static CombatTagApi combatApi;

	public CombatTagComunicator() {
		if (Bukkit.getServer().getPluginManager().getPlugin("CombatTag") != null) {
			combatApi = new CombatTagApi((CombatTag) Bukkit.getServer().getPluginManager().getPlugin("CombatTag"));
		}
	}

	/**
	 * Returns if the entity is an NPC
	 * 
	 * @param player
	 * @return true if the player is an NPC
	 */
	public static boolean isNPC(Entity player) {
		try {
			if (Bukkit.getServer().getPluginManager().getPlugin("CombatTag") != null) {
				combatApi = new CombatTagApi((CombatTag) Bukkit.getServer().getPluginManager().getPlugin("CombatTag"));
				return combatApi.isNPC(player);
			}
		} catch (ClassCastException ex) {
			return false;
		} catch (NullPointerException npe) {
			return false;
		} catch (NoClassDefFoundError ncdfe) {
			return false;
		}
		return false;
	}

}
