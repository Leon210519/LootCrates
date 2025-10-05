package com.lootcrates.economy;

public class EconomyManager {
    public EconomyManager() {}
    public boolean hasVaultEconomy() {
        // Dummy: always false
        return false;
    }
    public boolean hasPlayerPoints() {
        // Dummy: always false
        return false;
    }
    public boolean hasTokenManager() {
        // Dummy: always false
        return false;
    }
    public double getBalance(Object player) {
        // Dummy: always 0
        return 0.0;
    }
    public boolean withdraw(Object player, double amount) {
        // Dummy: always true
        return true;
    }
    public boolean deposit(Object player, double amount) {
        // Dummy: always true
        return true;
    }
    // Add other dummy methods as needed
}