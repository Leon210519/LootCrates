package com.lootcrates.hooks;

import org.bukkit.inventory.ItemStack;

public interface ItemHook {
    ItemStack getCustomItem(String itemId);
    boolean isCustomItem(ItemStack item);
    String getCustomItemId(ItemStack item);
}