package fr.xephi.authme.cache.limbo;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.events.StoreInventoryEvent;


public class LimboCache {

    private static LimboCache singleton = null;
    public HashMap<String, LimboPlayer> cache;
    public AuthMe plugin;

    private LimboCache(AuthMe plugin) {
    	this.plugin = plugin;
        this.cache = new HashMap<String, LimboPlayer>();
    }

    public void addLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        Location loc = player.getLocation();
        loc.setY(loc.getY() + 0.4D);
        GameMode gameMode = player.getGameMode();
        ItemStack[] arm = null;
        ItemStack[] inv = null;
        boolean operator = player.isOp();
        String playerGroup = "";
        boolean flying = player.isFlying();

        StoreInventoryEvent storeevent = new StoreInventoryEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(storeevent);
        if (!storeevent.isCancelled()) {
        	inv =  storeevent.getInventory();
        	arm =  storeevent.getArmor();
        }

        if(player.isDead()) {
        	loc = plugin.getSpawnLocation(player.getWorld());
        }
        try {
            if(cache.containsKey(name) && playerGroup.isEmpty()) {
                LimboPlayer groupLimbo = cache.get(name);
                playerGroup = groupLimbo.getGroup();
            }
        } catch (NullPointerException ex) {
        }
        cache.put(player.getName().toLowerCase(), new LimboPlayer(name, loc, inv, arm, gameMode, operator, playerGroup, flying));
    }

    public void addLimboPlayer(Player player, String group) {
        cache.put(player.getName().toLowerCase(), new LimboPlayer(player.getName().toLowerCase(), group));
    }

    public void deleteLimboPlayer(String name) {
        cache.remove(name);
    }

    public LimboPlayer getLimboPlayer(String name) {
        return cache.get(name);
    }

    public boolean hasLimboPlayer(String name) {
        return cache.containsKey(name);
    }

    public static LimboCache getInstance() {
        if (singleton == null) {
            singleton = new LimboCache(AuthMe.getInstance());
        }
        return singleton;
    }

}
