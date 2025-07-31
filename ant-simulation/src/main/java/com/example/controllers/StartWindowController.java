package com.example.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.App;
import com.example.graphics.GameCanvas;
import com.example.managers.SimulationManager;
import com.example.simulation.DemoSimulation;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

/**
 * Controller per la finestra di start (startWindow.fxml)
 */
public class StartWindowController implements Initializable {

    @FXML private Pane backgroundPane;
    @FXML private Button startButton;
    
    private GameCanvas demoCanvas;
    private SimulationManager demoManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("StartWindowController initialized");
        
        // Crea il canvas per la demo in background
        setupDemoBackground();
        
        // Avvia la demo simulation
        startDemoSimulation();
    }
    
    private void setupDemoBackground() {
        // Crea canvas che riempie tutto il background
        demoCanvas = new GameCanvas(1200, 800);
        demoCanvas.setBackgroundColor(javafx.scene.paint.Color.LIGHTGREEN);
        
        // Aggiungi il canvas al background pane
        backgroundPane.getChildren().add(demoCanvas);
        
        // Crea il simulation manager per la demo
        demoManager = new SimulationManager(demoCanvas);
    }
    
    private void startDemoSimulation() {
        // Crea una DemoSimulation di background
        DemoSimulation demoSimulation = new DemoSimulation(
            demoCanvas.getWidth(), 
            demoCanvas.getHeight()
        );
        
        // Avvia la simulazione demo
        demoManager.startSimulation(demoSimulation);
        
    }

    @FXML
    private void startFullSimulation() {
        
        try {
            // Ferma la demo
            demoManager.stopSimulation();
            
            // Passa alla scena di simulazione
            App.setRoot("simulation");
            
        } catch (IOException e) {
            System.err.println("Error loading simulation scene: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Chiamato quando si torna a questa finestra
     */
    public void onReturn() {
        // Riavvia la demo quando si torna al menu
        if (demoManager != null && !demoManager.isRunning()) {
            startDemoSimulation();
        }
    }
}