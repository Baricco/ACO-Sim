package com.example.model;

import java.util.Random;

import com.example.graphics.Coord;
import com.example.graphics.GameCanvas;
import com.example.managers.DensityFieldManager;

import javafx.scene.paint.Color;

public class Ant extends GameObject {

    public enum ANT_BEHAVIOUR {
        RANDOM,
        FOOD_PHEROMONE,
        ALL_PHEROMONES              // FOOD + HOME PHEROMONES
    }

    // Costanti
    public static final int ANT_SIZE = 20;
    public static final int ANT_FEEL_RADIUS = 50;               // Raggio di percezione per i feromoni
    public static final Color ANT_FEEL_COLOR = Color.rgb(255, 255, 0, 0.2); // Colore per il raggio di percezione
    public static final Color ANT_COLOR = Color.RED;
    public static final double ANT_SPEED = 120.0; // pixel/secondo
    public static final int WINDOW_BOUND_MARGIN = 2;

    private static final Random RANDOM = new Random();
    private static final double SMOOTH_MOVEMENT_FACTOR = 0.1;

    // Stato della formica
    protected Coord direction;
    protected GameObject foodLoad;
    protected double mapWidth;
    protected double mapHeight;
    protected double angle; // Per la rotazione visuale
    protected Nest nest; // Riferimento al nido della formica

    // Comportamento della formica
    protected ANT_BEHAVIOUR behaviour;
    protected DensityFieldManager densityFieldManager;          // Gestore del campo di densità per i feromoni
    
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

    private double startTrackTime;                 // Tempo di inizio tracking
    private int tripNumber = 0;                        // Numero di viaggi effettuati

    // Sensori per i feromoni
    private Sensor leftSensor, frontSensor, rightSensor;

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
        this.leftSensor = new Sensor(Math.PI / 6);   // 30 gradi a sinistra
        this.frontSensor = new Sensor(0);             // Direzione frontale
        this.rightSensor = new Sensor(-Math.PI / 6);  // 30 gradi a destra
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
        
        // Controlla i bordi
        checkBounds();
        
        // Aggiorna l'angolo per la visualizzazione
        updateAngle();

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
        
        if (!this.hasFoodLoad()) return false;

        dropFoodIfOnNest();

        // Se ha ancora cibo dopo il tentativo di drop, vai verso il nido
        if (this.hasFoodLoad()) {
            followNestPheromoneGradient();
        }
        
        return true;
    }

    private void followNestPheromoneGradient() {
        
        Coord pheromoneDirection = getPheromoneDirectionSensed(Pheromone.PheromoneType.HOME_TRAIL);

        // Vecchio codice con il gradiente
        //Coord pheromoneDirection = this.densityFieldManager.getPheromoneGradient(this.getCenter(), Pheromone.PheromoneType.HOME_TRAIL);
        
        double localIntensity = this.densityFieldManager.getTotalIntensity(
            this.getCenter(), Pheromone.PheromoneType.HOME_TRAIL);
        
        // Debug periodico per alcune formiche
        if (this.serialNumber == 100) {
            System.out.printf("Ant %d: Gradient=%.3f, LocalIntensity=%.3f, UsingPheromones=%s\n", 
                serialNumber, pheromoneDirection.length(), localIntensity, 
                (pheromoneDirection.length() > 0.001) ? "YES" : "NO");
        }
        
        // Se i feromoni sono troppo deboli, usa movimento casuale
        if (pheromoneDirection.length() <= 0.001) {
            pheromoneDirection = handleRandomSteering();
        } else {
            pheromoneDirection.normalize();
        }
        
        applyDirectionChange(pheromoneDirection);
    }

    private void updateDirectionRandomly() {
        Coord randomDirection = handleRandomSteering();
        applyDirectionChange(randomDirection);
    }

    private void applyDirectionChange(Coord newDirection) {

        if (newDirection == null || newDirection.length() <= 0) return;

        newDirection.multiply(SMOOTH_MOVEMENT_FACTOR);

        this.direction.sum(newDirection);

        this.direction.normalize();
    }

    private void followFoodPheromoneGradient() {

        Coord pheromoneDirection = getPheromoneDirectionSensed(Pheromone.PheromoneType.FOOD_TRAIL);

        // Vecchio codice con il gradiente
        //Coord pheromoneDirection = this.densityFieldManager.getPheromoneGradient(this.getCenter(), Pheromone.PheromoneType.FOOD_TRAIL);
        
        double localIntensity = this.densityFieldManager.getTotalIntensity(
            this.getCenter(), Pheromone.PheromoneType.FOOD_TRAIL);
        
        // Debug periodico per alcune formiche
        if (this.serialNumber == 100) {
            System.out.printf("Ant %d: Gradient=%.3f, LocalIntensity=%.3f, UsingPheromones=%s\n", 
                serialNumber, pheromoneDirection.length(), localIntensity, 
                (pheromoneDirection.length() > 0.001) ? "YES" : "NO");
        }
        
        // Se i feromoni sono troppo deboli, usa movimento casuale
        if (pheromoneDirection.length() <= 0.001) {
            pheromoneDirection = handleRandomSteering();
        } else {
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
            this.direction = calcDirectionToNest();
        }
        
        return true; // Indica che la formica aveva cibo e il comportamento è stato gestito
    }

    private void dropFoodIfOnNest() {

        Coord nestCenter = this.nest.getPos();
        Coord antCenter = this.getCenter();

        double threshold = this.nest.getSize() / 2.0 + this.getSize() / 2.0 + this.foodLoad.getSize() / 2.0;
        
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
            direction.x * deltaTime * ANT_SPEED,
            direction.y * deltaTime * ANT_SPEED
        );
        
        this.movePos(movement);
    }

    private void checkBounds() {
        
        Coord center = this.getCenter();

        // Rimbalza sui bordi
        if (
            (center.x - this.getSize() / 2 - this.getFoodLoad().getSize() / 2 < WINDOW_BOUND_MARGIN && this.direction.x < 0) || 
            (center.x + this.getSize() / 2 + this.getFoodLoad().getSize() / 2 > mapWidth - WINDOW_BOUND_MARGIN && this.direction.x > 0) ) {
            this.direction.x *= -1;
        }
        if ((center.y - this.getSize() / 2 - this.getFoodLoad().getSize() / 2 < WINDOW_BOUND_MARGIN && this.direction.y < 0) ||
            (center.y + this.getSize() / 2 + this.getFoodLoad().getSize() / 2 > mapHeight - WINDOW_BOUND_MARGIN && this.direction.y > 0)) {
            this.direction.y *= -1;
        }
    }

    private void updateAngle() {
        if (direction.length() > 0) {
            this.angle = Math.atan2(direction.y, direction.x);
        }
    }

    private void turnAround() {
        // Calcola l'angolo di base (180 gradi) per invertire la direzione
        double baseAngle = Math.PI;
        
        // Aggiungi un offset di 60 gradi
        double randomOffset = (RANDOM.nextDouble() - 0.5) * (Math.PI / 3);
        
        double newAngle = Math.atan2(direction.y, direction.x) + baseAngle + randomOffset;
        
        // Applica nuova direzione
        this.direction = new Coord(Math.cos(newAngle), Math.sin(newAngle));
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

        if (maxIntensity <= Pheromone.MIN_INTENSITY) {
            // Se tutte le intensità sono molto basse, usa un vettore casuale
            return handleRandomSteering();
        }

        if (frontIntensity >= leftIntensity && frontIntensity >= rightIntensity) {
            // Il sensore frontale ha il valore più alto
            return this.frontSensor.getPointingDirection();
        } else if (leftIntensity > rightIntensity) {
            // Il sensore sinistro ha il valore più alto
            return this.leftSensor.getPointingDirection();  
        } else {
            // Il sensore destro ha il valore più alto
            return this.rightSensor.getPointingDirection();
        }

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

    
    public void attachDensityManager(DensityFieldManager densityFieldManager) {
        if (densityFieldManager == null || this.densityFieldManager != null) return;
        this.densityFieldManager = densityFieldManager;
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

    // Getters per la visualizzazione
    public double getAngle() { return angle; }
    public Color getColor() { return ANT_COLOR; }
    public Coord getDirection() { return new Coord(direction.x, direction.y); }

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

    private class Sensor {
        private double angleOffset;  // Offset rispetto alla direzione della formica

        public Sensor(double angleOffset) {
            this.angleOffset = angleOffset;
        }

        public double getPheromoneSensorValue(Pheromone.PheromoneType type) {
            if (densityFieldManager == null) return 0.0;
            
            // Calcola posizione del sensore relativa alla direzione corrente
            double antAngle = Math.atan2(direction.y, direction.x);
            double sensorAngle = antAngle + angleOffset;
            
            Coord sensorPosition = new Coord(
                getCenter().x + ANT_FEEL_RADIUS * Math.cos(sensorAngle),
                getCenter().y + ANT_FEEL_RADIUS * Math.sin(sensorAngle)
            );
            
            return densityFieldManager.getTotalIntensity(sensorPosition, type);
        }

        public Coord getPointingDirection() {
            // Ritorna la DIREZIONE verso cui punta il sensore, non la posizione
            double antAngle = Math.atan2(direction.y, direction.x);
            double sensorAngle = antAngle + angleOffset;
            
            return new Coord(
                Math.cos(sensorAngle),
                Math.sin(sensorAngle)
            );
        }
    }

}