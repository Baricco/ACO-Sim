package com.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.graphics.Coord;
import com.example.simulation.Simulation;

public class FoodClump extends GameObject {

    private int initialFoodCount;
    private List<Food> foodPieces;
    private final Simulation simulationParent;
    private static final int CLUMP_SIZE = 100;  // Dimensione dell'ammasso di cibo in pixel
    private static final int MAX_ATTEMPTS = 500; // Numero massimo di tentativi per trovare una posizione valida
    private static final int MIN_DISTANCE = 200; // Distanza minima tra i clump
    private static final Random RANDOM = new Random();
    private boolean hasSpawned = false;

    public FoodClump(Coord pos, int foodNumber, Simulation simulationParent) {
        super(pos, GameObjType.FOOD_CLUMP, GameObject.getNewSerialNumber(), CLUMP_SIZE);
        this.simulationParent = simulationParent;
        this.foodPieces = new ArrayList<>();
        this.initialFoodCount = foodNumber;
        this.hasSpawned = false;
    }

    public FoodClump(int foodNumber, double mapWidth, double mapHeight, Simulation simulationParent) {
        super(findValidPosition(mapWidth, mapHeight, simulationParent), GameObjType.FOOD_CLUMP, GameObject.getNewSerialNumber(), CLUMP_SIZE);
        this.initialFoodCount = foodNumber;
        this.foodPieces = new ArrayList<>();
        this.simulationParent = simulationParent;
        this.hasSpawned = false;
    }

    private static Coord findValidPosition(double mapWidth, double mapHeight, Simulation parent) {

        double minHeight = 300;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Coord candidate = GameObject.generateRandomPosition(mapWidth, mapHeight - minHeight, CLUMP_SIZE);
            
            boolean validPosition = true;
            for (FoodClump existing : parent.getOriginalFoodClumps()) {
                if (candidate.distance(existing.getCenter()) < MIN_DISTANCE + CLUMP_SIZE * 2) {
                    validPosition = false;
                    break;
                }
            }
            
            if (validPosition) return candidate;
        }

        return GameObject.generateRandomPosition(mapWidth, mapHeight - minHeight, CLUMP_SIZE);  // Fallback: posizione casuale
    }

    public void spawnFood() {
        if (hasSpawned) return; // Evita spawn multipli
        
        // Genera i pezzi di cibo in un'area circolare attorno al centro del clump
        double clumpRadius = CLUMP_SIZE / 2.0;
        
        for (int i = 0; i < initialFoodCount; i++) {
            // Genera posizione casuale nel raggio del clump
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = RANDOM.nextDouble() * clumpRadius;
            
            Coord spawnPos = new Coord(
                this.pos.x + Math.cos(angle) * distance,
                this.pos.y + Math.sin(angle) * distance
            );
            
            Food food = new Food(spawnPos);
            foodPieces.add(food);
            simulationParent.addFood(food);
        }
        
        hasSpawned = true;
    }

    public int getCurrentFoodCount() {
        return (int) foodPieces.stream().filter(GameObject::isEnabled).count();
    }

    public int getInitialFoodCount() {
        return initialFoodCount;
    }

    public void removeFood(Food food) {
        foodPieces.remove(food);
    }

    public List<Food> getFoodPieces() {
        return new ArrayList<>(foodPieces); // Ritorna una copia per sicurezza
    }

    @Override
    public void update(double deltaTime) {
        if (!isEnabled()) return;

        // Se è il primo giro, spawna i pezzi di cibo
        if (!hasSpawned) {
            spawnFood();
        }

        // Verifica se tutti i pezzi di cibo sono stati raccolti
        long remainingFood = getCurrentFoodCount();
        
        if (remainingFood <= 0 && hasSpawned) {
            this.enabled = false; // Disabilita l'ammasso se non ci sono più pezzi di cibo
            System.out.println("FoodClump " + serialNumber + " is now empty and disabled");
        }
    }
}