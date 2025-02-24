package net.stormdev.mario.races;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.useful.ucarsCommon.StatValue;

import net.stormdev.mario.lesslag.DynamicLagReducer;
import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.players.PlayerQuitException;
import net.stormdev.mario.players.User;
import net.stormdev.mario.server.FullServerManager;
import net.stormdev.mario.tracks.RaceTrack;
import net.stormdev.mario.utils.DoubleValueComparator;

public class Race {
	public List<String> finished = new ArrayList<String>();
	private List<User> users = new ArrayList<User>();
	private UUID gameId = null;
	private RaceTrack track = null;
	private String trackName = "";
	private String winner = null;
	public String winning = "";
	public Boolean running = false;
	public long startTimeMS = 0;
	public long endTimeMS = 0;
	public long timeLimitS = 0;
	public long tickrate = 6;
	public long scorerate = 15;
	private BukkitTask task = null;
	private BukkitTask scoreCalcs = null;
	public int maxCheckpoints = 3;
	public int totalLaps = 3;
	public ArrayList<Location> reloadingItemBoxes = new ArrayList<Location>();
	public int finishCountdown = 60;
	Boolean ending = false;
	Boolean ended = false;
	public Scoreboard board = null;
	public Objective scores = null;
	public RaceType type = RaceType.RACE;
	public Objective scoresBoard = null;
	private int strikes = 0;
	final private Map<Integer, Location> checkLocs;

	public Race(RaceTrack track, String trackName, final RaceType type) {
		this.type = type;
		this.gameId = UUID.randomUUID();
		this.track = track;
		this.trackName = trackName;
		this.totalLaps = this.track.getLaps();
		this.maxCheckpoints = this.track.countCheckPoints() - 1;
		this.tickrate = MarioKart.config.getLong("general.raceTickrate");
		this.scorerate = (long) ((this.tickrate * 2) + (this.tickrate / 0.5));
		Bukkit.getScheduler().runTask(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				board = MarioKart.plugin.getServer().getScoreboardManager().getNewScoreboard();
				scores = board.registerNewObjective(" ", "dummy"," ");
				scores.setDisplaySlot(DisplaySlot.BELOW_NAME);
				if (type != RaceType.TIME_TRIAL) {
					scoresBoard = board.registerNewObjective(ChatColor.GOLD
							+ "Race Positions", "dummy","Race Positions");
				} else { // Time Trial
					scoresBoard = board.registerNewObjective(ChatColor.GOLD
							+ "Race Time(s)", "dummy","Race Time(s)");
				}
				scoresBoard.setDisplaySlot(DisplaySlot.SIDEBAR);
				return;
			}});
		
		int t = 100;
		while(scores == null && t>0){
			//Wait for above code to execute, or just presume OK if wait is too long
			t--;
		}
		
		this.timeLimitS = ((MarioKart.config
				.getInt("general.race.maxTimePerCheckpoint")
				* track.countCheckPoints()) * track.getLaps()) + 60;
		checkLocs = track.loadCheckpoints(Bukkit.getServer());
	}

	public RaceType getType() {
		return this.type;
	}
	
	public long getTimeLimitS(){
		return this.timeLimitS;
	}

	public synchronized User getUser(Player player) {
		String pname = player.getName();
		for (User user : users) {
			try {
				if (user.getPlayerName().equals(pname)) {
					return user;
				}
			} catch (Exception e) {
				if (!forceRemoveUser(user, true)) {
					MarioKart.logger.info("getUser() failed to remove user");
				}
			}
		}
		return null;
	}
	
	public synchronized void setFinished(Player player, Boolean bool) {
		getUser(player).setFinished(bool);
	}

	public synchronized User getUser(String playerName) {
		for (User user : users) {
			if (user.getPlayerName().equals(playerName)) {
				return user;
			}
		}
		return null;
	}

	public synchronized List<User> getUsers() {
		return new ArrayList<User>(users);
	}

	public synchronized void setUsers(List<User> users) {
		this.users = users;
		return;
	}

	public synchronized List<User> getUsersIn() {
		List<User> inUsers = new ArrayList<User>();
		for (User user : users) {
			if (user.isInRace()) {
				inUsers.add(user);
			}
		}
		return inUsers;
	}

	public List<String> getUsersFinished() {
		return new ArrayList<String>(finished);
	}

	public void playerOut(User user, boolean quit) {
		user.setInRace(false);
		MarioKart.plugin.hotBarManager.clearHotBar(user.getPlayerName());
		Player player = null;
		try {
			player = user.getPlayer();
		} catch (PlayerQuitException e) {
			if (!forceRemoveUser(user, quit)
					&& playerUserRegistered(user.getPlayerName())) {
				MarioKart.logger.info("race.playerOut failed to remove user");
			}
			return;
		}
		if(player != null){
			player.setLevel(user.getOldLevel());
			if(MarioKart.plugin.resourcedPlayers.contains(player.getName()) && !MarioKart.fullServer 
					&& MarioKart.plugin.emptyPackUrl != null
					&& MarioKart.plugin.emptyPackUrl.length() > 0){
				player.setResourcePack(MarioKart.plugin.emptyPackUrl);
				MarioKart.plugin.resourcedPlayers.remove(player.getName());
			}
			player.setExp(user.getOldExp());
		}
		return;
	}

	public synchronized Boolean join(Player player) {
		if (users.size() < this.track.getMaxPlayers()) {
			User user = new User(player, player.getLevel(), player.getExp());
			users.add(user);
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public void leave(User user, boolean quit) {
		Player ply = null;
		try {
			ply = user.getPlayer();
			if(ply != null){
				ply.setLevel(user.getOldLevel());
			}
		} catch (PlayerQuitException e1) {
			// User quit
		}
		final Player player = ply;
		if (quit) {
			if (!forceRemoveUser(user, quit)) {
				MarioKart.logger.info("race.quit failed to remove user");
			}
			if (type != RaceType.TIME_TRIAL) {
				if (users.size() < 2) {
					for (User u : getUsersIn()) {
						String msg = MarioKart.msgs.get("race.end.soon");
						try {
							u.getPlayer().sendMessage(
									MarioKart.colors.getInfo() + msg);
						} catch (PlayerQuitException e) {
							// Player is no longer in the game
							if (!forceRemoveUser(u, quit)) {
								MarioKart.logger
										.info("race.leave failed to remove user");
							}
						} catch (Exception e){
							//User is respawning
						}
					}
					startEndCount();
				}
			}
		}
		playerOut(user, quit);
		if (player != null) {
			player.removeMetadata("car.stayIn", MarioKart.plugin);
		}
		if (quit) {
			if (player != null) {
				scoresBoard.getScore(player.getDisplayName()).setScore(0);
				this.board.resetScores(player.getDisplayName());
				player.getInventory().clear();
				if (player.getVehicle() != null) {
					try {
						Vehicle veh = (Vehicle) player.getVehicle();
						veh.eject();
						veh.remove();
					} catch (ArrayIndexOutOfBoundsException e) {
						//Apparently triggered if someone tries to quit his minecraft (shift) on initial countdown
						Bukkit.getLogger().warning("Failed to eject cleanly.");
					}
				}
				player.removeMetadata("car.stayIn", MarioKart.plugin);
				player.getInventory().setContents(user.getOldInventory());
				player.setGameMode(user.getOldGameMode());
				try {
					player.teleport(this.track.getExit(MarioKart.plugin.getServer()));
					Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

						@Override
						public void run() {
							//Combat uCarsTrade's safeExit
							player.teleport(track.getExit(MarioKart.plugin.getServer()));
							return;
						}}, 4l);
				} catch (Exception e) {
					player.teleport(player.getWorld().getSpawnLocation());
				}
				player.sendMessage(ChatColor.GOLD
						+ "Successfully quit the race!");
				player.setScoreboard(MarioKart.plugin.getServer()
						.getScoreboardManager().getMainScoreboard());
				player.updateInventory();
			}
			for (User us : getUsers()) {
				try {
					us.getPlayer().sendMessage(
							ChatColor.GOLD + player.getName()
									+ " quit the race!");
				} catch (PlayerQuitException e) {
					if (!forceRemoveUser(us, quit)) {
						MarioKart.logger.info("race.quit failed to remove user");
					}
				} catch (Exception e){
					//User is respawning
				}
			}
		}
		try {
			recalculateGame();
		} catch (Exception e) {
		}
		return;
	}

	public void recalculateGame() {
		if (getUsersIn().size() < 1) {
			this.running = false;
			this.ended = true;
			this.ending = true;
			
			for(int i = 0; i < checkLocs.size(); i++) { //Clearing PowerUps on Track
				Location loc = checkLocs.get(i);
				Entity point = loc.getWorld().spawnEntity(loc, EntityType.ARROW);
				List<Entity> near = point.getNearbyEntities(25, 25, 25);
				for(Entity e : near) {
					if(e.getType() == EntityType.DROPPED_ITEM && MarioKart.powerupManager.isPowerup(((Item)e).getItemStack())) {
						e.remove();
					}
				}
				point.remove();
			}
			
			try {
				end();
			} catch (Exception e) {
			}
			MarioKart.plugin.raceScheduler.recalculateQueues();
		}
	}

	public synchronized Boolean isEmpty() {
		if (users.size() < 1) {
			return true;
		}
		return false;
	}

	public UUID getGameId() {
		return this.gameId;
	}

	public String getTrackName() {
		return this.trackName;
	}

	public RaceTrack getTrack() {
		return this.track;
	}

	public void setWinner(User winner) {
		this.winner = winner.getPlayerName();
	}

	public void startEndCount() {
		final int count = this.finishCountdown;

		MarioKart.plugin.getServer().getScheduler()
				.runTaskAsynchronously(MarioKart.plugin, new Runnable() {

					@Override
					public void run() {
						try {
							int z = count;
							while (z > 0) {
								z--;
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
								}
							}
							if (!ended) {
								try {
									try {
										MarioKart.plugin
												.getServer()
												.getScheduler()
												.runTask(MarioKart.plugin,
														new Runnable() {

															@Override
															public void run() {
																end();
																return;
															}
														});
									} catch (Exception e) {
										end();
									}
								} catch (IllegalArgumentException e) {
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e1) {
									}
									run();
									return;
								}
							}
						} catch (Exception e) {
							//User has left
						}
						return;
					}
				});
		return;
	}

	public String getWinner() {
		return this.winner;
	}

	public Boolean getRunning() {
		return this.running;
	}

	public void start() {
		this.running = true;
		final Race game = this;
		for (User user : getUsersIn()) {
			try {
				user.getPlayer().setScoreboard(board);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (PlayerQuitException e) {
				// User has left
				this.leave(user, true);
			} catch (Exception e){
				//User has quit
			}
		}
		MarioKart.plugin.musicManager.playMusic(this);
		final long ms = tickrate * 50;
		this.task = MarioKart.plugin.getServer().getScheduler()
				.runTaskAsynchronously(MarioKart.plugin, new Runnable() {

					@Override
					public void run() {
						long mis = ms;
						while (running && !ended) {
							double tps = DynamicLagReducer.getTPS();
							if (tps < 19.9) {								
								if (tps < 13) {
									if(strikes < 5){
										MarioKart.logger
										.info("[WARNING] Server at critical, Race "+getGameId()+" strike: "+strikes+"/5  (TPS: "+tps+")");
										strikes++;
										if(strikes > 3){
											System.gc(); //Try to prevent crash
										}
									}
									else{
										MarioKart.logger
										.info("[WARNING] Cancelling Race to compensate for resource loss!");
										broadcast(MarioKart.colors.getError()+"[Error] Race cancelled to compensate for "
												+ "server resource loss!");
										end();
										return;
									}
									mis = (long) (ms + (tps * 150));
								} else if (tps < 15) {
									MarioKart.logger.info("[INFO] Current system TPS (From independent service): "+tps);
									MarioKart.logger
									.info("[WARNING] Server running out of resources! - "
											+ "Compensating by reducing MarioKart tickRate (Accuracy)");
									mis = ms + 1000; // Go all out to keep up
								} else if (tps < 17) {
									MarioKart.logger
											.info("[WARNING] Server running out of resources! - "
													+ "Compensating by reducing MarioKart tickRate (Accuracy) (TPS: "+tps+")");
									mis = ms + 500; // Reduce lag
								} else if (tps < 19) {
									mis = ms + 100;
								} else {
									mis = ms + 10; // Slow down a tad
								}
								if(tps < 17){
									//Check that server still has capacity to handle
									DynamicLagReducer.overloadPrevention();
								}
								try {
									Thread.sleep(mis);
								} catch (InterruptedException e) {
									// Nothing
								}
							} else {
								try {
									Thread.sleep(ms);
								} catch (InterruptedException e) {
									// Nothing
								}
							}
							RaceExecutor.onRaceUpdate(game);
						}
						return;
					}
				});
		this.scoreCalcs = MarioKart.plugin.getServer().getScheduler()
				.runTaskTimer(MarioKart.plugin, new Runnable() {

					@Override
					public void run() {
						if (type != RaceType.TIME_TRIAL) {
							SortedMap<String, Double> sorted = game
									.getRaceOrder();
							Object[] keys = sorted.keySet().toArray();
							for (int i = 0; i < sorted.size(); i++) {
								int pos = i + 1;
								String pname = (String) keys[i];
								User u = getUser(pname);
								try {
									Player pl = u.getPlayer();
									if(pl != null){
										game.scores.getScore(pl.getDisplayName()).setScore(pos);
										game.scoresBoard.getScore(pl.getDisplayName())
											.setScore(-pos);
									}
								} catch (IllegalStateException e) {
									e.printStackTrace();
								} catch (IllegalArgumentException e) {
									e.printStackTrace();
								} catch (PlayerQuitException e) {
									// Player has left
									if (!forceRemoveUser(u, true)) {
										MarioKart.logger
												.info("race.scores failed to remove invalid user");
									}
								}
							}
						} else { // Time trial
							User user = null;
							try {
								user = game.getUsers().get(0);
							} catch (Exception e1) {
								return; // No players
							}
							try {
								Player pl = user.getPlayer();
								if(pl != null){
									long time = System.currentTimeMillis()
											- startTimeMS;
									time = time / 1000; // In s
									game.scores.getScore(pl.getDisplayName()).setScore((int) time);
									game.scoresBoard.getScore(pl.getDisplayName()).setScore(
											(int) time);
								}
							} catch (Exception e) {
								return;
								// Game ended or user has left or random error
								// with above code
							}
						}
						return;
					}
				}, this.scorerate, this.scorerate);
		try {
			this.startTimeMS = System.currentTimeMillis();
			Bukkit.getScheduler().runTask(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					try {
						RaceExecutor.onRaceStart(Race.this);
					} catch (Exception e) {
						e.printStackTrace();
						MarioKart.logger.log("Error starting race!", Level.SEVERE);
						end();
					}
					return;
				}});
			
		} catch (Exception e) {
			e.printStackTrace();
			MarioKart.logger.log("Error starting race!", Level.SEVERE);
			end();
		}
		return;
	}

	public synchronized SortedMap<String, Double> getRaceOrder() {
		Race game = this;
		HashMap<String, Double> checkpointDists = new HashMap<String, Double>();
		for (User user : new ArrayList<User>(users)) {
			try {
				Player player = user.getPlayer();
				if(player != null){
					if (player.hasMetadata("checkpoint.distance")) {
						List<MetadataValue> metas = player
								.getMetadata("checkpoint.distance");
						checkpointDists.put(user.getPlayerName(),
								(Double) ((StatValue) metas.get(0)).getValue());
					}
				}
			} catch (PlayerQuitException e) {
				leave(user, true);
				// Player is no longer in the race
			}
		}
		Map<String, Double> scores = new HashMap<String, Double>();
		for (User user : users) {
			if(!user.isRespawning()){
				try {
					int laps = game.totalLaps - user.getLapsLeft() + 1;
					int checkpoints = user.getCheckpoint();
					double distance = 1 / (checkpointDists.get(user.getPlayerName()));

					double score = (laps * game.getMaxCheckpoints()) + checkpoints
							+ distance;
					try {
						if (game.getWinner().equals(user)) {
							score = score + 1;
						}
					} catch (Exception e) {
					}
					scores.put(user.getPlayerName(), score);
				} catch (Exception e) {
					//User is respawning
				}
			}
		}
		DoubleValueComparator com = new DoubleValueComparator(scores);
		SortedMap<String, Double> sorted = new TreeMap<String, Double>(com);
		sorted.putAll(scores);
		if (sorted.size() >= 1) {
			this.winning = (String) sorted.keySet().toArray()[0];
		}
		return sorted;
	}

	@SuppressWarnings("unchecked")
	public void end() {
		if(ended){
			return;
		}
		this.running = false;
		ended = true;
		
		for (Location l : ((List<Location>) this.reloadingItemBoxes.clone())) {
			if(MarioKart.powerupManager.spawnItemPickupBox(l)){
				this.reloadingItemBoxes.remove(l);
			}
			else {//Have a second go
				MarioKart.powerupManager.spawnItemPickupBox(l);
				this.reloadingItemBoxes.remove(l);
			}
		}
		if (task != null) {
			task.cancel();
		}
		try {
			if (scoreCalcs != null) {
				scoreCalcs.cancel();
			}
		} catch (Exception e1) {
		}
		try {
			this.board.clearSlot(DisplaySlot.BELOW_NAME);
		} catch (IllegalArgumentException e) {
		}
		try {
			this.board.clearSlot(DisplaySlot.SIDEBAR);
		} catch (IllegalArgumentException e) {
		}
		try {
			this.scores.unregister();
			this.scoresBoard.unregister();
		} catch (IllegalStateException e) {
		}

		this.endTimeMS = System.currentTimeMillis();
		for (User user : getUsersIn()) {
			Player player = null;
			try {
				player = user.getPlayer();
                if(player != null){
                	Vehicle cart = ((Vehicle)player.getVehicle());
                	cart.remove();
                	
                	final Player pl = player;
                	
                	Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

						@Override
						public void run() {
							//Combat uCarsTrade's safeExit
							pl.teleport(track.getExit(MarioKart.plugin.getServer()));
	    					pl.setGameMode(user.getOldGameMode());
							return;
						}}, 4l);
                	
                	player.setScoreboard(MarioKart.plugin.getServer()
    						.getScoreboardManager().getMainScoreboard());
                	player.removeMetadata("car.stayIn", MarioKart.plugin);
    				player.getInventory().setContents(user.getOldInventory());
    				player.setLevel(user.getOldLevel());
    				player.setExp(user.getOldExp());
                }
			} catch (PlayerQuitException e) {
				leave(user, true);
			}
			try {
				RaceExecutor.finishRaceSync(this, user, true);
			} catch (Exception e) {
				//Race has been voided
			}
		}
		try {
			RaceExecutor.onRaceEnd(this);
		} catch (Exception e) {
			MarioKart.logger.info("[IMPORTANT] Failed to process trailing end of race!");
			e.printStackTrace();
			//Race voided
		}
		try {
			clear();
			MarioKart.plugin.raceScheduler.removeRace(this);
			MarioKart.plugin.raceScheduler.recalculateQueues();
		} catch (Exception e) {
			MarioKart.logger.info("[IMPORTANT] Failed to remove race");
			e.printStackTrace();
			//Race Voided
		}
		System.gc();
		return;
	}

	public synchronized void finish(User user) {
		if (!ending) {
			ending = true;
			startEndCount();
		}
		finished.add(user.getPlayerName());
		if (!forceRemoveUser(user, false)) {
			MarioKart.logger.info("race.finish failed to remove user");
		}
		user.setFinished(true);
		user.setInRace(false);
		users.add(user);
		try {
			Player player = user.getPlayer();
			if (player == null) {
				player = MarioKart.plugin.getServer()
						.getPlayer(user.getPlayerName()); // Player removed
															// prematurely
			}
			if(player != null){
				player.setLevel(user.getOldLevel());
				player.setExp(user.getOldExp());
			}
		} catch (Exception e) {
			// Player has left
		}
		this.endTimeMS = System.currentTimeMillis();
		RaceExecutor.finishRace(this, user, false);
		System.gc();
	}
	
	/**
     * 
     * @return Finish position or -1 if not finished
     */
    public int getFinishPosition(String playerName){
            return finished.indexOf(playerName)+1;
    }

	public synchronized User updateUser(Player player) {
		String playerName = player.getName();
		for (User u : getUsers()) {
			if (u.getPlayerName().equals(playerName)) {
				if (!forceRemoveUserSilent(u)) {
					MarioKart.logger.info("updateUser() failed to remove user");
				}
				u.setPlayer(player);
				users.add(u);
				return u;
			}
		}
		return null;
	}
	
	public synchronized User updateUser(User user) {
		for (User u : getUsers()) {
			if (u.getPlayerName().equals(user.getPlayerName())) {
				if (!forceRemoveUser(u, false)) {
					MarioKart.logger.info("updateUser() failed to remove user");
				}
				users.add(user);
				return user;
			}
		}
		return null;
	}

	public CheckpointCheck playerAtCheckpoint(Integer[] checks, Player p,
			Server server) {
		int checkpoint = 0;
		Boolean at = false;
		Location pl = p.getLocation();
		for (Integer key : checks) {
			try {
				Location check = checkLocs.get(key);
				if(check == null){
					continue;
				}
				double dist = check.distanceSquared(pl); // Squared because
															// of
				// better
				// performance
				p.removeMetadata("checkpoint.distance", MarioKart.plugin);
				p.setMetadata("checkpoint.distance", new StatValue(dist,
						MarioKart.plugin));
				if (dist < MarioKart.plugin.checkpointRadiusSquared) {
					at = true;
					checkpoint = key;
					return new CheckpointCheck(at, checkpoint);
				}
			} catch (Exception e) {
				// Un-measureable distance (Diff. world or sommat)
			}
		}
		return new CheckpointCheck(at, checkpoint);
	}

	public int getMaxCheckpoints() {
		return maxCheckpoints; // Starts at 0
	}

	public Boolean atLine(Server server, Location loc) {
		Location line1 = this.track.getLine1(server);
		Location line2 = this.track.getLine2(server);
		String lineAxis = "x";
		Boolean at = false;
		Boolean l1 = true;
		if (line1.getX() + 0.5 > line2.getX() - 0.5
				&& line1.getX() - 0.5 < line2.getX() + 0.5) {
			lineAxis = "z";
		}
		if (lineAxis == "x") {
			if (line2.getX() < line1.getX()) {
				l1 = false;
			}
			if (l1) {
				if (line2.getX() + 0.5 > loc.getX()
						&& loc.getX() > line1.getX() - 0.5) {
					at = true;
				}
			} else {
				if (line1.getX() + 0.5 > loc.getX()
						&& loc.getX() > line2.getX() - 0.5) {
					at = true;
				}
			}
			if (at) {
				if (line1.getZ() + 4 > loc.getZ()
						&& line1.getZ() - 4 < loc.getZ()) {
					if (line1.getY() + 4 > loc.getY()
							&& line1.getY() - 4 < loc.getY()) {
						return true;
					}
				}
			}
		} else if (lineAxis == "z") {
			if (line2.getZ() < line1.getZ()) {
				l1 = false;
			}
			if (l1) {
				if (line2.getZ() + 0.5 > loc.getZ()
						&& loc.getZ() > line1.getZ() - 0.5) {
					at = true;
				}
			} else {
				if (line1.getZ() + 0.5 > loc.getZ()
						&& loc.getZ() > line2.getZ() - 0.5) {
					at = true;
				}
			}
			if (at) {
				if (line1.getX() + 4 > loc.getX()
						&& line1.getX() - 4 < loc.getX()) {
					if (line1.getY() + 4 > loc.getY()
							&& line1.getY() - 4 < loc.getY()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public void broadcast(String msg) {
		for (User user : getUsersIn()) {
			Player player = null;
			try {
				player = user.getPlayer();
			} catch (PlayerQuitException e) {
				leave(user, true);
			}
			if(player != null){
				player.sendMessage(MarioKart.colors.getInfo() + msg);
			}
		}
		return;
	}

	public synchronized void clear() {
		users.clear();
		finished.clear();
		this.ended = true;
		this.ending = true;
		if(this.scoreCalcs != null)
			this.scoreCalcs.cancel();
		System.gc();
		return;
	}

	public synchronized Boolean removeUser(User user, boolean quit) {
		if (!users.contains(user)) {
			return false;
		}
		users.remove(user);
		user.clear();
		if(MarioKart.fullServer){
			final Player player;
			try {
				player = user.getPlayer();
			} catch (PlayerQuitException e) {
				return true; //GOOD
			}
			if(player != null && player.isOnline()){
				FullServerManager.get().onEnd(player, quit);
			}
		}
		return true;
	}
	
	public synchronized Boolean removeUserSilent(User user) {
		if (!users.contains(user)) {
			return false;
		}
		users.remove(user);
		user.clear();
		return true;
	}

	public synchronized Boolean removeUser(String user, boolean quit) {
		for (User u : new ArrayList<User>(users)) {
			if (u.getPlayerName().equals(user)) {
				return removeUser(u, quit);
			}
		}
		return false;
	}
	
	public synchronized Boolean removeUserSilent(String user) {
		for (User u : new ArrayList<User>(users)) {
			if (u.getPlayerName().equals(user)) {
				return removeUserSilent(u);
			}
		}
		return false;
	}

	public synchronized Boolean forceRemoveUser(User user, boolean quit) {
		if (!removeUser(user, quit)) {
			if (!removeUser(user.getPlayerName(), quit)) {
				user.clear();
				return false;
			}
		}
		user.clear();
		return true;
	}
	
	public synchronized Boolean forceRemoveUserSilent(User user) {
		if (!removeUserSilent(user)) {
			if (!removeUserSilent(user.getPlayerName())) {
				user.clear();
				return false;
			}
		}
		user.clear();
		return true;
	}

	public synchronized Boolean playerUserRegistered(String name) {
		for (User u : users) {
			if (u.getPlayerName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public synchronized void clearUsers(){
		users.clear();
	}
}
