package com.lootcrates.hooks;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.data.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Locale;

public class PlaceholderAPIHook {

    private final LootCratesPlugin plugin;

    private Class<?> eventClass;
    private Method getPlaceholderMethod;
    private Method getPlayerMethod;
    private Method setResultMethod;

    public PlaceholderAPIHook(LootCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public boolean register() {
        try {
            eventClass = Class.forName("me.clip.placeholderapi.events.PlaceholderRequestEvent");
            getPlaceholderMethod = eventClass.getMethod("getPlaceholder");
            getPlayerMethod = eventClass.getMethod("getPlayer");
            setResultMethod = eventClass.getMethod("setResult", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            plugin.getLogger().warning("PlaceholderAPI not found or unsupported version: " + ex.getMessage());
            return false;
        }

        Listener dummyListener = new Listener() {
        };

        plugin.getServer().getPluginManager().registerEvent(
            (Class<? extends Event>) eventClass,
            dummyListener,
            EventPriority.NORMAL,
            (listener, event) -> handleEvent(event),
            plugin,
            true
        );

        return true;
    }

    private void handleEvent(Event event) {
        if (!eventClass.isInstance(event)) {
            return;
        }

        try {
            String placeholder = (String) getPlaceholderMethod.invoke(event);
            if (placeholder == null) {
                return;
            }

            String identifier = placeholder.toLowerCase(Locale.ROOT);
            if (!identifier.startsWith("lootcrates")) {
                return;
            }

            String params = identifier.substring("lootcrates".length());
            if (params.startsWith("_")) {
                params = params.substring(1);
            }

            OfflinePlayer offlinePlayer = (OfflinePlayer) getPlayerMethod.invoke(event);
            Player player = offlinePlayer != null ? offlinePlayer.getPlayer() : null;
            String result = resolve(player, params);

            if (result != null) {
                setResultMethod.invoke(event, result);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to process PlaceholderAPI request: " + ex.getMessage());
        }
    }

    private String resolve(Player player, String params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        String[] args = params.split("_");
        if (args.length == 0) {
            return "";
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "keys":
                if (player == null || args.length < 2) {
                    return "0";
                }
                return Integer.toString(getPlayerKeys(player, args[1]));
            case "opens":
                if (player == null) {
                    return "0";
                }
                return Integer.toString(getCrateOpens(player, args.length >= 2 ? args[1] : null));
            case "lastwinner":
                return getLastWinner(args.length >= 2 ? args[1] : "");
            case "lastreward":
                if (player == null) {
                    return "None";
                }
                return getLastReward(player);
            case "rank":
                if (player == null || args.length < 2) {
                    return "0";
                }
                return Integer.toString(getPlayerRank(player, args[1]));
            case "daily":
                if (player == null || args.length < 3 || !"limit".equalsIgnoreCase(args[1])) {
                    return "";
                }
                return getDailyLimitStatus(player, args[2]);
            default:
                return "";
        }
    }

    private int getPlayerKeys(Player player, String crateId) {
        if (crateId == null) {
            return 0;
        }
        Crate crate = plugin.getCrateManager().get(crateId);
        if (crate == null) {
            return 0;
        }

        ItemStack keyItem = crate.getKey().createItem(1);
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(keyItem)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private int getCrateOpens(Player player, String crateId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null ? data.getTotalOpens() : 0;
    }

    private String getLastWinner(String crateId) {
        return "Unknown";
    }

    private String getLastReward(Player player) {
        return "None";
    }

    private int getPlayerRank(Player player, String type) {
        return 0;
    }

    private String getDailyLimitStatus(Player player, String crateId) {
        if (crateId == null) {
            return "";
        }
        Crate crate = plugin.getCrateManager().get(crateId);
        if (crate == null || crate.getDailyLimit() <= 0) {
            return "\u221e";
        }

        int limit = crate.getDailyLimit();
        return limit + "/" + limit;
    }
}
