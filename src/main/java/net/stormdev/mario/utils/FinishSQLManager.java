package net.stormdev.mario.utils;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.stormdev.mkstormapi.SQL.MySQL;
import org.stormdev.mkstormapi.SQL.SQLManager;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.uuidapi.PlayerIDFinder;

public class FinishSQLManager {
	private SQLManager sqlManager = null;
	private boolean sql = true;
	
	private static final String SQL_WIN_TABLE = "MK_WinList";
	private static final String SQL_TIME_TABLE = "MK_TimeList";
	
	private static final String SQL_KEY = "id";
	private static final String SQL_UUID_KEY = "playerid";
	private static final String SQL_NAME_KEY = "playername";
	private static final String SQL_TRACK_KEY = "track";
	
	private static final String SQL_WIN_KEY = "wins";
	private static final String SQL_TIME_KEY = "time";
	
	private static final String SQL_DATE_KEY = "set_on";
	
	public FinishSQLManager() {		
		try {	//Try connecting to database - catch fail
			String sqlHost = MarioKart.config.getString("general.winlist.sqlHostName");
			String sqlDB = MarioKart.config
					.getString("general.winlist.sqlDataBaseName");
			int port = Integer.parseInt(MarioKart.config.getString("general.winlist.sqlPort"));
			String url = "jdbc:mysql://"
					+ sqlHost + ":" + port + "/" + sqlDB;
				
			sqlManager = new SQLManager(new MySQL(MarioKart.plugin, url, MarioKart.config.getString("general.winlist.sqlUsername"), MarioKart.config.getString("general.winlist.sqlPassword")), MarioKart.plugin);
		} catch (Exception e) {
			sql = false;
		}
		
		try { // Check that it loaded okay... catch fail
			if (this.isActive()) {
				if(!checkIfOldAndFix()) {	//If it fixed something it would have returned true
					createTables();
					if(!sqlManager.tableExists(SQL_TIME_TABLE)) {		//IF there was a time-table it doesn't need filling
						fillTimes();
					}
				}
			}
		} catch (Exception e) {
			sql = false;
		}
	}
	
	public void giveWin(String trackname, Player player) {		
		Bukkit.getScheduler().runTaskAsynchronously(MarioKart.plugin, () -> {
			try {
				String playerId = PlayerIDFinder.getMojangID(player).getID();
				Integer id = (Integer) sqlManager.searchTable(SQL_WIN_TABLE, SQL_UUID_KEY, playerId, SQL_TRACK_KEY, trackname, SQL_KEY);
				if(id == null){
					int one = 1;
					sqlManager.setInTable(SQL_WIN_TABLE, Arrays.asList(new String[]{SQL_UUID_KEY, SQL_NAME_KEY, SQL_TRACK_KEY, SQL_WIN_KEY}), Arrays.asList(new Object[]{playerId, player.getName(), trackname, one}));
					
					id = (Integer) sqlManager.searchTable(SQL_WIN_TABLE, SQL_UUID_KEY, playerId, SQL_TRACK_KEY, trackname, SQL_KEY);
				} else {
					Object o = sqlManager.searchTable(SQL_WIN_TABLE, SQL_KEY, id.toString(), SQL_WIN_KEY);
					int wins = (int) o;
					wins++;
					
					sqlManager.setInTable(SQL_WIN_TABLE, SQL_KEY, id.toString(), SQL_WIN_KEY, wins);
					String nameInTable = sqlManager.searchTable(SQL_WIN_TABLE, SQL_KEY, id.toString(), SQL_NAME_KEY).toString();
					if(!nameInTable.equals(player.getName())) {
						sqlManager.setInTable(SQL_WIN_TABLE, SQL_KEY, id.toString(), SQL_NAME_KEY, player.getName());
					}
				}
				sqlManager.setInTable(SQL_WIN_TABLE, SQL_KEY, id.toString(), SQL_DATE_KEY, new Date(new java.util.Date().getTime()));
			} catch (SQLException e) {
				//BUGZ
				e.printStackTrace();
			}
			return;
		});
	}
	
	public void setTime(String trackname, Player player, double time) {
		Bukkit.getScheduler().runTaskAsynchronously(MarioKart.plugin, () -> {
			try {
				String playerId = PlayerIDFinder.getMojangID(player).getID();
				Integer id = (Integer) sqlManager.searchTable(SQL_TIME_TABLE, SQL_UUID_KEY, playerId, SQL_TRACK_KEY, trackname, SQL_KEY);
				if(id == null) {
					sqlManager.setInTable(SQL_TIME_TABLE, Arrays.asList(new String[]{SQL_UUID_KEY, SQL_NAME_KEY, SQL_TRACK_KEY, SQL_TIME_KEY, SQL_DATE_KEY}), 
							Arrays.asList(new Object[]{playerId, player.getName(), trackname, time, new Date(new java.util.Date().getTime())}));
					return;
				} else if(time < (double) sqlManager.searchTable(SQL_TIME_TABLE, SQL_KEY, id.toString(), SQL_TIME_KEY)) {
					sqlManager.setInTable(SQL_TIME_TABLE, SQL_KEY, id.toString(), SQL_TIME_KEY, time);
					
					sqlManager.setInTable(SQL_TIME_TABLE, SQL_KEY, id.toString(), SQL_DATE_KEY, new Date(new java.util.Date().getTime()));
				}
				String nameInTable = sqlManager.searchTable(SQL_TIME_TABLE, SQL_KEY, id.toString(), SQL_NAME_KEY).toString();
				if(!nameInTable.equals(player.getName())) {
					sqlManager.setInTable(SQL_TIME_TABLE, SQL_KEY, id.toString(), SQL_NAME_KEY, player.getName());
				}
			} catch (SQLException e) {
				//BUGZ
				e.printStackTrace();
			}
			return;
		});
	}
	
	public boolean isActive() {
		return sql;
	}
	
	private boolean checkIfOldAndFix() throws SQLException {
		String SQL_WIN_TABLE_OLD = "WinList";
		String SQL_TIME_TABLE_OLD = "TimeList";
		
		if(sqlManager.tableExists(SQL_WIN_TABLE)) {
			//Newest version - proceed with normal checks
			return false;
		}
		
		if(sqlManager.tableExists(SQL_WIN_TABLE_OLD)) {
			if(!sqlManager.tableExists(SQL_TIME_TABLE_OLD)) {
				//Very old, hasn't got times-list
				updateTables(false);
				return true;
			} else if(sqlManager.hasColumn(SQL_WIN_TABLE_OLD, "playerIdWithTrackName")) {
				//Middle-Child - still needs full update
				updateTables(false);
				return true;
			} else {
				//Only need to rename tables, everything else should be fine
				updateTables(true);
				return true;
			}
		}
		//NOTHING whatsoever
		return false;
	}
	
	private void updateTables(boolean onlyRename) throws SQLException {
		String SQL_WIN_TABLE_OLD = "WinList";
		String SQL_TIME_TABLE_OLD = "TimeList";
		if(onlyRename) {
			String statement = "ALTER TABLE"+SQL_WIN_TABLE_OLD+"RENAME TO"+SQL_WIN_TABLE+";";
			sqlManager.exec(statement);
			
			//We know that the winlist needs renaming but maybe the timelist doesn't even exist
			if(sqlManager.tableExists(SQL_TIME_TABLE)) {
				statement = "ALTER TABLE"+SQL_TIME_TABLE_OLD+"RENAME TO"+SQL_TIME_TABLE+";";
				sqlManager.exec(statement);
			} else {
				createTables();
				fillTimes();
			}
			return;
		}
		
		
		List<Map<Object,Object>> winListOld = null;
		MarioKart.logger.info("Trying to update the database. This might take a while");
		
		if(sqlManager.tableExists(SQL_WIN_TABLE_OLD)) {
			winListOld = sqlManager.getAll(SQL_WIN_TABLE_OLD , Arrays.asList(new String[]{"playerIdWithTrackname", "wins", "set_on"}));
			sqlManager.deleteTable(SQL_WIN_TABLE_OLD);
		}
		
		if(sqlManager.tableExists(SQL_TIME_TABLE_OLD)) {
			sqlManager.deleteTable(SQL_TIME_TABLE_OLD);
		}
		
		createTables();
		
		for(Map<Object,Object> current:winListOld) {
			List<String> keys = new ArrayList<String>();
			List<Object> values = new ArrayList<Object>();
			
			//Get keys and values for the given "row"
			for(Object obj:current.keySet()) {
				keys.add(obj.toString());
				values.add(current.get(obj));
			}
			if(!keys.contains(SQL_DATE_KEY)) {
				keys.add(SQL_DATE_KEY);
				values.add(new Date(new java.util.Date().getTime()));
			}
			
			String[] idSplit = values.get(keys.indexOf("playerIdWithTrackname")).toString().split("\\|");
			String playerId = idSplit[0];
			String playerName = Bukkit.getOfflinePlayer(UUID.fromString(playerId)).getName();
			String trackname = idSplit[1];
			int wins = (int) values.get(keys.indexOf(SQL_WIN_KEY));
			Date date = (Date) values.get(keys.indexOf(SQL_DATE_KEY));
			
			sqlManager.setInTable(SQL_WIN_TABLE, Arrays.asList(new String[]{SQL_UUID_KEY, SQL_NAME_KEY, SQL_TRACK_KEY, SQL_WIN_KEY, SQL_DATE_KEY}),
					Arrays.asList(new Object[]{playerId, playerName, trackname, wins, date}));
		}
		
		fillTimes();
	}
	
	private void fillTimes() throws SQLException {
		MarioKart.logger.info("Filling SQL-Time-Table - This might take a while");
		List<String> tracknames = MarioKart.plugin.trackManager.getRaceTrackNames();
		
			
		for(String trackname:tracknames) {
			ConcurrentHashMap<String,Double> times = MarioKart.plugin.raceTimes.getTimes(trackname);
			for(String key:times.keySet()) {
				OfflinePlayer player = null;
				for(OfflinePlayer p:Bukkit.getOfflinePlayers()) {
					if(p.getName().equals(key)) {
						player = p;
					}
				}
				
				sqlManager.setInTable(SQL_TIME_TABLE, Arrays.asList(new String[]{SQL_UUID_KEY, SQL_NAME_KEY, SQL_TRACK_KEY, SQL_TIME_KEY, SQL_DATE_KEY}),
						Arrays.asList(new Object[]{player.getUniqueId(), player.getName(), trackname, (double) times.get(key), new Date(new java.util.Date().getTime())}));
			}
		}
	}
	
	private void createTables() {
		sqlManager.createTable(SQL_WIN_TABLE, new String[] {
				SQL_KEY, SQL_UUID_KEY, SQL_NAME_KEY, SQL_TRACK_KEY, SQL_WIN_KEY, SQL_DATE_KEY }, new String[] {
				"MEDIUMINT NOT NULL PRIMARY KEY AUTO_INCREMENT", "VARCHAR(255)" , "VARCHAR(255)" , "VARCHAR(255)" , "INTEGER", "DATE" });
		sqlManager.createTable(SQL_TIME_TABLE, new String[] {
				SQL_KEY, SQL_UUID_KEY, SQL_NAME_KEY, SQL_TRACK_KEY, SQL_TIME_KEY, SQL_DATE_KEY }, new String[] {
				"MEDIUMINT NOT NULL PRIMARY KEY AUTO_INCREMENT", "VARCHAR(255)" , "VARCHAR(255)" , "VARCHAR(255)" , "DOUBLE", "DATE" });
	}
}
