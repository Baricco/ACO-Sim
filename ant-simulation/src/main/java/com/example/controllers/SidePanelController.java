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
    @FXML private Slider antSensorRadiusSlider;
    @FXML private Label antSensorRadiusValue;
    @FXML private Slider antFeelRadiusSlider;
    @FXML private Label antFeelRadiusValue;
    @FXML private Slider antSpeedSlider;
    @FXML private Label antSpeedValue;
    @FXML private Slider explorationRateSlider;
    @FXML private Label explorationRateValue;
    @FXML private Slider antPheromoneSensibilitySlider;
    @FXML private Label antPheromoneSensibilityValue;

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

        // Evaporation rate
        setupSlider(
            evaporationRateSlider,
            evaporationRateValue, 
            SimulationParameters.Constraints.EVAPORATION_RATE_MIN, 
            SimulationParameters.Constraints.EVAPORATION_RATE_MAX, 
            params.getEvaporationRate(), 
            2, 
            params::setEvaporationRate
        );

        // Max intensity
        setupSlider(
            maxIntensitySlider, 
            maxIntensityValue, 
            SimulationParameters.Constraints.MAX_INTENSITY_MIN, 
            SimulationParameters.Constraints.MAX_INTENSITY_MAX, 
            params.getMaxIntensity(), 
            1, 
            params::setMaxIntensity
        );

        // Min intensity
        setupSlider(
            minIntensitySlider, 
            minIntensityValue, 
            SimulationParameters.Constraints.MIN_INTENSITY_MIN, 
            SimulationParameters.Constraints.MIN_INTENSITY_MAX, 
            params.getMinIntensity(), 
            2, 
            params::setMinIntensity
        );

        // Initial intensity
        setupSlider(
            initialIntensitySlider,
            initialIntensityValue, 
            SimulationParameters.Constraints.INITIAL_INTENSITY_MIN, 
            SimulationParameters.Constraints.INITIAL_INTENSITY_MAX, 
            params.getInitialIntensity(), 
            1, 
            params::setInitialIntensity
        );

        // Max trail length
        setupSlider(
            maxTrailLengthSlider,
            maxTrailLengthValue, 
            SimulationParameters.Constraints.MAX_TRAIL_LENGTH_MIN, 
            SimulationParameters.Constraints.MAX_TRAIL_LENGTH_MAX, 
            (int)params.getMaxPheromoneTrailLength(), 
            v -> params.setMaxPheromoneTrailLength(v)
        );

        // Ant sight radius
        setupSlider(
            antSightRadiusSlider,
            antSightRadiusValue, 
            SimulationParameters.Constraints.ANT_SIGHT_RADIUS_MIN, 
            SimulationParameters.Constraints.ANT_SIGHT_RADIUS_MAX, 
            params.getAntSightRadius(), 
            params::setAntSightRadius
        );

        // Ant feel radius
        setupSlider(
            antFeelRadiusSlider,
            antFeelRadiusValue, 
            SimulationParameters.Constraints.ANT_FEEL_RADIUS_MIN, 
            SimulationParameters.Constraints.ANT_FEEL_RADIUS_MAX, 
            params.getAntFeelRadius(), 
            params::setAntFeelRadius
        );

        setupSlider(
            antSensorRadiusSlider,
            antSensorRadiusValue, 
            SimulationParameters.Constraints.ANT_SENSOR_RADIUS_MIN, 
            SimulationParameters.Constraints.ANT_SENSOR_RADIUS_MAX, 
            params.getAntSensorRadius(),
            params::setAntSensorRadius
        );

        // Ant speed
        setupSlider(
            antSpeedSlider,
            antSpeedValue, 
            SimulationParameters.Constraints.ANT_SPEED_MIN, 
            SimulationParameters.Constraints.ANT_SPEED_MAX, 
            (int)params.getAntSpeed(), 
            v -> params.setAntSpeed(v)
        );

        // Exploration rate
        setupSlider(
            explorationRateSlider,
            explorationRateValue, 
            SimulationParameters.Constraints.EXPLORATION_RATE_MIN, 
            SimulationParameters.Constraints.EXPLORATION_RATE_MAX, 
            params.getExplorationRate(), 
            2, 
            params::setExplorationRate
        );

        setupSlider(
            antPheromoneSensibilitySlider, 
            antPheromoneSensibilityValue, 
            SimulationParameters.Constraints.ANT_PHEROMONE_SENSIBILITY_MIN, 
            SimulationParameters.Constraints.ANT_PHEROMONE_SENSIBILITY_MAX, 
            params.getAntPheromoneSensibility(), 
            2, 
            params::setAntPheromoneSensibility
        );

        // Diffusion rate
        setupSlider(
            diffusionRateSlider,
            diffusionRateValue, 
            SimulationParameters.Constraints.DIFFUSION_RATE_MIN, 
            SimulationParameters.Constraints.DIFFUSION_RATE_MAX, 
            params.getDiffusionRate(), 
            1, 
            params::setDiffusionRate
        );
    }

    private void setupSlider(Slider slider, Label valueLabel, 
                                double min, double max, double initialValue, 
                                int decimalPlaces, java.util.function.DoubleConsumer setter) {
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(initialValue);
        valueLabel.setText(String.format("%." + decimalPlaces + "f", initialValue));

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = Math.round(newVal.doubleValue() * Math.pow(10, decimalPlaces)) / Math.pow(10, decimalPlaces);
            valueLabel.setText(String.format("%." + decimalPlaces + "f", value));
            setter.accept(value);
        });
    }

    private void setupSlider(Slider slider, Label valueLabel, 
                            double min, double max, int initialValue, 
                            java.util.function.IntConsumer setter) {
        slider.setMin(min);
        slider.setMax(max);
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
        antSensorRadiusValue.setText(String.valueOf(params.getAntSensorRadius()));
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