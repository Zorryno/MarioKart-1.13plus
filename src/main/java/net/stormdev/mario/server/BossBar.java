package net.stormdev.mario.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.stormdev.barapi_1_17.BarAPI;
import net.stormdev.mario.mariokart.MarioKart;

/*
 * Include BarAPI from 1.8_BarAPI with refactored packaged and if not found, use that
 * 
 */
public class BossBar {
	private static void execAsync(Runnable run){
		if(Bukkit.isPrimaryThread()){
			run.run();
		}
		else {
			Bukkit.getScheduler().runTask(MarioKart.plugin, run);
		}
	}
	
	public static void setMessage(final Player player, final String message){
		execAsync( () -> {
			BarAPI.setMessage(player, message);
			return;
		});
	}
	
	public static void setMessage(final Player player, final String message, final float percent){
		execAsync( () -> {
			BarAPI.setMessage(player, message, percent);
			return;
		});
	}
	
	public static void setMessage(final Player player, final String message, final int seconds){
		execAsync( () -> {
			BarAPI.setMessage(player, message, seconds);
			return;
		});
	}
	
	public static boolean hasBar(Player player){
		return BarAPI.hasBar(player);
	}
	
	public static void removeBar(final Player player){
		execAsync( () -> {
			BarAPI.removeBar(player);
			return;
		});		
	}
	
	public static void setHealth(final Player player, final float percent){
		execAsync( () -> {
			BarAPI.setHealth(player, percent);
			return;
		});
	}
}