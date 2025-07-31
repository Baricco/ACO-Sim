package com.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Carica la finestra di start invece di primary
        scene = new Scene(loadFXML("startWindow"), 1200, 800);

        scene.getStylesheets().add(getClass().getResource("/com/example/css/style.css").toExternalForm());

        stage.setTitle("Ant Simulation");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    /**
     * Cambia la scena principale
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }
    
    /**
     * Ottieni la scena corrente (utile per i controller)
     */
    public static Scene getScene() {
        return scene;
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/example/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}