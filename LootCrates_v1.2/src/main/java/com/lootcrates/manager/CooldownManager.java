package com.lootcrates.manager;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final LootCratesPlugin plugin;
    private final Map<String, Long> cooldownCache = new ConcurrentHashMap<>();
    
    public CooldownManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
        loadCooldowns();
    }
    
    private void loadCooldowns() {
        plugin.getDatabaseManager().queryAsync(connection -> {
            String sql = "SELECT player_uuid, crate_id, expires_at FROM lc_cooldowns WHERE expires_at > ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String key = rs.getString("player_uuid") + ":" + rs.getString("crate_id");
                        cooldownCache.put(key, rs.getLong("expires_at"));
                    }
                }
            }
            return null;
        });
    }
    
    public boolean hasCooldown(Player player, String crateId) {
        if (player.hasPermission("lootcrates.bypass.cooldown")) {
            return false;
        }
        
        String key = player.getUniqueId() + ":" + crateId;
        Long expiry = cooldownCache.get(key);
        
        if (expiry == null) {
            return false;
        }
        
        if (expiry <= System.currentTimeMillis()) {
            cooldownCache.remove(key);
            removeCooldownFromDatabase(player.getUniqueId(), crateId);
            return false;
        }
        
        return true;
    }
    
    public long getCooldownExpiry(Player player, String crateId) {
        String key = player.getUniqueId() + ":" + crateId;
        return cooldownCache.getOrDefault(key, 0L);
    }
    
    public long getRemainingCooldown(Player player, String crateId) {
        long expiry = getCooldownExpiry(player, crateId);
        return Math.max(0, expiry - System.currentTimeMillis());
    }
    
    public void setCooldown(Player player, String crateId, long durationSeconds) {
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        String key = player.getUniqueId() + ":" + crateId;
        
        cooldownCache.put(key, expiryTime);
        saveCooldownToDatabase(player.getUniqueId(), crateId, expiryTime);
    }
    
    public void removeCooldown(Player player, String crateId) {
        String key = player.getUniqueId() + ":" + crateId;
        cooldownCache.remove(key);
        removeCooldownFromDatabase(player.getUniqueId(), crateId);
    }
    
    private void saveCooldownToDatabase(UUID playerUuid, String crateId, long expiryTime) {
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = """
                INSERT INTO lc_cooldowns (player_uuid, crate_id, expires_at)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at)
            """;
            
            if (plugin.getDatabaseManager().getDatabaseType() == 
                com.lootcrates.database.DatabaseManager.DatabaseType.SQLITE) {
                sql = """
                    INSERT OR REPLACE INTO lc_cooldowns (player_uuid, crate_id, expires_at)
                    VALUES (?, ?, ?)
                """;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, crateId);
                stmt.setLong(3, expiryTime);
                stmt.executeUpdate();
            }
        });
    }
    
    private void removeCooldownFromDatabase(UUID playerUuid, String crateId) {
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = "DELETE FROM lc_cooldowns WHERE player_uuid = ? AND crate_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, crateId);
                stmt.executeUpdate();
            }
        });
    }
    
    public String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "0s";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", 
                days, hours % 24, minutes % 60, seconds % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", 
                hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        cooldownCache.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        
        // Also cleanup database
        plugin.getDatabaseManager().executeAsync(connection -> {
            String sql = "DELETE FROM lc_cooldowns WHERE expires_at <= ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, currentTime);
                stmt.executeUpdate();
            }
        });
    }
}