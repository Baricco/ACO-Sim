package com.example.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.App;
import com.example.config.SimulationParameters;
import com.example.graphics.GameCanvas;
import com.example.managers.SimulationManager;
import com.example.simulation.DemoSimulation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    
    @FXML
    private void showOptions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/options.fxml"));
            Parent root = loader.load();
            
            Stage optionsStage = new Stage();
            optionsStage.setTitle("Simulation Options");
            optionsStage.initModality(Modality.APPLICATION_MODAL);
            optionsStage.setScene(new Scene(root));
            optionsStage.setResizable(false);
            
            optionsStage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error loading options.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private void setupOptionsDialog(FXMLLoader loader, Stage stage) {
        Parent root = loader.getRoot();
        SimulationParameters params = SimulationParameters.getInstance();
        
        // Trova i controlli tramite lookup
        Slider nestSlider = (Slider) root.lookup("#nestNumberSlider");
        Slider antSlider = (Slider) root.lookup("#antNumberSlider");
        Slider clumpSizeSlider = (Slider) root.lookup("#clumpSizeSlider");
        Slider clumpNumberSlider = (Slider) root.lookup("#clumpNumberSlider");
        
        Label nestValue = (Label) root.lookup("#nestNumberValue");
        Label antValue = (Label) root.lookup("#antNumberValue");
        Label clumpSizeValue = (Label) root.lookup("#clumpSizeValue");
        Label clumpNumberValue = (Label) root.lookup("#clumpNumberValue");
        
        Button resetButton = (Button) root.lookup("#resetButton");
        Button cancelButton = (Button) root.lookup("#cancelButton");
        Button okButton = (Button) root.lookup("#okButton");
        
        // Imposta valori iniziali
        nestSlider.setValue(params.getNestNumber());
        antSlider.setValue(params.getAntNumber());
        clumpSizeSlider.setValue(params.getClumpSize());
        clumpNumberSlider.setValue(params.getClumpNumber());
        
        // Listener per aggiornare valori
        nestSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            nestValue.setText(String.valueOf(value));
            params.setNestNumber(value);
        });
        
        antSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            antValue.setText(String.valueOf(value));
            params.setAntNumber(value);
        });
        
        clumpSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            clumpSizeValue.setText(String.valueOf(value));
            params.setClumpSize(value);
        });
        
        clumpNumberSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            clumpNumberValue.setText(String.valueOf(value));
            params.setClumpNumber(value);
        });
        
        // Azioni pulsanti
        resetButton.setOnAction(e -> {
            params.resetToDefaults();
            nestSlider.setValue(params.getNestNumber());
            antSlider.setValue(params.getAntNumber());
            clumpSizeSlider.setValue(params.getClumpSize());
            clumpNumberSlider.setValue(params.getClumpNumber());
        });
        
        cancelButton.setOnAction(e -> stage.close());
        okButton.setOnAction(e -> stage.close());
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