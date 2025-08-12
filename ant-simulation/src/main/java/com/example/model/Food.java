package com.example.model;

import java.util.Random;

import com.example.graphics.Coord;

import javafx.scene.paint.Color;

public class Food extends GameObject {
    
    public static final int FOOD_DIM = 5;                 // Dimensione del cibo (20 :)
    private static final Color FOOD_COLOR = Color.GREEN;    // Colore del cibo

    private static Random RANDOM = new Random();

    public Food(double mapWidth, double mapHeight) {
        super(
        GameObject.generateRandomPosition(mapWidth, mapHeight, FOOD_DIM), 
        GameObjType.FOOD, 
        GameObject.getNewSerialNumber(), 
        FOOD_DIM
    );
        //this.setSprite(GameCanvas.loadImageCached("foodSprite.png"));
    }

    public Food(Coord pos) {
        super(pos, GameObjType.FOOD, GameObject.getNewSerialNumber(), FOOD_DIM);
        //this.setSprite(GameCanvas.loadImageCached("foodSprite.png"));

    }

    
    public void update(double deltaTime) {
        // Il cibo non ha logica di movimento, quindi non fa nulla
        //if (!this.isEnabled()) return;
    }

    public Color getColor() {
        return FOOD_COLOR;
    }


}
