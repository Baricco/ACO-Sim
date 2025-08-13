package com.example.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.example.config.SimulationParameters;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

/**
 * Controller per il dialog delle opzioni di simulazione
 */
public class OptionsController implements Initializable {

    @FXML private Slider nestNumberSlider;
    @FXML private Slider antNumberSlider;
    @FXML private Slider clumpSizeSlider;
    @FXML private Slider clumpNumberSlider;
    
    @FXML private Label nestNumberValue;
    @FXML private Label antNumberValue;
    @FXML private Label clumpSizeValue;
    @FXML private Label clumpNumberValue;
    
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button okButton;

    private SimulationParameters params;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        params = SimulationParameters.getInstance();
        setupSliders();
        updateLabels();
    }

    private void setupSliders() {
        // Imposta valori iniziali
        nestNumberSlider.setValue(params.getNestNumber());
        antNumberSlider.setValue(params.getAntNumber());
        clumpSizeSlider.setValue(params.getClumpSize());
        clumpNumberSlider.setValue(params.getClumpNumber());

        // Aggiungi listener per aggiornare parametri e label
        nestNumberSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            nestNumberValue.setText(String.valueOf(value));
            params.setNestNumber(value);
        });

        antNumberSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            antNumberValue.setText(String.valueOf(value));
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
    }

    private void updateLabels() {
        nestNumberValue.setText(String.valueOf(params.getNestNumber()));
        antNumberValue.setText(String.valueOf(params.getAntNumber()));
        clumpSizeValue.setText(String.valueOf(params.getClumpSize()));
        clumpNumberValue.setText(String.valueOf(params.getClumpNumber()));
    }

    @FXML
    private void resetToDefaults() {
        params.resetToDefaults();
        
        // Aggiorna slider (che automaticamente aggiornano le label)
        nestNumberSlider.setValue(params.getNestNumber());
        antNumberSlider.setValue(params.getAntNumber());
        clumpSizeSlider.setValue(params.getClumpSize());
        clumpNumberSlider.setValue(params.getClumpNumber());
    }

    @FXML
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void startSimulation() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }
}