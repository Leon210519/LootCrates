package com.lootcrates.manager;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.data.PlayerData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    
    private final LootCratesPlugin plugin;
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    
    public PlayerDataManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }
    
    public PlayerData getPlayerData(UUID uuid, String username) {
        return playerDataCache.computeIfAbsent(uuid, k -> loadPlayerData(uuid, username));
    }
    
    private PlayerData loadPlayerData(UUID uuid, String username) {
        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM lc_player_data WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PlayerData data = new PlayerData(uuid, username);
                        data.setTotalOpens(rs.getInt("total_opens"));
                        data.setMoneyEarned(rs.getDouble("money_earned"));
                        data.setItemsReceived(rs.getInt("items_received"));
                        data.setRareFinds(rs.getInt("rare_finds"));
                        data.setLastOpen(rs.getLong("last_open"));
                        data.setDataCreated(rs.getLong("data_created"));
                        data.setDataUpdated(rs.getLong("data_updated"));
                        return data;
                    } else {
                        // Create new player data
                        PlayerData newData = new PlayerData(uuid, username);
                        savePlayerData(newData);
                        return newData;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
            return new PlayerData(uuid, username); // Return empty data as fallback
        }
    }
    
    public void savePlayerData(PlayerData data) {
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = """
                INSERT INTO lc_player_data 
                (uuid, username, total_opens, money_earned, items_received, rare_finds, last_open, data_created, data_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                total_opens = VALUES(total_opens),
                money_earned = VALUES(money_earned),
                items_received = VALUES(items_received),
                rare_finds = VALUES(rare_finds),
                last_open = VALUES(last_open),
                data_updated = VALUES(data_updated)
            """;
            
            // For SQLite, use INSERT OR REPLACE
            if (plugin.getDatabaseManager().getDatabaseType() == 
                com.lootcrates.database.DatabaseManager.DatabaseType.SQLITE) {
                sql = """
                    INSERT OR REPLACE INTO lc_player_data 
                    (uuid, username, total_opens, money_earned, items_received, rare_finds, last_open, data_created, data_updated)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, data.getUuid().toString());
                stmt.setString(2, data.getUsername());
                stmt.setInt(3, data.getTotalOpens());
                stmt.setDouble(4, data.getMoneyEarned());
                stmt.setInt(5, data.getItemsReceived());
                stmt.setInt(6, data.getRareFinds());
                stmt.setLong(7, data.getLastOpen());
                stmt.setLong(8, data.getDataCreated());
                stmt.setLong(9, System.currentTimeMillis());
                
                stmt.executeUpdate();
            }
        });
    }
    
    public void saveAllData() {
        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }
    }
    
    public void unloadPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.remove(uuid);
        if (data != null) {
            savePlayerData(data);
        }
    }
    
    public void updatePlayerOpening(Player player, String crateId, double moneyReward, int itemsReward, boolean wasRare) {
        PlayerData data = getPlayerData(player);
        data.setTotalOpens(data.getTotalOpens() + 1);
        data.setMoneyEarned(data.getMoneyEarned() + moneyReward);
        data.setItemsReceived(data.getItemsReceived() + itemsReward);
        if (wasRare) {
            data.setRareFinds(data.getRareFinds() + 1);
        }
        data.setLastOpen(System.currentTimeMillis());
        
        savePlayerData(data);
    }
}