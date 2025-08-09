package com.example.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.graphics.Coord;
import com.example.model.GameObject;

public class HashGrid<T extends GameObject> {
    public final double CELL_SIZE; // Dimensione celle in pixel
    private final Map<String, List<T>> grid = new HashMap<>();
    private final StringBuilder keyBuilder = new StringBuilder(16);


    public HashGrid() {
        this.CELL_SIZE = 10; // Valore di default
    }

    public HashGrid(double cellSize) {
        this.CELL_SIZE = cellSize;
    }

    public void clear() {
        grid.clear();
    }
    
    public void addGameObject(T gameObject) {
        if (!gameObject.isEnabled()) return;

        String key = getCellKey(gameObject.getCenter());
        grid.computeIfAbsent(key, k -> new ArrayList<>()).add(gameObject);
    }
    
    public List<T> getAllObjects() {
        List<T> allObjects = new ArrayList<>();
        for (List<T> cellObjects : grid.values()) {
            for (T obj : cellObjects) {
                if (obj.isEnabled()) { 
                    allObjects.add(obj);
                }
            }
        }
        return allObjects;
    }

    public Map<String, List<T>> getGrid() {
        return grid;
    }

    public List<T> getGameObjectsNear(Coord position, double radius) {
        List<T> nearbyGameObjects = new ArrayList<>();
        
        // Calcola quante celle dobbiamo controllare
        int cellRadius = (int) Math.ceil(radius / CELL_SIZE);
        
        int centerX = (int) (position.x / CELL_SIZE);
        int centerY = (int) (position.y / CELL_SIZE);
        
        // Controlla tutte le celle nel raggio
        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dy = -cellRadius; dy <= cellRadius; dy++) {
                String key = (centerX + dx) + "," + (centerY + dy);
                List<T> cellObjects = grid.get(key);
                if (cellObjects != null) {
                    nearbyGameObjects.addAll(cellObjects);
                }
            }
        }
        
        return nearbyGameObjects;
    }

    public List<T> getGameObjectsNear(Coord position) {
        return getGameObjectsNear(position, CELL_SIZE); // Equivale alle 8 celle adiacenti
    }

    public Coord getNearestGameObjectDirection(Coord pos, double maxDistance) {
        double minDistance = maxDistance;
        Coord nearestGameObjectPos = null;

        for (T obj : getGameObjectsNear(pos, maxDistance)) {
            if (!obj.isEnabled()) continue;
            
            double distance = obj.getCenter().distance(pos);
            if (distance < minDistance) {
                minDistance = distance;
                nearestGameObjectPos = obj.getCenter();
            }
        }

        if (nearestGameObjectPos == null) return null;

        nearestGameObjectPos.subtract(pos);
        nearestGameObjectPos.normalize();

        return nearestGameObjectPos;
    }


    private String getCellKey(Coord position) {

        long cellX = (long) Math.floor(position.x * 1 / CELL_SIZE);
        long cellY = (long) Math.floor(position.y * 1 / CELL_SIZE);

        keyBuilder.setLength(0);
        keyBuilder.append(cellX).append(',').append(cellY);
        return keyBuilder.toString();
    }
}
