package com.lootcrates.data;

import java.util.UUID;

public class PlayerData {
    
    private final UUID uuid;
    private String username;
    private int totalOpens;
    private double moneyEarned;
    private int itemsReceived;
    private int rareFinds;
    private long lastOpen;
    private long dataCreated;
    private long dataUpdated;
    
    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.totalOpens = 0;
        this.moneyEarned = 0.0;
        this.itemsReceived = 0;
        this.rareFinds = 0;
        this.lastOpen = 0;
        this.dataCreated = System.currentTimeMillis();
        this.dataUpdated = System.currentTimeMillis();
    }
    
    // Getters
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public int getTotalOpens() { return totalOpens; }
    public double getMoneyEarned() { return moneyEarned; }
    public int getItemsReceived() { return itemsReceived; }
    public int getRareFinds() { return rareFinds; }
    public long getLastOpen() { return lastOpen; }
    public long getDataCreated() { return dataCreated; }
    public long getDataUpdated() { return dataUpdated; }
    
    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setTotalOpens(int totalOpens) { this.totalOpens = totalOpens; }
    public void setMoneyEarned(double moneyEarned) { this.moneyEarned = moneyEarned; }
    public void setItemsReceived(int itemsReceived) { this.itemsReceived = itemsReceived; }
    public void setRareFinds(int rareFinds) { this.rareFinds = rareFinds; }
    public void setLastOpen(long lastOpen) { this.lastOpen = lastOpen; }
    public void setDataCreated(long dataCreated) { this.dataCreated = dataCreated; }
    public void setDataUpdated(long dataUpdated) { this.dataUpdated = dataUpdated; }
    
    // Utility methods
    public double getLuckRatio() {
        return totalOpens > 0 ? (double) rareFinds / totalOpens : 0.0;
    }
    
    public double getAverageMoneyPerOpen() {
        return totalOpens > 0 ? moneyEarned / totalOpens : 0.0;
    }
}