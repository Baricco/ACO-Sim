package com.example.model;

import java.util.Random;

import com.example.graphics.Coord;

import javafx.scene.image.Image;

public abstract class GameObject {

    public enum GameObjType {
        VOID_OBJ,
        ANT,
        FOOD,
        NEST,
        FOOD_CLUMP,
        PHEROMONE
    }

    private static Random RANDOM = new Random();
    private static int SNCounter = 0;
    
    
    protected GameObjType type;
    protected Coord pos;
    protected int serialNumber;
    protected boolean enabled;
    protected int size;
    protected Image sprite;

    public GameObject(Coord pos, GameObjType type, int serialNumber, int size) {
        this.pos = pos;
        this.type = type;
        this.serialNumber = serialNumber;
        this.size = size;
        this.enabled = true;
        this.sprite = null;
    }

    public GameObject(GameObjType type, int serialNumber, int size, Coord mapSize) {

        this(
            GameObject.generateRandomPosition(mapSize.x, mapSize.y, size),
            type, 
            serialNumber, 
            size
        );

    }   
    
    public static int getNewSerialNumber() { 
        SNCounter++;
        return SNCounter;
    }

    // Getters
    public int getSerialNumber() { return this.serialNumber; }

    public GameObjType getType() { return this.type; }
    
    public Coord getPos() { return new Coord(this.pos); }
    
    public int getSize() { return this.size; }
    
    public boolean isEnabled() { return enabled; }

    // Metodi di movimento
    public void movePos(Coord delta) {
        this.pos.sum(delta);
    }

    public void setPos(Coord coord) {
        this.pos.x = coord.x;
        this.pos.y = coord.y;
    }

    public Coord getCenter() { 
        return new Coord(this.pos.x + (this.size / 2.0), this.pos.y + (this.size / 2.0));
    }

    public boolean isType(GameObjType type) {
        return this.getType().equals(type);
    }

    public void disable() { 
        enabled = false; 
    }

    public boolean hasSprite() {
        return this.sprite != null;
    }

    public void setSprite(Image sprite) {
        this.sprite = sprite;
    }

    public Image getSprite() {
        return this.sprite;
    }

    protected static Coord generateRandomPosition(double mapWidth, double mapHeight, double dim) {

        double margin = dim / 2.0;
        
        // Assicurati che ci sia spazio sufficiente
        double availableWidth = mapWidth - (2 * margin);
        double availableHeight = mapHeight - (2 * margin);
        
        if (availableWidth <= 0) availableWidth = mapWidth * 0.8;
        if (availableHeight <= 0) availableHeight = mapHeight * 0.8;
        
        double x = margin + RANDOM.nextDouble() * availableWidth;
        double y = margin + RANDOM.nextDouble() * availableHeight;

        return new Coord(x, y);
    }

    // Metodo astratto per l'update della logica
    public abstract void update(double deltaTime);
}