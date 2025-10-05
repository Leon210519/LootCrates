package com.lootcrates.hooks;

public class ItemsAdderHook {
    public ItemsAdderHook() {}
    public Object getCustomItem(String itemId) {
        // Dummy: return null
        return null;
    }
    public boolean isCustomItem(Object item) {
        // Dummy: always false
        return false;
    }
    public String getCustomItemId(Object item) {
        // Dummy: return empty string
        return "";
    }
}