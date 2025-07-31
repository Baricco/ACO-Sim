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


    public List<T> getGameObjectsNear(Coord position) {
        List<T> nearbyGameObjects = new ArrayList<>();

        int cellX = (int) (position.x / CELL_SIZE);
        int cellY = (int) (position.y / CELL_SIZE);
        
        // Controlla cella corrente e le 8 celle adiacenti
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                String key = (cellX + dx) + "," + (cellY + dy);
                List<T> cellGameObjects = grid.get(key);
                if (cellGameObjects != null) {
                    nearbyGameObjects.addAll(cellGameObjects);
                }
            }
        }

        return nearbyGameObjects;
    }

    private String getCellKey(Coord position) {

        long cellX = (long) Math.floor(position.x * 1 / CELL_SIZE);
        long cellY = (long) Math.floor(position.y * 1 / CELL_SIZE);

        keyBuilder.setLength(0);
        keyBuilder.append(cellX).append(',').append(cellY);
        return keyBuilder.toString();
    }
}
