package fr.xephi.authme.cache.limbo;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class LimboPlayer {

	private String name;
	private ItemStack[] inventory;
	private ItemStack[] armour;
	private Location loc = null;
	private int timeoutTaskId = -1;
	private int messageTaskId = -1;
	private GameMode gameMode = GameMode.SURVIVAL;
	private boolean operator = false;
	private boolean flying = false;

	public LimboPlayer(String name, Location loc, ItemStack[] inventory, ItemStack[] armour, GameMode gameMode, boolean operator, boolean flying) {
		this.name = name;
		this.loc = loc;
		this.inventory = inventory;
		this.armour = armour;
		this.gameMode = gameMode;
		this.operator = operator;
		this.flying = flying;
	}

	public LimboPlayer(String name, Location loc, GameMode gameMode, boolean operator, boolean flying) {
		this.name = name;
		this.loc = loc;
		this.gameMode = gameMode;
		this.operator = operator;
		this.flying = flying;
	}

	public LimboPlayer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Location getLoc() {
		return loc;
	}

	public ItemStack[] getArmour() {
		return armour;
	}

	public ItemStack[] getInventory() {
		return inventory;
	}

	public void setArmour(ItemStack[] armour) {
		this.armour = armour;
	}

	public void setInventory(ItemStack[] inventory) {
		this.inventory = inventory;
	}

	public GameMode getGameMode() {
		return gameMode;
	}

	public boolean getOperator() {
		return operator;
	}

	public void setTimeoutTaskId(int i) {
		this.timeoutTaskId = i;
	}

	public int getTimeoutTaskId() {
		return timeoutTaskId;
	}

	public void setMessageTaskId(int messageTaskId) {
		this.messageTaskId = messageTaskId;
	}

	public int getMessageTaskId() {
		return messageTaskId;
	}

	public boolean isFlying() {
		return flying;
	}

}
