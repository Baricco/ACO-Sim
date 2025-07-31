package com.example.managers;

import java.util.List;

import com.example.graphics.Coord;
import com.example.model.Ant;
import com.example.model.Food;

public class MultiHashGrid {
    
    HashGrid<Ant> antGrid;
    HashGrid<Food> foodGrid;

    public MultiHashGrid() {
        this.antGrid = new HashGrid<>();
        this.foodGrid = new HashGrid<>();
    }

    public void addAnt(Ant ant) {
        antGrid.addGameObject(ant);
    }

    public void addFood(Food food) {
        foodGrid.addGameObject(food);
    }

    public void addAnts(List<Ant> ants) {
        for (Ant ant : ants) addAnt(ant);
    }

    public void addFoods(List<Food> foods) {
        for (Food food : foods) addFood(food);
    }


    public void clear() {
        antGrid.clear();
        foodGrid.clear();
    }

    public List<Ant> getNearAnts(Coord position) {
        return antGrid.getGameObjectsNear(position);
    }

    public List<Food> getNearFood(Coord position) {
        return foodGrid.getGameObjectsNear(position);
    }
    

}
