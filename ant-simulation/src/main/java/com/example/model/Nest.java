package com.example.model;

import com.example.graphics.Coord;
import static com.example.model.GameObject.GameObjType.NEST;
import com.example.simulation.Simulation;

public class Nest extends GameObject {

    private static final int NEST_SIZE = 50;

    private final Simulation simulationParent;

    private final Ant.ANT_BEHAVIOUR antsBehaviour;

    private int foodCount;          // Numero di pezzi di cibo portati al nido

    private int antNumber;

    private boolean stopped = false;

    public Nest(Coord pos, int antNumber, int serialNumber, int size, Simulation simulationParent) {
        super(pos, NEST, serialNumber, size);
        this.antNumber = antNumber;
        this.simulationParent = simulationParent;
        this.foodCount = 0; // Inizializza il conteggio del cibo
        this.antsBehaviour = simulationParent.getBehaviour();
    }

    public Nest(int antNumber, Coord pos, Simulation simulationParent) {
        this(pos, antNumber, GameObject.getNewSerialNumber(), NEST_SIZE, simulationParent);
    }


    public void spawnAnts() {
        for (int i = 0; i < antNumber; i++) {
            
        Coord spawnPos = new Coord(
            this.pos.x - Ant.ANT_SIZE / 2.0,
            this.pos.y - Ant.ANT_SIZE / 2.0
        );

            simulationParent.addAnt(new Ant(spawnPos, simulationParent.getMapWidth(), simulationParent.getMapHeight(), this));
        }
    }


    public void incrementFoodCount() {
        this.foodCount++;
    }

    public int getFoodCount() {
        return this.foodCount;
    }

    private void stop() { this.stopped = true; }

    private boolean isStopped() { return this.stopped; }

    @Override
    public void update(double deltaTime) {

        // Se il nido è disabilitato, non fare nulla        
        if (!isEnabled() || isStopped()) return;

        // Se è il primo giro, spawn delle formiche
        spawnAnts();

        // ferma il nido dopo aver spawnato le formiche
        this.stop();
   
    }

    Ant.ANT_BEHAVIOUR getBehaviour() {
        return this.antsBehaviour;
    }

}