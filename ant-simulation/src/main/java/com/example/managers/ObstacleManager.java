package com.example.managers;

import java.util.ArrayList;
import java.util.List;

import com.example.graphics.Coord;
import com.example.model.GameObject;
import com.example.model.Obstacle;

public class ObstacleManager {
    private List<Obstacle> obstacles;
    double mapWidth, mapHeight;
    
    public ObstacleManager(double mapWidth, double mapHeight) {
        obstacles = new ArrayList<>();
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }
    
    public void addObstacle(Obstacle obstacle) {
        obstacles.add(obstacle);
    }
    
    public boolean isCollidingWithObstacle(GameObject gameObject, int padding) {
        double radius = (gameObject.getSize() / 2.0) + padding;
        Coord pos = gameObject.getPos();
        
        // Early exit se non ci sono ostacoli
        if (obstacles.isEmpty()) return false;
        
        // Controllo diretto 
        for (Obstacle obstacle : obstacles) {
            // Quick AABB check prima dell'intersects più costoso
            double obstacleHalfWidth = obstacle.getSizeCoord().x / 2.0;
            double obstacleHalfHeight = obstacle.getSizeCoord().y / 2.0;
            Coord obstaclePos = obstacle.getPos();
            
            // Bounding box rapido: se la formica è troppo lontana, skip
            if (Math.abs(pos.x - obstaclePos.x) > obstacleHalfWidth + radius ||
                Math.abs(pos.y - obstaclePos.y) > obstacleHalfHeight + radius) {
                continue;
            }
            
            // Solo se passa il bounding box, fai l'intersects completo
            if (obstacle.intersects(pos, radius)) {
                return true;
            }
        }
        return false;
    }

    public Coord findNearestFreePosition(GameObject gameObject, int margin) {
        Coord currentPos = gameObject.getPos();
        double radius = gameObject.getSize() / 2.0;
        
        // Cerca in un raggio crescente
        for (double searchRadius = radius + margin; searchRadius <= (radius + margin) * 4; searchRadius += 15) {
            // Prova 8 direzioni attorno alla posizione corrente
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Coord testPos = new Coord(
                    currentPos.x + Math.cos(angle) * searchRadius,
                    currentPos.y + Math.sin(angle) * searchRadius
                );
                
                // Verifica che sia dentro i bordi della mappa
                if (testPos.x < margin || testPos.x > mapWidth - margin ||
                    testPos.y < margin || testPos.y > mapHeight - margin) {
                    continue;
                }
                
                // Crea oggetto temporaneo per test
                Coord originalPos = gameObject.getPos();
                gameObject.setPos(testPos); // Temporaneamente sposta per test
                
                boolean isFree = !isCollidingWithObstacle(gameObject, margin);
                
                gameObject.setPos(originalPos); // Ripristina posizione
                
                if (isFree) {
                    return testPos;
                }
            }
        }
        
        // Fallback: sposta verso il centro della mappa
        return new Coord(mapWidth / 2.0, mapHeight / 2.0);
    }
    
    // Ottimizzazione per path checking
    public boolean isPathClear(Coord from, Coord to, double antRadius) {
        // Early exit se non ci sono ostacoli
        if (obstacles.isEmpty()) return true;
        
        double distance = from.distance(to);
        
        for (Obstacle obstacle : obstacles) {
            // Quick distance check: se l'ostacolo è troppo lontano dal percorso, skip
            double obstacleMaxRadius = Math.max(obstacle.getSizeCoord().x, obstacle.getSizeCoord().y) / 2.0;
            double maxInteractionDistance = obstacleMaxRadius + antRadius + distance;
            
            if (from.distance(obstacle.getPos()) > maxInteractionDistance) {
                continue;
            }
            
            if (lineIntersectsObstacle(from, to, obstacle, antRadius)) {
                return false;
            }
        }
        return true;
    }
    
    // Ottimizzazione per avoidance
    public Coord findAvoidancePoint(Coord antPos, Coord targetDir, double antRadius) {
        // Normalizza la direzione target
        targetDir = targetDir.copy();
        targetDir.normalize();
        
        double angle = Math.atan2(targetDir.y, targetDir.x);
        double avoidanceDistance = 50.0;
        
        // Pre-calcola le direzioni candidate
        Coord rightAvoid = new Coord(
            Math.cos(angle + Math.PI/4), 
            Math.sin(angle + Math.PI/4)
        );
        rightAvoid.multiply(avoidanceDistance);
        Coord rightTarget = new Coord(antPos.x + rightAvoid.x, antPos.y + rightAvoid.y);

        if (isPathClear(antPos, rightTarget, antRadius)) {
            rightAvoid.normalize();
            return rightAvoid;
        }
        
        Coord leftAvoid = new Coord(
            Math.cos(angle - Math.PI/4), 
            Math.sin(angle - Math.PI/4)
        );
        leftAvoid.multiply(avoidanceDistance);
        Coord leftTarget = new Coord(antPos.x + leftAvoid.x, antPos.y + leftAvoid.y);

        if (isPathClear(antPos, leftTarget, antRadius)) {
            leftAvoid.normalize();
            return leftAvoid;
        }
        
        // Fallback: vai indietro
        return new Coord(-targetDir.x, -targetDir.y);
    }
    
    // Metodi helper invariati...
    private boolean lineIntersectsObstacle(Coord from, Coord to, Obstacle obstacle, double antRadius) {
        double obstacleRadius = obstacle.getSize() / 2.0;
        double totalRadius = obstacleRadius + antRadius;
        
        double lineLength = from.distance(to);
        if (lineLength == 0) {
            return from.distance(obstacle.getPos()) < totalRadius;
        }
        
        double distance = distancePointToLine(obstacle.getPos(), from, to);
        
        if (distance < totalRadius) {
            double projectionParameter = getProjectionParameter(obstacle.getPos(), from, to);
            return projectionParameter >= 0 && projectionParameter <= 1;
        }
        
        return false;
    }
    
    private double distancePointToLine(Coord point, Coord lineStart, Coord lineEnd) {
        double A = lineEnd.y - lineStart.y;
        double B = lineStart.x - lineEnd.x;
        double C = lineEnd.x * lineStart.y - lineStart.x * lineEnd.y;
        
        double lineLength = Math.sqrt(A * A + B * B);
        if (lineLength == 0) return point.distance(lineStart);
        
        return Math.abs(A * point.x + B * point.y + C) / lineLength;
    }
    
    private double getProjectionParameter(Coord point, Coord lineStart, Coord lineEnd) {
        double dx = lineEnd.x - lineStart.x;
        double dy = lineEnd.y - lineStart.y;
        
        if (dx == 0 && dy == 0) return 0;
        
        double t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (dx * dx + dy * dy);
        return t;
    }
    
    public List<Obstacle> getObstacles() {
        return new ArrayList<>(obstacles);
    }
}