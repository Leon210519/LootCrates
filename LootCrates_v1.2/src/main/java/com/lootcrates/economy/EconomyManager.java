package com.lootcrates.economy;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class EconomyManager {

    private final LootCratesPlugin plugin;
    private final net.milkbowl.vault.economy.Economy vaultEconomy;

    private final Object playerPointsAPI;
    private final Method playerPointsAdd;
    private final Method playerPointsTake;
    private final Method playerPointsLook;

    public EconomyManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
        this.vaultEconomy = plugin.getEconomy();

        Object pointsApi = null;
        Method add = null;
        Method take = null;
        Method look = null;

        Plugin playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (playerPoints != null) {
            try {
                Class<?> apiClass = Class.forName("org.black_ixx.playerpoints.PlayerPointsAPI");
                Method getAPI = playerPoints.getClass().getMethod("getAPI");
                pointsApi = getAPI.invoke(playerPoints);
                add = apiClass.getMethod("give", OfflinePlayer.class, int.class);
                take = apiClass.getMethod("take", OfflinePlayer.class, int.class);
                look = apiClass.getMethod("look", OfflinePlayer.class);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to hook into PlayerPoints: " + ex.getMessage());
                pointsApi = null;
            }
        }

        this.playerPointsAPI = pointsApi;
        this.playerPointsAdd = add;
        this.playerPointsTake = take;
        this.playerPointsLook = look;
    }

    public boolean hasVaultEconomy() {
        return vaultEconomy != null;
    }

    public boolean hasPlayerPoints() {
        return playerPointsAPI != null;
    }

    public double getBalance(Player player) {
        if (!hasVaultEconomy()) {
            return 0.0;
        }
        return vaultEconomy.getBalance(player);
    }

    public boolean deposit(Player player, double amount) {
        if (!hasVaultEconomy()) {
            return false;
        }
        vaultEconomy.depositPlayer(player, amount);
        return true;
    }

    public boolean withdraw(Player player, double amount) {
        if (!hasVaultEconomy()) {
            return false;
        }
        if (vaultEconomy.getBalance(player) < amount) {
            return false;
        }
        vaultEconomy.withdrawPlayer(player, amount);
        return true;
    }

    public int getPlayerPoints(OfflinePlayer player) {
        if (!hasPlayerPoints() || playerPointsLook == null) {
            return 0;
        }
        try {
            Object result = playerPointsLook.invoke(playerPointsAPI, player);
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to read PlayerPoints balance: " + ex.getMessage());
            return 0;
        }
    }

    public boolean addPlayerPoints(OfflinePlayer player, int amount) {
        if (!hasPlayerPoints() || playerPointsAdd == null) {
            return false;
        }
        try {
            playerPointsAdd.invoke(playerPointsAPI, player, amount);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to add PlayerPoints: " + ex.getMessage());
            return false;
        }
    }

    public boolean takePlayerPoints(OfflinePlayer player, int amount) {
        if (!hasPlayerPoints() || playerPointsTake == null) {
            return false;
        }
        try {
            playerPointsTake.invoke(playerPointsAPI, player, amount);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to remove PlayerPoints: " + ex.getMessage());
            return false;
        }
    }
}