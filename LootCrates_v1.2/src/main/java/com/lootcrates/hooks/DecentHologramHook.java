package com.lootcrates.hooks;

public class DecentHologramHook {
    public DecentHologramHook() {}
    public void createHologram(String id, Object location, java.util.List<String> lines) {
        // Dummy: no-op
    }
    public void updateHologram(String id, java.util.List<String> lines) {
        // Dummy: no-op
    }
    public void deleteHologram(String id) {
        // Dummy: no-op
    }
    public boolean hologramExists(String id) {
        // Dummy: always false
        return false;
    }
    public void setHologramLocation(String id, Object location) {
        // Dummy: no-op
    }
}