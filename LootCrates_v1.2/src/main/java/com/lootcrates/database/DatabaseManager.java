package com.lootcrates.database;

public class DatabaseManager {
    public DatabaseManager() {}
    public boolean initialize() {
        // Dummy: always return true
        return true;
    }
    public void close() {
        // Dummy: no-op
    }
    public boolean isConnected() {
        // Dummy: always true
        return true;
    }
    public Object getConnection() {
        // Dummy: return null
        return null;
    }
    // Add other dummy methods as needed
}