package com.example.ui;

import com.example.config.SimulationParameters;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog per le impostazioni della simulazione (parametri di setup)
 * Mostrato dal menu iniziale prima di avviare la simulazione
 */
public class OptionsDialog {

    private Stage stage;
    private SimulationParameters params;
    private boolean confirmed = false;

    // Slider per i parametri
    private Slider nestNumberSlider;
    private Slider antNumberSlider;
    private Slider clumpSizeSlider;
    private Slider clumpNumberSlider;

    // Label per i valori
    private Label nestNumberValue;
    private Label antNumberValue;
    private Label clumpSizeValue;
    private Label clumpNumberValue;

    public OptionsDialog() {
        this.params = SimulationParameters.getInstance();
        createDialog();
    }

    private void createDialog() {
        stage = new Stage();
        stage.setTitle("Simulation Options");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);

        // Titolo
        Label titleLabel = new Label("Simulation Setup");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Griglia per i controlli
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Nest Number
        createSliderRow(grid, 0, "Nests:", 
            params.getNestNumber(), 
            SimulationParameters.Constraints.NEST_NUMBER_MIN, 
            SimulationParameters.Constraints.NEST_NUMBER_MAX,
            (slider, valueLabel) -> {
                nestNumberSlider = slider;
                nestNumberValue = valueLabel;
                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    int value = newVal.intValue();
                    valueLabel.setText(String.valueOf(value));
                    params.setNestNumber(value);
                });
            });

        // Ant Number
        createSliderRow(grid, 1, "Ants:", 
            params.getAntNumber(), 
            SimulationParameters.Constraints.ANT_NUMBER_MIN, 
            SimulationParameters.Constraints.ANT_NUMBER_MAX,
            (slider, valueLabel) -> {
                antNumberSlider = slider;
                antNumberValue = valueLabel;
                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    int value = newVal.intValue();
                    valueLabel.setText(String.valueOf(value));
                    params.setAntNumber(value);
                });
            });

        // Clump Size
        createSliderRow(grid, 2, "Food per Clump:", 
            params.getClumpSize(), 
            SimulationParameters.Constraints.CLUMP_SIZE_MIN, 
            SimulationParameters.Constraints.CLUMP_SIZE_MAX,
            (slider, valueLabel) -> {
                clumpSizeSlider = slider;
                clumpSizeValue = valueLabel;
                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    int value = newVal.intValue();
                    valueLabel.setText(String.valueOf(value));
                    params.setClumpSize(value);
                });
            });

        // Clump Number
        createSliderRow(grid, 3, "Food Clumps:", 
            params.getClumpNumber(), 
            SimulationParameters.Constraints.CLUMP_NUMBER_MIN, 
            SimulationParameters.Constraints.CLUMP_NUMBER_MAX,
            (slider, valueLabel) -> {
                clumpNumberSlider = slider;
                clumpNumberValue = valueLabel;
                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    int value = newVal.intValue();
                    valueLabel.setText(String.valueOf(value));
                    params.setClumpNumber(value);
                });
            });

        // Pulsanti
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button resetButton = new Button("Reset to Default");
        resetButton.setOnAction(e -> resetToDefaults());

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            confirmed = false;
            stage.close();
        });

        Button okButton = new Button("Start Simulation");
        okButton.setOnAction(e -> {
            confirmed = true;
            stage.close();
        });

        buttonBox.getChildren().addAll(resetButton, cancelButton, okButton);

        mainLayout.getChildren().addAll(titleLabel, grid, buttonBox);

        Scene scene = new Scene(mainLayout, 400, 300);
        stage.setScene(scene);
    }

    private void createSliderRow(GridPane grid, int row, String labelText, 
                                 double currentValue, double min, double max,
                                 SliderCreatedCallback callback) {
        
        Label label = new Label(labelText);
        
        Slider slider = new Slider(min, max, currentValue);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit((max - min) / 4);
        slider.setPrefWidth(200);
        
        Label valueLabel = new Label(String.valueOf((int)currentValue));
        valueLabel.setPrefWidth(50);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        
        grid.add(label, 0, row);
        grid.add(slider, 1, row);
        grid.add(valueLabel, 2, row);
        
        callback.onSliderCreated(slider, valueLabel);
    }

    private void resetToDefaults() {
        params.resetToDefaults();
        
        // Aggiorna slider
        nestNumberSlider.setValue(params.getNestNumber());
        antNumberSlider.setValue(params.getAntNumber());
        clumpSizeSlider.setValue(params.getClumpSize());
        clumpNumberSlider.setValue(params.getClumpNumber());
        
        // Aggiorna label
        nestNumberValue.setText(String.valueOf(params.getNestNumber()));
        antNumberValue.setText(String.valueOf(params.getAntNumber()));
        clumpSizeValue.setText(String.valueOf(params.getClumpSize()));
        clumpNumberValue.setText(String.valueOf(params.getClumpNumber()));
    }

    public boolean showAndWait() {
        stage.showAndWait();
        return confirmed;
    }

    @FunctionalInterface
    private interface SliderCreatedCallback {
        void onSliderCreated(Slider slider, Label valueLabel);
    }
}