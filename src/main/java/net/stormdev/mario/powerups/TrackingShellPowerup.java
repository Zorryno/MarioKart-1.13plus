package net.stormdev.mario.powerups;

import java.util.List;
import java.util.regex.Pattern;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.players.User;
import net.stormdev.mario.races.Race;
import net.stormdev.mario.races.RaceExecutor;
import net.stormdev.mario.sound.MarioKartSound;
import net.stormdev.mario.utils.SerializableLocation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.useful.ucarsCommon.StatValue;

public abstract class TrackingShellPowerup extends ShellPowerup implements TrackingShell {
	private String target;
	private int currentCheckpoint = 0;
	private BukkitTask task = null;

	@Override
	public void doLeftClickAction(User user, Player player, Minecart car,
			Location carLoc, Race race, ItemStack inHand){
		return; //Don't do anything
	}

	@Override
	public void setCurrentCheckpoint(int check) {
		this.currentCheckpoint = check;
	}

	@Override
	public void move() {
		if(!isFired()){
			return;
		}
		Item shell = super.getFiredItem();
		int sound = 0;
		if (shell.hasMetadata("shell.sound")) {
			sound = (Integer) ((StatValue) shell.getMetadata("shell.sound")
					.get(0)).getValue();
		}
		if (sound < 1) {
			//Shell Tracking sound
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){
				@Override
				public void run() {
					List<Entity> near = shell.getNearbyEntities(15, 5, 15);
					for(Entity e:near){
						if(e instanceof Player){
							MarioKart.plugin.musicManager.playCustomSound((Player) e, MarioKartSound.TRACKING_BLEEP);
						}
					}
					return;
				}
			}, 4l);
			sound = 3;
			shell.removeMetadata("shell.sound", MarioKart.plugin);
			shell.setMetadata("shell.sound", new StatValue(sound, MarioKart.plugin));
		} else {
			sound--;
			shell.removeMetadata("shell.sound", MarioKart.plugin);
			shell.setMetadata("shell.sound", new StatValue(sound, MarioKart.plugin));
		}

		Vector v = calculateVelocity();
		shell.setVelocity(v); //Move the shell

	}

	@Override
	public void collide(Player target) {
		String msg = MarioKart.msgs.get("mario.hit");
		msg = msg.replaceAll(Pattern.quote("%name%"), "tracking shell");
		MarioKart.plugin.musicManager.playCustomSound(target, MarioKartSound.SHELL_HIT);
		target.sendMessage(ChatColor.RED + msg);
		Entity cart = target.getVehicle();
		if(cart == null){
			return;
		}
		if(!(cart instanceof Minecart)){
			while(!(cart instanceof Minecart) && cart.getVehicle() != null){
				cart = cart.getVehicle();
			}
			if(!(cart instanceof Minecart)){
				return;
			}
		}

		MarioKart.plugin.raceMethods.createExplode(cart.getLocation(), 1);
		if(getFiredItem().getItemStack().getItemMeta().getDisplayName().equals("Blue shell")) {
			RaceExecutor.penalty(target, ((Minecart) cart), 4, 1.5);
		} else {
			RaceExecutor.penalty(target, ((Minecart) cart), 2, 1);
		}
		setExpiry(0);
		return;
	}

	@Override
	public void setTarget(String player) {
		this.target = player;
	}

	@Override
	public String getTarget() {
		return this.target;
	}

	@Override
	public Vector calculateVelocity() {
		Location shellLoc = getFiredItem().getLocation();
		double speed = 1.2;
		boolean isBlue = getFiredItem().getItemStack().getItemMeta().getDisplayName().equals("Blue shell");
		if(isBlue) {
			speed = 1.5;
		}
		final Player target = MarioKart.plugin.getServer().getPlayer(getTarget());
		Race game = MarioKart.plugin.raceMethods.inAGame(target, false);

		Location targetLoc = null;
		boolean goToCheck = false;
	
		if(game == null || game.getUser(target) == null) {
			setExpiry(0);
			return new Vector(0,0,0);
		}

		if(this.currentCheckpoint != game.getUser(target).getCheckpoint()) {		//Better Tracking: First to same Checkpoint, then old tracking
			if(game.getTrack().getCheckpoint(this.currentCheckpoint + 1) != null) {
				targetLoc = SerializableLocation.returnLocation(game.getTrack().getCheckpoint(this.currentCheckpoint + 1)); //Where to go
			} else {
				targetLoc = SerializableLocation.returnLocation(game.getTrack().getCheckpoint(0));
			}

			Location shLoc = shellLoc.clone();
			Chunk chunk = shLoc.getChunk();
			if (!chunk.isLoaded()) {
				chunk.load(true);
			}
			chunk = shLoc.add(item.getVelocity()).getChunk();
			if (!chunk.isLoaded()) {
				chunk.load(true);
			}
			goToCheck = true;
		} else {
			targetLoc = target.getLocation();
		}

		double x = targetLoc.getX() - shellLoc.getX();
		double z = targetLoc.getZ() - shellLoc.getZ();
		Boolean ux = true;
		double px = Math.abs(x);
		double pz = Math.abs(z);
		if (px > pz) {
			ux = false;
		}
		if (ux) {
			// x is smaller
			// long mult = (long) (pz/speed);
			x = (x / pz) * speed;
			z = (z / pz) * speed;
		} else {
			// z is smaller
			// long mult = (long) (px/speed);
			x = (x / px) * speed;
			z = (z / px) * speed;
		}


		if (!goToCheck && !isBlue && pz < 1.1 && px < 1.1) {
			collide(target);
		} else if (!goToCheck && isBlue && pz < 1.5 && px < 1.5) {
			collide(target);
		} else if (pz < 1.5 && px < 1.5) {	//Reached Checkpoint
			this.setCurrentCheckpoint(this.currentCheckpoint + 1);
		}

		Location loc = getFiredItem().getLocation();

		//Bouncing off walls
		Vector dir = new Vector(x,0,z);
		if(Math.abs(dir.getX()) > 1) {
			dir.setX(dir.getX()/Math.abs(dir.getX()));
		}
		if(Math.abs(dir.getZ()) > 1) {
			dir.setZ(dir.getZ()/Math.abs(dir.getZ()));
		}
		Block toHit = loc.add(dir).getBlock();
		Vector vel = null;
		if(!toHit.isEmpty() && !toHit.isLiquid()){
			if(toHit.getType().name().toLowerCase().contains("slab")) { //RedShells need to climb up steps
				vel = new Vector(x,0.5,z);
			} else {
				toHit = getFiredItem().getLocation().add(dir.setX(0)).getBlock();
				if(!toHit.isEmpty() && !toHit.isLiquid()) {
					vel =  new Vector(x,0,-z);
				} else {
					vel = new Vector(-x,0,z);
				}
			}
		} else {
			vel = new Vector(x, 0, z);
		}

		//Y-Course (move with track on Y-Axis)
		int height = getHeight(loc);
		if(getFiredItem().getItemStack().getItemMeta().getDisplayName().contains("Blue")) {
			vel.add(new Vector(0,0.15,0)); 									//Correct gravity of BlueShell
			if(height > 3 && loc.getY() > targetLoc.getY()) { 			  	//"Smooth" Y-Axis Movement
				vel.subtract(new Vector(0,0.1,0));
				if(height > 4) {
					vel.subtract(new Vector(0,0.3,0));
				}
				if(height > 5) {
					vel.subtract(new Vector(0,0.5,0));
				}
			} else if(height < 3) {
				vel.add(new Vector(0,0.5,0));
			}
		} else {
			if(height > 1) {
				vel.subtract(new Vector(0,0.5,0));
			}
		}
		return vel;
	}

	static int getHeight(Location loc) {
		boolean blockFound = false;
		int height = 0;
		while(!blockFound) {
			if(!loc.getBlock().isEmpty()) {
				blockFound = true;
			} else {
				loc.subtract(0,1,0);
				height++;
			}
		}
		return height;
	}

	@Override
	public void start() {
		if(!isFired()){
			return;
		}

		Race game = MarioKart.plugin.raceMethods.inAGame(Bukkit.getPlayer(owner), false);
		User user = game.getUser(owner);

		this.setCurrentCheckpoint(user.getCheckpoint());
		super.setCooldown(0); //No cooldown for tracking shells
		super.setExpiry(200); //Expire after moving 33 times

		task = Bukkit.getScheduler().runTaskAsynchronously(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				while(!remove()){
					Item item = getFiredItem();
					item.setTicksLived(1);
					item.setPickupDelay(Integer.MAX_VALUE);

					//Move the item
					move();

					//Decrease the cooldown and expiry
					decrementCooldown();
					decrementExpiry();

					try {
						Thread.sleep(150);
					} catch (InterruptedException e) {
						return;
					}
				}
				return;
			}});
	}

	@Override
	public boolean remove(){
		if(!super.isExpired()){
			return false;
		}
		target = null;
		if(super.item != null)
			super.item.remove();
		super.item = null;
		super.owner = null;
		if(task != null){
			task.cancel();
		}
		return true;
	}

}
