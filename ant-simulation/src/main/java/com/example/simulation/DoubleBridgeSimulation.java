package com.example.simulation;

import com.example.graphics.Coord;
import com.example.managers.ObstacleManager;
import com.example.metrics.MetricsCollector;
import com.example.model.Ant;
import com.example.model.FoodClump;
import com.example.model.Nest;
import com.example.model.Obstacle;

public class DoubleBridgeSimulation extends Simulation {
    

    public DoubleBridgeSimulation(double mapWidth, double mapHeight) {
        super(1, 500, 0, mapWidth, mapHeight);
        initDensityManager();
        this.ANTS_BEHAVIOUR = Ant.ANT_BEHAVIOUR.ALL_PHEROMONES;
        this.hasObstacles = true;
    }
    
    @Override
    protected synchronized void startupSimulation() {
        
        obstacleManager = new ObstacleManager(mapWidth, mapHeight);

        MetricsCollector.getInstance().startExperiment("DoubleBridge");
        
        // Nido al centro-sinistra
        Coord nestPos = new Coord(mapWidth * 0.2, mapHeight * 0.5);
        nests.add(new Nest(ANTS_NUMBER, nestPos, this));
        
        // Un foodClump a destra
        Coord foodClumpPos = new Coord(nestPos.x + 600, nestPos.y);
        foodClumps.add(new FoodClump(foodClumpPos, 2000, this));


        double obstacleX = (nestPos.x + foodClumpPos.x) / 2;

        // Due foodClump piccoli per indirizzare il traffico
        foodClumps.add(new FoodClump(new Coord(obstacleX, nestPos.y - 150), 250, this));
        foodClumps.add(new FoodClump(new Coord(obstacleX, foodClumpPos.y + 300), 250, this));

        // Un ostacolo tra nido e cibo
        obstacleManager.addObstacle(new Obstacle(
            new Coord(obstacleX, nestPos.y + 100),
            100,
            300
        ));



        MetricsCollector.getInstance().logEvent("SETUP_COMPLETE", 
            "Double bridge experiment initialized", nestPos, 
            "foodClumpPos=" + foodClumpPos.toString());
    }
    
    @Override
    public void start() throws InterruptedException {
        startupSimulation();
        
        while (!exit) {
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { }
        }
    }
}