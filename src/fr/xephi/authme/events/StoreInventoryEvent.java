package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Xephi59
 */
public class StoreInventoryEvent extends CustomEvent {

	private ItemStack[] inventory;
	private ItemStack[] armor;
	private Player player;

	public StoreInventoryEvent(Player player) {
		this.player = player;
		this.inventory = player.getInventory().getContents();
		this.armor = player.getInventory().getArmorContents();
	}

	public ItemStack[] getInventory() {
		return this.inventory;
	}

	public void setInventory(ItemStack[] inventory) {
		this.inventory = inventory;
	}

	public ItemStack[] getArmor() {
		return this.armor;
	}

	public void setArmor(ItemStack[] armor) {
		this.armor = armor;
	}

	public Player getPlayer() {
		return this.player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

}
