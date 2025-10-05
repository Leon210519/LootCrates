package com.lootcrates.manager;

public class PityProtectionManager {
    public PityProtectionManager() {}
    public void loadPityCounters() {
        // Dummy: no-op
    }
    public int getPityCount(Object player, String crateId) {
        // Dummy: always 0
        return 0;
    }
    public void incrementPity(Object player, String crateId) {
        // Dummy: no-op
    }
    public void resetPity(Object player, String crateId) {
        // Dummy: no-op
    }
    public boolean shouldTriggerPity(Object player, Object crate) {
        // Dummy: no-op
        return false;
    }
    public Object rollWithPity(Object player, Object crate, Object random) {
        // Dummy: return null
        return null;
    }
}