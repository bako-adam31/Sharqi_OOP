package org.example.sharqi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ParfumApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ParfumApplication.class.getResource("parfum-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 750);
        stage.setTitle("Parfum Kereso");
        stage.setScene(scene);
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}