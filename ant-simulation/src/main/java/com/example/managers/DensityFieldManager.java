package com.example.managers;

import java.util.Random;
import java.util.stream.IntStream;

import com.example.config.ParameterAdapter;
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
        
    // Costanti per limitare il piazzamento dei feromoni
    private static final double MIN_DISTANCE_BETWEEN_PHEROMONES = Pheromone.PHEROMONE_SIZE; // Distanza minima tra i feromoni
    private static final double JITTER_RADIUS = MIN_DISTANCE_BETWEEN_PHEROMONES * 0.15;
    private static final double MIN_TIME_BETWEEN_PHEROMONES = MIN_DISTANCE_BETWEEN_PHEROMONES / ParameterAdapter.getAntSpeed(); // secondi
    private static final double FOOD_PHEROMONES_BOOSTER = 1.75;

    private static final double[][] GAUSSIAN_KERNEL = {             // Kernel gaussiano 3x3 pre-calcolato
        {0.077847, 0.123317, 0.077847},
        {0.123317, 0.195346, 0.123317}, 
        {0.077847, 0.123317, 0.077847}
    };

    private double totalFoodIntensity;
    private double totalHomeIntensity;

    private static final Random RANDOM = new Random();

    public DensityFieldManager(double mapWidth, double mapHeight) {
        this.gridWidth = (int) Math.ceil(mapWidth / CELL_SIZE);
        this.gridHeight = (int) Math.ceil(mapHeight / CELL_SIZE);
        
        // Inizializza griglie
        this.foodDensity = new double [gridWidth][gridHeight];
        this.homeDensity = new double [gridWidth][gridHeight];
        
        System.out.printf("DensityFieldManager initialized: %dx%d grid (%.1f cell size)\n", 
            gridWidth, gridHeight, CELL_SIZE);

        this.totalFoodIntensity = 0;
        this.totalHomeIntensity = 0;
    }
    
    /**
     * Aggiunge feromone al campo di densità
     */
    public void addPheromone(Coord pos, Pheromone.PheromoneType type, double  intensity) {

        if (intensity <= ParameterAdapter.getPheromoneMinIntensity()) return;

        int x = (int) (pos.x / CELL_SIZE);
        int y = (int) (pos.y / CELL_SIZE);

        if (type == Pheromone.PheromoneType.FOOD_TRAIL) intensity *= FOOD_PHEROMONES_BOOSTER;

        if (isValidCell(x, y)) {
            double [][] targetField = getDensityField(type);
                
            targetField[x][y] = Math.min(ParameterAdapter.getPheromoneMaxIntensity(), targetField[x][y] + intensity);
        }

        switch(type) {
            case FOOD_TRAIL:
                totalFoodIntensity += intensity;
                break;
            case HOME_TRAIL:
                totalHomeIntensity += intensity;
                break;
        }
    }

    public void addPheromone(Ant ant, Pheromone.PheromoneType type) {
        
        if (!shouldPlacePheromone(ant)) return;
        
        
        // Aggiunge jitter per un aspetto più organico
        Coord jitteredPosition = addJitter(ant.getCenter());
    
        // Aggiunge feromone alla griglia
        addPheromone(jitteredPosition, type, calcPheromoneIntensity(ant));
        
        // Aggiorna lo stato della formica
        ant.setLastPheromonePosition(jitteredPosition);
        ant.setLastPheromoneTime(System.nanoTime());


    }

    private double calcPheromoneIntensity(Ant ant) {

        // Sistema che usa il tempo

        double lastMilestoneTime = ant.getStartTrackTime();

        if (lastMilestoneTime <= 0) return ParameterAdapter.getPheromoneInitialIntensity(); // Se non c'è milestone, usa intensità iniziale QUESTA RIGA VA CAMBIATA

        double timeSinceLastMilestone = System.nanoTime() - lastMilestoneTime;

        timeSinceLastMilestone /= 1_000_000_000.0; // Converti nanosecondi in secondi

        double normalizedTime = Math.min(timeSinceLastMilestone / Pheromone.MAX_PHEROMONE_TRAIL_DURATION, 1.0);

        if (normalizedTime >= 1.0) return 0;

        //System.out.printf("Ant %d pheromone intensity: %.2f (time: %.2f)\n", ant.getSerialNumber(), Pheromone.INITIAL_INTENSITY * (1 - normalizedTime), timeSinceLastMilestone);

        return Math.max(ParameterAdapter.getPheromoneMinIntensity(), ParameterAdapter.getPheromoneInitialIntensity() * (1 - normalizedTime));

        /*

        // Sistema che usa la distanza (distanza dalla milestone, non distanza percorsa, infatti era sbagliato)

        Coord milestonePos = ant.getLastMilestonePosition();

        if (milestonePos == null) return Pheromone.INITIAL_INTENSITY; // Se non c'è milestone, usa intensità iniziale

        // calcola la distanza normalizzata tra la posizione della formica e la milestone
        double normalizedDistance = Math.min(ant.getCenter().distance(milestonePos) / Pheromone.MAX_PHEROMONE_TRAIL_DISTANCE, 1.0);

        // Calcola l'intensità in base alla distanza normalizzata
        return Math.max(Pheromone.MIN_INTENSITY, Pheromone.INITIAL_INTENSITY * (1 - normalizedDistance));
        */
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
        double timeSinceLastPheromone = System.nanoTime() - lastTime;
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

        double frameDecay = Math.pow(ParameterAdapter.getPheromoneEvaporationRate(), deltaTime);

        // evaporazione
        IntStream.range(0, gridWidth).parallel().forEach(x -> {
            for (int y = 0; y < gridHeight; y++) {
                
                // Non processare celle sotto soglia
                if (field[x][y] < ParameterAdapter.getPheromoneMinIntensity()) continue;

                // Applica decay
                field[x][y] *= frameDecay;

                // Pulisci sotto soglia
                if (field[x][y] < ParameterAdapter.getPheromoneMinIntensity()) field[x][y] = 0;
            }
        });

        this.totalFoodIntensity *= frameDecay;
        this.totalHomeIntensity *= frameDecay;

        // diffusione
        applyDiffusion(field, deltaTime);

    }
    

    private void applyDiffusion(double[][] field, double deltaTime) {
        // Calcola intensità diffusione basata su deltaTime
        // Più deltaTime = più diffusione per mantenere consistenza temporale

        double diffusion = ParameterAdapter.getDiffusionRate() * deltaTime;
        
        // Array temporaneo per evitare race conditions durante il calcolo
        // Non possiamo modificare 'field' mentre lo leggiamo
        double[][] tempField = new double[gridWidth][gridHeight];
        
        // Itera su ogni cella della griglia
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                
                // Skip celle vuote per ottimizzazione - non calcolare diffusione su zero
                if (field[x][y] < ParameterAdapter.getPheromoneMinIntensity()) {
                    tempField[x][y] = 0; // Assicurati che sia zero
                    continue;
                }
                
                // Applica kernel gaussiano 3x3 centrato sulla cella corrente
                double weightedSum = 0.0;   // Somma pesata usando kernel gaussiano
                double totalWeight = 0.0;   // Peso totale (per normalizzazione ai bordi)
                
                // Itera attraverso il kernel 3x3
                for (int kernelX = 0; kernelX < 3; kernelX++) {
                    for (int kernelY = 0; kernelY < 3; kernelY++) {
                        
                        // Calcola coordinate nella griglia principale
                        // Kernel è centrato, quindi offset di -1
                        int gridX = x + kernelX - 1;  // -1, 0, +1 offset dal centro
                        int gridY = y + kernelY - 1;  // -1, 0, +1 offset dal centro
                        
                        // Verifica bounds - gestisce automaticamente i bordi
                        if (isValidCell(gridX, gridY)) {
                            
                            // Ottieni peso gaussiano pre-calcolato dal kernel
                            double weight = GAUSSIAN_KERNEL[kernelX][kernelY];
                            
                            // Accumula valore pesato della cella vicina
                            weightedSum += field[gridX][gridY] * weight;
                            
                            // Accumula peso totale per normalizzazione
                            totalWeight += weight;
                        }
                    }
                }
                
                // Calcola media pesata gaussiana
                // Se totalWeight < 1.0 significa che siamo al bordo e alcuni vicini mancano
                double gaussianAverage = (totalWeight > 0) ? weightedSum / totalWeight : field[x][y];
                
                // Applica interpolazione lineare tra valore originale e media gaussiana
                // Formula: nuovo = originale * (1-diffusion) + media_gaussiana * diffusion
                // Questo conserva la massa totale del sistema e previene instabilità numeriche
                tempField[x][y] = field[x][y] * (1.0 - diffusion) + gaussianAverage * diffusion;
                
                // Clamp per sicurezza numerica - previene valori negativi o troppo alti
                if (tempField[x][y] < 0) tempField[x][y] = 0;
                if (tempField[x][y] > ParameterAdapter.getPheromoneMaxIntensity()) tempField[x][y] = ParameterAdapter.getPheromoneMaxIntensity();
            }
        }
        
        // Copia risultato finale nel campo originale
        // Usa System.arraycopy per prestazioni ottimali
        for (int x = 0; x < gridWidth; x++) {
            System.arraycopy(tempField[x], 0, field[x], 0, gridHeight);
        }
    }



    /*
     * 
     * 
     * 
     *     public double getTotalIntensity(Coord position, Pheromone.PheromoneType type, double radius) {
        
        int x = (int) (position.x / CELL_SIZE);
        int y = (int) (position.y / CELL_SIZE);
        
        if (!isValidCell(x, y)) return 0;
        
        double [][] field = getDensityField(type);

        double totalIntensity = 0;
        for (double dx = -radius; dx <= radius; dx++) {
            for (double dy = -radius; dy <= radius; dy++) {
                if (Math.abs(dx) + Math.abs(dy) <= radius) {
                    totalIntensity += getFieldValue(field, x + (int)Math.round(dx), y + (int)Math.round(dy));
                }
            }
        }
        return totalIntensity;
    }
     */

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
        
        int delta = ParameterAdapter.getAntFeelRadius();

        int x = (int) (position.x / CELL_SIZE);
        int y = (int) (position.y / CELL_SIZE);

        double [][] field = getDensityField(type);
        
        // Componente X (derivata parziale rispetto a x)
        double gradX = (getFieldValue(field, x+delta, y) - getFieldValue(field, x-delta, y)) / (2.0 * delta);

        // Componente Y (derivata parziale rispetto a y)  
        double gradY = (getFieldValue(field, x, y+delta) - getFieldValue(field, x, y-delta)) / (2.0 * delta);

        // Normalizza e scala
        Coord gradient = new Coord(gradX, gradY);

        gradient.multiply(2);

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
                if (foodDensity[x][y] > ParameterAdapter.getPheromoneMinIntensity() || homeDensity[x][y] > ParameterAdapter.getPheromoneMinIntensity()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public double getAverageIntensity(Pheromone.PheromoneType type) {
        switch(type) {
            case FOOD_TRAIL:
                return this.totalFoodIntensity / (gridWidth * gridHeight);
            case HOME_TRAIL:
                return this.totalHomeIntensity / (gridWidth * gridHeight);
            default:
                return 0;
        }
    }
}