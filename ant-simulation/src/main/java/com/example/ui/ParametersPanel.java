package com.example.ui;

import com.example.config.SimulationParameters;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

/**
 * Pannello per i parametri real-time della simulazione
 * Viene mostrato nella parte sinistra dell'interfaccia durante la simulazione
 */
public class ParametersPanel extends VBox {

    private SimulationParameters params;

    // Slider e label per Pheromone
    private Slider evaporationRateSlider;
    private Label evaporationRateValue;
    private Slider maxIntensitySlider;
    private Label maxIntensityValue;
    private Slider minIntensitySlider;
    private Label minIntensityValue;
    private Slider initialIntensitySlider;
    private Label initialIntensityValue;
    private Slider maxTrailLengthSlider;
    private Label maxTrailLengthValue;

    // Slider e label per Ant
    private Slider antSightRadiusSlider;
    private Label antSightRadiusValue;
    private Slider antFeelRadiusSlider;
    private Label antFeelRadiusValue;
    private Slider antSpeedSlider;
    private Label antSpeedValue;
    private Slider explorationRateSlider;
    private Label explorationRateValue;

    // Slider e label per Density Field
    private Slider diffusionRateSlider;
    private Label diffusionRateValue;

    public ParametersPanel() {
        this.params = SimulationParameters.getInstance();
        setupUI();
    }

    private void setupUI() {
        setSpacing(5);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_LEFT);
        setPrefWidth(250);

        // Titolo
        Label titleLabel = new Label("Real-time Parameters");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        getChildren().add(titleLabel);
        getChildren().add(new Separator());

        // Sezione Pheromone
        addSectionHeader("Pheromone");
        
        createSlider("Evaporation Rate:", 
                     params.getEvaporationRate(), 
                     SimulationParameters.Constraints.EVAPORATION_RATE_MIN, 
                     SimulationParameters.Constraints.EVAPORATION_RATE_MAX,
                     0.01, 3,
                     (slider, valueLabel) -> {
                         evaporationRateSlider = slider;
                         evaporationRateValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             double value = Math.round(newVal.doubleValue() * 100.0) / 100.0;
                             valueLabel.setText(String.format("%.2f", value));
                             params.setEvaporationRate(value);
                         });
                     });

        createSlider("Max Intensity:", 
                     params.getMaxIntensity(), 
                     SimulationParameters.Constraints.MAX_INTENSITY_MIN, 
                     SimulationParameters.Constraints.MAX_INTENSITY_MAX,
                     0.1, 2,
                     (slider, valueLabel) -> {
                         maxIntensitySlider = slider;
                         maxIntensityValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             double value = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                             valueLabel.setText(String.format("%.1f", value));
                             params.setMaxIntensity(value);
                         });
                     });

        createSlider("Min Intensity:", 
                     params.getMinIntensity(), 
                     SimulationParameters.Constraints.MIN_INTENSITY_MIN, 
                     SimulationParameters.Constraints.MIN_INTENSITY_MAX,
                     0.01, 3,
                     (slider, valueLabel) -> {
                         minIntensitySlider = slider;
                         minIntensityValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             double value = Math.round(newVal.doubleValue() * 100.0) / 100.0;
                             valueLabel.setText(String.format("%.2f", value));
                             params.setMinIntensity(value);
                         });
                     });

        createSlider("Initial Intensity:", 
                     params.getInitialIntensity(), 
                     SimulationParameters.Constraints.INITIAL_INTENSITY_MIN, 
                     SimulationParameters.Constraints.INITIAL_INTENSITY_MAX,
                     0.1, 2,
                     (slider, valueLabel) -> {
                         initialIntensitySlider = slider;
                         initialIntensityValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             double value = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                             valueLabel.setText(String.format("%.1f", value));
                             params.setInitialIntensity(value);
                         });
                     });

        createSlider("Max Trail Length:", 
                     params.getMaxPheromoneTrailLength(), 
                     SimulationParameters.Constraints.MAX_TRAIL_LENGTH_MIN, 
                     SimulationParameters.Constraints.MAX_TRAIL_LENGTH_MAX,
                     50, 2,
                     (slider, valueLabel) -> {
                         maxTrailLengthSlider = slider;
                         maxTrailLengthValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             int value = (int) Math.round(newVal.doubleValue());
                             valueLabel.setText(String.valueOf(value));
                             params.setMaxPheromoneTrailLength(value);
                         });
                     });

        getChildren().add(new Separator());

        // Sezione Ant
        addSectionHeader("Ant Behavior");

        createSlider("Sight Radius:", 
                     params.getAntSightRadius(), 
                     SimulationParameters.Constraints.ANT_SIGHT_RADIUS_MIN, 
                     SimulationParameters.Constraints.ANT_SIGHT_RADIUS_MAX,
                     10, 2,
                     (slider, valueLabel) -> {
                         antSightRadiusSlider = slider;
                         antSightRadiusValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             int value = (int) Math.round(newVal.doubleValue());
                             valueLabel.setText(String.valueOf(value));
                             params.setAntSightRadius(value);
                         });
                     });

        createSlider("Feel Radius:", 
                     params.getAntFeelRadius(), 
                     SimulationParameters.Constraints.ANT_FEEL_RADIUS_MIN, 
                     SimulationParameters.Constraints.ANT_FEEL_RADIUS_MAX,
                     10, 2,
                     (slider, valueLabel) -> {
                         antFeelRadiusSlider = slider;
                         antFeelRadiusValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             int value = (int) Math.round(newVal.doubleValue());
                             valueLabel.setText(String.valueOf(value));
                             params.setAntFeelRadius(value);
                         });
                     });

        createSlider("Speed:", 
                     params.getAntSpeed(), 
                     SimulationParameters.Constraints.ANT_SPEED_MIN, 
                     SimulationParameters.Constraints.ANT_SPEED_MAX,
                     25, 2,
                     (slider, valueLabel) -> {
                         antSpeedSlider = slider;
                         antSpeedValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             int value = (int) Math.round(newVal.doubleValue());
                             valueLabel.setText(String.valueOf(value));
                             params.setAntSpeed(value);
                         });
                     });

        createSlider("Exploration Rate:", 
                     params.getExplorationRate(), 
                     SimulationParameters.Constraints.EXPLORATION_RATE_MIN, 
                     SimulationParameters.Constraints.EXPLORATION_RATE_MAX,
                     0.1, 2,
                     (slider, valueLabel) -> {
                         explorationRateSlider = slider;
                         explorationRateValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             double value = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                             valueLabel.setText(String.format("%.1f", value));
                             params.setExplorationRate(value);
                         });
                     });

        getChildren().add(new Separator());

        // Sezione Density Field
        addSectionHeader("Field Diffusion");

        createSlider("Diffusion Rate:", 
                     params.getDiffusionRate(), 
                     SimulationParameters.Constraints.DIFFUSION_RATE_MIN, 
                     SimulationParameters.Constraints.DIFFUSION_RATE_MAX,
                     0.1, 2,
                     (slider, valueLabel) -> {
                         diffusionRateSlider = slider;
                         diffusionRateValue = valueLabel;
                         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                             double value = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                             valueLabel.setText(String.format("%.1f", value));
                             params.setDiffusionRate(value);
                         });
                     });

        getChildren().add(new Separator());

        // Pulsante Reset
        Button resetButton = new Button("Reset to Default");
        resetButton.setOnAction(e -> resetToDefaults());
        resetButton.setPrefWidth(200);

        getChildren().add(resetButton);
    }

    private void addSectionHeader(String text) {
        Label sectionLabel = new Label(text);
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        getChildren().add(sectionLabel);
    }

    private void createSlider(String labelText, double currentValue, 
                             double min, double max, double majorTick, int decimalPlaces,
                             SliderCreatedCallback callback) {
        
        Label nameLabel = new Label(labelText);
        nameLabel.setStyle("-fx-font-size: 10px;");
        
        Slider slider = new Slider(min, max, currentValue);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setMajorTickUnit(majorTick);
        slider.setPrefWidth(180);
        
        Label valueLabel = new Label();
        valueLabel.setPrefWidth(50);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        valueLabel.setStyle("-fx-font-size: 10px;");
        
        // Formatta il valore iniziale
        if (decimalPlaces == 0) {
            valueLabel.setText(String.valueOf((int)currentValue));
        } else {
            valueLabel.setText(String.format("%." + decimalPlaces + "f", currentValue));
        }
        
        getChildren().addAll(nameLabel, slider, valueLabel);
        
        callback.onSliderCreated(slider, valueLabel);
    }

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
        
        // Aggiorna le label
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

    @FunctionalInterface
    private interface SliderCreatedCallback {
        void onSliderCreated(Slider slider, Label valueLabel);
    }
}