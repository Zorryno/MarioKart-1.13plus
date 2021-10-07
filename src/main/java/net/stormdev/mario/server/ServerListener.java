package net.stormdev.mario.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.zorryno.zorrynosystems.minecraftutils.playerhead.chatmessageapi.ImageChar;
import de.zorryno.zorrynosystems.minecraftutils.playerhead.chatmessageapi.ImageMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.races.MarioKartRaceEndEvent;
import net.stormdev.mario.ui.VoteUI;
import net.stormdev.mario.utils.MetaValue;

import javax.imageio.ImageIO;

public class ServerListener implements Listener {
	private FullServerManager fsm;
	private final String MOVE_META = "mariokart.moved";
	
	public ServerListener(){
		this.fsm = FullServerManager.get();
		Bukkit.getScheduler().runTaskTimer(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {			//Testing if player has moved - every 9 seconds
				if(fsm.getStage().equals(ServerStage.BUILDING)){
					return;
				}
				Collection<? extends Player> online = Bukkit.getOnlinePlayers();
				for(Player player:online){
					if(!player.hasMetadata(MOVE_META)){
						player.setMetadata(MOVE_META, new MetaValue(System.currentTimeMillis(), MarioKart.plugin));
						continue;
					}
					Object o = player.getMetadata(MOVE_META).get(0).value();
					String s = o.toString();
					long moved;
					try {
						moved = Long.parseLong(s);
					} catch (NumberFormatException e) {
						continue;
					}
					long diff = System.currentTimeMillis()-moved;
					if(diff > 50000 && diff < 60000){ //They haven't moved for about 50 seconds
						//They are afk!
						player.sendMessage(ChatColor.RED+"WARNING: If you do not move in the next 10 seconds, you'll be afk kicked!");
						continue;
					}
					else if(diff >= 60000){
						player.kickPlayer("Kicked for AFK");
					}
				}
				return;
			}}, 9*20l, 9*20l);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	void interact(PlayerInteractEvent event){
		Player player = event.getPlayer();
		if(player.getInventory().getItemInMainHand().getType().equals(Material.EGG)){
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	void projectileThrow(ProjectileLaunchEvent event){
		if(event.getEntityType().equals(EntityType.EGG)){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	void onMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		player.removeMetadata(MOVE_META, MarioKart.plugin);
		player.setMetadata(MOVE_META, new MetaValue(System.currentTimeMillis(), MarioKart.plugin));
	}
	
	@EventHandler
	void onCarMove(VehicleMoveEvent event) {
		if(!event.getVehicle().getPassengers().isEmpty() && event.getVehicle().getPassengers().get(0) != null) {
			Player player = (Player) event.getVehicle().getPassengers().get(0);
			player.removeMetadata(MOVE_META, MarioKart.plugin);
			player.setMetadata(MOVE_META, new MetaValue(System.currentTimeMillis(), MarioKart.plugin));
		}
	}
	
	@EventHandler
	void invClick(InventoryClickEvent event){
		Entity e = event.getWhoClicked();
		if(!(e instanceof Player)){
			return;
		}
		
		if(!fsm.getStage().equals(ServerStage.WAITING)){
			return;
		}
		ItemStack clicked = event.getCurrentItem();
		if(!clicked.isSimilar(FullServerManager.exitItem)
				|| !(clicked.getItemMeta().getDisplayName().equals(FullServerManager.exitItem.getItemMeta().getDisplayName()))){
			return;
		}
		if(!clicked.isSimilar(FullServerManager.voteItem)
				|| !(clicked.getItemMeta().getDisplayName().equals(FullServerManager.voteItem.getItemMeta().getDisplayName()))){
			return;
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler
	void playerDmg(EntityDamageEvent event){
		if(!(event.getEntity() instanceof Player)){
			return;
		}
		
		Player player = (Player) event.getEntity();
		
		if(!event.getCause().equals(DamageCause.VOID)){
			return;
		}
		
		player.damage(10);
	}
	
	@EventHandler
	void useWaitingHotBarItem(PlayerInteractEvent event){
		Player player = event.getPlayer();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		if(!fsm.getStage().equals(ServerStage.WAITING)){
			return;
			
		}
		if(inHand.isSimilar(FullServerManager.exitItem)
				|| (inHand.getItemMeta().getDisplayName().equals(FullServerManager.exitItem.getItemMeta().getDisplayName()))){
			player.teleport(fsm.lobbyLoc); //For when they next login
			player.sendMessage(ChatColor.GRAY+"Teleporting...");
			fsm.sendToLobby(player);
		}
		if(inHand.isSimilar(FullServerManager.voteItem)
				|| (inHand.getItemMeta().getDisplayName().equals(FullServerManager.voteItem.getItemMeta().getDisplayName()))){
			MarioKart.plugin.getUIManager().assignUI(player, new VoteUI());
		}
	}
	
	@EventHandler
	void entityDamage(EntityDamageByEntityEvent event){ //Not part of MK
		event.setDamage(0);
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	void respawn(PlayerRespawnEvent event){
		if(fsm.getStage().equals(ServerStage.WAITING) || fsm.getStage().equals(ServerStage.STARTING)){
			event.setRespawnLocation(fsm.lobbyLoc);
		}
	}
	
	@EventHandler
	void onPing(ServerListPingEvent event){
		event.setMotd(fsm.getMOTD());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	void disconnect(PlayerQuitEvent event){
		event.setQuitMessage(null);
		Player player = event.getPlayer();
		player.removeMetadata(MOVE_META, MarioKart.plugin);
		if(player.getVehicle() != null){
			player.getVehicle().eject();
			player.getVehicle().remove();
		}
		if(fsm != null && fsm.voter != null){
			fsm.voter.removePlayerFromBoard(player);
		}
	}
	
	@EventHandler
	void prePlayerJoin(AsyncPlayerPreLoginEvent event){
		if(!fsm.getStage().getAllowJoin()){
			String reason = "Unable to join server at this time! ("+fsm.getStage().name()+")";
			event.disallow(Result.KICK_OTHER, reason);
			return;
		}
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	void playerJoin(PlayerJoinEvent event){
		event.setJoinMessage(null);
		final Player player = event.getPlayer();
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		player.setFoodLevel(20);
		player.getInventory().clear();
		
		boolean showVoteMsg = true;
		if(!fsm.getStage().getAllowJoin()){
			player.kickPlayer("Unable to join server at this time! ("+fsm.getStage().name()+")");
			/*
			player.sendMessage(ChatColor.RED+"Unable to join server at this time!");
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					fsm.sendToLobby(player);
					return;
				}}, 5*20l);
				*/
			return;
		}
		
		if(!MarioKart.reducedText) {
			player.sendMessage(ChatColor.BOLD+""+ChatColor.GOLD+"------------------------------");
			player.sendMessage(ChatColor.DARK_RED+"Welcome to MarioKart, "+ChatColor.WHITE+player.getName()+ChatColor.DARK_RED+"!");
			player.sendMessage(ChatColor.BOLD+""+ChatColor.GOLD+"------------------------------");
		}
		
		//Enable resource pack for them:
		if(!MarioKart.reducedText) {
			String rl = MarioKart.plugin.packUrl;                           //Send them the download url, etc for if they haven't get server RPs enabled
			player.sendMessage(MarioKart.colors.getInfo()
					+ MarioKart.msgs.get("resource.download"));
			String msg = MarioKart.msgs.get("resource.downloadHelp");
			msg = msg.replaceAll(Pattern.quote("%url%"),
					Matcher.quoteReplacement(ChatColor.RESET + ""));
			player.sendMessage(MarioKart.colors.getInfo() + msg);
			player.sendMessage(rl); //new line
		}
		
		if(!MarioKart.plugin.resourcedPlayers.contains(player.getName()) //Send them the RP for if they have got server RPs enabled
				&& MarioKart.plugin.fullPackUrl != null
				&& MarioKart.plugin.fullPackUrl.length() > 0){
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					player.setResourcePack(MarioKart.plugin.fullPackUrl);
					MarioKart.plugin.resourcedPlayers.add(player.getName());
					return;
				}}, 20l);
		}
		
		final Location spawnLoc = fsm.lobbyLoc;
		if(player.getVehicle() != null){
			player.getVehicle().eject();
			player.getVehicle().remove();
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					player.teleport(spawnLoc);
					return;
				}}, 2l);
		}
		else {
			player.teleport(spawnLoc);
		}
		player.setGameMode(GameMode.SURVIVAL);
		
		if(fsm.getStage().equals(ServerStage.WAITING)){
			player.getInventory().setItem(0,FullServerManager.voteItem.clone());
			player.getInventory().setItem(8,FullServerManager.exitItem.clone());
			if(fsm.voter == null){
				showVoteMsg = false;
				fsm.changeServerStage(ServerStage.WAITING);
			}
			fsm.voter.addPlayerToBoard(player);
			if(showVoteMsg){
				Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

					@Override
					public void run() {
						player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
						if(fsm.voter != null){
							player.sendMessage(fsm.voter.getHelpString());
							player.sendMessage(fsm.voter.getAvailTracksString());
						}
						player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
						return;
					}}, 2l);
			}
		}
		else if(fsm.getStage().equals(ServerStage.STARTING)){
			player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
			player.sendMessage(ChatColor.GOLD+"Game starting in under 10 seconds...");
			player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
		}
		else if(fsm.getStage().equals(ServerStage.BUILDING)){
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					if(!player.hasPermission(FullServerManager.BUILD_PERM)){
						player.kickPlayer("Sorry, server closed!");
						return;
					}
					else {
						player.sendMessage(ChatColor.GRAY+"(Server is in build mode)");
						player.setGameMode(GameMode.CREATIVE);
					}
				}}, 2l);
		}
	}
	
	@EventHandler
	public void raceEnding(MarioKartRaceEndEvent event){
		Player player = Bukkit.getPlayer(event.getRace().getWinner());

		List<String> headText = new ArrayList<>();
		for(int i = 1; i <= 8; i++) {
			headText.add(MarioKart.msgs.get("race.end.playerHead." + i));
		}
		broadcastPlayerHeadAsync(player, headText);
		fsm.restart();
	}

	private static void broadcastPlayerHeadAsync(final Player p, final List<String> text) {
		new Thread(() -> {
			try {
				Bukkit.broadcastMessage("");
				BufferedImage bi = ImageIO.read(new URL("https://crafatar.com/avatars/" + p.getUniqueId() + ".png"));
				(new ImageMessage(bi, 8, ImageChar.BLOCK.getChar()).appendText(text)).broadcast();
				Bukkit.broadcastMessage("");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@EventHandler
	void foodChange(FoodLevelChangeEvent event){
		Entity e = event.getEntity();
		if(!(e instanceof Player)){
			return;
		}
		event.setFoodLevel(20);
		event.setCancelled(true);
	}
	
	@EventHandler
	void itemDrop(PlayerDropItemEvent event){
		event.setCancelled(true);
	}
	
	@EventHandler
	void join(PlayerJoinEvent e){
		fsm.voter.reloadScoreboardValues();
	}
	
	@EventHandler
	void leave(PlayerQuitEvent e){
		if(fsm.voter != null) {
			if(fsm.voter.hasVoted(e.getPlayer())) {
				fsm.voter.decrementVote(e.getPlayer().getUniqueId());
				e.getPlayer().removeMetadata("mariokart.vote", MarioKart.plugin);
			} else {
				fsm.voter.reloadScoreboardValues();
			}
		}
	}
}
