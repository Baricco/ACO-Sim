package com.example.simulation;

import com.example.config.ParameterAdapter;
import com.example.model.Ant;

public class FullSimulation extends Simulation {

    public FullSimulation(double mapWidth, double mapHeight) {
        super(
            ParameterAdapter.getNestNumber(),
            ParameterAdapter.getAntNumber(), 
            ParameterAdapter.getClumpSize(), 
            ParameterAdapter.getClumpNumber(), 
            mapWidth, 
            mapHeight
        );
        initDensityManager();
        this.ANTS_BEHAVIOUR = Ant.ANT_BEHAVIOUR.ALL_PHEROMONES;      // Comportamento predefinito per la simulazione
        this.hasObstacles = false;
    }

    @Override
    public void start() throws InterruptedException {

        // Inizializzazione dell'arraylist di formiche e di cibo
        startupSimulation();
        
        // selectedItemIndex = 0;
        // zoom = false;

        // simulationGraphicsSetup rimossa - sarà gestita dal controller JavaFX

        while (!exit) {
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { } 
            /*
                if(zoom) {
                    if(selectedItemObj!=null && selectedItemObj.checkAlive()){
                        centerOnPoint(selectedItemObj.getCircle().getCenter());
                        lbl_stats_title.setText(selectedItemObj.getObjType().toUpperCase());
                        lbl_stats_text.setText(selectedItemObj.toHTML());
                    }
                }
            */
        }   
    }
}