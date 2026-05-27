package org.gymbrot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-SemiBold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-ExtraBold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Black.ttf"), 14);

        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_24pt-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_24pt-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_24pt-SemiBold.ttf"), 14);

        Font.loadFont(getClass().getResourceAsStream("/fonts/SpaceGrotesk-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/SpaceGrotesk-Bold.ttf"), 14);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

        Scene scene = new Scene(root, 1040, 700);
        scene.getStylesheets().add(getClass().getResource("/css/gymbrot.css").toExternalForm());

        stage.setTitle("GYMBROT");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);

        scene.setFill(javafx.scene.paint.Color.web("#111316"));

        try {
            stage.getIcons().addAll(
                    new Image(getClass().getResourceAsStream("/images/logo.png"), 1024, 1024, true, true)
            );
        } catch (Exception e) {
            System.out.println("Error cargando icono: " + e.getMessage());
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}