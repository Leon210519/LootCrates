package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.crate.Reward;
import com.lootcrates.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CrateGUI {
    
    public static void openPreview(Player player, Crate crate) {
        String title = ColorUtil.colorize("&8Preview: " + crate.getDisplay());
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        int slot = 0;
        for (Reward reward : crate.getRewards()) {
            if (slot >= 54) break;
            
            ItemStack displayItem = reward.getDisplay().clone();
            ItemMeta meta = displayItem.getItemMeta();
            
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                
                // Add preview information
                lore.add("");
                lore.add(ColorUtil.colorize("&7Weight: &f" + reward.getWeight()));
                
                String tier = reward.getTier();
                if (tier != null) {
                    String tierColor = getTierColor(tier);
                    lore.add(ColorUtil.colorize("&7Tier: " + tierColor + tier.substring(0, 1).toUpperCase() + tier.substring(1).toLowerCase()));
                }
                
                // Calculate percentage
                int totalWeight = crate.getRewards().stream().mapToInt(Reward::getWeight).sum();
                double percentage = totalWeight > 0 ? (double) reward.getWeight() / totalWeight * 100 : 0;
                lore.add(ColorUtil.colorize("&7Chance: &f" + String.format("%.2f%%", percentage)));
                
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            
            inventory.setItem(slot, displayItem);
            slot++;
        }
        
        player.openInventory(inventory);
    }
    
    public static void openStats(Player player, Player target) {
        String title = ColorUtil.colorize("&6&l" + target.getName() + "'s Stats");
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        var playerData = LootCratesPlugin.getInstance().getPlayerDataManager().getPlayerData(target);
        
        // Total opens
        inventory.setItem(10, createStatItem(Material.CHEST, 
            "&6Total Crate Opens", 
            String.valueOf(playerData.getTotalOpens())));
        
        // Money earned
        inventory.setItem(12, createStatItem(Material.GOLD_INGOT, 
            "&6Money Earned", 
            "$" + String.format("%.2f", playerData.getMoneyEarned())));
        
        // Items received
        inventory.setItem(14, createStatItem(Material.DIAMOND, 
            "&6Items Received", 
            String.valueOf(playerData.getItemsReceived())));
        
        // Rare finds
        inventory.setItem(16, createStatItem(Material.EMERALD, 
            "&6Rare Finds", 
            String.valueOf(playerData.getRareFinds())));
        
        player.openInventory(inventory);
    }
    
    public static void openShop(Player player) {
        String title = ColorUtil.colorize(LootCratesPlugin.getInstance().getConfig().getString("shop.gui_title", "&6&lCrate Key Shop"));
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        // Implementation for shop items would go here
        // This would read from config.yml shop section
        
        player.openInventory(inventory);
    }
    
    public static void openHistory(Player player, Player target) {
        String title = ColorUtil.colorize("&6&l" + target.getName() + "'s History");
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        // Implementation for opening history would go here
        // This would read from database
        
        player.openInventory(inventory);
    }
    
    private static ItemStack createStatItem(Material material, String name, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&f" + value));
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private static String getTierColor(String tier) {
        if (tier == null) return "&7";
        
        return switch (tier.toLowerCase()) {
            case "common" -> "&7";
            case "uncommon" -> "&a";
            case "rare" -> "&9";
            case "epic" -> "&5";
            case "legendary" -> "&6";
            case "mythic" -> "&c";
            default -> "&7";
        };
    }
}