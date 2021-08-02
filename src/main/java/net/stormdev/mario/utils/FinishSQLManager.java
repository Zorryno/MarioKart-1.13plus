package net.stormdev.mario.utils;

import java.sql.Date;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.stormdev.mkstormapi.SQL.MySQL;
import org.stormdev.mkstormapi.SQL.SQLManager;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.uuidapi.PlayerIDFinder;

public class FinishSQLManager {
	private SQLManager sqlManager = null;
	private boolean sql = true;
	
	private static final String SQL_WIN_TABLE = "WinList";
	private static final String SQL_TIME_TABLE = "TimeList";
	
	private static final String SQL_KEY = "playerIdWithTrackname";
	
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
			if (sql) { 
				sqlManager.createTable(SQL_WIN_TABLE, new String[] {
						SQL_KEY, SQL_WIN_KEY, SQL_DATE_KEY }, new String[] {
						"VARCHAR(255) NOT NULL PRIMARY KEY", "INTEGER", "DATE" });
				sqlManager.createTable(SQL_TIME_TABLE, new String[] {
						SQL_KEY, SQL_TIME_KEY, SQL_DATE_KEY }, new String[] {
						"VARCHAR(255) NOT NULL PRIMARY KEY", "DOUBLE", "DATE" });
				
				updateColumns();
			}
		} catch (Exception e) {
			sql = false;
		}
	}
	
	public void giveWin(String trackname, Player player) {		
		Bukkit.getScheduler().runTaskAsynchronously(MarioKart.plugin, () -> {
			try {
				String id = PlayerIDFinder.getMojangID(player).getID() + "|" + trackname;
				Object o = sqlManager.searchTable(SQL_WIN_TABLE, SQL_KEY, id, SQL_WIN_KEY);
				if(o == null){
					sqlManager.setInTable(SQL_WIN_TABLE, SQL_KEY, id, SQL_WIN_KEY, 1);
				} else {
					int wins = (int) o;
					wins++;
					
					sqlManager.setInTable(SQL_WIN_TABLE, SQL_KEY, id, SQL_WIN_KEY, wins);
				}
				sqlManager.setInTable(SQL_WIN_TABLE, SQL_KEY, id, SQL_DATE_KEY, new Date(new java.util.Date().getTime()));
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
				String id = PlayerIDFinder.getMojangID(player).getID() + "|" + trackname;
				Object o = sqlManager.searchTable(SQL_TIME_TABLE, SQL_KEY, id, SQL_TIME_KEY);
				if(o == null || time < (double) o) {
					sqlManager.setInTable(SQL_TIME_TABLE, SQL_KEY, id, SQL_TIME_KEY, time);
					sqlManager.setInTable(SQL_TIME_TABLE, SQL_KEY, id, SQL_DATE_KEY, new Date(new java.util.Date().getTime()));
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
	
	private void updateColumns() throws SQLException {
		//Add date-column to winlist
		if(!sqlManager.hasColumn(SQL_WIN_TABLE, SQL_DATE_KEY)) {
			String sql = "ALTER TABLE " + SQL_WIN_TABLE
				+ " ADD COLUMN "+ SQL_DATE_KEY + " DATE";
			sqlManager.executeStatement(sql);
		}
		
		//Add date-column to timeslist
		if(!sqlManager.hasColumn(SQL_TIME_TABLE, SQL_DATE_KEY)) {
			String sql = "ALTER TABLE " + SQL_TIME_TABLE
				+ " ADD COLUMN "+ SQL_DATE_KEY + " DATE";
			sqlManager.executeStatement(sql);
		}
	}
}
