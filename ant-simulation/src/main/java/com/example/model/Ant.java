package com.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.config.ParameterAdapter;
import com.example.config.SimulationParameters;
import com.example.graphics.Coord;
import com.example.graphics.GameCanvas;
import com.example.managers.DensityFieldManager;
import com.example.managers.MultiHashGrid;
import com.example.managers.ObstacleManager;
import com.example.metrics.MetricsCollector;

import javafx.scene.paint.Color;

public class Ant extends GameObject {

    public enum ANT_BEHAVIOUR {
        RANDOM,
        FOOD_PHEROMONE,
        ALL_PHEROMONES              // FOOD + HOME PHEROMONES
    }

    // Costanti
    public static final int ANT_SIZE = 20;

    public static final double TURN_AROUND_ANGLE_OFFSET = Math.PI / 4;  // Offset di 45 gradi nel turn around della formica
    public static final Color ANT_FEEL_COLOR = Color.rgb(255, 255, 0, 0.2); // Colore per il raggio di percezione
    public static final Color ANT_SENSOR_COLOR = Color.rgb(255, 0, 255, 0.2); // Colore per il raggio di sensori
    public static final Color ANT_COLOR = Color.RED;
    public static final int WINDOW_BOUND_MARGIN = -ANT_SIZE / 2;              // Margine per il rimbalzo sui bordi della finestra
    public static final int MAX_FOOD_SEARCH_TIME = 10000;                   // tempo massimo di ricerca del cibo in millisecondi

    private static final Random RANDOM = new Random();
    private static final double SMOOTH_MOVEMENT_FACTOR = 0.2;

    // Stato della formica
    protected Coord direction;
    protected GameObject foodLoad;
    protected double mapWidth;
    protected double mapHeight;
    protected double angle;                                  // Per la rotazione visuale
    protected Nest nest;                                     // Riferimento al nido della formica

    // Comportamento della formica
    protected ANT_BEHAVIOUR behaviour;
    protected DensityFieldManager densityFieldManager;          // Gestore del campo di densità per i feromoni
    protected MultiHashGrid multiHashGrid;                      // Gestiore dei gameObject
    
    // Tracking temporale per feromoni
    private Coord lastPheromonePosition;
    private double lastPheromoneTime = -1;

    // Tracking temporale per scoperta cibo
    private double lastFoodDiscoveryTime = 0;          // quanto tempo ci ha messo a trovare il cibo
    private double lastNestDiscoveryTime = 0;          // quanto tempo ci ha messo a trovare il nido
    private double lastTripTime = 0;                   // quanto tempo ci ha messo a fare un viaggio
    private double meanTripTime = 0;                   // tempo medio di viaggio

    // Tracking delle milestone per intensità feromoni
    private Coord lastMilestonePosition;
    private double lastMilestoneTime;

    private double startTrackTime;                      // Tempo di inizio tracking
    private int tripNumber = 0;                         // Numero di viaggi effettuati

    // Sensori per i feromoni
    private Sensor leftSensor, frontSensor, rightSensor;

    // Memoria della formica
    private double pheromoneMovingAverage = 0;

    // Sistema di Logging
    private List<Coord> pathHistory = new ArrayList<>();
    private long lastPathLogTime = 0;
    private long lastDecisionLogTime = 0;
    private static final long LOG_INTERVAL = 500_000_000; // 0.5 secondi in nanosecondi

    // Sistema di evitamento ostacoli

    private ObstacleManager obstacleManager;


    public Ant(double mapWidth, double mapHeight, Nest nest) {
        super(GameObjType.ANT, GameObject.getNewSerialNumber(), ANT_SIZE, GameObject.generateRandomPosition(mapWidth, mapHeight, ANT_SIZE));
        this.direction = generateRandomVector();
        this.foodLoad = new VoidObj();          // new Food(pos);
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.nest = nest;
        this.angle = 0;
        this.enabled = true;
        this.setSprite(GameCanvas.loadImageCached("antSprite.png"));
        
        // INIZIALIZZA tracking temporale
        this.lastPheromonePosition = null;
        
        // Inizializza i sensori
        this.leftSensor = new Sensor(SimulationParameters.getInstance().getAntSensorAngle());   // 30 gradi a sinistra
        this.frontSensor = new Sensor(0);             // Direzione frontale
        this.rightSensor = new Sensor(-SimulationParameters.getInstance().getAntSensorAngle());  // 30 gradi a destra
    }

    public Ant(Coord position, double mapWidth, double mapHeight, Nest nest) {
        this(mapWidth, mapHeight, nest);
        this.pos = position;
        this.startTrackTime = System.nanoTime(); // Inizializza il tempo di tracking
        this.lastMilestonePosition = position.copy(); // Inizializza la posizione dell'ultima milestone
        this.lastMilestoneTime = System.nanoTime(); // Inizializza il tempo dell'ultima milestone
        this.behaviour = this.nest.getBehaviour();                                      // Comportamento iniziale della formica
    }

    @Override
    public void update(double deltaTime) {
        if (!isEnabled()) return;

        // Aggiorna direzione (movimento casuale)
        updateDirection(deltaTime);
        
        // Muovi la formica
        move(deltaTime);
        
        // Aggiorna l'angolo per la visualizzazione
        updateAngle();

        // Logga lo stato della formica
        logPath();

    }
    
    private void updateDirection(double deltaTime) {
        
        switch(behaviour) {
            case RANDOM:
                randomBehaviour();
                break;
            case FOOD_PHEROMONE:
                foodPheromonesBehaviour();
                break;
            case ALL_PHEROMONES:
                allPheromonesBehaviour();
                break;
        }
    }
    

    private void allPheromonesBehaviour() {
        // Gestisci prima il caso di ritorno con cibo
        if (handlePheromoneFoodReturn()) return;
        
        // Se manca il density manager, usa comportamento casuale
        if (this.densityFieldManager == null) {
            updateDirectionRandomly();
            return;
        }
        
        // Segui il gradiente dei feromoni del cibo e del nido
        followFoodPheromoneGradient();
    }

    private void foodPheromonesBehaviour() {
        // Gestisci prima il caso di ritorno con cibo
        if (handleFoodReturn()) return;
        
        // Se manca il density manager, usa comportamento casuale
        if (this.densityFieldManager == null) {
            updateDirectionRandomly();
            return;
        }
        
        // Segui il gradiente dei feromoni del cibo
        followFoodPheromoneGradient();
    }

    private void randomBehaviour() {
        // Gestisci il caso di ritorno con cibo
        if (handleFoodReturn()) {
            // Se dopo il drop non ha più cibo, fai movimento casuale
            if (!this.hasFoodLoad()) {
                updateDirectionRandomly();
            }
            return;
        }
        
        // Movimento casuale quando non ha cibo
        updateDirectionRandomly();
    }

    private boolean handlePheromoneFoodReturn() {

        if (!this.hasFoodLoad()) {
            // Se non ha il cibo, ma sta vagando da troppo tempo, torna al nido
            if (this.getStartTrackTime() - System.nanoTime() > Ant.MAX_FOOD_SEARCH_TIME) {
                System.out.printf("Sono la formica: %d e sto tornando al nido perchè non trovo niente", this.serialNumber);
                followNestPheromoneGradient();
            }
            return false;
        }

        dropFoodIfOnNest();

        // Se ha ancora cibo dopo il tentativo di drop, vai verso il nido
        if (this.hasFoodLoad()) {
            followNestPheromoneGradient();
        }
        
        return true;
    }

    private void followNestPheromoneGradient() {
        
        double threshold = getAntSightRadius() + this.nest.getSize();

        // Se il Nest è nel raggio di visione della formica, vai diretto al Nest
        if (nest.getPos().distanceSquared(this.getCenter()) <= threshold * threshold) {
            setDirection(calcDirectionToNest());
            logBehavioralDecision("FOLLOW_NEST", 0, false);
            return;
        }

        Coord pheromoneDirection = getPheromoneDirectionSensed(Pheromone.PheromoneType.HOME_TRAIL);

        // Vecchio codice con il gradiente
        //Coord pheromoneDirection = this.densityFieldManager.getPheromoneGradient(this.getCenter(), Pheromone.PheromoneType.HOME_TRAIL);
        
        /*
        double localIntensity = this.densityFieldManager.getTotalIntensity(
            this.getCenter(), Pheromone.PheromoneType.HOME_TRAIL);
        
        // Debug periodico per alcune formiche
        if (this.serialNumber == 100) {
            System.out.printf("Ant %d: Gradient=%.3f, LocalIntensity=%.3f, UsingPheromones=%s\n", 
                serialNumber, pheromoneDirection.length(), localIntensity, 
                (pheromoneDirection.length() > 0.001) ? "YES" : "NO");
        }
                
        */
        
        // Se i feromoni sono troppo deboli, usa movimento casuale
        if (pheromoneDirection.length() <= ParameterAdapter.getPheromoneMinIntensity()) {
            pheromoneDirection = handleRandomSteering();
            logBehavioralDecision("FOLLOW_NEST_RANDOM", 0, false);
        } else {
            logBehavioralDecision("FOLLOW_NEST_PHEROMONE", pheromoneDirection.length(), true);
            pheromoneDirection.normalize();
        }
        
        


        applyDirectionChange(pheromoneDirection);
    }

    private void updateDirectionRandomly() {
        Coord randomDirection = handleRandomSteering();
        logBehavioralDecision("RANDOM_WALK", 0, false);
        applyDirectionChange(randomDirection);
    }

    private void setDirection(Coord newDirection) {
        if (newDirection == null || newDirection.length() <= 0) return;

        newDirection.multiply(SMOOTH_MOVEMENT_FACTOR);

        this.direction = newDirection.copy();

        this.direction.normalize();

    }

    private void applyDirectionChange(Coord newDirection) {

        if (newDirection == null || newDirection.length() <= 0) return;

        newDirection.multiply(SMOOTH_MOVEMENT_FACTOR);

        this.direction.sum(newDirection);

        this.direction.normalize();
    }

    private void followFoodPheromoneGradient() {

        // Se il cibo è nel raggio di visione della formica, vai diretto al cibo
        Coord foodDirection = this.multiHashGrid.getNearestFoodDirection(pos, getAntFeelRadius());
        if (foodDirection != null) {
            setDirection(foodDirection);
            logBehavioralDecision("FOLLOW_FOOD", 0, false);
            return;
        }
        
        Coord pheromoneDirection = getPheromoneDirectionSensed(Pheromone.PheromoneType.FOOD_TRAIL);
        
        // Se i feromoni sono troppo deboli, usa movimento casuale
        if (pheromoneDirection.length() <= ParameterAdapter.getPheromoneMinIntensity()) {
            logBehavioralDecision("FOLLOW_FOOD_RANDOM", 0, false);
            pheromoneDirection = handleRandomSteering();
        } else {
            logBehavioralDecision("FOLLOW_FOOD_PHEROMONE", pheromoneDirection.length(), true);
            pheromoneDirection.normalize();
        }
        
        applyDirectionChange(pheromoneDirection);
    }

    private Coord handleRandomSteering() {
        Coord randomDirection;
        do {
            randomDirection = generateRandomVector();
        } while (randomDirection.dot(this.direction) <= 0);
        
        return randomDirection;
    }

    private boolean handleFoodReturn() {
        if (!this.hasFoodLoad()) return false;
        
        dropFoodIfOnNest();
        
        // Se ha ancora cibo dopo il tentativo di drop, vai verso il nido
        if (this.hasFoodLoad()) {
            applyDirectionChange(calcDirectionToNest());
        }
        
        return true; // Indica che la formica aveva cibo e il comportamento è stato gestito
    }

    private void dropFoodIfOnNest() {

        Coord nestCenter = this.nest.getPos();
        Coord antCenter = this.getCenter();

        double threshold = this.nest.getSize() / 2.0;
        
        double distanceToNest = antCenter.distance(nestCenter);

        // Controlla se è abbastanza vicina al nido per consegnare il cibo
        if (distanceToNest < threshold) {

            // Consegna il cibo al nido
            this.dropFood();            // il cibo viene buttato
            nest.incrementFoodCount();
            
            // Aggiorna il tempo di viaggio
            this.lastNestDiscoveryTime = System.nanoTime() - this.startTrackTime;                       // tempo impiegato dal cibo al nido
            
            this.lastTripTime = this.lastNestDiscoveryTime;                                             // tempo totale di viaggio (prima metà)
            if (this.lastFoodDiscoveryTime > 0) this.lastTripTime += this.lastFoodDiscoveryTime;        // tempo totale di viaggio (seconda metà)
            
            this.meanTripTime = (this.meanTripTime * this.tripNumber + this.lastTripTime) / (this.tripNumber + 1); // aggiorna il tempo medio di viaggio
            this.tripNumber++;
            
            this.startTrackTime = System.nanoTime();                                                    // Reset per il prossimo viaggio

            this.updateMilestoneTracking(); // Aggiorna le coordinate dell'ultima milestone

            MetricsCollector.getInstance().logEvent(
                "FOOD_DROP", 
                "Ant " + this.getSerialNumber() + " dropped food", 
                nestCenter,
                null
            );

            this.turnAround();

            this.lastPheromonePosition = null; // Reset per nuovo percorso
            
        }
    }

    protected Coord calcDirectionToNest() {

        Coord directionToNest = this.nest.getCenter().copy();
        directionToNest.subtract(this.getCenter());
        directionToNest.subtract(new Coord(this.nest.getSize() / 2.0, this.getSize() / 2.0));

        directionToNest.normalize();
        
        return directionToNest;
    }

    protected Coord generateRandomVector() {
        return new Coord(
                    (RANDOM.nextDouble() * 2.0) - 1.0,
                    (RANDOM.nextDouble() * 2.0) - 1.0
                );
    }

    private void move(double deltaTime) {
        if (direction == null) return;

        Coord movement = new Coord(
            direction.x * deltaTime * getAntSpeed(),
            direction.y * deltaTime * getAntSpeed()
        );
        
        Coord futureCenter = this.getCenter().copy();
        futureCenter.sum(movement);
        
        // Controlla se la posizione futura causerebbe collisioni
        if (wouldCollideWithBounds(futureCenter) || wouldCollideWithObstacles(futureCenter)) {

            // Invece di muoversi, cambia direzione
            this.direction = getDirectionAwayFromBounds(futureCenter);
            
            Coord escapeMovement = new Coord(
                        this.direction.x * deltaTime * getAntSpeed(),
                        this.direction.y * deltaTime * getAntSpeed()
                    );
            
            this.movePos(escapeMovement);
            
            return; // Non muovere se causerebbe collisione
        }
        
        // Solo se sicuro, effettua il movimento
        this.movePos(movement);
    }

    private Coord getDirectionAwayFromBounds(Coord futureCenter) {
        double halfSize = this.getSize() / 2.0;
        double halfFoodSize = this.getFoodLoad().getSize() / 2.0;
        
        // Controlla quale bordo sta collidendo e punta nella direzione opposta
        if (futureCenter.y + halfSize + halfFoodSize >= mapHeight - WINDOW_BOUND_MARGIN) {
            return new Coord(0, -1); // Bordo inferiore → vai su
        }
        if (futureCenter.y - halfSize - halfFoodSize <= WINDOW_BOUND_MARGIN) {
            return new Coord(0, 1);  // Bordo superiore → vai giù  
        }
        if (futureCenter.x + halfSize + halfFoodSize >= mapWidth - WINDOW_BOUND_MARGIN) {
            return new Coord(-1, 0); // Bordo destro → vai sinistra
        }
        if (futureCenter.x - halfSize - halfFoodSize <= WINDOW_BOUND_MARGIN) {
            return new Coord(1, 0);  // Bordo sinistro → vai destra
        }
        
        return getTurnAroundAngle(); // Fallback
    }

    private boolean wouldCollideWithBounds(Coord futureCenter) {
        double halfSize = this.getSize() / 2.0;
        double halfFoodSize = this.getFoodLoad().getSize() / 2.0;
        
        Coord currentCenter = this.getCenter();
        
        // Calcola margini futuri
        double leftMargin = futureCenter.x - halfSize - halfFoodSize;
        double rightMargin = futureCenter.x + halfSize + halfFoodSize;
        double topMargin = futureCenter.y - halfSize - halfFoodSize;
        double bottomMargin = futureCenter.y + halfSize + halfFoodSize;

        return (leftMargin <= WINDOW_BOUND_MARGIN && futureCenter.x < currentCenter.x) ||     // Vai a sinistra verso il bordo
            (rightMargin >= mapWidth - WINDOW_BOUND_MARGIN && futureCenter.x > currentCenter.x) || // Vai a destra verso il bordo
            (topMargin <= WINDOW_BOUND_MARGIN && futureCenter.y < currentCenter.y) ||      // Vai su verso il bordo
            (bottomMargin >= mapHeight - WINDOW_BOUND_MARGIN && futureCenter.y > currentCenter.y); // Vai giù verso il bordo
    }

    private boolean wouldCollideWithObstacles(Coord futurePos) {
        if (obstacleManager == null) return false;
        
        // Crea un oggetto temporaneo per testare la posizione futura
        Coord originalPos = pos;
        pos = futurePos; // Temporaneamente sposta alla posizione futura
        
        boolean collision = obstacleManager.isCollidingWithObstacle(this, WINDOW_BOUND_MARGIN);
        
        pos = originalPos; // Ripristina posizione originale
        
        return collision;
    }

    private void updateAngle() {
        if (direction.length() > 0) {
            this.angle = Math.atan2(direction.y, direction.x);
        }
    }

    private Coord getTurnAroundAngle() {
        // Calcola l'angolo di base (180 gradi) per invertire la direzione
        double baseAngle = Math.PI;

        // Aggiungi un offset di 30 gradi
        double randomOffset = (RANDOM.nextDouble() - 0.5) * Ant.TURN_AROUND_ANGLE_OFFSET;

        double newAngle = Math.atan2(direction.y, direction.x) + baseAngle + randomOffset;

        return new Coord(Math.cos(newAngle), Math.sin(newAngle));

    }

    private void turnAround() {
        // Applica nuova direzione
        this.direction = getTurnAroundAngle();
    }

    // Metodi per il cibo
    public boolean hasFoodLoad() {
        return (this.foodLoad != null && this.foodLoad.isType(GameObjType.FOOD));
    }

    public GameObject getFoodLoad() {
        return this.foodLoad;
    }

    public void pickupFood(GameObject food) {
        if (food != null && food.isType(GameObjType.FOOD)) {
            this.foodLoad = food;
            food.disable();

            // Aggiorna il tempo di scoperta del cibo
            this.lastFoodDiscoveryTime = System.nanoTime() - this.startTrackTime;           // tempo impiegato per trovare il cibo  
            this.startTrackTime = System.nanoTime();                                        // Reset per il prossimo viaggio  
            
            // Aggiorna le milestone per i feromoni
            this.updateMilestoneTracking();

            this.turnAround();
        }
    }

    private Coord getPheromoneDirectionSensed(Pheromone.PheromoneType pheromoneType) {
        
        if (this.densityFieldManager == null) return handleRandomSteering();

        double leftIntensity = this.leftSensor.getPheromoneSensorValue(pheromoneType);
        double frontIntensity = this.frontSensor.getPheromoneSensorValue(pheromoneType);
        double rightIntensity = this.rightSensor.getPheromoneSensorValue(pheromoneType);

        double maxIntensity = Math.max(leftIntensity, Math.max(frontIntensity, rightIntensity));


        pheromoneMovingAverage = (1 - ParameterAdapter.getAntMemoryEMAAlpha()) * pheromoneMovingAverage + ParameterAdapter.getAntMemoryEMAAlpha() * maxIntensity;


        if (maxIntensity <= ParameterAdapter.getPheromoneMinIntensity()) {

            // Se tutte le intensità sono molto basse, torna indietro se l'ultima intensità era alta, se no vai a caso
            if (pheromoneMovingAverage > ParameterAdapter.getPheromoneMinIntensity()) return getTurnAroundAngle();

            return handleRandomSteering();
        }

        // Se stiamo tornando a casa, non vogliamo esplorare
        double explorationRate = ParameterAdapter.getExplorationRate();

        if (pheromoneType == Pheromone.PheromoneType.HOME_TRAIL) explorationRate = 0;

        return selectDirection(leftIntensity, frontIntensity, rightIntensity, explorationRate);

    }

    private Coord selectDirection(double leftIntensity, double frontIntensity, double rightIntensity, double explorationRate) {

        double totalIntensity = leftIntensity + frontIntensity + rightIntensity;


        // Calcola la probabilità per ogni direzione come intensità relativa + tasso di esplorazione
        double leftProbability = (leftIntensity / totalIntensity) + (explorationRate / 3);
        double frontProbability = (frontIntensity / totalIntensity) + (explorationRate / 3);
        double rightProbability = (rightIntensity / totalIntensity) + (explorationRate / 3);


        // normalizzazione delle probabilità

        totalIntensity = leftProbability + frontProbability + rightProbability;

        leftProbability /= totalIntensity;
        frontProbability /= totalIntensity;
        //rightProbability /= totalIntensity;

        // numero random per selezione probabilistica
        double rand = RANDOM.nextDouble();

        if (rand < frontProbability) {
            // si è scelto il sensore frontale
            return this.frontSensor.getPointingDirection();
        } else if (rand < leftProbability + frontProbability) {
            // si è scelto il sensore sinistro
            return this.leftSensor.getPointingDirection();
        } else {
            // si è scelto il sensore destro
            return this.rightSensor.getPointingDirection();
        }
    }

    private void logPath() {
        long currentTime = System.nanoTime();
        if (currentTime - lastPathLogTime > LOG_INTERVAL) {
            pathHistory.add(pos.copy());
            MetricsCollector.getInstance().logEvent("ANT_POSITION", 
                "Ant " + serialNumber, pos, getCurrentState());
            lastPathLogTime = currentTime;
        }
    }

    private void logBehavioralDecision(String decisionType, double pheromoneIntensity, boolean usingPheromones) {
        if (System.nanoTime() - lastDecisionLogTime > LOG_INTERVAL) {
            MetricsCollector.getInstance().logEvent(
                "ANT_DECISION", 
                String.format("Ant %d - %s", serialNumber, decisionType),
                getCenter(),
                String.format("pheromone_intensity=%.4f,using_pheromones=%s,behavior=%s", 
                            pheromoneIntensity, usingPheromones, behaviour.toString())
            );
            lastDecisionLogTime = System.nanoTime();
        }
    }

    private String getCurrentState() {
        return hasFoodLoad() ? "RETURNING" : "SEARCHING";
    }

    private void updateMilestoneTracking() {
        // Aggiorna le coordinate dell'ultima milestone
        this.lastMilestonePosition = this.getCenter().copy();
        this.lastMilestoneTime = System.nanoTime();
    }

    public GameObject dropFood() {
        GameObject droppedFood = this.foodLoad;
        this.foodLoad = new VoidObj();
        return droppedFood;
    }

    public void attachMultiHashGrid(MultiHashGrid multiHashGrid) {
        if (this.multiHashGrid != null || multiHashGrid == null) return;
        this.multiHashGrid = multiHashGrid;
    }

    public void attachDensityManager(DensityFieldManager densityFieldManager) {
        if (this.densityFieldManager != null || densityFieldManager == null) return;
        this.densityFieldManager = densityFieldManager;
    }

    public void attachObstacleManager(ObstacleManager obstacleManager) {
        if (obstacleManager == null || this.obstacleManager != null) return;  // Solo se parametro è null
        this.obstacleManager = obstacleManager;
        System.out.println("ObstacleManager ASSEGNATO alla formica " + getSerialNumber());
    }

    public Coord getLastPheromonePosition() { 
        return lastPheromonePosition; 
    }
    
    public void setLastPheromonePosition(Coord pos) { 
        this.lastPheromonePosition = pos != null ? pos.copy() : null; 
    }
    
    public double getLastPheromoneTime() { 
        return lastPheromoneTime; 
    }
    
    public void setLastPheromoneTime(double time) { 
        this.lastPheromoneTime = time; 
    }

    public double getLastFoodDiscoveryTime() {
        return lastFoodDiscoveryTime;
    }

    public double getLastNestDiscoveryTime() {
        return lastNestDiscoveryTime;
    }

    public double getLastTripTime() {
        return lastTripTime;
    }

    public double getMeanTripTime() {
        return meanTripTime;
    }

    public double getStartTrackTime() {
        return startTrackTime;
    }

    public int getTripNumber() {
        return tripNumber;
    }

    public Coord getLastMilestonePosition() {
        return lastMilestonePosition != null ? lastMilestonePosition.copy() : null;
    }

    public double getLastMilestoneTime() {
        return lastMilestoneTime;
    }


    /*
     * Ritorna i sensori nell'ordine: sinistro, frontale, destro
     */
    public Sensor[] getSensors() {
        return new Sensor[] { leftSensor, frontSensor, rightSensor };
    }

    // Getters per la visualizzazione
    public double getAngle() { return angle; }
    public Color getColor() { return ANT_COLOR; }
    public Coord getDirection() { return new Coord(direction.x, direction.y); }

    public static int getAntSightRadius() {
        return ParameterAdapter.getAntSightRadius();
    }

    public static int getAntFeelRadius() {
        return ParameterAdapter.getAntFeelRadius();
    }

    public static double getAntSpeed() {
        return ParameterAdapter.getAntSpeed();
    }

    public static double getExplorationRate() {
        return ParameterAdapter.getExplorationRate();
    }

    @Override
    public String toString() {
        return String.format(
            "Ant ID: %d\nPosition: (%.1f, %.1f)\nDirection: (%.1f, %.1f)\nFood Load: %s\n",
            this.serialNumber,
            this.getCenter().x, this.getCenter().y,
            direction.x, direction.y,
            this.hasFoodLoad() ? this.foodLoad.getSerialNumber() : "None"
        );
    }

    public void setSize(double newSize) {
        this.size = newSize;
    }

    public class Sensor {
        
        private double angleOffset;  // Offset rispetto alla direzione della formica


        public Sensor(double angleOffset) {
            this.angleOffset = angleOffset;
        }

        public double getPheromoneSensorValue(Pheromone.PheromoneType type) {
            if (densityFieldManager == null) return 0.0;
            
            // Calcola posizione del sensore relativa alla direzione corrente
            
            return densityFieldManager.getMeanIntensity(getSensorPosition(), type, ParameterAdapter.getAntSensorRadius());
        }

        public Coord getSensorPosition() {

            double antAngle = Math.atan2(direction.y, direction.x);
            double sensorAngle = antAngle + angleOffset;

            return new Coord(
                getCenter().x + getAntFeelRadius() * Math.cos(sensorAngle),
                getCenter().y + getAntFeelRadius() * Math.sin(sensorAngle)
            );
        }

        public Coord getPointingDirection() {
            // Ritorna la direzione verso cui punta il sensore
            double antAngle = Math.atan2(direction.y, direction.x);
            double sensorAngle = antAngle + angleOffset;
            
            return new Coord(
                Math.cos(sensorAngle),
                Math.sin(sensorAngle)
            );
        }
    }

}