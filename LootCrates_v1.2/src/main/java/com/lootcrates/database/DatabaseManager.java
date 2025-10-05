package com.lootcrates.database;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

public class DatabaseManager {

    public enum DatabaseType {
        SQLITE,
        MYSQL
    }

    private final LootCratesPlugin plugin;
    private final ExecutorService executor;

    private DatabaseType databaseType = DatabaseType.SQLITE;
    private String sqlitePath;

    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private boolean mysqlUseSSL;

    public DatabaseManager(LootCratesPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "LootCrates-DB-" + counter++);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public boolean initialize() {
        ConfigurationSection databaseSection = plugin.getConfig().getConfigurationSection("settings.database");
        String typeName = databaseSection != null ? databaseSection.getString("type", "SQLITE") : "SQLITE";

        try {
            databaseType = DatabaseType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown database type '" + typeName + "', falling back to SQLITE");
            databaseType = DatabaseType.SQLITE;
        }

        try {
            if (databaseType == DatabaseType.SQLITE) {
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                    plugin.getLogger().severe("Failed to create plugin data folder for database storage.");
                    return false;
                }

                sqlitePath = new File(dataFolder, "lootcrates.db").getAbsolutePath();

                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException ignored) {
                    plugin.getLogger().warning("SQLite JDBC driver not found. The server distribution usually ships with it, but if the plugin fails to connect please add it manually.");
                }
            } else {
                if (databaseSection == null) {
                    plugin.getLogger().severe("MySQL selected but settings.database section is missing!");
                    return false;
                }

                ConfigurationSection mysqlSection = databaseSection.getConfigurationSection("mysql");
                if (mysqlSection == null) {
                    plugin.getLogger().severe("MySQL selected but settings.database.mysql section is missing!");
                    return false;
                }

                mysqlHost = mysqlSection.getString("host", "localhost");
                mysqlPort = mysqlSection.getInt("port", 3306);
                mysqlDatabase = mysqlSection.getString("database", "lootcrates");
                mysqlUsername = mysqlSection.getString("username", "root");
                mysqlPassword = mysqlSection.getString("password", "");
                mysqlUseSSL = mysqlSection.getBoolean("ssl", false);

                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException ex) {
                    plugin.getLogger().warning("MySQL driver not found. Please ensure the server provides it.");
                }
            }

            try (Connection connection = createConnection()) {
                if (connection == null) {
                    plugin.getLogger().severe("Failed to create database connection.");
                    return false;
                }
                setupSchema(connection);
            }

            return true;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialise database", ex);
            return false;
        }
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public boolean isConnected() {
        try (Connection connection = createConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        return createConnection();
    }

    public void closeConnection() {
        executor.shutdownNow();
    }

    public void executeAsync(SQLConsumer consumer) {
        executor.submit(() -> {
            try (Connection connection = createConnection()) {
                consumer.accept(connection);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Database execution error", ex);
            }
        });
    }

    public <T> void queryAsync(SQLFunction<T> function) {
        executor.submit(() -> {
            try (Connection connection = createConnection()) {
                function.apply(connection);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Database query error", ex);
            }
        });
    }

    private Connection createConnection() throws SQLException {
        if (databaseType == DatabaseType.SQLITE) {
            return DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        }

        String url = String.format(
            Locale.ROOT,
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=%s",
            mysqlHost,
            mysqlPort,
            mysqlDatabase,
            mysqlUseSSL);
        return DriverManager.getConnection(url, mysqlUsername, mysqlPassword);
    }

    private void setupSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    total_opens INT NOT NULL DEFAULT 0,
                    money_earned DOUBLE NOT NULL DEFAULT 0,
                    items_received INT NOT NULL DEFAULT 0,
                    rare_finds INT NOT NULL DEFAULT 0,
                    last_open BIGINT NOT NULL DEFAULT 0,
                    data_created BIGINT NOT NULL DEFAULT 0,
                    data_updated BIGINT NOT NULL DEFAULT 0
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lc_cooldowns (
                    player_uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    expires_at BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, crate_id)
                )
            """);

            String queuePrimaryKey = databaseType == DatabaseType.SQLITE
                ? "INTEGER PRIMARY KEY AUTOINCREMENT"
                : "INT NOT NULL AUTO_INCREMENT PRIMARY KEY";

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lc_offline_queue (" +
                "id " + queuePrimaryKey + "," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "crate_id VARCHAR(64)," +
                "reward_id VARCHAR(128)," +
                "payload TEXT," +
                "created_at BIGINT NOT NULL" +
                ")");
        }
    }

    @FunctionalInterface
    public interface SQLConsumer {
        void accept(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SQLFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}