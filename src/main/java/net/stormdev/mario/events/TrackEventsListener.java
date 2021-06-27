package net.stormdev.mario.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.tracks.TrackCreator;

public class TrackEventsListener implements Listener {
	@SuppressWarnings("unused")
	private MarioKart plugin;
	
	public TrackEventsListener(MarioKart plugin){
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onWandClickEvent(PlayerInteractEvent event) {
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR)
				&& !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		if(event.getHand().equals(EquipmentSlot.OFF_HAND)){
			return;
		}
		Player player = event.getPlayer();
		if (!TrackCreator.trackCreators.containsKey(player.getName())) {
			return;
		}
		
		event.setCancelled(true);
		
		TrackCreator creator = TrackCreator.trackCreators.get(player.getName());
		Boolean wand = false;
		if(player.getInventory().getItemInMainHand() != null) {
			String handItem = player.getInventory().getItemInMainHand().getType().name();
			if (handItem.equals(MarioKart.config.getString("setup.create.wand"))) {
				wand = true;
			}
		}
		creator.set(wand);
		return;
	}
}
