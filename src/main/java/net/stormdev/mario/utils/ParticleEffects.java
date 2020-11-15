package net.stormdev.mario.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;



public enum ParticleEffects {
	
	

	HUGE_EXPLODE(Particle.EXPLOSION_HUGE, 0), LARGE_EXPLODE(Particle.EXPLOSION_LARGE, 1), FIREWORK_SPARK(
			Particle.FIREWORKS_SPARK, 2), AIR_BUBBLE(Particle.WATER_BUBBLE, 3), SUSPEND(Particle.SUSPENDED, 4), DEPTH_SUSPEND(
			Particle.SUSPENDED_DEPTH, 5), TOWN_AURA(Particle.TOWN_AURA, 6), CRITICAL_HIT(Particle.CRIT,
			7), MAGIC_CRITICAL_HIT(Particle.CRIT_MAGIC, 8), MOB_SPELL(Particle.SPELL_MOB, 9), MOB_SPELL_AMBIENT(
					Particle.SPELL_MOB_AMBIENT, 10), SPELL(Particle.SPELL, 11), INSTANT_SPELL(
			Particle.SPELL_INSTANT, 12), BLUE_SPARKLE(Particle.SPELL_WITCH, 13), NOTE_BLOCK(
			Particle.NOTE, 14), ENDER(Particle.PORTAL, 15), ENCHANTMENT_TABLE(
			Particle.ENCHANTMENT_TABLE, 16), EXPLODE(Particle.EXPLOSION_NORMAL, 17), FIRE(Particle.FLAME, 18), LAVA_SPARK(
			Particle.LAVA, 19), SPLASH(Particle.WATER_SPLASH, 21), LARGE_SMOKE(
			Particle.SMOKE_LARGE, 22), CLOUD(Particle.CLOUD, 23), REDSTONE_DUST(Particle.REDSTONE, 24), SNOWBALL_HIT(
			Particle.SNOWBALL, 25), DRIP_WATER(Particle.DRIP_WATER, 26), DRIP_LAVA(
			Particle.DRIP_LAVA, 27), SNOW_DIG(Particle.SNOW_SHOVEL, 28), SLIME(Particle.SLIME, 29), HEART(
			Particle.HEART, 30), ANGRY_VILLAGER(Particle.VILLAGER_ANGRY, 31), GREEN_SPARKLE(
			Particle.VILLAGER_HAPPY, 32), ICONCRACK(Particle.BLOCK_CRACK, 33), TILECRACK(
			Particle.BLOCK_CRACK, 34);

	/*private static Class<?> getCraftClass(String name) {
		String version = getVersion() + ".";
		String className = "net.minecraft.server." + version + name;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return clazz;
	}*/

	/*private static String getVersion() {
		return Bukkit.getServer().getClass().getPackage().getName()
				.replace(".", ",").split(",")[3];
	}*/

	/**
	 * Send a particle effect to all players
	 * 
	 * @param effect
	 *            The particle effect to send
	 * @param location
	 *            The location to send the effect to
	 * @param offsetX
	 *            The x range of the particle effect
	 * @param offsetY
	 *            The y range of the particle effect
	 * @param offsetZ
	 *            The z range of the particle effect
	 * @param speed
	 *            The speed (or color depending on the effect) of the particle
	 *            effect
	 * @param count
	 *            The count of effects
	 */
	public static void sendToLocation(ParticleEffects effect,
			Location location, float offsetX, float offsetY, float offsetZ,
			float speed, int count) {
		try {
			//Object packet = createPacket(effect, location, offsetX, offsetY,
			//		offsetZ, speed, count);
			//for (Player player : Bukkit.getOnlinePlayers()) {
			//	sendPacket(player, packet);
			//}
			if(effect.get() == Particle.REDSTONE) {
				Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 5);
				location.getWorld().spawnParticle(Particle.REDSTONE, location, count, offsetX, offsetY, offsetZ, speed, dust);
			} else {
				location.getWorld().spawnParticle(effect.get(), location, count, offsetX, offsetY, offsetZ, speed);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void sendToLocation(Particle effect,
			Location location, float offsetX, float offsetY, float offsetZ,
			float speed, int count) {
		try {
			//Object packet = createPacket(effect, location, offsetX, offsetY,
			//		offsetZ, speed, count);
			//for (Player player : Bukkit.getOnlinePlayers()) {
			//	sendPacket(player, packet);
			//}
			if(effect == Particle.REDSTONE) {
				Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 5);
				location.getWorld().spawnParticle(Particle.REDSTONE, location, count, offsetX, offsetY, offsetZ, speed, dust);
			} else {
				location.getWorld().spawnParticle(effect, location, count, offsetX, offsetY, offsetZ, speed);
			}
			location.getWorld().spawnParticle(effect, location, count, offsetX, offsetY, offsetZ, speed);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a particle effect to a player
	 * 
	 * @param effect
	 *            The particle effect to send
	 * @param player
	 *            The player to send the effect to
	 * @param location
	 *            The location to send the effect to
	 * @param offsetX
	 *            The x range of the particle effect
	 * @param offsetY
	 *            The y range of the particle effect
	 * @param offsetZ
	 *            The z range of the particle effect
	 * @param speed
	 *            The speed (or color depending on the effect) of the particle
	 *            effect
	 * @param count
	 *            The count of effects
	 */
	public static void sendToPlayer(ParticleEffects effect, Player player,
			Location location, float offsetX, float offsetY, float offsetZ,
			float speed, int count) {
		try {
			//Object packet = createPacket(effect, location, offsetX, offsetY,
			//		offsetZ, speed, count);
			//sendPacket(player, packet);
			if(effect.get() == Particle.REDSTONE) {
				Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 5);
				player.spawnParticle(effect.get(), location, count, offsetX, offsetY, offsetZ, speed, dust);
			} else {
				player.spawnParticle(effect.get(), location, count, offsetX, offsetY, offsetZ, speed);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Particle name;

	private int id;

	ParticleEffects(Particle name, int id) {
		this.name = name;
		this.id = id;
	}

	/**
	 * Gets the id of the Particle Effect
	 * 
	 * @return The id of the Particle Effect
	 */
	int getId() {
		return id;
	}

	/**
	 * Gets the name of the Particle Effect
	 * 
	 * @return The particle effect name
	 */
	Particle get() {
		return name;
	}

}