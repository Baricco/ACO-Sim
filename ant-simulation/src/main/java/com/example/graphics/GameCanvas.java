package com.example.graphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.config.ParameterAdapter;
import com.example.model.Ant;
import com.example.model.Food;
import com.example.model.FoodClump;
import com.example.model.Nest;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Canvas specializzato per il rendering dei GameObject
 */
public class GameCanvas extends Canvas {
    
    private GraphicsContext gc;
    private static Map<String, Image> imageCache;
    private Color backgroundColor;
    
    public GameCanvas(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();
        this.imageCache = new HashMap<>();
        this.backgroundColor = Color.LIGHTGREEN;
    }
    
    /**
     * Pulisce il canvas e disegna lo sfondo
     */
    public void clear() {
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, getWidth(), getHeight());
    }


    public void renderFoodClumps(List<FoodClump> foodClumps) {
        for (FoodClump foodClump : foodClumps) {
            if (foodClump.isEnabled()) {
                renderFoodClump(foodClump);
            }
        }
    }

    public void renderFoodClump(FoodClump foodClump) {
        for (Food food : foodClump.getFoodPieces()) {
            if (!food.isEnabled()) continue;
            renderFoodItem(food);
        }
    }

    public void renderNests(List<Nest> nests) {
        for (Nest nest : nests) {
            if (nest.isEnabled()) {
                renderNest(nest);
            }
        }
    }

    public void renderNest(Nest nest) {
        Coord pos = nest.getPos();
        double size = nest.getSize();
        
        // Disegna il nido come un cerchio
        gc.setFill(Color.BROWN);
        gc.fillOval(pos.x - (size / 2), pos.y - (size / 2), size, size);

        // Scrivi il numero di cibo raccolto

        String foodCountText = String.valueOf(nest.getFoodCount());
        
        // Calcola la posizione del testo per centrarlo
        // Usa una stima più accurata della larghezza del testo (font size 16)
        double estimatedTextWidth = foodCountText.length() * 9; // ~9 pixel per carattere con font 16
        double textX = pos.x - estimatedTextWidth / 2;
        double textY = pos.y; // Offset per centrare verticalmente (font baseline)

        this.renderText(foodCountText, textX, textY, Color.WHITE, 16);
    }
    
    /**
     * Renderizza una lista di formiche
     */
    public void renderAnts(List<Ant> ants, Ant selectedAnt) {
        for (Ant ant : ants) {
            if (ant.isEnabled()) {
                renderAnt(ant, ant == selectedAnt);
            }
        }
    }
    
    private void renderAnt(Ant ant, boolean isSelected) {
        // Disegna la formica con uno sprite se selezionata, se no usa lo sprite normale
        if (isSelected) {
            // Seleziona lo sprite speciale per la formica selezionata
            renderAnt(ant, loadImageCached("selectedAntSprite.png"));
            // Disegna il raggio di percezione della formica
            gc.setFill(Ant.ANT_FEEL_COLOR);
            gc.fillOval(
                ant.getCenter().x - ParameterAdapter.getAntSightRadius(),
                ant.getCenter().y - ParameterAdapter.getAntSightRadius(),
                ParameterAdapter.getAntSightRadius() * 2,
                ParameterAdapter.getAntSightRadius() * 2
            );

        }
        else renderAnt(ant);
        
    }

    private void renderAnt(Ant ant) {
        renderAnt(ant, ant.getSprite());
    }

    /**
     * Renderizza una singola formica
     */
    private void renderAnt(Ant ant, Image sprite) {
        
        Coord pos = ant.getPos();
        double size = ant.getSize();
        
        // Salva lo stato del context
        gc.save();
        
        // Trasla al centro della formica per la rotazione
        gc.translate(pos.x + size/2, pos.y + size/2);
        gc.rotate(Math.toDegrees(ant.getAngle()));

        // Se la formica ha uno sprite, disegnalo
        if (ant.hasSprite()) {
            gc.drawImage(sprite, -size/2, -size/2, size, size);
        }
        else {
            
            // Disegna la formica come cerchio rosso
            gc.setFill(ant.getColor());
            gc.fillOval(-size/2, -size/2, size, size);
            
            // Disegna una linea per indicare la direzione
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeLine(0, 0, size/2, 0);
        
        }
            
        // Se ha del cibo, disegnalo
        if (ant.hasFoodLoad()) {
            Food food = (Food)ant.getFoodLoad();
            double offsetDistance = size * 0.6; // distanza dal centro
            
            // Renderizza il cibo nelle coordinate trasformate
            if (food.hasSprite()) {
                gc.drawImage(food.getSprite(), 
                    offsetDistance - food.getSize()/2, 
                    -food.getSize()/2, 
                    food.getSize(), 
                    food.getSize());
            } else {
                gc.setFill(food.getColor());
                gc.fillOval(offsetDistance - food.getSize()/2, 
                           -food.getSize()/2, 
                           food.getSize(), 
                           food.getSize());
            }
        }

        // Ripristina lo stato
        gc.restore();
    }
    
    /**
     * Renderizza una lista di cibo
     */
    public void renderFood(List<Food> foods) {
        for (Food food : foods) {
            if (food.isEnabled()) {
                renderFoodItem(food);
            }
        }
    }
    
    /**
     * Renderizza un singolo pezzo di cibo
     */
    private void renderFoodItem(Food food) {
        Coord pos = food.getPos();
        double size = food.getSize();
        
        if (food.hasSprite()) {
            gc.drawImage(food.getSprite(), pos.x, pos.y, size, size);
        }
        else {
            gc.setFill(food.getColor());
            gc.fillOval(pos.x, pos.y, size, size);
            
            // Bordo scuro per visibilità
            gc.setStroke(Color.DARKGREEN);
            gc.setLineWidth(1);
            gc.strokeOval(pos.x, pos.y, size, size);
        }
    }
    
    /**
     * Renderizza una linea generica
     */
    public void renderLine(double x1, double y1, double x2, double y2, Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.strokeLine(x1, y1, x2, y2);
    }
    /**
     * Renderizza cerchio generico
     */
    public void renderCircle(double x, double y, double radius, Color color) {
        gc.setFill(color);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    /**
     * Renderizza ellisse per scie di feromoni
     */
    public void renderEllipse(double x, double y, double width, double height, double rotation, Color color) {
        gc.save();
        gc.setFill(color);
        gc.translate(x + width/2, y + height/2);
        gc.rotate(Math.toDegrees(rotation));
        gc.fillOval(-width/2, -height/2, width, height);
        gc.restore();
    }
    
    /**
     * Renderizza rettangolo generico
     */
    public void renderRectangle(double x, double y, double width, double height, Color color) {
        gc.setFill(color);
        gc.fillRect(x, y, width, height);
    }
    
    /**
     * Renderizza immagine
     */
    public void renderImage(String imagePath, double x, double y, double width, double height) {
        Image image = loadImageCached(imagePath);
        if (image != null) {
            gc.drawImage(image, x, y, width, height);
        } else {
            // Fallback: rettangolo rosso
            gc.setFill(Color.RED);
            gc.fillRect(x, y, width, height);
        }
    }

    public static Image loadImage(String imageName) {
        try {
            return new Image(GameCanvas.class.getResourceAsStream("/com/example/images/" + imageName));
        } catch (Exception e) {
            System.out.println("Sprite non trovata: " + imageName);
            return null;
        }
    }

    /**
     * Carica un'immagine con cache
     */
    public static Image loadImageCached(String imageName) {
        if (!imageCache.containsKey(imageName)) {
            try {
                Image image = new Image(GameCanvas.class.getResourceAsStream("/com/example/images/" + imageName));
                imageCache.put(imageName, image);
            } catch (Exception e) {
                System.err.println("Errore caricamento immagine: " + imageName);
                imageCache.put(imageName, null);
            }
        }
        return imageCache.get(imageName);
    }
    
    /**
     * Renderizza testo
     */
    public void renderText(String text, double x, double y, Color color, double fontSize) {
        gc.setFill(color);
        gc.setFont(javafx.scene.text.Font.font(fontSize));
        gc.fillText(text, x, y);
    }
    
    // Getters/Setters
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
    
    public Color getBackgroundColor() {
        return backgroundColor;
    }
}