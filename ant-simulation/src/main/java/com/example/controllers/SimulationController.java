package com.example.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.App;
import com.example.graphics.GameCanvas;
import com.example.managers.SimulationManager;
import com.example.metrics.MetricsCollector;
import com.example.simulation.DemoSimulation;
import com.example.simulation.DoubleBridgeSimulation;
import com.example.simulation.FullSimulation;
import com.example.simulation.Simulation;
import com.example.simulation.SimulationType;
import com.example.simulation.TJunctionSimulation;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 * Controller per la scena di simulazione
 */
public class SimulationController implements Initializable {

    @FXML private Pane canvasContainer;
    @FXML private Pane sidePanelContainer;
    @FXML private Button pauseButton;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;

    private GameCanvas gameCanvas;
    private SimulationManager simulationManager;
    private SidePanelController sidePanelController;

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

        // Crea il canvas per il rendering
        gameCanvas = new GameCanvas(width, height);
        
        // Binding per il ridimensionamento
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty());
        
        canvasContainer.getChildren().add(gameCanvas);
        
        // Carica il side panel
        loadSidePanel();
        
        // Crea il simulation manager
        simulationManager = new SimulationManager(gameCanvas);
        simulationManager.setStatsUpdateCallback(this::updateStatsLabel);
        
        // Inizializza UI
        updateUI();
        
        // Auto-start della simulazione
        handleStart();
    }

    private void loadSidePanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/sidePanel.fxml"));
            Node sidePanel = loader.load();
            
            // Ottieni il controller del sidePanel
            sidePanelController = loader.getController();
            sidePanelController.setParentController(this);
            
            sidePanelContainer.getChildren().add(sidePanel);
            
        } catch (IOException e) {
            System.err.println("Error loading sidePanel.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatsLabel(long activeAnts, long activeFood, double fps) {
        
        double antStats[] = simulationManager.getSelectedAntStats();

        // Delega l'aggiornamento delle ant stats al sidePanelController
        if (sidePanelController != null) {
            Platform.runLater(() -> {
                sidePanelController.updateAntStats(antStats);
            });
        }

        // Aggiorna la Top UI sul thread JavaFX
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
        if (width <= 100 || height <= 100) {
            Platform.runLater(() -> waitForCanvasReady());
            return;
        }
        
        // Canvas è pronto, avvia la simulazione
        startSimulationNow(width, height);
    }

    public void startSimulationNow(double width, double height) {
        
        Simulation simulation;
        SimulationType experimentType = App.getSelectedSimulation();

        switch (experimentType) {
            case DOUBLE_BRIDGE:
                simulation = new DoubleBridgeSimulation(width, height);
                break;
            case T_JUNCTION:
                simulation = new TJunctionSimulation(width, height);
                break;
            case DEMO:
                simulation = new DemoSimulation(width, height);
                break;
            default:
                simulation = new FullSimulation(width, height);
        }
        
        System.out.println("Starting " + experimentType.getDisplayName() + 
                        " with " + simulation.ANTS_NUMBER + " ants");
        
        simulationManager.startSimulation(simulation);
        updateUI();
    }

    @FXML
    private void exportMetrics() {
        try {
            String filename = "experiment_" + System.currentTimeMillis() + ".csv";
            MetricsCollector.getInstance().exportToCSV(filename);
            System.out.println("Metrics exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Error exporting metrics: " + e.getMessage());
        }
    }

    @FXML
    private void handleStart() {
        if (gameCanvas == null || simulationManager == null) {
            System.err.println("Canvas or SimulationManager not initialized yet");
            return;
        }
                
        waitForCanvasReady();
    }

    @FXML
    public void selectNextAnt() {
        if (simulationManager == null) return;
        
        simulationManager.selectNextAnt();
        updateUI();
    }

    @FXML
    public void selectPreviousAnt() {
        if (simulationManager == null) return;
        
        simulationManager.selectPreviousAnt();
        updateUI();
    }

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