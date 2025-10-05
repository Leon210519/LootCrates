package com.lootcrates.util;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    
    private final LootCratesPlugin plugin;
    private FileConfiguration messages;
    private final Map<String, String> cachedMessages = new HashMap<>();
    
    public MessageManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        cacheMessages();
        plugin.getLogger().info("Loaded " + cachedMessages.size() + " messages.");
    }
    
    private void cacheMessages() {
        cachedMessages.clear();
        cacheSection("", messages);
    }
    
    private void cacheSection(String prefix, org.bukkit.configuration.ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                cacheSection(fullKey, section.getConfigurationSection(key));
            } else if (section.isString(key)) {
                cachedMessages.put(fullKey, section.getString(key));
            }
        }
    }
    
    public String getMessage(String key) {
        return cachedMessages.getOrDefault(key, "§cMessage not found: " + key);
    }
    
    public String getMessage(String key, Placeholder... placeholders) {
        String message = getMessage(key);
        for (Placeholder placeholder : placeholders) {
            message = message.replace(placeholder.getKey(), placeholder.getValue());
        }
        return ColorUtil.colorize(message);
    }
    
    public void sendMessage(Player player, String key, Placeholder... placeholders) {
        String message = getMessage(key, placeholders);
        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }
    }
    
    public void sendMessageWithPrefix(Player player, String key, Placeholder... placeholders) {
        String prefix = getMessage("general.prefix");
        String message = getMessage(key, placeholders);
        if (!message.trim().isEmpty()) {
            player.sendMessage(ColorUtil.colorize(prefix + message));
        }
    }
    
    public void reloadMessages() {
        loadMessages();
    }
    
    public static class Placeholder {
        private final String key;
        private final String value;
        
        public Placeholder(String key, String value) {
            this.key = "{" + key + "}";
            this.value = value != null ? value : "";
        }
        
        public String getKey() { return key; }
        public String getValue() { return value; }
        
        public static Placeholder of(String key, Object value) {
            return new Placeholder(key, String.valueOf(value));
        }
    }
}