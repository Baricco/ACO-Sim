package com.example.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.example.config.SimulationParameters;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

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

    private Pane overlay;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        params = SimulationParameters.getInstance();
        setupSliders();
        updateLabels();
    }

    private void configureSlider(Slider slider, double min, double max, double value, double majorTick) {
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(value);
        slider.setMajorTickUnit(majorTick);
    }

    private void setupSliders() {

        // Imposta i valori degli slider
        configureSlider(nestNumberSlider, 
            SimulationParameters.Constraints.NEST_NUMBER_MIN,
            SimulationParameters.Constraints.NEST_NUMBER_MAX,
            params.getNestNumber(),
            (int)(SimulationParameters.Constraints.NEST_NUMBER_MAX - SimulationParameters.Constraints.NEST_NUMBER_MIN) / 4.0);
            
        configureSlider(antNumberSlider,
            SimulationParameters.Constraints.ANT_NUMBER_MIN,
            SimulationParameters.Constraints.ANT_NUMBER_MAX,
            params.getAntNumber(),
            (SimulationParameters.Constraints.ANT_NUMBER_MAX - SimulationParameters.Constraints.ANT_NUMBER_MIN) / 4.0);
            
        configureSlider(clumpSizeSlider,
            SimulationParameters.Constraints.CLUMP_SIZE_MIN,
            SimulationParameters.Constraints.CLUMP_SIZE_MAX,
            params.getClumpSize(),
            (SimulationParameters.Constraints.CLUMP_SIZE_MAX - SimulationParameters.Constraints.CLUMP_SIZE_MIN) / 4.0);
            
        configureSlider(clumpNumberSlider,
            SimulationParameters.Constraints.CLUMP_NUMBER_MIN,
            SimulationParameters.Constraints.CLUMP_NUMBER_MAX,
            params.getClumpNumber(),
            (SimulationParameters.Constraints.CLUMP_NUMBER_MAX - SimulationParameters.Constraints.CLUMP_NUMBER_MIN) / 4.0);


        // Converti le label degli slider ad int, in modo da non mostrare numeri decimali
        StringConverter<Double> intConverter = new StringConverter<Double>() {
            @Override
            public String toString(Double n) { return String.valueOf(n.intValue()); }
            @Override
            public Double fromString(String s) { return Double.valueOf(s); }
        };

        nestNumberSlider.setLabelFormatter(intConverter);
        antNumberSlider.setLabelFormatter(intConverter);
        clumpSizeSlider.setLabelFormatter(intConverter);
        clumpNumberSlider.setLabelFormatter(intConverter);

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

    public void setOverlay(Pane overlay) {
        this.overlay = overlay;
    }

    @FXML
    private void closeOverlay() {
        if (overlay != null && overlay.getParent() != null) {
            ((StackPane)overlay.getParent()).getChildren().remove(overlay);
        }
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

}