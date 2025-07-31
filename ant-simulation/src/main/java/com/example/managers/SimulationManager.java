package com.example.managers;

import java.util.List;

import com.example.graphics.GameCanvas;
import com.example.graphics.PheromoneRenderer;
import com.example.model.Ant;
import com.example.model.Food;
import com.example.model.FoodClump;
import com.example.model.Nest;
import com.example.model.Pheromone;
import com.example.simulation.Simulation;

import javafx.animation.AnimationTimer;

/**
 * Manager che coordina simulazione e rendering
 */
public class SimulationManager {
    

    private static final double FRAME_SKIP = 3;         // Aggiorna i feromoni ogni 3 frame per ridurre il carico

    private Simulation currentSimulation;
    private GameCanvas canvas;
    private AnimationTimer gameLoop;
    private AnimationTimer pausedGameLoop = null;
    private boolean paused = false;
    private boolean running;
    private long lastUpdate;

    private long frameCount;
    private double fps;
    private long fpsLastTime = 0;

    private final MultiHashGrid gameObjectGrid = new MultiHashGrid();        
    private final PheromoneRenderer pheromoneRenderer = new PheromoneRenderer();

    private boolean pheromonesEnabled = true;                               
    
    private StatsUpdateCallback statsCallback;                              
    
    public interface StatsUpdateCallback {
        void updateStats(long activeAnts, long activeFood, double fps);
    }
    
    public SimulationManager(GameCanvas canvas) {
        this.canvas = canvas;
        this.running = false;
        this.lastUpdate = 0;
        this.frameCount = 0;
        this.fps = 0;
    }
    
    public void setStatsUpdateCallback(StatsUpdateCallback callback) {
        this.statsCallback = callback;
    }

    /**
     * Avvia una simulazione
     */
    public void startSimulation(Simulation simulation) {
        stopSimulation();
        
        this.currentSimulation = simulation;
        this.running = true;
        
        // Avvia il game loop
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                
                update(deltaTime);
                render();
                updateFPS(now);
                updateStatsCallback();
            }
        };
        
        gameLoop.start();
        
        // Avvia thread simulazione
        Thread simulationThread = new Thread(() -> {
            try {
                simulation.start();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        simulationThread.setDaemon(true);
        simulationThread.start();
    }
    
    public void pauseSimulation() {
        if (!running || paused) return;
        
        paused = true;
        
        if (gameLoop != null) {
            gameLoop.stop();
            pausedGameLoop = gameLoop;
            gameLoop = null;
        }
        
        System.out.println("Simulazione messa in pausa");
    }

    public void resumeSimulation() {
        if (!running || !paused) return;
        
        paused = false;
        
        if (pausedGameLoop != null) {
            gameLoop = pausedGameLoop;
            pausedGameLoop = null;
            lastUpdate = 0;
            gameLoop.start();
        }
        
        System.out.println("Simulazione ripresa");
    }

    /**
     * Ferma la simulazione corrente
     */
    public void stopSimulation() {
        running = false;
        
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        
        if (currentSimulation != null) {
            currentSimulation.setExit(true);
            try {
                currentSimulation.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Aggiorna la logica della simulazione
     */
    private void update(double deltaTime) {
        if (!running || currentSimulation == null) return;

        long updateStart = System.nanoTime();
        
        // Pulisci griglia gameObjects
        gameObjectGrid.clear();

        // Ottieni liste
        var ants = currentSimulation.getAnts();
        var foods = currentSimulation.getFoods();
        var nests = currentSimulation.getNests();
        var foodClumps = currentSimulation.getFoodClumps();

        // Popola griglia
        gameObjectGrid.addAnts(ants);
        gameObjectGrid.addFoods(foods);

        // Aggiorna nidi
        for (Nest nest : nests) {
            nest.update(deltaTime);
        }

        // Aggiorna formiche
        for (Ant ant : ants) {
            ant.update(deltaTime);

            // Rilascia feromoni nel density field

            if (this.pheromonesEnabled) releasePheromone(ant);
        }
        
        // Gestisci interazioni cibo
        handleFoodInteraction(ants);

        // Aggiorna food clumps
        for (FoodClump foodClump : foodClumps) {
            foodClump.update(deltaTime);
        }

        long pheromoneStart = System.nanoTime();

        // Aggiorna density field
        if (this.pheromonesEnabled && currentSimulation.getDensityManager() != null) {
            if (frameCount % FRAME_SKIP != 0) return;                // Aggiorna feromoni ogni FRAME_SKIP frame

            currentSimulation.getDensityManager().update(deltaTime * FRAME_SKIP);
            
            if (frameCount % 120 == 0) { // Debug ogni 2 secondi
                DensityFieldManager manager = currentSimulation.getDensityManager();
                System.out.printf("Active density cells: %d | Avg Food: %.2f | Avg Home: %.2f\n",
                    manager.getTotalActiveCells(),
                    manager.getAverageIntensity(Pheromone.PheromoneType.FOOD_TRAIL),
                    manager.getAverageIntensity(Pheromone.PheromoneType.HOME_TRAIL));
            }
        }
        
        long pheromoneTime = System.nanoTime() - pheromoneStart;
        long totalTime = System.nanoTime() - updateStart;

        // Debug performance ogni 60 frame
        if (frameCount % 60 == 0) {
            System.out.printf("UPDATE - Total: %.2fms | Density: %.2fms\n",
                totalTime / 1_000_000.0, pheromoneTime / 1_000_000.0);
        }
    }

    /**
     * Rilascio feromoni
     */
    private void releasePheromone(Ant ant) {
        if (!ant.isEnabled()) return;

        DensityFieldManager densityManager = currentSimulation.getDensityManager();
        if (densityManager == null) return;

        if (ant.hasFoodLoad()) {
            // Rilascia FOOD_TRAIL quando torna a casa con cibo
            densityManager.addPheromone(ant, Pheromone.PheromoneType.FOOD_TRAIL);
        } else {
            // Rilascia HOME_TRAIL quando cerca cibo
            densityManager.addPheromone(ant, Pheromone.PheromoneType.HOME_TRAIL);
        }
    }

    private void handleFoodInteraction(List<Ant> ants) {
        for (Ant ant : ants) {
            if (!ant.isEnabled() || ant.hasFoodLoad()) continue;

            List<Food> nearbyFoods = gameObjectGrid.getNearFood(ant.getPos());
            
            for (Food food : nearbyFoods) {
                if (!food.isEnabled()) continue;

                double distance = ant.getPos().distance(food.getPos());
                if (distance < ant.getSize() + food.getSize() / 2) {
                    ant.pickupFood(food);
                    food.disable();
                    updateFoodClump(food);
                    break;
                }
            }
        }
    }

    private void updateFoodClump(Food takenFood) {
        if (currentSimulation == null) return;
        
        List<FoodClump> foodClumps = currentSimulation.getFoodClumps();
        
        for (FoodClump clump : foodClumps) {
            if (!clump.isEnabled()) continue;
            
            for (Food clumpFood : clump.getFoodPieces()) {
                if (clumpFood.getSerialNumber() == takenFood.getSerialNumber()) {
                    clump.removeFood(takenFood);
                    return;
                }
            }
        }
    }
    
    /**
     * Rendering principale
     */
    private void render() {
        if (!running || currentSimulation == null) return;
        
        long renderStart = System.nanoTime();
        
        // Pulisci canvas
        canvas.clear();

        // Ottieni liste
        var foods = currentSimulation.getFoods();
        var ants = currentSimulation.getAnts();
        var nests = currentSimulation.getNests();
        var foodClumps = currentSimulation.getFoodClumps();

        long pheromoneRenderStart = System.nanoTime();

        // Renderizza density field
        if (pheromonesEnabled && currentSimulation.getDensityManager() != null) {
            pheromoneRenderer.renderDensityTrails(canvas, currentSimulation.getDensityManager());
        }
        
        long pheromoneRenderTime = System.nanoTime() - pheromoneRenderStart;
        long otherRenderStart = System.nanoTime();

        // Renderizza altri oggetti
        if (foodClumps.isEmpty()) {
            canvas.renderFood(foods);
        } else {
            canvas.renderFoodClumps(foodClumps);
        }

        canvas.renderAnts(ants);
        canvas.renderNests(nests);

        long otherRenderTime = System.nanoTime() - otherRenderStart;
        long totalTime = System.nanoTime() - renderStart;

        // Debug performance rendering
        if (frameCount % 60 == 0) {
            System.out.printf("HYBRID RENDER - Total: %.2fms | Trails: %.2fms | Others: %.2fms\n", totalTime / 1_000_000.0, pheromoneRenderTime / 1_000_000.0, otherRenderTime / 1_000_000.0);
        }
    }

    private void updateStatsCallback() {
        if (statsCallback != null && currentSimulation != null) {
            long activeAnts = currentSimulation.getAnts().stream()
                .mapToLong(ant -> ant.isEnabled() ? 1 : 0).sum();
            long activeFood = currentSimulation.getFoods().stream()
                .mapToLong(food -> food.isEnabled() ? 1 : 0).sum();
            
            statsCallback.updateStats(activeAnts, activeFood, fps);
        }
    }

    public void togglePause() {
        if (paused) {
            resumeSimulation();
        } else {
            pauseSimulation();
        }
    }

    public boolean isPaused() { 
        return paused; 
    }

    public boolean isRunning() { 
        return running && !paused; 
    }

    /**
     * Aggiorna calcolo FPS
     */
    private void updateFPS(long now) {
        frameCount++;
        
        if (fpsLastTime == 0) {
            fpsLastTime = now;
        }
        
        long elapsedTime = now - fpsLastTime;
        
        if (elapsedTime >= 1_000_000_000) {
            fps = frameCount;
            frameCount = 0;
            fpsLastTime = now;
        }
    }
    
    // Getters
    public Simulation getCurrentSimulation() { return currentSimulation; }
    public double getFPS() { return fps; }
}