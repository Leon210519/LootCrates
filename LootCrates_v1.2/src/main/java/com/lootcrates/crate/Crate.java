package com.lootcrates.crate;

import com.lootcrates.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Crate {
    
    private final String id;
    private final String display;
    private final String tier;
    private final KeyDef key;
    private final OpenMethod openMethod;
    private final List<Reward> rewards = new ArrayList<>();
    
    // Restrictions
    private final boolean enabled;
    private final long cooldown; // seconds
    private final int dailyLimit;
    private final String requiredPermission;
    
    // Pity protection
    private final boolean pityEnabled;
    private final int pityThreshold;
    private final double rareWeightMultiplier;
    
    // Animation settings
    private final boolean animationEnabled;
    private final int animationSpeed;
    private final int animationDuration;
    private final AnimationStyle animationStyle;
    
    // Location settings
    private final List<Location> locations = new ArrayList<>();
    
    // Hologram settings
    private final boolean hologramEnabled;
    private final double hologramHeight;
    private final List<String> hologramLines = new ArrayList<>();
    
    // Time restrictions
    private final LocalDateTime availableFrom;
    private final LocalDateTime availableUntil;
    
    public enum OpenMethod {
        GUI, BLOCK, BOTH
    }
    
    public enum AnimationStyle {
        ROULETTE, EXPANDING, CASCADE
    }

    public Crate(String id, String display, String tier, KeyDef key, OpenMethod openMethod,
                boolean enabled, long cooldown, int dailyLimit, String requiredPermission,
                boolean pityEnabled, int pityThreshold, double rareWeightMultiplier,
                boolean animationEnabled, int animationSpeed, int animationDuration, AnimationStyle animationStyle,
                boolean hologramEnabled, double hologramHeight, List<String> hologramLines,
                LocalDateTime availableFrom, LocalDateTime availableUntil) {
        this.id = id;
        this.display = display;
        this.tier = tier;
        this.key = key;
        this.openMethod = openMethod;
        this.enabled = enabled;
        this.cooldown = cooldown;
        this.dailyLimit = dailyLimit;
        this.requiredPermission = requiredPermission;
        this.pityEnabled = pityEnabled;
        this.pityThreshold = pityThreshold;
        this.rareWeightMultiplier = rareWeightMultiplier;
        this.animationEnabled = animationEnabled;
        this.animationSpeed = animationSpeed;
        this.animationDuration = animationDuration;
        this.animationStyle = animationStyle;
        this.hologramEnabled = hologramEnabled;
        this.hologramHeight = hologramHeight;
        this.hologramLines.addAll(hologramLines);
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
    }

    public static class KeyDef {
        private final String display;
        private final Material material;
        private final Integer customModelData;
        private final List<String> lore;
        private final boolean glow;

        public KeyDef(String display, Material material, Integer customModelData, List<String> lore, boolean glow) {
            this.display = display;
            this.material = material;
            this.customModelData = customModelData;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.glow = glow;
        }

        public ItemStack createItem(int amount) {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.colorize(display));
                if (customModelData != null) {
                    meta.setCustomModelData(customModelData);
                }
                if (!lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(ColorUtil.colorize(line));
                    }
                    meta.setLore(coloredLore);
                }
                if (glow) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }
            return item;
        }
        
        // Getters
        public String getDisplay() { return display; }
        public Material getMaterial() { return material; }
        public Integer getCustomModelData() { return customModelData; }
        public List<String> getLore() { return lore; }
        public boolean isGlow() { return glow; }
    }

    @SuppressWarnings("unchecked")
    public static Crate fromConfig(String id, ConfigurationSection sec) {
        String display = sec.getString("display", id);
        String tier = sec.getString("tier", "common");
        boolean enabled = sec.getBoolean("enabled", true);
        
        // Key configuration
        ConfigurationSection keySection = sec.getConfigurationSection("key");
        String keyDisplay = keySection.getString("display", "&eKey");
        Material keyMaterial = Material.matchMaterial(keySection.getString("material", "TRIPWIRE_HOOK"));
        Integer customModelData = keySection.isInt("custom_model_data") ? keySection.getInt("custom_model_data") : null;
        List<String> keyLore = keySection.getStringList("lore");
        boolean keyGlow = keySection.getBoolean("glow", false);
        
        KeyDef keyDef = new KeyDef(keyDisplay, keyMaterial, customModelData, keyLore, keyGlow);
        
        // Open method
        String methodString = sec.getString("open_method", "BLOCK");
        OpenMethod openMethod = OpenMethod.valueOf(methodString.toUpperCase());
        
        // Restrictions
        long cooldown = sec.getLong("cooldown", 0);
        int dailyLimit = sec.getInt("daily_limit", -1);
        String requiredPermission = sec.getString("required_permission");
        
        // Pity protection
        ConfigurationSection pitySection = sec.getConfigurationSection("pity");
        boolean pityEnabled = pitySection != null && pitySection.getBoolean("enabled", false);
        int pityThreshold = pitySection != null ? pitySection.getInt("threshold", 10) : 10;
        double rareWeightMultiplier = pitySection != null ? pitySection.getDouble("rare_weight_multiplier", 2.0) : 2.0;
        
        // Animation settings
        ConfigurationSection animSection = sec.getConfigurationSection("animation");
        boolean animationEnabled = animSection != null && animSection.getBoolean("enabled", true);
        int animationSpeed = animSection != null ? animSection.getInt("speed", 2) : 2;
        int animationDuration = animSection != null ? animSection.getInt("duration", 120) : 120;
        String styleString = animSection != null ? animSection.getString("style", "ROULETTE") : "ROULETTE";
        AnimationStyle animationStyle = AnimationStyle.valueOf(styleString.toUpperCase());
        
        // Hologram settings
        ConfigurationSection holoSection = sec.getConfigurationSection("hologram");
        boolean hologramEnabled = holoSection != null && holoSection.getBoolean("enabled", false);
        double hologramHeight = holoSection != null ? holoSection.getDouble("height_offset", 2.0) : 2.0;
        List<String> hologramLines = holoSection != null ? holoSection.getStringList("lines") : new ArrayList<>();
        
        // Time restrictions
        LocalDateTime availableFrom = null;
        LocalDateTime availableUntil = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        if (sec.isString("available_from")) {
            try {
                availableFrom = LocalDateTime.parse(sec.getString("available_from"), formatter);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Invalid available_from date format for crate " + id);
            }
        }
        
        if (sec.isString("available_until")) {
            try {
                availableUntil = LocalDateTime.parse(sec.getString("available_until"), formatter);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Invalid available_until date format for crate " + id);
            }
        }
        
        Crate crate = new Crate(id.toUpperCase(Locale.ROOT), display, tier, keyDef, openMethod,
            enabled, cooldown, dailyLimit, requiredPermission,
            pityEnabled, pityThreshold, rareWeightMultiplier,
            animationEnabled, animationSpeed, animationDuration, animationStyle,
            hologramEnabled, hologramHeight, hologramLines,
            availableFrom, availableUntil);
        
        // Load locations
        if (sec.isList("locations")) {
            for (Map<?, ?> locationMap : sec.getMapList("locations")) {
                try {
                    String worldName = (String) locationMap.get("world");
                    int x = (Integer) locationMap.get("x");
                    int y = (Integer) locationMap.get("y");
                    int z = (Integer) locationMap.get("z");
                    
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        crate.locations.add(new Location(world, x, y, z));
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Invalid location configuration for crate " + id);
                }
            }
        }
        
        // Load rewards
        if (sec.isList("rewards")) {
            List<Map<?, ?>> list = sec.getMapList("rewards");
            for (Map<?, ?> map : list) {
                try {
                    MemoryConfiguration tmp = new MemoryConfiguration();
                    ConfigurationSection rewardSection = tmp.createSection("reward", map);
                    crate.rewards.add(Reward.fromConfig(rewardSection));
                } catch (Exception ex) {
                    Bukkit.getLogger().warning("Failed to load reward in crate " + id + ": " + ex.getMessage());
                }
            }
        } else if (sec.isConfigurationSection("rewards")) {
            ConfigurationSection rewardsSection = sec.getConfigurationSection("rewards");
            for (String rewardKey : rewardsSection.getKeys(false)) {
                try {
                    crate.rewards.add(Reward.fromConfig(rewardsSection.getConfigurationSection(rewardKey)));
                } catch (Exception ex) {
                    Bukkit.getLogger().warning("Failed to load reward " + rewardKey + " in crate " + id + ": " + ex.getMessage());
                }
            }
        }
        
        return crate;
    }

    public Reward roll(Random rng) {
        if (rewards.isEmpty()) {
            throw new IllegalStateException("Crate " + id + " has no rewards configured!");
        }
        
        int total = rewards.stream().mapToInt(Reward::getWeight).sum();
        int pick = rng.nextInt(Math.max(total, 1)) + 1;
        int cumulative = 0;
        
        for (Reward reward : rewards) {
            cumulative += reward.getWeight();
            if (pick <= cumulative) {
                return reward;
            }
        }
        
        return rewards.get(0); // Fallback
    }
    
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        if (availableFrom != null && now.isBefore(availableFrom)) {
            return false;
        }
        
        if (availableUntil != null && now.isAfter(availableUntil)) {
            return false;
        }
        
        return true;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplay() { return display; }
    public String getTier() { return tier; }
    public KeyDef getKey() { return key; }
    public OpenMethod getOpenMethod() { return openMethod; }
    public List<Reward> getRewards() { return rewards; }
    public boolean isEnabled() { return enabled; }
    public long getCooldown() { return cooldown; }
    public int getDailyLimit() { return dailyLimit; }
    public String getRequiredPermission() { return requiredPermission; }
    public boolean isPityEnabled() { return pityEnabled; }
    public int getPityThreshold() { return pityThreshold; }
    public double getRareWeightMultiplier() { return rareWeightMultiplier; }
    public boolean isAnimationEnabled() { return animationEnabled; }
    public int getAnimationSpeed() { return animationSpeed; }
    public int getAnimationDuration() { return animationDuration; }
    public AnimationStyle getAnimationStyle() { return animationStyle; }
    public List<Location> getLocations() { return locations; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public double getHologramHeight() { return hologramHeight; }
    public List<String> getHologramLines() { return hologramLines; }
    public LocalDateTime getAvailableFrom() { return availableFrom; }
    public LocalDateTime getAvailableUntil() { return availableUntil; }
    
    // Legacy getters for compatibility
    public String display() { return display; }
    public KeyDef key() { return key; }
    public String openMethod() { return openMethod.name(); }
}
