package org.stormdev.mkstormapi.SQL;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import net.stormdev.mario.mariokart.MarioKart;

/**
 * 
 * Amazing class for dealing with SQL tasks programmaticaly
 *
 */
public class SQLManager {
	private Connection c;
	private MySQL sqlConnection;
	private Plugin plugin;
	
	/**
	 * Create an SQL handler for a database
	 * @param sqlConnection The database
	 * @param plugin Your plugin
	 */
	public SQLManager(MySQL sqlConnection, Plugin plugin){
	        this.plugin = plugin;
		c = sqlConnection.getConnection();
		try {
			if (c == null)
				throw new Exception("SQL is not connected!");
			
			this.sqlConnection = sqlConnection;
			c.setAutoCommit(true);
		} catch (Exception e) {
			plugin.getLogger().info("Error connecting to SQL database!");
		}
	}
	/**
	 * Close the connection to the database
	 */
	public void closeConnection(){
		try {
			c.close();
		} catch (Exception e) {
			plugin.getLogger().info("Error connecting to SQL database!");
		}
	}
	private boolean checkConnectionStatus(){
		try {
			if(!c.isValid(1000)){
				closeConnection();
				sqlConnection.connect(); //Reconnect
				c = sqlConnection.getConnection();
				try {
					c.setAutoCommit(true);
				} catch (Exception e) {
					plugin.getLogger().info("Error connecting to SQL database!");
				}
				if(!c.isValid(1000)){
					return false;
				}
				plugin.getLogger().info("Successfully re-established connection with the SQL server!");
			}
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	/**
	 * Check if connected to the database
	 */
	public void checkConnection(){
		if(!checkConnectionStatus()){
			plugin.getLogger().info("Lost connection to the SQL database! Is it offline?");
		}
	}
	
	/**
	 * Search the table for the value in a column. <br>
	 * Eg. in a table of: <br>
	 * |ID|Money| <br>
	 * |storm|30k| <br>
	 * |bjarn|2k| <br>
	 * doing searchTable("tablename", "ID", "storm", "Money") Would return '30k'
	 * 
	 * @param tableName The table to search
	 * @param keyName The name of the SQL table's PRIMARY key
	 * @param keyValue The value of the SQL table's PRIMARY key to search for the row of
	 * @param valueName The column name you want the value of
	 * @return The value of the found cell, or null if not found
	 * @throws SQLException SQL error
	 */
	public synchronized Object searchTable(String tableName, String keyName, String keyValue, String valueName) throws SQLException{
		checkConnection();
		Statement statement = c.createStatement();
		ResultSet res = statement.executeQuery("SELECT * FROM "+tableName+" WHERE "+keyName+" = '" + keyValue + "';");
		res.next();
		Object found;
		try {
			found = null;
			if(res.getString(keyName) != null) {
				found = res.getObject(valueName);
			}
		} catch (Exception e) {
			found = null;
		}
		res.close();
		statement.close();
		return found;
	}
	
	/**
	 * Search the table for the value in a column
	 * that meets two conditions
	 * 
	 * @param tableName
	 * @param key1Name
	 * @param key1Value
	 * @param key2Name
	 * @param key2Value
	 * @param valueName
	 * @return
	 * @throws SQLException
	 */
	public synchronized Object searchTable(String tableName, String key1Name, String key1Value, String key2Name, String key2Value, String valueName) throws SQLException {
		checkConnection();
		Statement statement = c.createStatement();
		ResultSet res = statement.executeQuery("SELECT * FROM "+tableName+" WHERE "+key1Name+" = '" + key1Value + "' AND " +key2Name+ " = '" + key2Value + "';");
		res.next();
		Object found;
		try {
			found = null;
			if(res.getString(key1Name) != null) {
				found = res.getObject(valueName);
			}
		} catch (Exception e) {
			found = null;
		}
		res.close();
		statement.close();
		return found;
	}
	
	/**
	 * Execute an SQL statement on the database
	 * 
	 * @param statement The statement to execute
	 * @throws SQLException An SQL error
	 */
	public synchronized void exec(String statement) throws SQLException{
		checkConnection();
		PreparedStatement placeStatement = c.prepareStatement(statement);
		placeStatement.executeUpdate();
		placeStatement.close();
	}
	/**
	 * Set a cell value in an SQL table
	 * 
	 * @param tableName The table to modify
	 * @param keyName The table's PRIMARY KEY
	 * @param keyValue The table's PRIMARY KEY's value to change the row of
	 * @param valueName The name of the column to edit the value for
	 * @param value The value of the cell to edit
	 * @return True if edited, false if not
	 * @throws SQLException SQL error
	 */
	public synchronized Boolean setInTable(String tableName, String keyName, String keyValue, String valueName, Object value) throws SQLException{
		checkConnection();
		String replace = "INSERT INTO "+tableName+" (`"+keyName+"`, `"+valueName+"`) VALUES (?, ?)"
				+ " ON DUPLICATE KEY UPDATE "+valueName+" = ?;";
		PreparedStatement placeStatement = c.prepareStatement(replace);
		placeStatement.setString(1, keyValue);
		placeStatement.setString(2, value+"");
		placeStatement.setString(3, value+"");
		placeStatement.executeUpdate();
		placeStatement.close();
		
		/*
		String replace = "REPLACE INTO "+tableName+" (`"+keyName+"`, `"+valueName+"`) VALUES (?, ?);";
		PreparedStatement placeStatement = c.prepareStatement(replace);
		placeStatement.setString(1, keyValue);
		placeStatement.setString(2, value+"");
		placeStatement.executeUpdate();
		placeStatement.close();
		*/
		return true;
	}
	
	/**
	 * Set's a list of keys and values into the table
	 * 
	 * @param tableName
	 * @param keys
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	public synchronized Boolean setInTable(String tableName, List<String> keys, List<Object> values) throws SQLException{
		checkConnection();
		String set = "INSERT INTO "+tableName+" (`";
		
		boolean first = true;
		for(String key:keys) {
			if(first) {
				first = false;
				set += key;
			} else {
				set += "`, `" + key;
			}
		}
		set+="`) VALUES ('";
		first = true;
		for(Object val:values) {
			if(first) {
				first = false;
				set += val;
			} else {
				set += "', '" + val;
			}
		}
		set+="');";
		
		PreparedStatement placeStatement = c.prepareStatement(set);
		try {
			placeStatement.executeUpdate();
		} catch(SQLException e) {
			plugin.getLogger().info("Error in query:");
			plugin.getLogger().info("Query: "+set);
			plugin.getLogger().info("Error: "+e.getMessage());
		}
		placeStatement.close();
		return true;
	}
	
	/**
	 * Creates a table, if not already existing, of the desired specification.
	 * 
	 * @param tableName The table's name
	 * @param columns The columns of the table's names
	 * @param types The types of the columns
	 */
	public synchronized void createTable(String tableName, String[] columns, String[] types){
		checkConnection();
		String query = null;
		try {
			query = "CREATE TABLE IF NOT EXISTS "+tableName+"(";
			Boolean first = true;
			for(int i=0;i<columns.length&&i<types.length;i++){
				if(!first){
					query = query + ", ";
				}
				else{			
					first = false;
				}
				String col = columns[i];
				String type = types[i];
				query = query + col + " " + type;
			}
			query = query + ");";
			//Query is assembles
			Statement statement = c.createStatement();
			statement.executeUpdate(query);
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().info("Error in query:");
			plugin.getLogger().info("Query: "+query);
			plugin.getLogger().info("Error: "+e.getMessage());
		}
	}
	/**
	 * Removes a table.
	 * 
	 * @param tableName The table's name
	 * @param columns The columns of the table's names
	 * @param types The types of the columns
	 */
	public synchronized void deleteTable(String tableName){
		checkConnection();
		String query = null;
		try {
			query = "DROP TABLE "+tableName+";";
			Statement statement = c.createStatement();
			statement.executeUpdate(query);
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().info("Error in query:");
			plugin.getLogger().info("Query: "+query);
			plugin.getLogger().info("Error: "+e.getMessage());
		}
	}
	
	/**
	 * Remove a row from the table
	 * 
	 * @param tableName The table name
	 * @param keyName The name of the table's PRIMARY KEY
	 * @param keyValue The value of the table's PRIMARY KEY at desired row
	 * @throws SQLException SQL Error
	 */
	public synchronized void deleteFromTable(String tableName, String keyName, String keyValue) throws SQLException{
		checkConnection();
		String query = "DELETE FROM "+tableName+" WHERE "+tableName+"."+keyName+"=?;";
		PreparedStatement placeStatement = c.prepareStatement(query);
		//placeStatement.setString(1, tableName);
		//placeStatement.setString(2, keyName);
		placeStatement.setString(1, keyValue);
		placeStatement.executeUpdate();
		placeStatement.close();
	}
	
	/**
	 * Gets a row from the table
	 * 
	 * @param tableName The table to get the row from
	 * @param keyName The SQL table's PRIMARY KEY name
	 * @param keyValue The SQL table's PRIMARY KEY value at the desired row
	 * @param columns The columns you want to return the values for at that row
	 * @return A map containing the column names and their values
	 * @throws SQLException SQL Error
	 */
	public synchronized List<Map<Object, Object>> getRows(String tableName, String keyName, String keyValue, String... columns) throws SQLException{
		checkConnection();
		String query = "SELECT * FROM "+tableName+" WHERE ?=?;";
		PreparedStatement placeStatement = c.prepareStatement(query);
		placeStatement.setString(1, keyName);
		placeStatement.setString(2, keyValue);
		ResultSet res = placeStatement.executeQuery();
		List<Map<Object, Object>> list = new ArrayList<Map<Object, Object>>();
		while(res.next()){
			try {
				Map<Object, Object> obs = new HashMap<Object, Object>();
				for(String col:columns){
					try {
						Object o = res.getObject(col);
						obs.put(col, o);
					} catch (Exception e) {
						continue;
					}
				}
				list.add(obs);
			} catch (Exception e) {
				continue;
			}
		}
		res.close();
		placeStatement.close();
		return list;
	}
	
	/**
	 * Get all values of a column in a table
	 * 
	 * @param tableName The table
	 * @param column The column name
	 * @return A list of the values
	 * @throws SQLException SQL Error
	 */
	public synchronized List<Object> getColumn(String tableName, String column) throws SQLException{
		checkConnection();
		Statement statement = c.createStatement();
		ResultSet res = statement.executeQuery("SELECT * FROM "+tableName+";");
		List<Object> list = new ArrayList<Object>();
		while(res.next()){
			try {
				Object o = res.getObject(column);
				list.add(o);
			} catch (Exception e) {
				continue; //Error reading object
			}
		}
		res.close();
		statement.close();
		return list;
	}
	
	/**
	 * Check if a table has the specified column
	 * 
	 * @param tableName
	 * @param column
	 * @return
	 * @throws SQLException
	 */
	
	public synchronized boolean hasColumn(String tableName, String column) throws SQLException {
		checkConnection();
		DatabaseMetaData md = c.getMetaData();
		ResultSet rs = md.getColumns(null, null, tableName, column);
		if(rs.next()) {
			return true;
		}
		return false;
	}
	
	/**
	 * Get's an entire table
	 * 
	 * @param tableName
	 * @param columns
	 * @return
	 * @throws SQLException
	 */
	public synchronized List<Map<Object, Object>> getAll(String tableName, List<String> columns) throws SQLException{
		checkConnection();
		String query = "SELECT * FROM "+tableName+";";
		PreparedStatement placeStatement = c.prepareStatement(query);
		ResultSet res = placeStatement.executeQuery();
		List<Map<Object, Object>> list = new ArrayList<Map<Object, Object>>();
		while(res.next()){
			try {
				Map<Object, Object> obs = new HashMap<Object, Object>();
				for(String col:columns){
					try {
						Object o = res.getObject(col);
						obs.put(col, o);
					} catch (Exception e) {
						continue;
					}
				}
				list.add(obs);
			} catch (Exception e) {
				plugin.getLogger().info("Error in query:");
				plugin.getLogger().info("Query: "+query);
				plugin.getLogger().info("Error: "+e.getMessage());
			}
		}
		res.close();
		placeStatement.close();
		return list;
	}
	
	/**
	 * Checks if a table exists
	 * 
	 * @param tableName
	 * @return
	 * @throws SQLException 
	 */
	public synchronized boolean tableExists(String tableName) throws SQLException {
		checkConnection();
		DatabaseMetaData meta = c.getMetaData();
	    ResultSet resultSet = meta.getTables(null, null, tableName, new String[] {"TABLE"});

	    return resultSet.next();
	}
	
	public MySQL getSQL() {
		return sqlConnection;
	}
}