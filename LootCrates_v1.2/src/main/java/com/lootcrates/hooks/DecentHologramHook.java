package com.lootcrates.hooks;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DecentHologramHook implements HologramHook {

    private final LootCratesPlugin plugin;
    private final boolean available;

    private final Class<?> dhApiClass;
    private final Method createMethod;
    private final Method updateMethod;
    private final Method deleteMethod;
    private final Method existsMethod;
    private final Method teleportMethod;

    private final Map<String, List<String>> fallbackHolograms = new HashMap<>();

    public DecentHologramHook(LootCratesPlugin plugin) {
        this.plugin = plugin;
        Class<?> apiClass = null;
        Method create = null;
        Method update = null;
        Method delete = null;
        Method exists = null;
        Method teleport = null;
        boolean hooked = false;

        try {
            apiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            create = apiClass.getMethod("createHologram", Plugin.class, String.class, Location.class, List.class);
            update = apiClass.getMethod("setHologramLines", String.class, List.class);
            delete = apiClass.getMethod("removeHologram", String.class);
            exists = apiClass.getMethod("hologramExists", String.class);
            teleport = apiClass.getMethod("moveHologram", String.class, Location.class);
            hooked = true;
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().warning("DecentHolograms API not found. Falling back to simple hologram storage.");
        } catch (NoSuchMethodException ex) {
            plugin.getLogger().warning("DecentHolograms API changed: " + ex.getMessage());
        }

        this.available = hooked;
        this.dhApiClass = apiClass;
        this.createMethod = create;
        this.updateMethod = update;
        this.deleteMethod = delete;
        this.existsMethod = exists;
        this.teleportMethod = teleport;
    }

    @Override
    public void createHologram(String id, Location location, List<String> lines) {
        if (available) {
            try {
                createMethod.invoke(null, plugin, formatId(id), location, lines);
                return;
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to create hologram via DecentHolograms: " + ex.getMessage());
            }
        }
        fallbackHolograms.put(formatId(id), lines);
    }

    @Override
    public void updateHologram(String id, List<String> lines) {
        if (available) {
            try {
                updateMethod.invoke(null, formatId(id), lines);
                return;
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to update hologram via DecentHolograms: " + ex.getMessage());
            }
        }
        fallbackHolograms.put(formatId(id), lines);
    }

    @Override
    public void deleteHologram(String id) {
        if (available) {
            try {
                deleteMethod.invoke(null, formatId(id));
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to delete hologram via DecentHolograms: " + ex.getMessage());
            }
        }
        fallbackHolograms.remove(formatId(id));
    }

    @Override
    public boolean hologramExists(String id) {
        if (available) {
            try {
                Object result = existsMethod.invoke(null, formatId(id));
                if (result instanceof Boolean bool) {
                    return bool;
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to check hologram existence: " + ex.getMessage());
            }
        }
        return fallbackHolograms.containsKey(formatId(id));
    }

    @Override
    public void setHologramLocation(String id, Location location) {
        if (available) {
            try {
                teleportMethod.invoke(null, formatId(id), location);
                return;
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to move hologram: " + ex.getMessage());
            }
        }
    }

    public Map<String, List<String>> getFallbackHolograms() {
        return Collections.unmodifiableMap(fallbackHolograms);
    }

    private String formatId(String id) {
        return id == null ? "lootcrates" : id.toLowerCase(Locale.ROOT);
    }
}