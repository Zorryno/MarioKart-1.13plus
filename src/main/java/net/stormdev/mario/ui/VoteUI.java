package net.stormdev.mario.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.server.FullServerManager;
import net.stormdev.mario.tracks.RaceTrack;

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
		
		RaceTrack rTrack = MarioKart.plugin.trackManager.getRaceTrack(track);
		meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + track);
		List<String> lore = new ArrayList<String>();
		lore.add(ChatColor.GRAY + "Click to vote for " + ChatColor.AQUA + track);
		lore.add(ChatColor.GOLD + "MinPlayers: "+rTrack.getMinPlayers());
		lore.add(ChatColor.GOLD + "MaxPlayers: "+rTrack.getMaxPlayers());
		lore.add(ChatColor.GOLD + "Laps: "+rTrack.getLaps());
		lore.add(ChatColor.GOLD + "Votes: "+FullServerManager.get().voter.getVotes(track));
		meta.setLore(lore);
		
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
