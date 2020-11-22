package net.stormdev.mario.powerups;


import org.bukkit.util.Vector;

import net.stormdev.mario.players.User;

public interface TrackingShell extends Shell {
	public void setTarget(String player);
	public String getTarget();
	public Vector calculateVelocity();
	public void setCurrentCheckpoint(int check);
}
