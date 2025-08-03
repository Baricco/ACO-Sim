package com.example.model;

import java.util.Random;

import com.example.graphics.Coord;
import com.example.graphics.GameCanvas;

import javafx.scene.paint.Color;

public class Ant extends GameObject {

    // Costanti
    public static final int ANT_SIZE = 20;
    public static final Color ANT_COLOR = Color.RED;
    public static final double ANT_SPEED = 2.0; // pixel/secondo
    public static final int WINDOW_BOUND_MARGIN = 2;

    private static final Random RANDOM = new Random();
    private static final double SMOOTH_MOVEMENT_FACTOR = 20;

    // Stato della formica
    protected Coord direction;
    protected GameObject foodLoad;
    protected double mapWidth;
    protected double mapHeight;
    protected double angle; // Per la rotazione visuale
    protected Nest nest; // Riferimento al nido della formica

    // Tracking temporale per feromoni
    private Coord lastPheromonePosition;
    private double lastPheromoneTime = -1;

    // Tracking temporale per scoperta cibo
    private double lastFoodDiscoveryTime = 0;          // quanto tempo ci ha messo a trovare il cibo
    private double lastNestDiscoveryTime = 0;          // quanto tempo ci ha messo a trovare il nido
    private double lastTripTime = 0;                   // quanto tempo ci ha messo a fare un viaggio
    private double meanTripTime = 0;                   // tempo medio di viaggio

    
    private double startTrackTime;                 // Tempo di inizio tracking
    private int tripNumber = 0;                        // Numero di viaggi effettuati

    public Ant(double mapWidth, double mapHeight, Nest nest) {
        super(GameObjType.ANT, GameObject.getNewSerialNumber(), ANT_SIZE, GameObject.generateRandomPosition(mapWidth, mapHeight, ANT_SIZE));
        this.direction = new Coord(0, 0);
        this.foodLoad = new VoidObj();          // new Food(pos);
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.nest = nest;
        this.angle = 0;
        this.enabled = true;
        this.setSprite(GameCanvas.loadImageCached("antSprite.png"));
        
        // INIZIALIZZA tracking temporale
        this.lastPheromonePosition = null;
        
    }

    public Ant(Coord position, double mapWidth, double mapHeight, Nest nest) {
        this(mapWidth, mapHeight, nest);
        this.pos = position;
        this.startTrackTime = System.nanoTime(); // Inizializza il tempo di tracking
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
        
        // se la formica ha un foodLoad, torna verso il Nest
        if (this.hasFoodLoad()) {
            Coord nestCenter = this.nest.getCenter();
            Coord antCenter = this.getCenter();
            
            double distanceToNest = antCenter.distance(nestCenter);

            // Controlla se è abbastanza vicina al nido per consegnare il cibo
            if (distanceToNest < nest.getSize() / 2 + this.getSize() / 2) {
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

                this.lastPheromonePosition = null; // Reset per nuovo percorso
                
                // Dopo aver consegnato, continua con movimento casuale
                // Non fare return qui, lascia che il codice continui per aggiornare la direzione
            }

            // Se non ha ancora consegnato o dopo aver consegnato, calcola la direzione
            if (this.hasFoodLoad()) {
                // Se ha ancora del cibo, vai verso il nido
                this.direction = calcDirectionToNest();
                
            } else {
                // Se ha appena consegnato o non ha cibo, movimento casuale
                Coord randomDirection = generateRandomVector();
                randomDirection.multiply(SMOOTH_MOVEMENT_FACTOR);
                this.direction.sum(randomDirection);
                this.direction.normalize();
            }
            
        } else {
            // Movimento casuale quando non ha cibo
            Coord nextDirection = generateRandomVector();
            nextDirection.multiply(SMOOTH_MOVEMENT_FACTOR);
            
            this.direction.sum(nextDirection);
            this.direction.normalize();
        }
        
        // Applica la velocità finale
        this.direction.multiply(ANT_SPEED / deltaTime);
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
            direction.x * deltaTime,
            direction.y * deltaTime
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
        }
    }

    public GameObject dropFood() {
        GameObject droppedFood = this.foodLoad;
        this.foodLoad = new VoidObj();
        return droppedFood;
    }

    // NUOVI METODI per tracking feromoni intelligenti
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
}