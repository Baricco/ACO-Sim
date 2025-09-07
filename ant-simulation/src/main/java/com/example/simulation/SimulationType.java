package com.example.simulation;

public enum SimulationType {
    FULL_SIMULATION("Full Simulation", "Standard foraging simulation"),
    DEMO("Demo", "Simple demonstration"),
    DOUBLE_BRIDGE("Double Bridge", "Beckers et al. shortest path experiment"),
    T_JUNCTION("T-Junction", "Trail-following accuracy test");

    
    private final String displayName;
    private final String description;

    SimulationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}