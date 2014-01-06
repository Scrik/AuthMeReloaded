package fr.xephi.authme.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Settings;


public class AuthMeEntityListener implements Listener{

    private DataSource data;
    public AuthMe instance;

    public AuthMeEntityListener(DataSource data, AuthMe instance) {
        this.data = data;
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
 
        if (!(entity instanceof Player)) {
            return;
        }

        if(Utils.getInstance().isUnrestricted((Player)entity)) {
            return;
        }
        
        if(instance.citizens.isNPC(entity, instance))
        	return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();
        
        if(CombatTagComunicator.isNPC(player))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        player.setFireTicks(0);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        
        if(instance.citizens.isNPC(entity, instance))
        	return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

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
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void EntityRegainHealthEvent(EntityRegainHealthEvent event) {

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        
        if(instance.citizens.isNPC(entity, instance))
        	return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

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
 
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if(instance.citizens.isNPC(player, instance))
        	return;

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

}
