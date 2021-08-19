package net.stormdev.mario.ui;

import com.google.common.collect.ImmutableList;
import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.server.FullServerManager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/26/2017.
 */
public class VoteUI extends ChestUI {
	@Override
	public void build(final Player player, Inventory inv) {		
		int i = 0;
		for(String track: FullServerManager.get().voter.getMaps()) {
			this.addItem(inv, i, getTrackItem(track), () -> {
				FullServerManager.get().voter.castVote(player , track);
			});
			i++;
		}
	}

	private ItemStack getTrackItem(String track) {
		ItemStack item = new ItemStack(Material.PAPER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + track);
		meta.setLore(ImmutableList.of(ChatColor.GRAY + "Click to vote for " + ChatColor.AQUA + track));
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "War";
	}

	@Override
	public int getSize() {
		return 9;
	}
}
