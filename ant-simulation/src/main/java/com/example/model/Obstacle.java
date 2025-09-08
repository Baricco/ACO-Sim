package com.example.model;

import com.example.graphics.Coord;

public class Obstacle extends GameObject {
    private ObstacleType type;
    private int width, height;
    private final double halfWidth;
    private final double halfHeight;

    public enum ObstacleType {
        WALL,           // Muro rettangolare 
        CIRCLE          // Ostacolo circolare
    }
    
    public Obstacle(Coord pos, int width, int height) {
        super(pos, GameObjType.OBSTACLE, getNewSerialNumber(), Math.max(width, height));
        this.type = ObstacleType.WALL;
        this.width = width;
        this.height = height;
        this.halfWidth = width * 0.5;
        this.halfHeight = height * 0.5;

        // Test rapido della logica intersects
        Coord testPoint = new Coord(pos.x, pos.y); // Punto al centro dell'ostacolo
        boolean shouldBeTrue = intersects(testPoint, 1.0);
        System.out.println("Test intersects al centro: " + shouldBeTrue + " (dovrebbe essere true)");
    }

    public Obstacle(Coord pos, int radius) {
        super(pos, GameObjType.OBSTACLE, getNewSerialNumber(), radius);
        this.type = ObstacleType.CIRCLE;
        this.width = radius * 2;
        this.height = radius * 2;
        this.halfWidth = radius;
        this.halfHeight = radius;
    }
    
    public boolean intersects(Coord point, double radius) {
        if (type == ObstacleType.CIRCLE) {
            // Per ostacoli circolari
            double distance = point.distance(pos);
            return distance < (halfWidth + radius);
        } else {
            // Per ostacoli rettangolari
            double dx = Math.abs(point.x - pos.x) - halfWidth;
            double dy = Math.abs(point.y - pos.y) - halfHeight;
            
            if (dx <= 0 && dy <= 0) return true;
            if (dx > radius || dy > radius) return false;
            
            if (dx > 0 && dy > 0) {
                return dx * dx + dy * dy <= radius * radius;
            }
            
            return true;
        }
    }
    
    @Override
    public void update(double deltaTime) {
        // Ostacoli statici, nessun update necessario
    }

    public Coord getPos() {
        return pos;
    }

    public Coord getSizeCoord() {
        return new Coord(width, height);
    }

    public ObstacleType getObstacleType() {
        return type;
    }
}