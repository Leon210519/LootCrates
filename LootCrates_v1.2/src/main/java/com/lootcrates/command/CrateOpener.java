package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.crate.Reward;
import com.lootcrates.economy.EconomyManager;
import com.lootcrates.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrateOpener {

    private static final Set<Reward.Type> SPECIAL_REWARD_TYPES = EnumSet.of(
        Reward.Type.SPECIAL_ITEM,
        Reward.Type.SPECIALITEM,
        Reward.Type.SPECIALITEM_CHOICE,
        Reward.Type.SPECIALITEM_SET
    );

    private static final Map<UUID, Crate> activeRolls = new ConcurrentHashMap<>();

    private CrateOpener() {
    }

    public static void tryOpenWithKey(LootCratesPlugin plugin, Player player, Crate crate, boolean searchInventory) {
        if (plugin == null || player == null || crate == null) {
            return;
        }

        if (plugin.getCooldownManager().hasCooldown(player, crate.getId())) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player, crate.getId());
            String formatted = plugin.getCooldownManager().formatTime(remaining);
            plugin.getMessageManager().sendMessageWithPrefix(player, "cooldowns.active",
                MessageManager.Placeholder.of("time", formatted),
                MessageManager.Placeholder.of("crate_display", crate.getDisplay()));
            return;
        }

        if (!searchInventory && !hasKeyInHand(player, crate)) {
            if (!consumeKeyFromInventory(player, crate)) {
                plugin.getMessageManager().sendMessageWithPrefix(player, "keys.no_key",
                    MessageManager.Placeholder.of("crate_display", crate.getDisplay()));
                return;
            }
        } else {
            if (!consumeKeyFromInventory(player, crate)) {
                plugin.getMessageManager().sendMessageWithPrefix(player, "keys.no_key",
                    MessageManager.Placeholder.of("crate_display", crate.getDisplay()));
                return;
            }
        }

        openCrate(plugin, player, crate, true);
    }

    public static void forceOpen(LootCratesPlugin plugin, Player player, Crate crate) {
        openCrate(plugin, player, crate, false);
    }

    public static void handleCloseDuringRoll(Player player) {
        if (player != null) {
            activeRolls.remove(player.getUniqueId());
        }
    }

    private static void openCrate(LootCratesPlugin plugin, Player player, Crate crate, boolean consumeCooldown) {
        if (plugin == null || player == null || crate == null) {
            return;
        }

        activeRolls.put(player.getUniqueId(), crate);

        Reward reward = selectReward(plugin, player, crate);
        giveReward(plugin, player, crate, reward);

        if (consumeCooldown && crate.getCooldown() > 0) {
            plugin.getCooldownManager().setCooldown(player, crate.getId(), crate.getCooldown());
        }

        activeRolls.remove(player.getUniqueId());
    }

    private static boolean hasKeyInHand(Player player, Crate crate) {
        ItemStack keyTemplate = crate.getKey().createItem(1);
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && mainHand.isSimilar(keyTemplate);
    }

    private static boolean consumeKeyFromInventory(Player player, Crate crate) {
        ItemStack template = crate.getKey().createItem(1);
        PlayerInventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.isSimilar(template)) {
                continue;
            }

            int amount = item.getAmount();
            if (amount <= 1) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(amount - 1);
            }
            return true;
        }

        return false;
    }

    private static Reward selectReward(LootCratesPlugin plugin, Player player, Crate crate) {
        Reward reward;
        Random random = plugin.getCrateManager().rng();

        if (plugin.getPityManager().shouldTriggerPity(player, crate)) {
            reward = plugin.getPityManager().rollWithPity(player, crate, random);
            plugin.getPityManager().resetPity(player, crate.getId());
        } else {
            reward = crate.roll(random);
            plugin.getPityManager().incrementPity(player, crate.getId());
        }

        return reward;
    }

    private static void giveReward(LootCratesPlugin plugin, Player player, Crate crate, Reward reward) {
        if (reward == null) {
            plugin.getLogger().warning("No reward generated for crate " + crate.getId());
            return;
        }

        switch (reward.getType()) {
            case MONEY_XP -> {
                EconomyManager economy = plugin.getEconomyManager();
                if (economy != null && reward.money > 0) {
                    economy.deposit(player, reward.money);
                }
                if (reward.xp > 0) {
                    player.giveExp(reward.xp);
                }
            }
            case MONEY -> {
                EconomyManager economy = plugin.getEconomyManager();
                if (economy != null && reward.getAmount() > 0) {
                    economy.deposit(player, reward.getAmount());
                }
            }
            case EXPERIENCE -> {
                if (reward.getAmount() > 0) {
                    player.giveExp((int) reward.getAmount());
                }
            }
            case ITEM, SPECIAL_ITEM, BUNDLE, SPECIALITEM, SPECIALITEM_CHOICE, SPECIALITEM_SET -> {
                for (ItemStack item : reward.getItems()) {
                    if (item == null) {
                        continue;
                    }
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                    leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
                }

                if (reward.money > 0) {
                    EconomyManager economy = plugin.getEconomyManager();
                    if (economy != null) {
                        economy.deposit(player, reward.money);
                    }
                }

                if (reward.xp > 0) {
                    player.giveExp(reward.xp);
                }
            }
            case COMMAND -> {
                if (reward.getCommands() != null) {
                    for (String command : reward.getCommands()) {
                        String parsed = command
                            .replace("{player}", player.getName())
                            .replace("{crate}", crate.getId())
                            .replace("{crate_display}", crate.getDisplay());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                    }
                }
            }
            case KEY -> {
                String targetCrate = reward.getKeyCrate() != null ? reward.getKeyCrate() : crate.getId();
                plugin.getCrateManager().giveKey(player, targetCrate, reward.getKeyAmount());
            }
            case CURRENCY -> {
                if ("PLAYER_POINTS".equalsIgnoreCase(reward.getCurrencyType())) {
                    EconomyManager economy = plugin.getEconomyManager();
                    if (economy != null) {
                        economy.addPlayerPoints(player, (int) Math.round(reward.getAmount()));
                    }
                }
            }
        }

        plugin.getPlayerDataManager().updatePlayerOpening(player, crate.getId(), reward.money, reward.getItems().size(), isRareReward(reward));

        String rewardName = reward.getId();
        if (reward.getDisplay() != null && reward.getDisplay().getItemMeta() != null && reward.getDisplay().getItemMeta().hasDisplayName()) {
            rewardName = reward.getDisplay().getItemMeta().getDisplayName();
        }

        plugin.getMessageManager().sendMessageWithPrefix(player, "rewards.reward_received",
            MessageManager.Placeholder.of("reward", rewardName));
    }

    private static boolean isRareReward(Reward reward) {
        if (reward.getTier() != null) {
            String tier = reward.getTier().toLowerCase(Locale.ROOT);
            if (tier.contains("rare") || tier.contains("legendary") || tier.contains("mythic")) {
                return true;
            }
        }
        return SPECIAL_REWARD_TYPES.contains(reward.getType());
    }
}
