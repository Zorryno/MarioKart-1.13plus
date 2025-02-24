package net.stormdev.mario.powerups;

import java.util.ArrayList;
import java.util.List;

import net.stormdev.mario.items.ItemStacks;
import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.players.User;
import net.stormdev.mario.races.Race;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.useful.ucarsCommon.StatValue;

public class BombPowerup extends PowerupBase {
	
	public BombPowerup(){
		super.setItemStack(getBaseItem());
	}

	@Override
	public void doRightClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand) {
		inHand.setAmount(inHand.getAmount() - 1);
		final Vector vel = player.getEyeLocation().getDirection();
		final Vector dir = player.getEyeLocation().getDirection().multiply(2);
		final TNTPrimed tnt = (TNTPrimed) car.getLocation().getWorld()
				.spawnEntity(car.getLocation().add(dir), EntityType.PRIMED_TNT);
		tnt.setFuseTicks(60);
		tnt.setMetadata("explosion.none", new StatValue(null, MarioKart.plugin));
		vel.setY(0.5); // Distance to throw it
		tnt.setVelocity(vel);
		MarioKart.powerupManager.addTNT(tnt);
	}

	@Override
	public void doLeftClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand) {
		inHand.setAmount(inHand.getAmount() - 1);
		final Vector dir = player.getEyeLocation().getDirection().multiply(-2);
		dir.setY(0);
		final TNTPrimed tnt = (TNTPrimed) car.getLocation().getWorld()
				.spawnEntity(car.getLocation().add(dir), EntityType.PRIMED_TNT);
		tnt.setFuseTicks(60);
		tnt.setMetadata("explosion.none", new StatValue(null, MarioKart.plugin));
		MarioKart.powerupManager.addTNT(tnt);
	}
	
	private static final ItemStack getBaseItem(){
		String id = MarioKart.config.getString("mariokart.bomb");
		ItemStack i = ItemStacks.get(id);
		
		List<String> lore = new ArrayList<String>();
		lore.add("+Throws an ignited bomb");
		lore.add("*Right click to deploy");
		lore.add("*Left click to plant behind you");
		
		ItemMeta im = i.getItemMeta();
		im.setLore(lore);
		im.setDisplayName(MarioKart.colors.getInfo()+"Bomb");
		i.setItemMeta(im);
		
		return i;
	}
	
	public static boolean isItemSimilar(ItemStack i){
		return getBaseItem().isSimilar(i);
	}

	@Override
	public PowerupType getType() {
		return PowerupType.BOMB;
	}
	
	public static PowerupType getPowerupType() {
		return PowerupType.BOMB;
	}
}
