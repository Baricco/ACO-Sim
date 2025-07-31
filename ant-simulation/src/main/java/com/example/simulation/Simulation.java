package com.example.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.graphics.Coord;
import com.example.managers.DensityFieldManager;
import com.example.model.Ant;
import com.example.model.Food;
import com.example.model.FoodClump;
import com.example.model.Nest;

public class Simulation {

    public static final int REFRESH_RATE = 50;

    public final int ANTS_NUMBER;
    public final int FOODS_NUMBER;
    public final int NESTS_NUMBER;
    public final int FOOD_CLUMP_NUMBER;
    public final int FOOD_CLUMP_SIZE;          // Numero di pezzi di cibo in un ammasso

    protected List<Nest> nests = Collections.synchronizedList(new ArrayList<>());
    protected List<Ant> ants = Collections.synchronizedList(new ArrayList<>());
    protected List<Food> foods = Collections.synchronizedList(new ArrayList<>());
    protected List<FoodClump> foodClumps = Collections.synchronizedList(new ArrayList<>());

    protected DensityFieldManager densityManager;

    protected boolean exit = false;

    protected double mapWidth;
    protected double mapHeight;

    public Simulation(int nestsNumber, int antsNumber, int foodsNumber, double mapWidth, double mapHeight) {
        this.NESTS_NUMBER = nestsNumber;
        this.ANTS_NUMBER = antsNumber;
        this.FOODS_NUMBER = foodsNumber;
        this.FOOD_CLUMP_NUMBER = 0;                     // Non usato in questa simulazione
        this.FOOD_CLUMP_SIZE = 0;                       // Non usato in questa simulazione
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    public Simulation(int nestsNumber, int antsNumber, int foodClumpSize, int foodClumpNumber, double mapWidth, double mapHeight) {
        this.NESTS_NUMBER = nestsNumber;
        this.ANTS_NUMBER = antsNumber;
        this.FOOD_CLUMP_SIZE = foodClumpSize;
        this.FOOD_CLUMP_NUMBER = foodClumpNumber;
        this.FOODS_NUMBER = foodClumpSize * foodClumpNumber; // Numero totale di pezzi di cibo
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }


    public void start() throws InterruptedException {
        throw new IllegalArgumentException("Tipo di simulazione non supportato: ");
    }

    public void initDensityManager() {
        this.densityManager = new DensityFieldManager(this.mapWidth, this.mapHeight);
    }

    public DensityFieldManager getDensityManager() {
        return densityManager;
    }

    public void updateDensityField(double deltaTime) {
        if (densityManager == null) return;

        densityManager.update(deltaTime);
    }

    public synchronized void reset() throws InterruptedException {

        for (Nest nest : nests) {
            nest.disable();
        }

        nests.clear();

        for (Ant ant : ants) {
            ant.disable();
        }

        ants.clear();

        for (Food food : foods) {
            food.disable();
        }

        foods.clear();

        for (FoodClump foodClump : foodClumps) {
            foodClump.disable();
        }

        foodClumps.clear();

        if (densityManager != null) {
            densityManager.clear();
        }

    }

    public synchronized void addFood(Food food) {
        this.foods.add(food);
    }

    public synchronized void addAnt(Ant ant) {
        this.ants.add(ant);
    }

    protected synchronized void startupSimulation() {

        System.out.println(new Coord(mapWidth/2, mapHeight/2));


        for (int i = 0; i < NESTS_NUMBER; i++) {
            nests.add(new Nest(ANTS_NUMBER, new Coord(mapWidth/2, mapHeight), this));

        }
        for(int i = 0; i < FOOD_CLUMP_NUMBER; i++){
            foodClumps.add(new FoodClump(FOOD_CLUMP_SIZE, mapWidth, mapHeight, this));
        }
    }

    public List<Nest> getNests() { 
        synchronized(nests) {
            return new ArrayList<>(nests);
        }
    }

    public List<FoodClump> getFoodClumps() { 
        synchronized(foodClumps) {
            return new ArrayList<>(foodClumps);
        }
    }

    public List<FoodClump> getOriginalFoodClumps() { 
    synchronized(foodClumps) {
        return foodClumps; 
    }
}

    public List<Ant> getAnts() { 
        synchronized(ants) {
            return new ArrayList<>(ants);
        }
    }

    public List<Food> getFoods() { 
        synchronized(foods) {
            return new ArrayList<>(foods);
        }
    }
    
    public void setExit(boolean exit) { this.exit = exit; }
    public boolean isExit() { return exit; }
    
    public double getMapWidth() { return mapWidth; }
    public double getMapHeight() { return mapHeight; }
}