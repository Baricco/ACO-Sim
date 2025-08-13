package com.example.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.example.config.SimulationParameters;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

/**
 * Controller per il pannello laterale della simulazione
 * Gestisce le statistiche delle formiche e i parametri real-time
 */
public class SidePanelController implements Initializable {

    // Ant Stats Labels
    @FXML private Label serialNumberLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label LFDTLabel;
    @FXML private Label LNDTLabel;
    @FXML private Label LTTLabel;
    @FXML private Label MTTLabel;

    // Navigation Buttons
    @FXML private Button prevAntButton;
    @FXML private Button nextAntButton;

    // Pheromone Parameters
    @FXML private Slider evaporationRateSlider;
    @FXML private Label evaporationRateValue;
    @FXML private Slider maxIntensitySlider;
    @FXML private Label maxIntensityValue;
    @FXML private Slider minIntensitySlider;
    @FXML private Label minIntensityValue;
    @FXML private Slider initialIntensitySlider;
    @FXML private Label initialIntensityValue;
    @FXML private Slider maxTrailLengthSlider;
    @FXML private Label maxTrailLengthValue;

    // Ant Parameters
    @FXML private Slider antSightRadiusSlider;
    @FXML private Label antSightRadiusValue;
    @FXML private Slider antFeelRadiusSlider;
    @FXML private Label antFeelRadiusValue;
    @FXML private Slider antSpeedSlider;
    @FXML private Label antSpeedValue;
    @FXML private Slider explorationRateSlider;
    @FXML private Label explorationRateValue;

    // Density Field Parameters
    @FXML private Slider diffusionRateSlider;
    @FXML private Label diffusionRateValue;

    // Reset Button
    @FXML private Button resetButton;

    private SimulationParameters params;
    private SimulationController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        params = SimulationParameters.getInstance();
        setupSliders();
        updateAllLabels();
    }

    /**
     * Imposta il riferimento al controller padre per comunicare
     */
    public void setParentController(SimulationController parentController) {
        this.parentController = parentController;
    }

    private void setupSliders() {
        setupDoubleSlider(evaporationRateSlider, evaporationRateValue, 
                         params.getEvaporationRate(), 2, params::setEvaporationRate);
        setupDoubleSlider(maxIntensitySlider, maxIntensityValue, 
                         params.getMaxIntensity(), 1, params::setMaxIntensity);
        setupDoubleSlider(minIntensitySlider, minIntensityValue, 
                         params.getMinIntensity(), 2, params::setMinIntensity);
        setupDoubleSlider(initialIntensitySlider, initialIntensityValue, 
                         params.getInitialIntensity(), 1, params::setInitialIntensity);
        setupIntSlider(maxTrailLengthSlider, maxTrailLengthValue, 
                      (int)params.getMaxPheromoneTrailLength(), v -> params.setMaxPheromoneTrailLength(v));

        setupIntSlider(antSightRadiusSlider, antSightRadiusValue, 
                      params.getAntSightRadius(), params::setAntSightRadius);
        setupIntSlider(antFeelRadiusSlider, antFeelRadiusValue, 
                      params.getAntFeelRadius(), params::setAntFeelRadius);
        setupIntSlider(antSpeedSlider, antSpeedValue, 
                      (int)params.getAntSpeed(), v -> params.setAntSpeed(v));
        setupDoubleSlider(explorationRateSlider, explorationRateValue, 
                         params.getExplorationRate(), 1, params::setExplorationRate);

        setupDoubleSlider(diffusionRateSlider, diffusionRateValue, 
                         params.getDiffusionRate(), 1, params::setDiffusionRate);
    }

    private void setupDoubleSlider(Slider slider, Label valueLabel, 
                                  double initialValue, int decimalPlaces, 
                                  java.util.function.DoubleConsumer setter) {
        slider.setValue(initialValue);
        valueLabel.setText(String.format("%." + decimalPlaces + "f", initialValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = Math.round(newVal.doubleValue() * Math.pow(10, decimalPlaces)) / Math.pow(10, decimalPlaces);
            valueLabel.setText(String.format("%." + decimalPlaces + "f", value));
            setter.accept(value);
        });
    }

    private void setupIntSlider(Slider slider, Label valueLabel, 
                               int initialValue, java.util.function.IntConsumer setter) {
        slider.setValue(initialValue);
        valueLabel.setText(String.valueOf(initialValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = (int) Math.round(newVal.doubleValue());
            valueLabel.setText(String.valueOf(value));
            setter.accept(value);
        });
    }

    private void updateAllLabels() {
        evaporationRateValue.setText(String.format("%.2f", params.getEvaporationRate()));
        maxIntensityValue.setText(String.format("%.1f", params.getMaxIntensity()));
        minIntensityValue.setText(String.format("%.2f", params.getMinIntensity()));
        initialIntensityValue.setText(String.format("%.1f", params.getInitialIntensity()));
        maxTrailLengthValue.setText(String.valueOf((int)params.getMaxPheromoneTrailLength()));
        antSightRadiusValue.setText(String.valueOf(params.getAntSightRadius()));
        antFeelRadiusValue.setText(String.valueOf(params.getAntFeelRadius()));
        antSpeedValue.setText(String.valueOf((int)params.getAntSpeed()));
        explorationRateValue.setText(String.format("%.1f", params.getExplorationRate()));
        diffusionRateValue.setText(String.format("%.1f", params.getDiffusionRate()));
    }

    /**
     * Aggiorna le statistiche della formica selezionata
     */
    public void updateAntStats(double[] antStats) {
        if (antStats == null) return;

        serialNumberLabel.setText(antStats[0] == -1 ? "N/A" : (int)antStats[0] + "");
        startTimeLabel.setText(antStats[1] == -1 ? "N/A" : antStats[1] + " s");
        LFDTLabel.setText(antStats[2] == -1 ? "N/A" : antStats[2] + " s");
        LNDTLabel.setText(antStats[3] == -1 ? "N/A" : antStats[3] + " s");
        LTTLabel.setText(antStats[4] == -1 ? "N/A" : antStats[4] + " s");
        MTTLabel.setText(antStats[5] == -1 ? "N/A" : antStats[5] + " s");
    }

    @FXML
    private void selectPreviousAnt() {
        if (parentController != null) {
            parentController.selectPreviousAnt();
        }
    }

    @FXML
    private void selectNextAnt() {
        if (parentController != null) {
            parentController.selectNextAnt();
        }
    }

    @FXML
    private void resetToDefaults() {
        params.resetToDefaults();
        
        // Aggiorna tutti gli slider
        evaporationRateSlider.setValue(params.getEvaporationRate());
        maxIntensitySlider.setValue(params.getMaxIntensity());
        minIntensitySlider.setValue(params.getMinIntensity());
        initialIntensitySlider.setValue(params.getInitialIntensity());
        maxTrailLengthSlider.setValue(params.getMaxPheromoneTrailLength());
        antSightRadiusSlider.setValue(params.getAntSightRadius());
        antFeelRadiusSlider.setValue(params.getAntFeelRadius());
        antSpeedSlider.setValue(params.getAntSpeed());
        explorationRateSlider.setValue(params.getExplorationRate());
        diffusionRateSlider.setValue(params.getDiffusionRate());
        
        updateAllLabels();
    }
}