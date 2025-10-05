package com.lootcrates.hooks;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class ItemsAdderHook implements ItemHook {

    private final boolean available;
    private final Class<?> customStackClass;
    private final Method getInstanceMethod;
    private final Method byItemStackMethod;
    private final Method getItemStackMethod;
    private final Method getIdMethod;

    public ItemsAdderHook() {
        Class<?> clazz = null;
        Method getInstance = null;
        Method byItemStack = null;
        Method getItemStack = null;
        Method getId = null;
        boolean hooked = false;

        try {
            clazz = Class.forName("dev.lone.itemsadder.api.CustomStack");
            getInstance = clazz.getMethod("getInstance", String.class);
            byItemStack = clazz.getMethod("byItemStack", ItemStack.class);
            getItemStack = clazz.getMethod("getItemStack");
            getId = clazz.getMethod("getId");
            hooked = true;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }

        this.available = hooked;
        this.customStackClass = clazz;
        this.getInstanceMethod = getInstance;
        this.byItemStackMethod = byItemStack;
        this.getItemStackMethod = getItemStack;
        this.getIdMethod = getId;
    }

    @Override
    public ItemStack getCustomItem(String itemId) {
        if (!available || itemId == null) {
            return null;
        }
        try {
            Object stack = getInstanceMethod.invoke(null, itemId);
            if (stack == null) {
                return null;
            }
            return (ItemStack) getItemStackMethod.invoke(stack);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        if (!available || item == null) {
            return false;
        }
        try {
            Object stack = byItemStackMethod.invoke(null, item);
            return stack != null;
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
            Object stack = byItemStackMethod.invoke(null, item);
            if (stack == null) {
                return "";
            }
            Object id = getIdMethod.invoke(stack);
            return id != null ? id.toString() : "";
        } catch (Exception ex) {
            return "";
        }
    }
}