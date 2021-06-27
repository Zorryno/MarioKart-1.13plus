package net.stormdev.mario.utils;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.stormdev.mkstormapi.SQL.MySQL;
import org.stormdev.mkstormapi.SQL.SQLManager;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.uuidapi.PlayerIDFinder;

public class WinnerSQLManager {
	private SQLManager sqlManager = null;
	private boolean sql = true;
	
	private static final String SQL_TABLE = "WinList";
	private static final String SQL_KEY = "playerid,trackname";
	private static final String SQL_VAL_KEY = "wins";
	
	public WinnerSQLManager() {		
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
				sqlManager.createTable(SQL_TABLE, new String[] {
						SQL_KEY, SQL_VAL_KEY }, new String[] {
						"varchar(255) NOT NULL PRIMARY KEY", "integer" });
			}
		} catch (Exception e) {
			sql = false;
		}
	}
	
	public void giveWin(String trackname, Player player) {
		String id = PlayerIDFinder.getMojangID(player).getID() + "," + trackname;
		
		Bukkit.getScheduler().runTaskAsynchronously(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				try {
					Object o = sqlManager.searchTable(SQL_TABLE, SQL_KEY, id, SQL_VAL_KEY);
					if(o == null){
						sqlManager.setInTable(SQL_TABLE, SQL_KEY, id, SQL_VAL_KEY, 1);
					}
					int wins = (int) o;
					wins++;
					
					sqlManager.setInTable(SQL_TABLE, SQL_KEY, id, SQL_VAL_KEY, wins);
				} catch (SQLException e) {
					//BUGZ
					e.printStackTrace();
				}
				return;
			}});
	}
	
	public boolean isActive() {
		return sql;
	}
}
