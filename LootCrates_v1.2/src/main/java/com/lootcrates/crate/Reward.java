package com.lootcrates.crate;

import com.lootcrates.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Reward {
    public enum Type { 
           MONEY_XP, MONEY, EXPERIENCE, ITEM, COMMAND, KEY, SPECIAL_ITEM, BUNDLE, CURRENCY,
           SPECIALITEM, SPECIALITEM_CHOICE, SPECIALITEM_SET
    }
    
    // Basic properties
    
    // Reward data
    public double money;
    public int xp;
    
    // Currency rewards
    
    // Bundle rewards
    
        // SpecialItem rewards
    
    // Display for GUI
        public final String id;
        public final int weight;
        public final Type type;
        public final String tier;
        public double amount; // Generic amount for new types
        public ItemStack item;
        public List<ItemStack> items; // For bundles and multiple items
        public int itemAmount = 1;
        public List<String> commands;
        public String keyCrate;
        public int keyAmount = 1;
        public String currencyType; // PLAYER_POINTS, TOKENS, etc.
        public String bundleName;
        public String templateId;
        public List<String> templateIds;
        public int itemLevel = 1;
        public long itemExperience = 0;
        public ItemStack display;

    public Reward(String id, int weight, Type type, String tier) { 
        this.id = id; 
        this.weight = weight; 
        this.type = type; 
        this.tier = tier;
        this.items = new ArrayList<>();
    }

    public static Reward fromConfig(ConfigurationSection sec) {
        String id = sec.getString("id", "unknown");
        int weight = sec.getInt("weight", 1);
        String typeString = sec.getString("type", "ITEM");
        String tier = sec.getString("tier", "common");
        
        Type type;
        try {
            type = Type.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Type.ITEM; // Fallback
        }
        
        Reward reward = new Reward(id, weight, type, tier);
        
        switch (type) {
            case MONEY_XP -> {
                reward.money = sec.getDouble("money", 0.0);
                reward.xp = sec.getInt("xp", 0);
            }
            case MONEY -> {
                reward.amount = sec.getDouble("amount", 0.0);
            }
            case EXPERIENCE -> {
                reward.amount = sec.getDouble("amount", 0.0);
            }
            case ITEM -> {
                if (sec.isList("items")) {
                    // Multiple items
                    for (Object itemObj : sec.getList("items")) {
                        if (itemObj instanceof ConfigurationSection itemSec) {
                            ItemStack item = readItem(itemSec);
                            if (item != null) {
                                reward.items.add(item);
                            }
                        }
                    }
                } else {
                    // Single item (legacy support)
                    reward.item = readItem(sec.getConfigurationSection("item"));
                    reward.itemAmount = sec.getInt("amount", 1);
                    if (reward.item != null) {
                        reward.item.setAmount(Math.max(1, reward.itemAmount));
                        reward.items.add(reward.item);
                    }
                }
            }
            case COMMAND -> {
                reward.commands = sec.getStringList("commands");
            }
            case KEY -> {
                ConfigurationSection keySec = sec.getConfigurationSection("key");
                if (keySec != null) {
                    reward.keyCrate = keySec.getString("crate");
                    reward.keyAmount = keySec.getInt("amount", 1);
                } else {
                    // Direct configuration
                    reward.keyCrate = sec.getString("crate");
                    reward.keyAmount = sec.getInt("amount", 1);
                }
            }
            case SPECIAL_ITEM -> {
                String templateId = sec.getString("template");
                if (templateId != null) {
                    try {
                        TemplateItems.TemplateItem template = TemplateItems.buildFrom(templateId, 
                            Configs.templates.getConfigurationSection("templates." + templateId));
                        reward.item = (template != null ? template.stack().clone() : null);
                        reward.itemAmount = sec.getInt("amount", 1);
                        if (reward.item != null) {
                            reward.item.setAmount(Math.max(1, reward.itemAmount));
                            reward.items.add(reward.item);
                        }
                    } catch (Exception e) {
                        // SpecialItems not available or template not found
                        reward.item = new ItemStack(Material.PAPER);
                        reward.items.add(reward.item);
                    }
                }
            }
            case BUNDLE -> {
                reward.bundleName = sec.getString("bundle_name", "Reward Bundle");
                
                // Load bundle items
                if (sec.isList("items")) {
                    for (Object itemObj : sec.getList("items")) {
                        if (itemObj instanceof ConfigurationSection itemSec) {
                            ItemStack item = readItem(itemSec);
                            if (item != null) {
                                reward.items.add(item);
                            }
                        }
                    }
                }
                
                // Bundle can also include money and XP
                reward.money = sec.getDouble("money", 0.0);
                reward.xp = sec.getInt("experience", 0);
            }
            case CURRENCY -> {
                reward.currencyType = sec.getString("currency_type", "PLAYER_POINTS");
                reward.amount = sec.getDouble("amount", 0.0);
            }
                case SPECIALITEM -> {
                    reward.templateId = sec.getString("template");
                    reward.itemLevel = sec.getInt("level", 1);
                    reward.itemExperience = sec.getLong("experience", 0L);
                }
                case SPECIALITEM_CHOICE -> {
                    reward.templateIds = sec.getStringList("templates");
                    reward.itemLevel = sec.getInt("level", 1);
                    reward.itemExperience = sec.getLong("experience", 0L);
                }
                case SPECIALITEM_SET -> {
                    reward.templateIds = sec.getStringList("templates");
                    reward.itemLevel = sec.getInt("level", 1);
                    reward.itemExperience = sec.getLong("experience", 0L);
                }
        }

        // Handle display item
        ConfigurationSection displaySec = sec.getConfigurationSection("display");
        if (displaySec != null) {
            reward.display = readItem(displaySec);
            if (reward.display != null) {
                reward.display.setAmount(1);
            }
        } else {
            // Generate default display
            reward.display = reward.generateDefaultDisplay();
        }

        // Ensure SPECIAL_ITEM displays retain their special enchantment lore
        if (type == Type.SPECIAL_ITEM && reward.item != null && reward.display != null) {
            reward.mergeSpecialItemLore();
        }

        return reward;
    }
    
    private ItemStack generateDefaultDisplay() {
        return switch (type) {
            case MONEY, MONEY_XP -> new ItemStack(Material.GOLD_INGOT);
            case EXPERIENCE -> new ItemStack(Material.EXPERIENCE_BOTTLE);
            case ITEM, SPECIAL_ITEM -> item != null ? item.clone() : new ItemStack(Material.CHEST);
                case SPECIALITEM, SPECIALITEM_CHOICE, SPECIALITEM_SET -> {
                    // Try to get display from SpecialItems plugin
                    try {
                        com.specialitems.SpecialItemsPlugin specialItems = com.specialitems.SpecialItemsPlugin.getInstance();
                        if (specialItems != null && templateId != null) {
                            yield specialItems.getTemplateManager().createItemStack(templateId, itemLevel, itemExperience);
                        }
                    } catch (Exception ignored) {}
                    yield new ItemStack(Material.NETHERITE_PICKAXE);
                }
            case COMMAND -> new ItemStack(Material.PAPER);
            case KEY -> new ItemStack(Material.TRIPWIRE_HOOK);
            case BUNDLE -> new ItemStack(Material.CHEST);
            case CURRENCY -> new ItemStack(Material.EMERALD);
        };
    }
    
    private void mergeSpecialItemLore() {
        if (item == null || display == null) return;
        
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta displayMeta = display.getItemMeta();
        
        if (itemMeta != null && displayMeta != null) {
            List<String> baseLore = itemMeta.getLore();
            if (baseLore != null && !baseLore.isEmpty()) {
                List<String> lore = displayMeta.hasLore() ? new ArrayList<>(displayMeta.getLore()) : new ArrayList<>();
                lore.addAll(0, baseLore);
                displayMeta.setLore(lore);
            }
            displayMeta.addItemFlags(itemMeta.getItemFlags().toArray(new ItemFlag[0]));
            display.setItemMeta(displayMeta);
        }
    }

    public static ItemStack readItem(ConfigurationSection sec) {
        if (sec == null) return new ItemStack(Material.PAPER);
        
        Material material = Material.matchMaterial(sec.getString("material", "PAPER"));
        ItemStack item = new ItemStack(Objects.requireNonNullElse(material, Material.PAPER));
        
        int amount = sec.getInt("amount", 1);
        item.setAmount(Math.max(1, amount));
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (sec.isString("name")) {
                meta.setDisplayName(ColorUtil.colorize(sec.getString("name")));
            }
            
            if (sec.isList("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : sec.getStringList("lore")) {
                    lore.add(ColorUtil.colorize(line));
                }
                meta.setLore(lore);
            }
            
            if (sec.isInt("custom_model_data")) {
                meta.setCustomModelData(sec.getInt("custom_model_data"));
            }
            
            if (sec.isList("enchantments")) {
                for (String enchantString : sec.getStringList("enchantments")) {
                    String[] parts = enchantString.split(":");
                    if (parts.length == 2) {
                        try {
                            org.bukkit.enchantments.Enchantment enchant = 
                                org.bukkit.enchantments.Enchantment.getByName(parts[0]);
                            int level = Integer.parseInt(parts[1]);
                            if (enchant != null) {
                                meta.addEnchant(enchant, level, true);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            
            if (sec.isList("flags")) {
                for (String flagString : sec.getStringList("flags")) {
                    try {
                        ItemFlag flag = ItemFlag.valueOf(flagString);
                        meta.addItemFlags(flag);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // Getters
    public String getId() { return id; }
    public int getWeight() { return weight; }
    public Type getType() { return type; }
    public String getTier() { return tier; }
    public double getAmount() { return amount; }
    public ItemStack getItem() { return item; }
    public List<ItemStack> getItems() { return items; }
    public int getItemAmount() { return itemAmount; }
    public List<String> getCommands() { return commands; }
    public String getKeyCrate() { return keyCrate; }
    public int getKeyAmount() { return keyAmount; }
    public String getCurrencyType() { return currencyType; }
    public String getBundleName() { return bundleName; }
    public ItemStack getDisplay() { return display; }
    
        // SpecialItem reward getters
        public String getTemplateId() { return templateId; }
        public List<String> getTemplateIds() { return templateIds; }
        public int getItemLevel() { return itemLevel; }
        public long getItemExperience() { return itemExperience; }
    
    // Setters for backward compatibility and runtime modifications
    public void setAmount(double amount) { this.amount = amount; }
    public void setItem(ItemStack item) { this.item = item; }
    public void setDisplay(ItemStack display) { this.display = display; }
    }

