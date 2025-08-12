package com.example.graphics;

import java.util.Random;

import com.example.managers.DensityFieldManager;
import com.example.model.Pheromone;

import javafx.scene.paint.Color;

public class PheromoneRenderer {
    
    private static final Random RANDOM = new Random();

    private static final double RANDOM_OFFSET = 0.75;           // Per jitter casuale
    private static final double SAMPLING_FACTOR = 0.5;          // Riduce il numero di punti renderizzati
    private static final double ALPHA_FACTOR = 1;            // Fattore di opacità per i feromoni

    private boolean renderingEnabled = true;
    
    /**
     * RENDERING PRINCIPALE - Renderizza scie di feromoni come punti
     */
    public void renderDensityTrails(GameCanvas canvas, DensityFieldManager densityManager) {
        if (!renderingEnabled) return;

        renderDots(canvas, densityManager);
    }
    
    
    /**
     * Renderizza i punti di feromone come cerchi
     */
    private void renderDots(GameCanvas canvas, DensityFieldManager densityManager) {
        
        double[][] foodField = densityManager.getFoodDensity();
        double[][] homeField = densityManager.getHomeDensity();
        
        double cellSize = densityManager.getCellSize();
        double gridWidth = densityManager.getGridWidth();
        double gridHeight = densityManager.getGridHeight();


        // Sampling più denso per catturare tutti i feromoni
        int step = Math.max(1, (int)(cellSize * SAMPLING_FACTOR));

        for (int x = 0; x < gridWidth; x += step) {
            for (int y = 0; y < gridHeight; y += step) {

                // Food dots
                double foodIntensity = foodField[x][y];
                if (foodIntensity > Pheromone.MIN_INTENSITY) {
                    renderDot(canvas, x * cellSize, y * cellSize, foodIntensity, Pheromone.PheromoneType.FOOD_TRAIL);
                }
                
                // Home dots
                double homeIntensity = homeField[x][y];
                if (homeIntensity > Pheromone.MIN_INTENSITY) {
                    renderDot(canvas, x * cellSize, y * cellSize, homeIntensity, Pheromone.PheromoneType.HOME_TRAIL);
                }
            }
        }
    }
    
    private void renderDot(GameCanvas canvas, double x, double y, double intensity, Pheromone.PheromoneType type) {
        // Raggio basato su intensità
        double radius = Pheromone.PHEROMONE_SIZE * (intensity / Pheromone.MAX_INTENSITY);

        Color color = Pheromone.getColorWithAlpha(type, intensity * ALPHA_FACTOR);

        //System.out.println(intensity*ALPHA_FACTOR);

        canvas.renderCircle(x + RANDOM.nextGaussian() * RANDOM_OFFSET, y + RANDOM.nextGaussian() * RANDOM_OFFSET, radius, color);
    }
    
    // Getters/Setters
    public void setRenderingEnabled(boolean enabled) { this.renderingEnabled = enabled; }
}