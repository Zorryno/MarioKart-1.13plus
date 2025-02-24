package net.stormdev.mario.server;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.tracks.RaceTrack;
import net.stormdev.mario.utils.MetaValue;
import net.stormdev.mario.utils.ObjectWrapper;

public class VoteHandler {
	
	private  String VOTE_META;
	private final String VOTE_META_KEY = "mariokart.vote";
	private final String VOTE_MESSAGE = ChatColor.GOLD+"Use \"/mvote <TrackName>\" to cast your vote!";
	private static int VOTE_TIME = 120;
	private Map<String, Integer> votes = new HashMap<String, Integer>();
	private Map<UUID, String> playerVotes = new HashMap<UUID, String>();
	private Scoreboard board;
	private Objective obj;
	private boolean closed = false;
	private BukkitTask voteCountdown = null;
	private long startTime;
	private List<String> maps;
	private List<Score> scores = new ArrayList<Score>();
	private Integer minPlayers;
	
	public VoteHandler(){
		startTime = System.currentTimeMillis();
		VOTE_META = UUID.randomUUID().toString();
		board = Bukkit.getScoreboardManager().getNewScoreboard();
		obj = board.registerNewObjective("votes", "dummy", ChatColor.BOLD+""+ChatColor.RED+"Votes:");
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		VOTE_TIME = MarioKart.config.getInt("general.server.votetime");
		calculateMapList();
		
		Collection<? extends Player> online = Bukkit.getOnlinePlayers();
		for(Player p:online){
			addPlayerToBoard(p);
			p.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
			p.sendMessage(getHelpString());
			p.sendMessage(getAvailTracksString());
			p.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
			try {
				bossBar(p);
			} catch (Exception e) {
				//OH WELL
			}
		}
		
		reloadScoreboardValues();
		
		voteCountdown = Bukkit.getScheduler().runTaskTimer(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				try {
					int i = getVoteTimeRemaining();
					obj.setDisplayName(ChatColor.BOLD+""+ChatColor.RED+"Votes: ("+i+")");
					if(i <= 0){
						//END VOTE
						voteCountdown.cancel();
						closeVotes();
					}
				} catch (Exception e) {
					return;
				}
				return;
			}}, 20l, 20l);
	}
	
	private void calculateMapList(){
		if(getMaps() != null){
			getMaps().clear();
		}
		else {
			setMaps(new ArrayList<String>());
		}
		List<RaceTrack> all = new ArrayList<RaceTrack>(MarioKart.plugin.trackManager.getRaceTracks());
		if(all.size() <= 5){
			for(RaceTrack track:all){
				getMaps().add(track.getTrackName());
				votes.put(track.getTrackName(), 0);
				if(minPlayers == null) { minPlayers = track.getMinPlayers(); } 					//If first Map set this as minPlayers
				if(minPlayers > track.getMinPlayers()) { minPlayers = track.getMinPlayers(); }	//Otherwise check if this track has a lower minPlayer-Number
			}
			return;
		}
		while(getMaps().size() < 5){
			RaceTrack rand = all.get(MarioKart.plugin.random.nextInt(all.size()));
			if(getMaps().contains(rand.getTrackName())){
				continue;
			}
			getMaps().add(rand.getTrackName());
			votes.put(rand.getTrackName(), 0);
			if(minPlayers == null) { minPlayers = rand.getMinPlayers(); } 					//If first Map set this as minPlayers
			if(minPlayers > rand.getMinPlayers()) { minPlayers = rand.getMinPlayers(); }	//Otherwise check if this track has a lower minPlayer-Number
		}
	}
	
	public boolean isBeingVotedOn(String track){
		return getMaps().contains(track);
	}
	
	public void bossBar(final Player player){
		
		final ObjectWrapper<BukkitTask> o = new ObjectWrapper<BukkitTask>();
		o.setValue(Bukkit.getScheduler().runTaskTimerAsynchronously(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				if(closed){
					BossBar.removeBar(player);
					o.getValue().cancel();
					return;
				}
				if(!FullServerManager.get().getStage().equals(ServerStage.WAITING)){
					o.getValue().cancel();
					return;
				}
				final float percent = (((float)(getVoteTimeRemaining())/(float)(getTotalTime()))*100);
				final int rem = getVoteTimeRemaining();
				Bukkit.getScheduler().runTask(MarioKart.plugin, new Runnable(){

					@Override
					public void run() {
						if(!BossBar.hasBar(player)){
							BossBar.setMessage(player, VOTE_MESSAGE, percent);
						}
						else {
							BossBar.setHealth(player, percent);
						}
						
						player.setLevel(rem);
						if(rem < 5){
							player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, Integer.MAX_VALUE);
						}
					}});
				return;
			}}, 20l, 20l));
	}
	
	public int getTotalTime(){
		int fullS = VOTE_TIME;
		int online = Bukkit.getOnlinePlayers().size();
		if(online < 2){
			fullS = VOTE_TIME;
		}
		else if(online <= 2){
			fullS = (int) (0.7*fullS);
		}
		else if(online < 4){
			fullS = (int) (0.6*fullS);
		}
		else if(online > 3 && online < 6){
			fullS = (int) (0.5*fullS);
		}
		else if(online > 6 && online < 10){
			fullS = (int) (0.25*fullS);
		}
		else if(online > 10){
			fullS = (int) (0.2*fullS);
		}
		return fullS;
	}
	
	public int getVoteTimeRemaining(){
		int fullS = getTotalTime();
		if(Bukkit.getOnlinePlayers().size() < minPlayers){
			startTime = System.currentTimeMillis();
			return fullS;
		}
		
		long diff = System.currentTimeMillis() - startTime;
		int rem = (int) ((fullS*1000)-diff)/1000;
		if(rem < 0){
			rem = 0;
		}
		return rem;
	}
	
	public void closeVotes(){
		if(closed){
			return;
		}
		closed = true;
		Collection<? extends Player> online = Bukkit.getOnlinePlayers();
		for(Player p:online){
			removePlayerFromBoard(p);
		}
		
		obj.unregister();
		
		String tName = null;
		int i = -1;
		if(votes.size() > 0){
			List<String> keys = new ArrayList<String>(votes.keySet());
			for(String key:keys){
				int z = votes.get(key);
				if(z>i){
					i = z;
					tName = key;
				}
			}
			if(tName == null){
				Bukkit.broadcastMessage("No tracks setup! Please setup some tracks and restart the server!");
				return;
			}
		}
		else {
			try {
				tName = MarioKart.plugin.trackManager.getRaceTrackNames().get(
						MarioKart.plugin.random.nextInt(
								MarioKart.plugin.trackManager.getRaceTrackNames().size()));
			} catch (Exception e) {
				//No tracks setup
				Bukkit.broadcastMessage("No tracks setup! Please setup some tracks and restart the server!");
				return;
			}
		}
		FullServerManager.get().trackSelected(tName);
	}
	
	public void removePlayerFromBoard(final Player player){
		player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		
		Bukkit.getScheduler().runTask(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				if(BossBar.hasBar(player)){
					BossBar.removeBar(player);
				}
				return;
			}});
	}
	
	public void addPlayerToBoard(Player player){
		player.setScoreboard(board);
		bossBar(player);
	}
	
	public String getHelpString(){
		return ChatColor.BOLD+""+ChatColor.GOLD+"Votes are open! Do \"/mvote <TrackName>\" to vote to race on your favourite track!";
	}
	
	public String getAvailTracksString(){
		StringBuilder avail = new StringBuilder(ChatColor.BOLD+""+ChatColor.DARK_RED+"Available tracks: ");
		List<String> tracks = getMaps();
		boolean f = true;
		for(String t:tracks){
			if(f){
				avail.append(ChatColor.WHITE+t+" (MinPlayers: "+MarioKart.plugin.trackManager.getRaceTrack(t).getMinPlayers()+")");
				f = false;
				continue;
			}
			avail.append(ChatColor.GOLD).append(", ").append(ChatColor.WHITE).append(t).append(" (MinPlayers: "+MarioKart.plugin.trackManager.getRaceTrack(t).getMinPlayers()+")");
		}
		return avail.toString();
	}
	
	private synchronized void incrementVote(String tName){
		int score = 0;
		if(votes.containsKey(tName)){
			score = votes.get(tName);
		}
		score++;
		votes.put(tName, score);
		
		reloadScoreboardValues();
	}
	
	public synchronized void decrementVote(UUID uuid){
		String tName = playerVotes.get(uuid);
		
		int score = votes.get(tName);
		score--;
		votes.put(tName, score);
		
		reloadScoreboardValues();
	}
	
	public boolean castVote(Player player, String trackName){
		if(closed){
			player.sendMessage(ChatColor.RED+"Sorry, track voting has closed");
			return false;
		}
		if(hasVoted(player)){
			player.sendMessage(ChatColor.RED+"You have already voted!");
			return false;
		}
		if(minPlayers > Bukkit.getOnlinePlayers().size()) {
			player.sendMessage(ChatColor.RED+"Not enough players!");
		}
		if(!MarioKart.plugin.trackManager.raceTrackExists(trackName)){
			player.sendMessage(ChatColor.RED+"That track doesn't exist! ("+trackName+")");
			player.sendMessage(getAvailTracksString());
			return false;
		}
		final String name = MarioKart.plugin.trackManager.getRaceTrack(trackName).getTrackName();
		if(!isBeingVotedOn(name)){
			player.sendMessage(ChatColor.RED+"That track is not being voted on, sorry.");
			return false;
		}
		Bukkit.getScheduler().runTaskAsynchronously(MarioKart.plugin, () -> {
				incrementVote(name);
				return;
		});
	
		player.setMetadata(VOTE_META_KEY, new MetaValue(VOTE_META, MarioKart.plugin));
		
		playerVotes.put(player.getUniqueId(), name);				//Store who voted for what		
		player.sendMessage(ChatColor.GREEN+"Cast your vote!");
		return true;
	}
	
	public void reloadScoreboardValues() {
		if(scores.size() > 0) {
			for(Score s : scores) {						//Clear scores
				board.resetScores(s.getEntry());
			}
			scores.clear();
		}
		
		if(minPlayers > Bukkit.getOnlinePlayers().size()) {						//Not enough players anyway -> Just... say that?
			ChatColor color = ChatColor.GRAY;
			String msg = MarioKart.msgs.get("server.notEnoughPlayers");
			Score line = obj.getScore(color + msg);
			line.setScore(0);
			scores.add(line);
		} else {
			for(Map.Entry<String, Integer> entry : votes.entrySet()) {			//Add every Score again
				ChatColor color = ChatColor.GRAY;
				if(entry.getValue() > 0) {
					color = ChatColor.GOLD;
				}
				Score score = obj.getScore(color + entry.getKey());
				score.setScore(entry.getValue());
		        scores.add(score);
			}
		}
	}
	
	public int getVotes(String track) {
		if(votes.get(track) == null ) {
			return 0;
		}
		return votes.get(track);
	}
	
	public boolean hasVoted(Player player){
		if(!player.hasMetadata(VOTE_META_KEY)){
			return false;
		}
		Object o = player.getMetadata(VOTE_META_KEY).get(0).value();
		boolean has = VOTE_META.equals(o.toString());
		if(!has){
			player.removeMetadata(VOTE_META_KEY, MarioKart.plugin);
		}
		return has;
	}

	public List<String> getMaps() {
		return maps;
	}

	public void setMaps(List<String> maps) {
		this.maps = maps;
	}
}
