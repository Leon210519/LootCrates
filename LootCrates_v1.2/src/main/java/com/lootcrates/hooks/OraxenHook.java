package com.lootcrates.hooks;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class OraxenHook implements ItemHook {

    private final boolean available;
    private final Class<?> apiClass;
    private final Method getItemMethod;
    private final Method getIdMethod;

    public OraxenHook() {
        Class<?> clazz = null;
        Method getItem = null;
        Method getId = null;
        boolean hooked = false;

        try {
            clazz = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            getItem = clazz.getMethod("getItemById", String.class);
            getId = clazz.getMethod("getId", ItemStack.class);
            hooked = true;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }

        this.available = hooked;
        this.apiClass = clazz;
        this.getItemMethod = getItem;
        this.getIdMethod = getId;
    }

    @Override
    public ItemStack getCustomItem(String itemId) {
        if (!available || itemId == null) {
            return null;
        }
        try {
            Object item = getItemMethod.invoke(null, itemId);
            if (item instanceof ItemStack stack) {
                return stack.clone();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        if (!available || item == null) {
            return false;
        }
        try {
            Object id = getIdMethod.invoke(null, item);
            return id != null;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String getCustomItemId(ItemStack item) {
        if (!available || item == null) {
            return "";
        }
        try {
            Object id = getIdMethod.invoke(null, item);
            return id != null ? id.toString() : "";
        } catch (Exception ex) {
            return "";
        }
    }
}