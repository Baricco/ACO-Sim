package com.example.simulation;

import com.example.graphics.Coord;
import com.example.model.Ant;
import com.example.model.Food;
import com.example.model.Nest;

public class DemoSimulation extends Simulation {

    private static final int NESTS_NUMBER = 1;
    private static final int ANTS_NUMBER = 10;
    private static final int FOODS_NUMBER = 80;

    public DemoSimulation(double mapWidth, double mapHeight) {
        super(NESTS_NUMBER, ANTS_NUMBER, FOODS_NUMBER, mapWidth, mapHeight);
        this.selectedAntIndex = -1;                    // Inizializza l'indice della formica selezionata
        this.ANTS_BEHAVIOUR = Ant.ANT_BEHAVIOUR.RANDOM; // Comportamento predefinito per la simulazione
    }

    @Override
    public void start() throws InterruptedException {

        startupSimulation();
        
        Thread.sleep(100);

        initialForm();
    }

    @Override
    protected synchronized void startupSimulation() {

        System.out.println(new Coord(mapWidth/2, mapHeight/2));

        for (int i = 0; i < NESTS_NUMBER; i++) {
            nests.add(new Nest(ANTS_NUMBER, new Coord(mapWidth/2, mapHeight/2), this));
        }
        for(int i = 0; i < FOODS_NUMBER; i++){
            foods.add(new Food(mapWidth, mapHeight));
        }
    }

    private void initialForm() throws InterruptedException {
        exit = false;

        while (!exit) {
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { }
        }

    }
}