package com.example.model;

import com.example.config.ParameterAdapter;

import javafx.scene.paint.Color;

public class Pheromone {
    
    public enum PheromoneType {
        FOOD_TRAIL,    // Feromone verso il cibo (rilasciato quando torna a casa)
        HOME_TRAIL     // Feromone verso casa (rilasciato quando cerca cibo)
    }

    // Colori dei feromoni (mantenuti uguali)
    public static final Color FOOD_TRAIL_COLOR = Color.color(0.8, 0.3, 0.2);    // Arancione
    public static final Color HOME_TRAIL_COLOR = Color.color(0.2, 0.4, 1.0);    // Blu
    
    // Costanti di configurazione (mantenute uguali)
    public static final int PHEROMONE_SIZE = 2;                         // Dimensione del feromone in pixel
    public static final double MAX_PHEROMONE_TRAIL_DURATION = ParameterAdapter.getMaxPheromoneTrailLength() / ParameterAdapter.getAntSpeed();  // Durata massima di rilascio di feromoni in secondi

    public static Color getColorForType(PheromoneType type) {
        switch(type) {
            case FOOD_TRAIL:
                return FOOD_TRAIL_COLOR;
            case HOME_TRAIL:
                return HOME_TRAIL_COLOR;
            default:
                return Color.BLACK;
        }
    }
    
    public static Color getColorWithAlpha(PheromoneType type, double intensity) {
        Color baseColor = getColorForType(type);
        double alpha = Math.max(0.1, intensity);

        //System.out.println(alpha);

        return new Color(
            baseColor.getRed(),
            baseColor.getGreen(),
            baseColor.getBlue(),
            alpha
        );
    }

    public static double getEvaporationRate() {
        return ParameterAdapter.getPheromoneEvaporationRate();
    }

    public static double getMaxIntensity() {
        return ParameterAdapter.getPheromoneMaxIntensity();
    }

    public static double getMinIntensity() {
        return ParameterAdapter.getPheromoneMinIntensity();
    }

    public static double getInitialIntensity() {
        return ParameterAdapter.getPheromoneInitialIntensity();
    }

    public static double getMaxPheromoneTrailLength() {
        return ParameterAdapter.getMaxPheromoneTrailLength();
    }

    public static double getMaxPheromoneTrailDuration() {
        return ParameterAdapter.getMaxPheromoneTrailDuration();
    }
}