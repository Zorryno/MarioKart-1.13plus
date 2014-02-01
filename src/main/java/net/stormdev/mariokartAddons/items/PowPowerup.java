package net.stormdev.mariokartAddons.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import net.stormdev.mario.mariokart.Race;
import net.stormdev.mario.mariokart.RaceExecutor;
import net.stormdev.mario.mariokart.User;
import net.stormdev.mario.mariokart.main;
import net.stormdev.mario.utils.ItemStackFromId;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PowPowerup extends PowerupBase {
	
	public PowPowerup(){
		super.setItemStack(getBaseItem());
	}

	@Override
	public void doRightClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand) {
		SortedMap<String, Double> sorted = race.getRaceOrder();
		Set<String> keys = sorted.keySet();
		final Object[] pls = keys.toArray();
		int pppos = 0;
		for (int i = 0; i < pls.length; i++) {
			if (pls[i].equals(player.getName())) {
				pppos = i;
			}
		}
		final int ppos = pppos;
		main.plugin.getServer().getScheduler()
				.runTaskAsynchronously(main.plugin, new Runnable() {
					@Override
					public void run() {
						int count = 3;
						while (count > 0) {
							for (int i = 0; i < pls.length && i <= ppos; i++) {
								Player pl = main.plugin.getServer()
										.getPlayer((String) pls[i]);
								pl.sendMessage(main.colors.getTitle()
										+ "[MarioKart:] "
										+ main.colors.getInfo() + count);
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							count--;
						}
						main.plugin.getServer().getScheduler()
								.runTask(main.plugin, new Runnable() {
									@Override
									public void run() {
										for (int i = 0; i < pls.length
												&& i < ppos; i++) {
											Player pl = main.plugin
													.getServer()
													.getPlayer(
															(String) pls[i]);
											Entity e = pl.getVehicle();
											while(e!=null && !(e instanceof Minecart) && e.getVehicle() != null){
												e = e.getVehicle();
											}
											if(e == null || !(e instanceof Minecart)){
												return;
											}
											Minecart cart = (Minecart) e;
											if (!cart
													.hasMetadata(
															"car.braking")
													&& !main.marioKart.isCarImmune(cart)) {
												String msg = main.msgs
														.get("mario.hit");
												msg = msg
														.replaceAll(
																Pattern.quote("%name%"),
																"pow block");
												pl.getWorld()
														.playSound(
																pl.getLocation(),
																Sound.STEP_WOOD,
																1f,
																0.25f);
												pl.sendMessage(ChatColor.RED
														+ msg);
												
												RaceExecutor.penalty(pl, 
																cart,
																2);
											}
										}
										return;
									}
								});
					}
				});
		inHand.setAmount(inHand.getAmount() - 1);
	}

	@Override
	public void doLeftClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand) {
		return; //Do nothing
	}
	
	private static final ItemStack getBaseItem(){
		String id = main.config.getString("mariokart.pow");
		ItemStack i = ItemStackFromId.get(id);
		
		List<String> lore = new ArrayList<String>();
		lore.add("+Freezes other players");
		lore.add("*Right click to deploy");
		
		ItemMeta im = i.getItemMeta();
		im.setLore(lore);
		im.setDisplayName(main.colors.getInfo()+"Pow Block");
		i.setItemMeta(im);
		
		return i;
	}
	
	public static boolean isItemSimilar(ItemStack i){
		return getBaseItem().isSimilar(i);
	}

	@Override
	public PowerupType getType() {
		return PowerupType.POW;
	}
	
	public static PowerupType getPowerupType() {
		return PowerupType.POW;
	}

}
