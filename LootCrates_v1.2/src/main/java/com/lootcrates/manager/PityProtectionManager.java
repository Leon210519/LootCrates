package com.lootcrates.manager;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.crate.Reward;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PityProtectionManager {

    private static final EnumSet<Reward.Type> GUARANTEED_REWARD_TYPES = EnumSet.of(
        Reward.Type.SPECIAL_ITEM,
        Reward.Type.SPECIALITEM,
        Reward.Type.SPECIALITEM_CHOICE,
        Reward.Type.SPECIALITEM_SET
    );

    private final LootCratesPlugin plugin;
    private final Map<UUID, Map<String, Integer>> pityCounters = new ConcurrentHashMap<>();

    public PityProtectionManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPityCounters() {
        pityCounters.clear();
    }

    public int getPityCount(Player player, String crateId) {
        if (player == null || crateId == null) {
            return 0;
        }
        Map<String, Integer> perPlayer = pityCounters.get(player.getUniqueId());
        if (perPlayer == null) {
            return 0;
        }
        return perPlayer.getOrDefault(crateId.toUpperCase(Locale.ROOT), 0);
    }

    public void incrementPity(Player player, String crateId) {
        if (player == null || crateId == null) {
            return;
        }

        pityCounters
            .computeIfAbsent(player.getUniqueId(), uuid -> new ConcurrentHashMap<>())
            .merge(crateId.toUpperCase(Locale.ROOT), 1, Integer::sum);
    }

    public void resetPity(Player player, String crateId) {
        if (player == null || crateId == null) {
            return;
        }

        Map<String, Integer> perPlayer = pityCounters.get(player.getUniqueId());
        if (perPlayer != null) {
            perPlayer.remove(crateId.toUpperCase(Locale.ROOT));
        }
    }

    public boolean shouldTriggerPity(Player player, Crate crate) {
        if (player == null || crate == null || !crate.isPityEnabled()) {
            return false;
        }

        return getPityCount(player, crate.getId()) >= Math.max(1, crate.getPityThreshold());
    }

    public Reward rollWithPity(Player player, Crate crate, Random random) {
        if (crate == null) {
            return null;
        }

        List<Reward> candidates = new ArrayList<>();
        for (Reward reward : crate.getRewards()) {
            if (reward == null) {
                continue;
            }

            String tier = reward.getTier() != null ? reward.getTier().toLowerCase(Locale.ROOT) : "";
            if (tier.contains("legendary") || tier.contains("mythic") || tier.contains("rare")) {
                candidates.add(reward);
                continue;
            }

            if (GUARANTEED_REWARD_TYPES.contains(reward.getType())) {
                candidates.add(reward);
            }
        }

        if (candidates.isEmpty()) {
            plugin.getLogger().warning("Pity triggered for crate " + crate.getId() + " but no suitable reward was found. Falling back to normal roll.");
            return crate.roll(random);
        }

        return candidates.get(random.nextInt(candidates.size()));
    }
}