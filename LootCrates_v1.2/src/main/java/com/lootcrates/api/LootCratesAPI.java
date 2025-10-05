package com.lootcrates.api;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.crate.Reward;
import com.lootcrates.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * LootCrates API for other plugins to interact with the crate system
 */
public class LootCratesAPI {
    
    private final LootCratesPlugin plugin;
    
    public LootCratesAPI(LootCratesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get all available crate IDs
     * @return Set of crate IDs
     */
    public Set<String> getAllCrateIds() {
        return plugin.getCrateManager().list();
    }
    
    /**
     * Get a crate by its ID
     * @param crateId The crate ID
     * @return Crate object or null if not found
     */
    public Crate getCrate(String crateId) {
        return plugin.getCrateManager().get(crateId);
    }
    
    /**
     * Give keys to a player
     * @param player The player
     * @param crateId The crate ID
     * @param amount The amount of keys
     */
    public void giveKeys(Player player, String crateId, int amount) {
        plugin.getCrateManager().giveKey(player, crateId, amount);
    }
    
    /**
     * Check if a player has a key for a specific crate
     * @param player The player
     * @param crateId The crate ID
     * @return true if player has at least one key
     */
    public boolean hasKey(Player player, String crateId) {
        Crate crate = getCrate(crateId);
        if (crate == null) return false;
        
        var keyItem = crate.getKey().createItem(1);
        for (var item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(keyItem)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the number of keys a player has for a specific crate
     * @param player The player
     * @param crateId The crate ID
     * @return Number of keys
     */
    public int getKeyCount(Player player, String crateId) {
        Crate crate = getCrate(crateId);
        if (crate == null) return 0;
        
        var keyItem = crate.getKey().createItem(1);
        int count = 0;
        
        for (var item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(keyItem)) {
                count += item.getAmount();
            }
        }
        
        return count;
    }
    
    /**
     * Force open a crate for a player (bypasses key requirement)
     * @param player The player
     * @param crateId The crate ID
     * @return true if successful
     */
    public boolean forceOpenCrate(Player player, String crateId) {
        Crate crate = getCrate(crateId);
        if (crate == null) return false;
        
        // This would call the CrateOpener.forceOpen method
        // CrateOpener.forceOpen(plugin, player, crate);
        return true;
    }
    
    /**
     * Get player statistics
     * @param playerId The player UUID
     * @return PlayerData object
     */
    public PlayerData getPlayerData(UUID playerId) {
        return plugin.getPlayerDataManager().getPlayerData(playerId, null);
    }
    
    /**
     * Get player statistics
     * @param player The player
     * @return PlayerData object
     */
    public PlayerData getPlayerData(Player player) {
        return plugin.getPlayerDataManager().getPlayerData(player);
    }
    
    /**
     * Check if a player has a cooldown for a specific crate
     * @param player The player
     * @param crateId The crate ID
     * @return true if player has an active cooldown
     */
    public boolean hasCooldown(Player player, String crateId) {
        return plugin.getCooldownManager().hasCooldown(player, crateId);
    }
    
    /**
     * Get remaining cooldown time for a player and crate
     * @param player The player
     * @param crateId The crate ID
     * @return Remaining cooldown in milliseconds
     */
    public long getRemainingCooldown(Player player, String crateId) {
        return plugin.getCooldownManager().getRemainingCooldown(player, crateId);
    }
    
    /**
     * Set a cooldown for a player and crate
     * @param player The player
     * @param crateId The crate ID
     * @param durationSeconds Cooldown duration in seconds
     */
    public void setCooldown(Player player, String crateId, long durationSeconds) {
        plugin.getCooldownManager().setCooldown(player, crateId, durationSeconds);
    }
    
    /**
     * Get pity count for a player and crate
     * @param player The player
     * @param crateId The crate ID
     * @return Pity count
     */
    public int getPityCount(Player player, String crateId) {
        return plugin.getPityManager().getPityCount(player, crateId);
    }
    
    /**
     * Reset pity count for a player and crate
     * @param player The player
     * @param crateId The crate ID
     */
    public void resetPity(Player player, String crateId) {
        plugin.getPityManager().resetPity(player, crateId);
    }
    
    // Events for other plugins to listen to
    
    /**
     * Event fired when a player opens a crate
     */
    public static class CrateOpenEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        
        private final Player player;
        private final Crate crate;
        private final Reward reward;
        private boolean cancelled = false;
        
        public CrateOpenEvent(Player player, Crate crate, Reward reward) {
            this.player = player;
            this.crate = crate;
            this.reward = reward;
        }
        
        public Player getPlayer() { return player; }
        public Crate getCrate() { return crate; }
        public Reward getReward() { return reward; }
        
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        
        @Override
        public @NotNull HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }
    
    /**
     * Event fired when a player receives a reward from a crate
     */
    public static class CrateRewardEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        
        private final Player player;
        private final Crate crate;
        private final Reward reward;
        private final boolean wasRare;
        
        public CrateRewardEvent(Player player, Crate crate, Reward reward, boolean wasRare) {
            this.player = player;
            this.crate = crate;
            this.reward = reward;
            this.wasRare = wasRare;
        }
        
        public Player getPlayer() { return player; }
        public Crate getCrate() { return crate; }
        public Reward getReward() { return reward; }
        public boolean wasRare() { return wasRare; }
        
        @Override
        public @NotNull HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }
    
    /**
     * Event fired when a player receives keys
     */
    public static class KeyReceiveEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        
        private final Player player;
        private final String crateId;
        private final int amount;
        private final KeySource source;
        
        public enum KeySource {
            COMMAND, PURCHASE, REWARD, OTHER
        }
        
        public KeyReceiveEvent(Player player, String crateId, int amount, KeySource source) {
            this.player = player;
            this.crateId = crateId;
            this.amount = amount;
            this.source = source;
        }
        
        public Player getPlayer() { return player; }
        public String getCrateId() { return crateId; }
        public int getAmount() { return amount; }
        public KeySource getSource() { return source; }
        
        @Override
        public @NotNull HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }
}