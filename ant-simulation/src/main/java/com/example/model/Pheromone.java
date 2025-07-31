package com.example.model;

import javafx.scene.paint.Color;

/**
 * Classe Pheromone semplificata
 * La logica di gestione è stata spostata in DensityFieldManager per performance
 */
public class Pheromone {
    
    public enum PheromoneType {
        FOOD_TRAIL,    // Feromone verso il cibo (rilasciato quando torna a casa)
        HOME_TRAIL     // Feromone verso casa (rilasciato quando cerca cibo)
    }

    // Colori dei feromoni (mantenuti uguali)
    public static final Color FOOD_TRAIL_COLOR = Color.color(0.6, 0.5, 0.2);    // Arancione
    public static final Color HOME_TRAIL_COLOR = Color.color(0.2, 0.4, 1.0);    // Blu
    
    // Costanti di configurazione (mantenute uguali)
    public static final int PHEROMONE_SIZE = 2;                         // Dimensione del feromone in pixel
    public static final double INITIAL_INTENSITY = 1;                // Intensità iniziale
    public static final double EVAPORATION_RATE = 0.75;                 // 75% evaporazione al secondo
    public static final double MAX_INTENSITY = 1.0;                     // Intensità massima
    public static final double MIN_INTENSITY = 0.1;                    // Intensità minima prima della rimozione
    
    /**
     * Ottieni colore per tipo di feromone
     */
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
    
    /**
     * Ottieni colore con alpha per rendering fluido
     */
    public static Color getColorWithAlpha(PheromoneType type, double intensity, double maxIntensity) {
        Color baseColor = getColorForType(type);
        double alpha = Math.min(0.8, intensity / maxIntensity);
        
        return new Color(
            baseColor.getRed(),
            baseColor.getGreen(),
            baseColor.getBlue(),
            alpha
        );
    }
}