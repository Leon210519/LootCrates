package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CrateCommandExecutor implements CommandExecutor, TabCompleter {
    
    private final LootCratesPlugin plugin;
    
    public CrateCommandExecutor(LootCratesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help" -> sendHelp(sender);
            case "list" -> handleList(sender);
            case "open" -> handleOpen(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "stats" -> handleStats(sender, args);
            case "shop" -> handleShop(sender);
            case "history" -> handleHistory(sender, args);
            case "givekey" -> handleGiveKey(sender, args);
            case "reload" -> handleReload(sender);
            case "bind" -> handleBind(sender, args);
            case "unbind" -> handleUnbind(sender);
            case "force" -> handleForce(sender, args);
            case "migrate" -> handleMigrate(sender);
            case "debug" -> handleDebug(sender, args);
            case "maintenance" -> handleMaintenance(sender, args);
            default -> plugin.getMessageManager().sendMessageWithPrefix((Player) sender, "general.invalid_command");
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        boolean isPlayer = sender instanceof Player;
        boolean isAdmin = sender.hasPermission("lootcrates.admin");
        
        sender.sendMessage(plugin.getMessageManager().getMessage("help.header"));
        
        if (isPlayer) {
            sender.sendMessage(plugin.getMessageManager().getMessage("help.commands.open"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.commands.preview"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.commands.list"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.commands.stats"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.commands.shop"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.commands.history"));
        }
        
        if (isAdmin) {
            sender.sendMessage(plugin.getMessageManager().getMessage("help.admin_commands.givekey"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.admin_commands.reload"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.admin_commands.bind"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.admin_commands.unbind"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.admin_commands.force"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.admin_commands.migrate"));
        }
        
        sender.sendMessage(plugin.getMessageManager().getMessage("help.footer"));
    }
    
    private void handleList(CommandSender sender) {
        var crates = plugin.getCrateManager().list();
        if (crates.isEmpty()) {
            sender.sendMessage("§cNo crates configured!");
            return;
        }
        
        String crateList = crates.stream()
            .map(id -> plugin.getCrateManager().get(id))
            .filter(crate -> crate != null && crate.isAvailable())
            .map(Crate::getDisplay)
            .collect(Collectors.joining("§7, "));
            
        sender.sendMessage("§6Available Crates: §f" + crateList);
    }
    
    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        if (!player.hasPermission("lootcrates.open")) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.no_permission");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /crate open <crate>");
            return;
        }
        
        if (plugin.isMaintenanceMode() && !player.hasPermission("lootcrates.admin")) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.maintenance_enabled");
            return;
        }
        
        String crateId = args[1].toUpperCase();
        Crate crate = plugin.getCrateManager().get(crateId);
        
        if (crate == null) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.invalid_crate",
                MessageManager.Placeholder.of("crate", crateId));
            return;
        }
        
        if (!crate.isAvailable()) {
            player.sendMessage("§cThis crate is currently not available!");
            return;
        }
        
        // Check permission
        if (crate.getRequiredPermission() != null && !player.hasPermission(crate.getRequiredPermission())) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.no_permission");
            return;
        }
        
        // Check cooldown
        if (plugin.getCooldownManager().hasCooldown(player, crateId)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player, crateId);
            String timeFormat = plugin.getCooldownManager().formatTime(remaining);
            
            plugin.getMessageManager().sendMessageWithPrefix(player, "cooldowns.active",
                MessageManager.Placeholder.of("time", timeFormat),
                MessageManager.Placeholder.of("crate_display", crate.getDisplay()));
            return;
        }
        
        // Try to open with key
        CrateOpener.tryOpenWithKey(plugin, player, crate, true);
    }
    
    private void handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        if (!player.hasPermission("lootcrates.preview")) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.no_permission");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /crate preview <crate>");
            return;
        }
        
        String crateId = args[1].toUpperCase();
        Crate crate = plugin.getCrateManager().get(crateId);
        
        if (crate == null) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.invalid_crate",
                MessageManager.Placeholder.of("crate", crateId));
            return;
        }
        
        CrateGUI.openPreview(player, crate);
    }
    
    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        Player target = player;
        
        if (args.length >= 2 && sender.hasPermission("lootcrates.admin.stats")) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.getMessageManager().sendMessageWithPrefix(player, "general.invalid_player",
                    MessageManager.Placeholder.of("player", args[1]));
                return;
            }
        }
        
        // Open stats GUI or send stats message
        CrateGUI.openStats(player, target);
    }
    
    private void handleShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        if (!plugin.getConfig().getBoolean("shop.enabled", false)) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.feature_disabled");
            return;
        }
        
        CrateGUI.openShop(player);
    }
    
    private void handleHistory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        Player target = player;
        
        if (args.length >= 2 && sender.hasPermission("lootcrates.admin.stats")) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.getMessageManager().sendMessageWithPrefix(player, "general.invalid_player",
                    MessageManager.Placeholder.of("player", args[1]));
                return;
            }
        }
        
        CrateGUI.openHistory(player, target);
    }
    
    private void handleGiveKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lootcrates.admin.givekey")) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.no_permission");
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /crate givekey <player> <crate> <amount>");
            return;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.invalid_player",
                MessageManager.Placeholder.of("player", args[1]));
            return;
        }
        
        String crateId = args[2].toUpperCase();
        Crate crate = plugin.getCrateManager().get(crateId);
        if (crate == null) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.invalid_crate",
                MessageManager.Placeholder.of("crate", crateId));
            return;
        }
        
        try {
            int amount = Integer.parseInt(args[3]);
            plugin.getCrateManager().giveKey(target, crateId, amount);
            
            plugin.getMessageManager().sendMessage((Player) sender, "keys.keys_given",
                MessageManager.Placeholder.of("amount", amount),
                MessageManager.Placeholder.of("key_display", crate.getKey().getDisplay()),
                MessageManager.Placeholder.of("player", target.getName()));
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.invalid_number",
                MessageManager.Placeholder.of("input", args[3]));
        }
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lootcrates.admin.reload")) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.no_permission");
            return;
        }
        
        plugin.reloadConfig();
        plugin.getMessageManager().reloadMessages();
        plugin.getCrateManager().reload();
        
        plugin.getMessageManager().sendMessage((Player) sender, "general.config_reloaded");
    }
    
    private void handleBind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        if (!player.hasPermission("lootcrates.admin.bind")) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.no_permission");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /crate bind <crate>");
            return;
        }
        
        String crateId = args[1].toUpperCase();
        Crate crate = plugin.getCrateManager().get(crateId);
        if (crate == null) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.invalid_crate",
                MessageManager.Placeholder.of("crate", crateId));
            return;
        }
        
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "blocks.look_at_block");
            return;
        }
        
        plugin.getCrateBlocks().bind(block.getLocation(), crate.getId());
        
        String locationString = String.format("%d, %d, %d", 
            block.getX(), block.getY(), block.getZ());
        
        plugin.getMessageManager().sendMessageWithPrefix(player, "blocks.bound",
            MessageManager.Placeholder.of("crate_display", crate.getDisplay()),
            MessageManager.Placeholder.of("location", locationString));
    }
    
    private void handleUnbind(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.player_only");
            return;
        }
        
        if (!player.hasPermission("lootcrates.admin.bind")) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "general.no_permission");
            return;
        }
        
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            plugin.getMessageManager().sendMessageWithPrefix(player, "blocks.look_at_block");
            return;
        }
        
        plugin.getCrateBlocks().unbind(block.getLocation());
        
        String locationString = String.format("%d, %d, %d", 
            block.getX(), block.getY(), block.getZ());
        
        plugin.getMessageManager().sendMessageWithPrefix(player, "blocks.unbound",
            MessageManager.Placeholder.of("location", locationString));
    }
    
    private void handleForce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lootcrates.admin.force")) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.no_permission");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /crate force <player> <crate>");
            return;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.invalid_player",
                MessageManager.Placeholder.of("player", args[1]));
            return;
        }
        
        String crateId = args[2].toUpperCase();
        Crate crate = plugin.getCrateManager().get(crateId);
        if (crate == null) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.invalid_crate",
                MessageManager.Placeholder.of("crate", crateId));
            return;
        }
        
        // Force open without key requirement
        CrateOpener.forceOpen(plugin, target, crate);
        sender.sendMessage("§aForced " + target.getName() + " to open " + crate.getDisplay());
    }
    
    private void handleMigrate(CommandSender sender) {
        if (!sender.hasPermission("lootcrates.admin")) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.no_permission");
            return;
        }
        
        // Implementation for data migration
        sender.sendMessage("§7Starting data migration... (Not implemented yet)");
    }
    
    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lootcrates.admin")) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.no_permission");
            return;
        }
        
        if (args.length >= 2) {
            boolean enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on");
            plugin.setDebugMode(enable);
            
            plugin.getMessageManager().sendMessage((Player) sender, 
                enable ? "admin.debug_enabled" : "admin.debug_disabled");
        } else {
            boolean current = plugin.isDebugMode();
            sender.sendMessage("§7Debug mode is currently: " + (current ? "§aEnabled" : "§cDisabled"));
        }
    }
    
    private void handleMaintenance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lootcrates.admin")) {
            plugin.getMessageManager().sendMessage((Player) sender, "general.no_permission");
            return;
        }
        
        if (args.length >= 2) {
            boolean enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on");
            plugin.setMaintenanceMode(enable);
            
            plugin.getMessageManager().sendMessage((Player) sender,
                enable ? "admin.maintenance_enabled" : "admin.maintenance_disabled");
        } else {
            boolean current = plugin.isMaintenanceMode();
            sender.sendMessage("§7Maintenance mode is currently: " + (current ? "§cEnabled" : "§aDisabled"));
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            List<String> commands = Arrays.asList("help", "list", "open", "preview", "stats", "shop", "history");
            
            if (sender.hasPermission("lootcrates.admin")) {
                commands = new ArrayList<>(commands);
                commands.addAll(Arrays.asList("givekey", "reload", "bind", "unbind", "force", "migrate", "debug", "maintenance"));
            }
            
            return commands.stream()
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "open", "preview", "bind", "force" -> {
                    return plugin.getCrateManager().list().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "givekey" -> {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "stats", "history" -> {
                    if (sender.hasPermission("lootcrates.admin.stats")) {
                        return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                }
                case "debug", "maintenance" -> {
                    return Arrays.asList("true", "false", "on", "off").stream()
                        .filter(option -> option.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("givekey")) {
            return plugin.getCrateManager().list().stream()
                .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return completions;
    }
}