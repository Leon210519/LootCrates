package com.lootcrates.rewards;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward type for SpecialItems integration
 */
public class SpecialItemReward {
    
    private final String templateId;
    private final int level;
    private final long experience;
    
    public SpecialItemReward(String id, String templateId, int level, long experience) {
        this.templateId = templateId;
        this.level = level;
        this.experience = experience;
    }
    
    // Dummy implementation
    public void give(Object player) {
        // No-op
    }
    
    public boolean isAvailable(Object player) {
        // Dummy: always available
        return true;
    }
    
    public Object getDisplayItem() {
        // Dummy: return null
        return null;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public long getExperience() {
        return experience;
    }
}

/**
 * Choice reward for SpecialItems - player can choose from multiple templates
 */
class SpecialItemChoiceReward {
    
    private final List<String> templateIds;
    private final int level;
    private final long experience;
    
    public SpecialItemChoiceReward(String id, List<String> templateIds, int level, long experience) {
        this.templateIds = templateIds;
        this.level = level;
        this.experience = experience;
    }
    
    public void give(Object player) {
        // Dummy: no-op
    }
    
    public boolean isAvailable(Object player) {
        // Dummy: always available
        return true;
    }
    
    public List<String> getTemplateIds() {
        return templateIds;
    }
    
    public int getLevel() {
        return level;
    }
    
    public long getExperience() {
        return experience;
    }
}

/**
 * Set reward for SpecialItems - gives multiple items as a set
 */
class SpecialItemSetReward {
    
    private final List<String> templateIds;
    private final int level;
    private final long experience;
    
    public SpecialItemSetReward(String id, List<String> templateIds, int level, long experience) {
        this.templateIds = templateIds;
        this.level = level;
        this.experience = experience;
    }
    
    public void give(Object player) {
        // Dummy: no-op
    }
    
    public boolean isAvailable(Object player) {
        // Dummy: always available
        return true;
    }
    
    public List<String> getTemplateIds() {
        return templateIds;
    }
    
    public int getLevel() {
        return level;
    }
    
    public long getExperience() {
        return experience;
    }
}