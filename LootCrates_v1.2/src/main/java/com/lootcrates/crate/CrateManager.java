package com.lootcrates.crate;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CrateManager {

    private final LootCratesPlugin plugin;
    private final Map<String, Crate> crates = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public CrateManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        crates.clear();

        loadFromConfiguration(plugin.getConfig());

        File crateFolder = new File(plugin.getDataFolder(), "crates");
        if (crateFolder.isDirectory()) {
            File[] files = crateFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
                        loadFromConfiguration(conf);
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to load crate file " + file.getName(), ex);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + crates.size() + " crates.");
    }

    private void loadFromConfiguration(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        ConfigurationSection crateRoot = section.getConfigurationSection("crates");
        if (crateRoot == null) {
            return;
        }

        for (String id : crateRoot.getKeys(false)) {
            ConfigurationSection crateSection = crateRoot.getConfigurationSection(id);
            if (crateSection == null) {
                continue;
            }

            try {
                Crate crate = Crate.fromConfig(id, crateSection);
                crates.put(crate.getId(), crate);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load crate " + id, ex);
            }
        }
    }

    public Set<String> list() {
        return Collections.unmodifiableSet(crates.keySet());
    }

    public Crate get(String id) {
        if (id == null) {
            return null;
        }
        return crates.get(id.toUpperCase(Locale.ROOT));
    }

    public Random rng() {
        return random;
    }

    public void giveKey(Player player, String crateId, int amount) {
        if (player == null || crateId == null) {
            return;
        }

        Crate crate = get(crateId);
        if (crate == null) {
            plugin.getLogger().warning("Tried to give keys for unknown crate " + crateId);
            return;
        }

        ItemStack key = crate.getKey().createItem(Math.max(1, amount));
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(key);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(ColorUtil.colorize("&cYour inventory was full, dropped the remaining keys at your feet."));
        }
    }
}
