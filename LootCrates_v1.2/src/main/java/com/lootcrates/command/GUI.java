package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import org.bukkit.entity.Player;

public final class GUI {

    private GUI() {
    }

    public static void preview(Player player, Crate crate) {
        if (player == null || crate == null) {
            return;
        }
        CrateGUI.openPreview(player, crate);
    }

    public static void open(Player player, Crate crate) {
        if (player == null || crate == null) {
            return;
        }
        CrateOpener.forceOpen(LootCratesPlugin.getInstance(), player, crate);
    }

    public static void tryOpenWithKey(LootCratesPlugin plugin, Player player, Crate crate, boolean searchInventory) {
        if (plugin == null || player == null || crate == null) {
            return;
        }
        CrateOpener.tryOpenWithKey(plugin, player, crate, searchInventory);
    }

    public static void handleCloseDuringRoll(Player player) {
        if (player == null) {
            return;
        }
        CrateOpener.handleCloseDuringRoll(player);
    }
}
