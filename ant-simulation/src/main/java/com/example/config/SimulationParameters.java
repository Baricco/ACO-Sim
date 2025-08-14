package com.example.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce tutti i parametri della simulazione con supporto per:
 * - Valori default, min, max
 * - Validazione automatica
 * - Observer pattern per aggiornamenti real-time
 * - Separazione logica tra parametri runtime e setup
 */
public class SimulationParameters {
    
    private static SimulationParameters instance;
    private List<ParameterChangeListener> listeners = new ArrayList<>();


        // ==================== DEFAULT VALUES ====================

        // Pheromone defaults
        public static final double DEFAULT_EVAPORATION_RATE = 0.15;
        public static final double DEFAULT_MAX_INTENSITY = 5.0;
        public static final double DEFAULT_MIN_INTENSITY = 0.05;
        public static final double DEFAULT_INITIAL_INTENSITY = 1.0;
        public static final double DEFAULT_MAX_PHEROMONE_TRAIL_LENGTH = 700.0;

        // Ant defaults
        public static final int DEFAULT_ANT_SIGHT_RADIUS = 70;
        public static final int DEFAULT_ANT_FEEL_RADIUS = 40;
        public static final int DEFAULT_ANT_SENSOR_RADIUS = 7;
        public static final double DEFAULT_ANT_SENSOR_ANGLE = Math.PI / 3;          // Angolo di 60 gradi per i sensori
        public static final double DEFAULT_ANT_SPEED = 250.0;
        public static final double DEFAULT_EXPLORATION_RATE = 0.5;
        public static final double ANT_MEMORY_EMA_ALPHA = 0.05;                     // peso della memoria delle formiche
        public static final double DEFAULT_ANT_PHEROMONE_SENSIBILITY = 0.05;        // 5% per stimoli chimici (Weber's Law)
        

        // Density Field defaults
        public static final double DEFAULT_DIFFUSION_RATE = 0.3;

        // Full Simulation defaults
        public static final int DEFAULT_NEST_NUMBER = 1;
        public static final int DEFAULT_ANT_NUMBER = 500;
        public static final int DEFAULT_CLUMP_SIZE = 500;
        public static final int DEFAULT_CLUMP_NUMBER = 10;

    
    // ==================== PARAMETRI REAL-TIME ====================
    
    // Pheromone Settings
    private double evaporationRate = DEFAULT_EVAPORATION_RATE;
    private double maxIntensity = DEFAULT_MAX_INTENSITY;
    private double minIntensity = DEFAULT_MIN_INTENSITY;
    private double initialIntensity = DEFAULT_INITIAL_INTENSITY;
    private double maxPheromoneTrailLength = DEFAULT_MAX_PHEROMONE_TRAIL_LENGTH;

    // Ant Settings
    private int antSightRadius = DEFAULT_ANT_SIGHT_RADIUS;
    private int antFeelRadius = DEFAULT_ANT_FEEL_RADIUS;
    private double antSpeed = DEFAULT_ANT_SPEED;
    private double explorationRate = DEFAULT_EXPLORATION_RATE;
    private int antSensorRadius = DEFAULT_ANT_SENSOR_RADIUS;
    private double antSensorAngle = DEFAULT_ANT_SENSOR_ANGLE;
    private double antPheromoneSensibility = DEFAULT_ANT_PHEROMONE_SENSIBILITY;

    // Density Field Settings
    private double diffusionRate = DEFAULT_DIFFUSION_RATE;

    // ==================== PARAMETRI SETUP ====================
    
    // Simulation Setup Settings
    private int nestNumber = DEFAULT_NEST_NUMBER;
    private int antNumber = DEFAULT_ANT_NUMBER;
    private int clumpSize = DEFAULT_CLUMP_SIZE;
    private int clumpNumber = DEFAULT_CLUMP_NUMBER;

    
    // ==================== CONSTRAINTS ====================
    
    public static class Constraints {
        // Pheromone constraints
        public static final double EVAPORATION_RATE_MIN = 0.1;
        public static final double EVAPORATION_RATE_MAX = 0.99;

        public static final double MAX_INTENSITY_MIN = 1.0;
        public static final double MAX_INTENSITY_MAX = 10.0;

        public static final double MIN_INTENSITY_MIN = 0.01;
        public static final double MIN_INTENSITY_MAX = 0.5;

        public static final double INITIAL_INTENSITY_MIN = 0.1;
        public static final double INITIAL_INTENSITY_MAX = 5.0;

        public static final double MAX_TRAIL_LENGTH_MIN = 100.0;
        public static final double MAX_TRAIL_LENGTH_MAX = 1500.0;
        
        // Ant constraints
        public static final int ANT_SIGHT_RADIUS_MIN = 20;
        public static final int ANT_SIGHT_RADIUS_MAX = 150;

        public static final int ANT_FEEL_RADIUS_MIN = 10;
        public static final int ANT_FEEL_RADIUS_MAX = 100;

        public static final int ANT_SENSOR_RADIUS_MIN = 1;
        public static final int ANT_SENSOR_RADIUS_MAX = 30;

        public static final double ANT_SPEED_MIN = 50.0;
        public static final double ANT_SPEED_MAX = 500.0;

        public static final double EXPLORATION_RATE_MIN = 0.0;
        public static final double EXPLORATION_RATE_MAX = 1.0;

        public static final double ANT_PHEROMONE_SENSIBILITY_MIN = 0.01;
        public static final double ANT_PHEROMONE_SENSIBILITY_MAX = 1;

        // Density Field constraints
        public static final double DIFFUSION_RATE_MIN = 0.0;
        public static final double DIFFUSION_RATE_MAX = 0.8;
        
        // Setup constraints
        public static final int NEST_NUMBER_MIN = 1;
        public static final int NEST_NUMBER_MAX = 5;

        public static final int ANT_NUMBER_MIN = 10;
        public static final int ANT_NUMBER_MAX = 500;
        
        public static final int CLUMP_SIZE_MIN = 50;
        public static final int CLUMP_SIZE_MAX = 1000;

        public static final int CLUMP_NUMBER_MIN = 1;
        public static final int CLUMP_NUMBER_MAX = 20;
    }
    
    // ==================== SINGLETON ====================
    
    public static SimulationParameters getInstance() {
        if (instance == null) {
            instance = new SimulationParameters();
        }
        return instance;
    }
    
    private SimulationParameters() {
        // Private constructor per Singleton
    }
    
    // ==================== OBSERVER PATTERN ====================
    
    public interface ParameterChangeListener {
        void onParameterChanged(String parameterName, Object oldValue, Object newValue);
    }
    
    public void addListener(ParameterChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(ParameterChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(String parameterName, Object oldValue, Object newValue) {
        for (ParameterChangeListener listener : listeners) {
            listener.onParameterChanged(parameterName, oldValue, newValue);
        }
    }
    
    // ==================== PHEROMONE GETTERS/SETTERS ====================
    
    public double getEvaporationRate() { return evaporationRate; }
    public void setEvaporationRate(double value) {
        double oldValue = evaporationRate;
        evaporationRate = clamp(value, Constraints.EVAPORATION_RATE_MIN, Constraints.EVAPORATION_RATE_MAX);
        notifyListeners("evaporationRate", oldValue, evaporationRate);
    }

    public double getMaxIntensity() { return maxIntensity; }
    public void setMaxIntensity(double value) {
        double oldValue = maxIntensity;
        maxIntensity = clamp(value, Constraints.MAX_INTENSITY_MIN, Constraints.MAX_INTENSITY_MAX);
        notifyListeners("maxIntensity", oldValue, maxIntensity);
    }
    
    public double getMinIntensity() { return minIntensity; }
    public void setMinIntensity(double value) {
        double oldValue = minIntensity;
        minIntensity = clamp(value, Constraints.MIN_INTENSITY_MIN, Constraints.MIN_INTENSITY_MAX);
        notifyListeners("minIntensity", oldValue, minIntensity);
    }
    
    public double getInitialIntensity() { return initialIntensity; }
    public void setInitialIntensity(double value) {
        double oldValue = initialIntensity;
        initialIntensity = clamp(value, Constraints.INITIAL_INTENSITY_MIN, Constraints.INITIAL_INTENSITY_MAX);
        notifyListeners("initialIntensity", oldValue, initialIntensity);
    }
    
    public double getMaxPheromoneTrailLength() { return maxPheromoneTrailLength; }
    public void setMaxPheromoneTrailLength(double value) {
        double oldValue = maxPheromoneTrailLength;
        maxPheromoneTrailLength = clamp(value, Constraints.MAX_TRAIL_LENGTH_MIN, Constraints.MAX_TRAIL_LENGTH_MAX);
        notifyListeners("maxPheromoneTrailLength", oldValue, maxPheromoneTrailLength);
    }
    
    // ==================== ANT GETTERS/SETTERS ====================
    
    public int getAntSightRadius() { return antSightRadius; }
    public void setAntSightRadius(int value) {
        int oldValue = antSightRadius;
        antSightRadius = (int) clamp(value, Constraints.ANT_SIGHT_RADIUS_MIN, Constraints.ANT_SIGHT_RADIUS_MAX);
        notifyListeners("antSightRadius", oldValue, antSightRadius);
    }
    
    public int getAntFeelRadius() { return antFeelRadius; }
    public void setAntFeelRadius(int value) {
        int oldValue = antFeelRadius;
        antFeelRadius = (int) clamp(value, Constraints.ANT_FEEL_RADIUS_MIN, Constraints.ANT_FEEL_RADIUS_MAX);
        notifyListeners("antFeelRadius", oldValue, antFeelRadius);
    }

    public int getAntSensorRadius() { return antSensorRadius; }
    public void setAntSensorRadius(int value) {
        int oldValue = antSensorRadius;
        antSensorRadius = (int) clamp(value, Constraints.ANT_SENSOR_RADIUS_MIN, Constraints.ANT_SENSOR_RADIUS_MAX);
        notifyListeners("antSensorRadius", oldValue, antSensorRadius);
    }

    // Per gli angoli dei sensori niente setter, perchè non sono modificabili in real-time
    public double getAntSensorAngle() { return antSensorAngle; }

    // Per la memoria delle formiche niente setter, perchè non è modificabile in real-time
    public double getAntMemoryEMAAlpha() { return ANT_MEMORY_EMA_ALPHA; }

    public double getAntSpeed() { return antSpeed; }
    public void setAntSpeed(double value) {
        double oldValue = antSpeed;
        antSpeed = clamp(value, Constraints.ANT_SPEED_MIN, Constraints.ANT_SPEED_MAX);
        notifyListeners("antSpeed", oldValue, antSpeed);
    }
    
    public double getExplorationRate() { return explorationRate; }
    public void setExplorationRate(double value) {
        double oldValue = explorationRate;
        explorationRate = clamp(value, Constraints.EXPLORATION_RATE_MIN, Constraints.EXPLORATION_RATE_MAX);
        notifyListeners("explorationRate", oldValue, explorationRate);
    }

    public double getAntPheromoneSensibility() { return antPheromoneSensibility; }
    public void setAntPheromoneSensibility(double value) {
        double oldValue = antPheromoneSensibility;
        antPheromoneSensibility = clamp(value, Constraints.ANT_PHEROMONE_SENSIBILITY_MIN, Constraints.ANT_PHEROMONE_SENSIBILITY_MAX);
        notifyListeners("antPheromoneSensibility", oldValue, antPheromoneSensibility);
    }

    // ==================== DENSITY FIELD GETTERS/SETTERS ====================
    
    public double getDiffusionRate() { return diffusionRate; }
    public void setDiffusionRate(double value) {
        double oldValue = diffusionRate;
        diffusionRate = clamp(value, Constraints.DIFFUSION_RATE_MIN, Constraints.DIFFUSION_RATE_MAX);
        notifyListeners("diffusionRate", oldValue, diffusionRate);
    }
    
    // ==================== SETUP GETTERS/SETTERS ====================
    
    public int getNestNumber() { return nestNumber; }
    public void setNestNumber(int value) {
        nestNumber = (int) clamp(value, Constraints.NEST_NUMBER_MIN, Constraints.NEST_NUMBER_MAX);
    }
    
    public int getAntNumber() { return antNumber; }
    public void setAntNumber(int value) {
        antNumber = (int) clamp(value, Constraints.ANT_NUMBER_MIN, Constraints.ANT_NUMBER_MAX);
    }
    
    public int getClumpSize() { return clumpSize; }
    public void setClumpSize(int value) {
        clumpSize = (int) clamp(value, Constraints.CLUMP_SIZE_MIN, Constraints.CLUMP_SIZE_MAX);
    }
    
    public int getClumpNumber() { return clumpNumber; }
    public void setClumpNumber(int value) {
        clumpNumber = (int) clamp(value, Constraints.CLUMP_NUMBER_MIN, Constraints.CLUMP_NUMBER_MAX);
    }
    
    // ==================== UTILITY METHODS ====================
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Reset ai valori di default
     */
    public void resetToDefaults() {
        setEvaporationRate(DEFAULT_EVAPORATION_RATE);
        setMaxIntensity(DEFAULT_MAX_INTENSITY);
        setMinIntensity(DEFAULT_MIN_INTENSITY);
        setInitialIntensity(DEFAULT_INITIAL_INTENSITY);
        setMaxPheromoneTrailLength(DEFAULT_MAX_PHEROMONE_TRAIL_LENGTH);
        setAntSightRadius(DEFAULT_ANT_SIGHT_RADIUS);
        setAntFeelRadius(DEFAULT_ANT_FEEL_RADIUS);
        setAntSpeed(DEFAULT_ANT_SPEED);
        setExplorationRate(DEFAULT_EXPLORATION_RATE);
        setAntPheromoneSensibility(DEFAULT_ANT_PHEROMONE_SENSIBILITY);
        setDiffusionRate(DEFAULT_DIFFUSION_RATE);
        setNestNumber(DEFAULT_NEST_NUMBER);
        setAntNumber(DEFAULT_ANT_NUMBER);
        setClumpSize(DEFAULT_CLUMP_SIZE);
        setClumpNumber(DEFAULT_CLUMP_NUMBER);
    }
    
    /**
     * Copia le impostazioni di setup in un'altra istanza
     */
    public void copySetupSettingsTo(SimulationParameters target) {
        target.nestNumber = this.nestNumber;
        target.antNumber = this.antNumber;
        target.clumpSize = this.clumpSize;
        target.clumpNumber = this.clumpNumber;
    }
}