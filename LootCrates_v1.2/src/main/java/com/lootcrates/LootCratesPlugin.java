package com.lootcrates;

import com.lootcrates.api.LootCratesAPI;
import com.lootcrates.command.CrateCommandExecutor;
import com.lootcrates.crate.CrateManager;
import com.lootcrates.database.DatabaseManager;
import com.lootcrates.economy.EconomyManager;
import com.lootcrates.hooks.*;
import com.lootcrates.listener.CrateBlockListener;
import com.lootcrates.listener.CrateListener;
import com.lootcrates.listener.PlayerListener;
import com.lootcrates.manager.*;
import com.lootcrates.storage.CrateBlocks;
import com.lootcrates.util.MessageManager;
import com.lootcrates.util.MetricsCollector;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class LootCratesPlugin extends JavaPlugin {

    private static LootCratesPlugin instance;
    
    // Core managers
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private CrateManager crateManager;
    private CrateBlocks crateBlocks;
    private EconomyManager economyManager;
    
    // Feature managers
    private PlayerDataManager playerDataManager;
    private CooldownManager cooldownManager;
    private PityProtectionManager pityManager;
    private QueueManager queueManager;
    private LeaderboardManager leaderboardManager;
    private ShopManager shopManager;
    private AnimationManager animationManager;
    private HologramManager hologramManager;
    
    // Hook managers
    private PlaceholderAPIHook placeholderHook;
    private HologramHook hologramHook;
    private ItemHook itemHook;
    
    // API
    private LootCratesAPI api;
    
    // Metrics
    private MetricsCollector metrics;
    
    // Economy
    private Economy economy;
    
    // Plugin state
    private boolean maintenanceMode = false;
    private boolean debugMode = false;

    public static LootCratesPlugin getInstance() { 
        return instance; 
    }
    
    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("Loading LootCrates Plugin v" + getDescription().getVersion());
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Initialize core managers
        if (!initializeCore()) {
            getLogger().severe("Failed to initialize core components! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize feature managers
        initializeManagers();
        
        // Setup hooks
        setupHooks();
        
        // Register commands and listeners
        registerCommands();
        registerListeners();
        
        // Initialize API
        this.api = new LootCratesAPI(this);
        
        // Start metrics collection
        this.metrics = new MetricsCollector(this);
        
        // Delayed initialization for dependent features
        Bukkit.getScheduler().runTaskLater(this, this::delayedInitialization, 1L);
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info(String.format("LootCrates enabled in %dms with %d crates loaded!", 
            loadTime, crateManager.list().size()));
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling LootCrates Plugin...");
        
        // Save all pending data
        if (queueManager != null) {
            queueManager.saveAllQueues();
        }
        
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        
        // Cleanup hologram displays
        if (hologramManager != null) {
            hologramManager.cleanup();
        }
        
        // Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        getLogger().info("LootCrates disabled successfully.");
    }
    
    private boolean initializeCore() {
        try {
            // Initialize message manager first
            this.messageManager = new MessageManager(this);
            
            // Initialize database
            this.databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("Failed to initialize database!");
                return false;
            }
            
            // Setup economy
            if (!setupEconomy()) {
                getLogger().severe("Failed to setup Vault economy!");
                return false;
            }
            
            this.economyManager = new EconomyManager(this);
            
            // Initialize crate system
            this.crateManager = new CrateManager(this);
            this.crateBlocks = new CrateBlocks(this);
            
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during core initialization!", e);
            return false;
        }
    }
    
    private void initializeManagers() {
        try {
            this.playerDataManager = new PlayerDataManager(this);
            this.cooldownManager = new CooldownManager(this);
            this.pityManager = new PityProtectionManager(this);
            this.queueManager = new QueueManager(this);
            this.leaderboardManager = new LeaderboardManager(this);
            this.shopManager = new ShopManager(this);
            this.animationManager = new AnimationManager(this);
            this.hologramManager = new HologramManager(this);
            
            getLogger().info("All managers initialized successfully.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing managers!", e);
        }
    }
    
    private void setupHooks() {
        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderHook = new PlaceholderAPIHook(this);
            placeholderHook.register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }
        
        // Hologram plugins
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            this.hologramHook = new DecentHologramHook(this);
            getLogger().info("DecentHolograms integration enabled.");
        } else if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null) {
            // Add HolographicDisplays support if needed
            getLogger().info("HolographicDisplays detected but not yet supported.");
        }
        
        // Item plugins
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            this.itemHook = new ItemsAdderHook();
            getLogger().info("ItemsAdder integration enabled.");
        } else if (Bukkit.getPluginManager().getPlugin("Oraxen") != null) {
            this.itemHook = new OraxenHook();
            getLogger().info("Oraxen integration enabled.");
        }
    }
    
    private void registerCommands() {
        getCommand("crate").setExecutor(new CrateCommandExecutor(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CrateListener(), this);
        getServer().getPluginManager().registerEvents(new CrateBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }
    
    private void delayedInitialization() {
        // Setup holograms for bound blocks
        if (hologramManager != null) {
            hologramManager.setupAllHolograms();
        }
        
        // Start background tasks
        startBackgroundTasks();
        
        getLogger().info("Delayed initialization completed.");
    }
    
    private void startBackgroundTasks() {
        // Leaderboard update task
        if (leaderboardManager != null) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, 
                leaderboardManager::updateLeaderboards, 
                20L * 60L, // 1 minute delay
                20L * getConfig().getInt("leaderboards.update_interval", 300) // configurable interval
            );
        }
        
        // Hologram update task
        if (hologramManager != null) {
            Bukkit.getScheduler().runTaskTimer(this,
                hologramManager::updateAllHolograms,
                20L * 10L, // 10 second delay
                20L * getConfig().getInt("settings.holograms.update_interval", 20)
            );
        }
        
        // Auto-save task
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataManager != null) {
                playerDataManager.saveAllData();
            }
            if (queueManager != null) {
                queueManager.saveAllQueues();
            }
        }, 20L * 300L, 20L * 300L); // Every 5 minutes
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    // Getters for managers
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public CrateManager getCrateManager() { return crateManager; }
    public CrateBlocks getCrateBlocks() { return crateBlocks; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public PityProtectionManager getPityManager() { return pityManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public ShopManager getShopManager() { return shopManager; }
    public AnimationManager getAnimationManager() { return animationManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    
    // Hook getters
    public PlaceholderAPIHook getPlaceholderHook() { return placeholderHook; }
    public HologramHook getHologramHook() { return hologramHook; }
    public ItemHook getItemHook() { return itemHook; }
    
    // API getter
    public LootCratesAPI getAPI() { return api; }
    
    // Economy getter
    public Economy getEconomy() { return economy; }
    
    // Legacy getters for compatibility
    public Economy economy() { return economy; }
    public CrateManager crates() { return crateManager; }
    public CrateBlocks blocks() { return crateBlocks; }
    
    // Plugin state
    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }
    
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    
    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
