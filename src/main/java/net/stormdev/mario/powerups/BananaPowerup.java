package net.stormdev.mario.powerups;

import java.util.ArrayList;
import java.util.List;

import net.stormdev.mario.items.ItemStacks;
import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.players.User;
import net.stormdev.mario.races.Race;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class BananaPowerup extends PowerupBase {
	
	public BananaPowerup(){
		super.setItemStack(getBaseItem());
	}
	
	@Override
	public ItemStack getNewItem(){
		//Shells can be between 1 and 3 in quantity
				ItemStack s = super.stack.clone();
				
				int rand = MarioKart.plugin.random.nextInt(6); //Between 0 and 6
				if(rand < 4) {
					rand = 1;
				} else {
					rand = 3;
				}
				
				s.setAmount(rand);
				
				return s;
	}

	@Override
	public void doRightClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand) {
		Location loc = player.getLocation().add(player.getEyeLocation().getDirection().multiply(-2));
		loc.add(0,1,0);
		loc.getWorld().dropItem(loc, super.stack.clone());
		inHand.setAmount(inHand.getAmount()-1);
	}

	@Override
	public void doLeftClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand) {
		Location loc = player.getLocation();
		loc.add(0,1,0);
		Item item = loc.getWorld().dropItem(loc, super.stack.clone());
		
		final Vector vel = player.getEyeLocation().getDirection();
		vel.setY(0.5);
		item.setVelocity(vel);
		inHand.setAmount(inHand.getAmount()-1);
	}
	
	private static final ItemStack getBaseItem(){
		String id = MarioKart.config.getString("mariokart.banana");
		ItemStack i = ItemStacks.get(id);
		
		List<String> lore = new ArrayList<String>();
		lore.add("+Slows players down");
		lore.add("*Right click to deploy");
		lore.add("*Left click to throw forwards");
		
		ItemMeta im = i.getItemMeta();
		im.setLore(lore);
		im.setDisplayName(MarioKart.colors.getInfo()+"Banana");
		i.setItemMeta(im);
		
		return i;
	}
	
	public static boolean isItemSimilar(ItemStack i){
		return getBaseItem().isSimilar(i);
	}

	@Override
	public PowerupType getType() {
		return PowerupType.BANANA;
	}
	
	public static PowerupType getPowerupType() {
		return PowerupType.BANANA;
	}

}
