package com.example.simulation;

import com.example.graphics.Coord;
import com.example.metrics.MetricsCollector;
import com.example.model.Ant;
import com.example.model.FoodClump;
import com.example.model.Nest;

public class TJunctionSimulation extends Simulation {
    
    private boolean phase1Complete = false;
    private boolean phase2Started = false;
    private long startTime;
    private static final long PHASE1_DURATION = 30_000; // 30 secondi
    
    public TJunctionSimulation(double mapWidth, double mapHeight) {
        super(1, 200, 0, mapWidth, mapHeight);
        initDensityManager();
        this.ANTS_BEHAVIOUR = Ant.ANT_BEHAVIOUR.ALL_PHEROMONES;
    }
    
    @Override
    protected synchronized void startupSimulation() {
        MetricsCollector.getInstance().startExperiment("TJunction");
        startTime = System.currentTimeMillis();
        
        // Nido al centro-basso
        Coord nestPos = new Coord(mapWidth * 0.5, mapHeight * 0.9);
        nests.add(new Nest(ANTS_NUMBER, nestPos, this));
        
        // FASE 1: Solo cluster centrale per stabilire il sentiero principale
        double junctionY = mapWidth * 0.4;
        Coord centralFood = new Coord(mapWidth * 0.5, junctionY);
        foodClumps.add(new FoodClump(centralFood, 500, this));
        
        MetricsCollector.getInstance().logEvent("PHASE1_START", 
            "Establishing main trail", nestPos, null);
    }
    
    @Override
    public void start() throws InterruptedException {
        startupSimulation();
        
        while (!exit) {
            // Controlla se è ora di passare alla fase 2
            if ((!phase1Complete && System.currentTimeMillis() - startTime > PHASE1_DURATION || !foodClumps.getFirst().isEnabled()) && !phase2Started) {
                startPhase2();
            }
            
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { }
        }
    }
    
    private void startPhase2() {
        phase1Complete = true;
        
        // FASE 2: Aggiungi cluster laterali più appetibili
        double junctionY = mapWidth * 0.4;
        double lateralDistance = 250;
        
        Coord leftFood = new Coord(mapWidth * 0.5 - lateralDistance, junctionY - 150);
        Coord rightFood = new Coord(mapWidth * 0.5 + lateralDistance, junctionY - 150);
        
        // Cluster laterali più grandi
        foodClumps.add(new FoodClump(leftFood, 1000, this));
        foodClumps.add(new FoodClump(rightFood, 1000, this));
        
        MetricsCollector.getInstance().logEvent("PHASE2_START", 
            "T-Junction choice phase activated", 
            new Coord(mapWidth * 0.5, junctionY), 
            "lateral_options_added");


        phase2Started = true;
    }
}