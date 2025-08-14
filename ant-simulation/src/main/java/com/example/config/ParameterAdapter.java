package com.example.config;

/**
 * Adapter per fornire accesso semplice ai parametri dinamici
 * Questo permette alle classi esistenti di ottenere i valori 
 * senza dover modificare significativamente il loro codice
 */
public class ParameterAdapter {
    
    private static SimulationParameters params = SimulationParameters.getInstance();
    
    // ==================== PHEROMONE PARAMETERS ====================
    
    public static double getPheromoneEvaporationRate() {
        return params.getEvaporationRate();
    }

    public static int getAntSensorRadius() {
        return params.getAntSensorRadius();
    }
    
    public static double getPheromoneMaxIntensity() {
        return params.getMaxIntensity();
    }
    
    public static double getPheromoneMinIntensity() {
        return params.getMinIntensity();
    }
    
    public static double getPheromoneInitialIntensity() {
        return params.getInitialIntensity();
    }
    
    public static double getMaxPheromoneTrailLength() {
        return params.getMaxPheromoneTrailLength();
    }
    
    public static double getMaxPheromoneTrailDuration() {
        return getMaxPheromoneTrailLength() / getAntSpeed();
    }
    
    // ==================== ANT PARAMETERS ====================
    
    public static int getAntSightRadius() {
        return params.getAntSightRadius();
    }
    
    public static int getAntFeelRadius() {
        return params.getAntFeelRadius();
    }
    
    public static double getAntSpeed() {
        return params.getAntSpeed();
    }
    
    public static double getExplorationRate() {
        return params.getExplorationRate();
    }

    public static double getAntMemoryEMAAlpha() {
        return params.getAntMemoryEMAAlpha();
    }

    public static double getAntPheromoneSensibility() {
        return params.getAntPheromoneSensibility();
    }
    
    // ==================== DENSITY FIELD PARAMETERS ====================
    
    public static double getDiffusionRate() {
        return params.getDiffusionRate();
    }
    
    // ==================== SIMULATION SETUP PARAMETERS ====================
    
    public static int getNestNumber() {
        return params.getNestNumber();
    }
    
    public static int getAntNumber() {
        return params.getAntNumber();
    }
    
    public static int getClumpSize() {
        return params.getClumpSize();
    }
    
    public static int getClumpNumber() {
        return params.getClumpNumber();
    }
}