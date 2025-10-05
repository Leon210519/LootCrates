package com.lootcrates.hooks;

import org.bukkit.Location;

import java.util.List;

public interface HologramHook {
    void createHologram(String id, Location location, List<String> lines);
    void updateHologram(String id, List<String> lines);
    void deleteHologram(String id);
    boolean hologramExists(String id);
    void setHologramLocation(String id, Location location);
}