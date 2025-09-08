package com.example.simulation;

import com.example.graphics.Coord;
import com.example.managers.ObstacleManager;
import com.example.metrics.MetricsCollector;
import com.example.model.Ant;
import com.example.model.FoodClump;
import com.example.model.Nest;
import com.example.model.Obstacle;

public class TJunctionSimulation extends Simulation {
    
    private boolean phase1Complete = false;
    private boolean phase2Started = false;
    private long startTime;
    private static final long PHASE1_DURATION = 30_000; // 30 secondi
    double junctionY = mapHeight * 0.5;

    
    
    public TJunctionSimulation(double mapWidth, double mapHeight) {
        super(1, 200, 0, mapWidth, mapHeight);
        initDensityManager();
        this.ANTS_BEHAVIOUR = Ant.ANT_BEHAVIOUR.ALL_PHEROMONES;
        this.hasObstacles = true;
    }

    public void generateTJunction() {

        int wallWidth = 400;

        // crea muro sinistro
        obstacleManager.addObstacle(
            new Obstacle(
                new Coord((mapWidth/2 - wallWidth/2) - 100, 560),
                wallWidth,
                400
            )
        );

        
        // crea muro destro
        obstacleManager.addObstacle(
            new Obstacle(
                new Coord((mapWidth/2 + wallWidth/2) + 100, 560),
                wallWidth,
                400
            )
        );

    }
    
    @Override
    protected synchronized void startupSimulation() {
        obstacleManager = new ObstacleManager(mapWidth, mapHeight);
        
        generateTJunction();

        MetricsCollector.getInstance().startExperiment("TJunction");
        startTime = System.currentTimeMillis();
        
        // Nido al centro-basso
        Coord nestPos = new Coord(mapWidth * 0.5, mapHeight);
        nests.add(new Nest(ANTS_NUMBER, nestPos, this));
        
        MetricsCollector.getInstance().logEvent("PHASE1_START", 
            "Establishing main trail", nestPos, null);

        double lateralDistance = 250;
        
        Coord leftFood = new Coord(mapWidth * 0.5 - lateralDistance, junctionY - 200);
        Coord rightFood = new Coord(mapWidth * 0.5 + lateralDistance, junctionY - 200);
        Coord centerFood = new Coord(mapWidth * 0.5, junctionY - 50);
        
        // Cluster laterali più grandi
        foodClumps.add(new FoodClump(leftFood, 1000, this));
        foodClumps.add(new FoodClump(rightFood, 1000, this));

         // Cluster centrale più piccolo per inidirizzare le formiche al centro
        foodClumps.add(new FoodClump(centerFood, 200, this));
    }

    
    @Override
    public void start() throws InterruptedException {
        startupSimulation();
        
        while (!exit) {
            
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { }
        }
    }
    
}