package com.example.managers;

import java.util.Random;
import java.util.stream.IntStream;

import com.example.graphics.Coord;
import com.example.model.Ant;
import com.example.model.Pheromone;


public class DensityFieldManager {
    
    // Configurazione griglia
    public static final double CELL_SIZE = Pheromone.PHEROMONE_SIZE; // Pixel per cella
    private final int gridWidth, gridHeight;
    
    // Campi di densità separati per tipo
    private final double [][] foodDensity;
    private final double [][] homeDensity;
    
    // Parametri di simulazione
    private static final double  MAX_INTENSITY = Pheromone.MAX_INTENSITY;
    private static final double  MIN_INTENSITY = Pheromone.MIN_INTENSITY;
    
    // Costanti per limitare il piazzamento dei feromoni
    private static final double MIN_DISTANCE_BETWEEN_PHEROMONES = Ant.ANT_SPEED * 2;
    private static final double JITTER_RADIUS = MIN_DISTANCE_BETWEEN_PHEROMONES * 0.15;
    private static final double MIN_TIME_BETWEEN_PHEROMONES = 0.025; // secondi
    
    private static final Random RANDOM = new Random();

    public DensityFieldManager(double mapWidth, double mapHeight) {
        this.gridWidth = (int) Math.ceil(mapWidth / CELL_SIZE);
        this.gridHeight = (int) Math.ceil(mapHeight / CELL_SIZE);
        
        // Inizializza griglie
        this.foodDensity = new double [gridWidth][gridHeight];
        this.homeDensity = new double [gridWidth][gridHeight];
        
        System.out.printf("DensityFieldManager initialized: %dx%d grid (%.1f cell size)\n", 
            gridWidth, gridHeight, CELL_SIZE);
    }
    
    /**
     * Aggiunge feromone al campo di densità
     */
    public void addPheromone(Coord pos, Pheromone.PheromoneType type, double  intensity) {
        int x = (int) (pos.x / CELL_SIZE);
        int y = (int) (pos.y / CELL_SIZE);
        
        if (isValidCell(x, y)) {
            double [][] targetField = getDensityField(type);
                
            targetField[x][y] = Math.min(MAX_INTENSITY, targetField[x][y] + intensity);
        }
    }
    

    public void addPheromone(Ant ant, Pheromone.PheromoneType type) {
        
        if (!shouldPlacePheromone(ant)) return;
        
        
        // Aggiunge jitter per un aspetto più organico
        Coord jitteredPosition = addJitter(ant.getCenter());
    
        // Aggiunge feromone alla griglia
        addPheromone(jitteredPosition, type, Pheromone.INITIAL_INTENSITY);
        
        // Aggiorna lo stato della formica
        ant.setLastPheromonePosition(jitteredPosition);
        ant.setLastPheromoneTime(getCurrentTime());
    }
    
    /**
     * CONTROLLO DISTANZA - Previene il "pheromone spam"
     */
    private boolean shouldPlacePheromone(Ant ant) {
        Coord lastPos = ant.getLastPheromonePosition();
        double lastTime = ant.getLastPheromoneTime();
        
        if (lastPos == null) return true;
        
        // Distanza minima
        double distance = ant.getCenter().distance(lastPos);
        if (distance < MIN_DISTANCE_BETWEEN_PHEROMONES) {
            return false;
        }
        
        // Tempo minimo
        double timeSinceLastPheromone = getCurrentTime() - lastTime;
        return timeSinceLastPheromone > MIN_TIME_BETWEEN_PHEROMONES;
    }

    /**
     * JITTER - Aggiunge casualità per un aspetto più organico
     */
    private Coord addJitter(Coord originalPosition) {
        // Offset casuale in cerchio
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double distance = RANDOM.nextDouble() * JITTER_RADIUS;
        
        return new Coord(
            originalPosition.x + Math.cos(angle) * distance,
            originalPosition.y + Math.sin(angle) * distance
        );
    }
    
    private double getCurrentTime() {
        return System.currentTimeMillis() / 1000.0;
    }
    
    public double[][] getDensityField(Pheromone.PheromoneType type) {
        switch(type) {
            case FOOD_TRAIL:
                return foodDensity;
            case HOME_TRAIL:
                return homeDensity;
            default:
                throw new IllegalArgumentException("Unknown pheromone type: " + type);
        }
    }

    /**
     * Aggiorna tutti i campi di densità
     */
    public void update(double deltaTime) {
        updateDensityField(foodDensity, deltaTime);
        updateDensityField(homeDensity, deltaTime);
    }
    
    /**
     * Aggiorna campo con decay
     */
    private void updateDensityField(double [][] field, double deltaTime) {
        
        double frameDecay = Math.pow(Pheromone.EVAPORATION_RATE, deltaTime);

        // evaporazione
        IntStream.range(0, gridWidth).parallel().forEach(x -> {
            for (int y = 0; y < gridHeight; y++) {
                
                // Non processare celle sotto soglia
                if (field[x][y] < MIN_INTENSITY) continue;
                
                // Applica decay
                field[x][y] *= frameDecay;

                // Pulisci sotto soglia
                if (field[x][y] < MIN_INTENSITY) field[x][y] = 0;
            }
        });
    }
    
    /**
     * Ottieni intensità totale in una posizione - per navigazione formiche
     */
    public double getTotalIntensity(Coord position, Pheromone.PheromoneType type) {
        int x = (int) (position.x / CELL_SIZE);
        int y = (int) (position.y / CELL_SIZE);
        
        if (!isValidCell(x, y)) return 0;
        
            double [][] field = getDensityField(type);
            
        return field[x][y];
    }
    
    /**
     * Calcola gradiente per navigazione formiche
     */
    public Coord getPheromoneGradient(Coord position, Pheromone.PheromoneType type) {
        int x = (int) (position.x / CELL_SIZE);
        int y = (int) (position.y / CELL_SIZE);

        double [][] field = getDensityField(type);
        
        // Sampling 3x3 invece di semplice differenza finita per maggiore precisione
        double totalDx = 0, totalDy = 0;
        int samples = 0;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                double value = getFieldValue(field, x + dx, y + dy);
                if (value > MIN_INTENSITY) {
                    totalDx += dx * value;
                    totalDy += dy * value;
                    samples++;
                }
            }
        }
        
        if (samples == 0) return new Coord(0, 0);
        
        // Normalizza e scala
        Coord gradient = new Coord(totalDx / samples, totalDy / samples);
        gradient.normalize();
        gradient.multiply(Ant.ANT_SPEED * 1.5); // Boost per seguire meglio i feromoni
        
        return gradient;
    }
    
    /**
     * Ottieni valore sicuro dal campo (0 se fuori bounds)
     */
    private double getFieldValue(double [][] field, int x, int y) {
        return isValidCell(x, y) ? field[x][y] : 0;
    }
    
    /**
     * Verifica validità coordinate cella
     */
    private boolean isValidCell(int x, int y) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
    }
    
    /**
     * Pulisce tutti i campi (per reset simulazione)
     */
    public void clear() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                foodDensity[x][y] = 0;
                homeDensity[x][y] = 0;
            }
        }
    }
    
    // Getters per renderer
    public double [][] getFoodDensity() { return foodDensity; }
    public double [][] getHomeDensity() { return homeDensity; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public double getCellSize() { return CELL_SIZE; }
    
    // Metodi di utilità per debug
    public int getTotalActiveCells() {
        int count = 0;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (foodDensity[x][y] > MIN_INTENSITY || homeDensity[x][y] > MIN_INTENSITY) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public double getAverageIntensity(Pheromone.PheromoneType type) {
        double [][] field = getDensityField(type);
            
        double total = 0;
        int count = 0;
        
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (field[x][y] > MIN_INTENSITY) {
                    total += field[x][y];
                    count++;
                }
            }
        }
        
        return count > 0 ? total / count : 0;
    }
}