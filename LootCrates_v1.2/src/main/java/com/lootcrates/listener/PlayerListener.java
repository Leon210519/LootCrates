package com.lootcrates.listener;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.util.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final LootCratesPlugin plugin;
    
    public PlayerListener(LootCratesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        plugin.getPlayerDataManager().getPlayerData(player);
        
        // Check for offline rewards
        if (plugin.getConfig().getBoolean("settings.offline_queue.enabled", true)) {
            // Check offline queue and notify player
            // This would be implemented in QueueManager
            
            // Placeholder notification
            int offlineRewards = 0; // Get from QueueManager
            if (offlineRewards > 0) {
                plugin.getMessageManager().sendMessageWithPrefix(player, "queue.offline_rewards",
                    MessageManager.Placeholder.of("count", offlineRewards),
                    MessageManager.Placeholder.of("s", offlineRewards > 1 ? "s" : ""));
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save and unload player data
        plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());
    }
}