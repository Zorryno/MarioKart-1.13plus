package net.stormdev.mario.ui;

import net.stormdev.mario.mariokart.MarioKart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public abstract class ChestUI {
	private Map<ItemStack, Runnable> actions;
	ChestUI() {
		actions = new HashMap<ItemStack, Runnable>();
	}

	protected void addItem(Inventory inv, int slot, ItemStack item, Runnable action) {
		ItemMeta iM = item.getItemMeta();
		iM.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(iM);

		inv.setItem(slot, item);
		actions.put(item, action);
	}

	public abstract void build(Player player, Inventory inv);

	public abstract String getTitle();

	public abstract int getSize();

	boolean processClick(ItemStack clicked, Inventory inventory) {
		if (actions.containsKey(clicked) && actions.get(clicked) != null) {
			MarioKart.plugin.getServer().getScheduler().runTask(MarioKart.plugin, actions.get(clicked));
			return true;
		}
		return false;
	}
}
