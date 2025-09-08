package com.example.simulation;

import com.example.graphics.Coord;
import com.example.metrics.MetricsCollector;
import com.example.model.Ant;
import com.example.model.FoodClump;
import com.example.model.Nest;

public class DoubleBridgeSimulation extends Simulation {
    
    private static final double SHORT_PATH_LENGTH = 300;
    private static final double LONG_PATH_LENGTH = 600;

    public DoubleBridgeSimulation(double mapWidth, double mapHeight) {
        super(1, 200, 1000, mapWidth, mapHeight);
        initDensityManager();
        this.ANTS_BEHAVIOUR = Ant.ANT_BEHAVIOUR.ALL_PHEROMONES;
        this.hasObstacles = false;
    }
    
    @Override
    protected synchronized void startupSimulation() {
        MetricsCollector.getInstance().startExperiment("DoubleBridge");
        
        // Nido al centro-sinistra
        Coord nestPos = new Coord(mapWidth * 0.2, mapHeight * 0.5);
        nests.add(new Nest(ANTS_NUMBER, nestPos, this));
        
        // Due fonti di cibo: una vicina (percorso corto) e una lontana (percorso lungo)
        Coord shortPathFood = new Coord(nestPos.x + SHORT_PATH_LENGTH, nestPos.y);
        Coord longPathFood = new Coord(nestPos.x + LONG_PATH_LENGTH, nestPos.y);

        foodClumps.add(new FoodClump(shortPathFood, 500, this));
        foodClumps.add(new FoodClump(longPathFood, 500, this));

        MetricsCollector.getInstance().logEvent("SETUP_COMPLETE", 
            "Double bridge experiment initialized", nestPos, 
            "short=" + SHORT_PATH_LENGTH + ",long=" + LONG_PATH_LENGTH);
    }
    
    @Override
    public void start() throws InterruptedException {
        startupSimulation();
        
        while (!exit) {
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { }
        }
    }
}