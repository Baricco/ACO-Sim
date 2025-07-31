package com.example.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.App;
import com.example.graphics.GameCanvas;
import com.example.managers.SimulationManager;
import com.example.simulation.FullSimulation;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 * Controller per la scena di simulazione
 */
public class SimulationController implements Initializable {

    @FXML private Pane canvasContainer;
    @FXML private Button pauseButton;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;
    
    private GameCanvas gameCanvas;
    private SimulationManager simulationManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SimulationController initialized");
        
        // Aspetta che la scena sia completamente caricata
        javafx.application.Platform.runLater(() -> {
            setupCanvas();
        });
    }
    
    private void setupCanvas() {
        // Ottieni le dimensioni del container
        double width = canvasContainer.getWidth();
        double height = canvasContainer.getHeight();

        // Se le dimensioni non sono ancora disponibili, usa valori di default
        if (width <= 0) width = 800;
        if (height <= 0) height = 600;
        

        // Crea il canvas per il rendering
        gameCanvas = new GameCanvas(width, height);
        
        // Binding per il ridimensionamento
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty());
        
        canvasContainer.getChildren().add(gameCanvas);
        
        // Crea il simulation manager
        simulationManager = new SimulationManager(gameCanvas);

        simulationManager.setStatsUpdateCallback(this::updateStatsLabel);
        
        // Inizializza UI
        updateUI();
        
        // Auto-start della simulazione
        handleStart();
    }

    private void updateStatsLabel(long activeAnts, long activeFood, double fps) {
        // Aggiorna la UI sul thread JavaFX
        Platform.runLater(() -> {
            if (statsLabel != null) {
                statsLabel.setText(String.format("Ants: %d | Food: %d | FPS: %.0f", 
                    activeAnts, activeFood, fps));
            }
        });
    }

    private void waitForCanvasReady() {
        double width = gameCanvas.getWidth();
        double height = gameCanvas.getHeight();
                
        // Se le dimensioni non sono ancora valide, riprova
        if (width <= 100 || height <= 100) { // Soglia ragionevole invece di 0
            Platform.runLater(() -> waitForCanvasReady());
            return;
        }
        
        // Canvas è pronto, avvia la simulazione
        startSimulationNow(width, height);
    }

    public void startSimulationNow(double width, double height) {
        
        // Crea simulazione
        FullSimulation simulation = new FullSimulation(
            gameCanvas.getWidth(), 
            gameCanvas.getHeight()
        );

        System.out.println("Starting simulation with " + simulation.ANTS_NUMBER + " ants and " + simulation.FOODS_NUMBER + " food");

        // Avvia simulazione
        simulationManager.startSimulation(simulation);
        
        updateUI();
    }

    @FXML
    private void handleStart() {
        if (gameCanvas == null || simulationManager == null) {
            System.err.println("Canvas or SimulationManager not initialized yet");
            return;
        }
                
        waitForCanvasReady();
    
    }

    /*
    @FXML
    private void handleDemo() {
        if (gameCanvas == null || simulationManager == null) return;
        
        System.out.println("Starting demo simulation");
        
        // Crea demo simulation
        DemoSimulation demo = new DemoSimulation(
            gameCanvas.getWidth(), 
            gameCanvas.getHeight()
        );
        
        simulationManager.startSimulation(demo);
        updateUI();
    }
    */

    @FXML  
    private void handlePause() {
        System.out.println("Pause requested");

        if (simulationManager == null) return;
        
        simulationManager.togglePause();
        updateUI();
        
    }

    @FXML
    private void handleBackToMenu() {
        System.out.println("Returning to main menu");
        
        try {
            // Ferma la simulazione corrente
            if (simulationManager != null) {
                simulationManager.stopSimulation();
            }
            
            // Torna al menu principale
            App.setRoot("startWindow");
            
        } catch (IOException e) {
            System.err.println("Error returning to menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateUI() {

        if (simulationManager == null) return;
        
        boolean running = simulationManager.isRunning();
        
        if (pauseButton != null) {
            pauseButton.setText(simulationManager.isPaused() ? "Start" : "Pause");
        }
        
        if (statusLabel != null) {
            if (running) {
                statusLabel.setText("Running");
            } else {
                statusLabel.setText("Paused");
            }
        }
    }

    // Getters per eventuale accesso dall'esterno
    public SimulationManager getSimulationManager() {
        return simulationManager;
    }

}