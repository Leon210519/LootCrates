package com.lootcrates.util;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector {
    
    private final LootCratesPlugin plugin;
    private final Map<String, Integer> crateOpenCounts = new HashMap<>();
    private final Map<String, Integer> rewardCounts = new HashMap<>();
    
    public MetricsCollector(LootCratesPlugin plugin) {
        this.plugin = plugin;
        
        // Start metrics collection if bStats is available
        if (Bukkit.getPluginManager().getPlugin("bStats") != null) {
            startMetrics();
        }
    }
    
    private void startMetrics() {
        // bStats integration would go here
        // This is a placeholder for metrics collection
        plugin.getLogger().info("Metrics collection initialized.");
    }
    
    public void recordCrateOpen(String crateId) {
        crateOpenCounts.merge(crateId, 1, Integer::sum);
    }
    
    public void recordReward(String rewardType) {
        rewardCounts.merge(rewardType, 1, Integer::sum);
    }
    
    public Map<String, Integer> getCrateOpenCounts() {
        return new HashMap<>(crateOpenCounts);
    }
    
    public Map<String, Integer> getRewardCounts() {
        return new HashMap<>(rewardCounts);
    }
}