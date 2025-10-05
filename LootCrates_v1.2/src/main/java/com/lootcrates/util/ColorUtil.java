package com.lootcrates.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public static String colorize(String message) {
        if (message == null) return "";
        
        // Handle hex colors first
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString();
            message = message.replace("&#" + hexCode, replacement);
        }
        
        // Handle legacy color codes
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColors(String message) {
        if (message == null) return "";
        
        // Remove hex colors
        message = HEX_PATTERN.matcher(message).replaceAll("");
        
        // Remove legacy colors
        return org.bukkit.ChatColor.stripColor(message);
    }
    
    public static String rainbow(String text) {
        StringBuilder result = new StringBuilder();
        org.bukkit.ChatColor[] colors = {
            org.bukkit.ChatColor.RED,
            org.bukkit.ChatColor.GOLD,
            org.bukkit.ChatColor.YELLOW,
            org.bukkit.ChatColor.GREEN,
            org.bukkit.ChatColor.AQUA,
            org.bukkit.ChatColor.BLUE,
            org.bukkit.ChatColor.LIGHT_PURPLE
        };
        
        int colorIndex = 0;
        for (char c : text.toCharArray()) {
            if (c != ' ') {
                result.append(colors[colorIndex % colors.length]);
                colorIndex++;
            }
            result.append(c);
        }
        
        return result.toString();
    }
}