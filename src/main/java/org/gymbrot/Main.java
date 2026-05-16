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

        // Cargar fuentes locales
        // Lexend
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-SemiBold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-ExtraBold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Lexend-Black.ttf"), 14);

        // Inter
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_24pt-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_24pt-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_24pt-SemiBold.ttf"), 14);

        // Space Grotesk
        Font.loadFont(getClass().getResourceAsStream("/fonts/SpaceGrotesk-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/SpaceGrotesk-Bold.ttf"), 14);

        // Cargar FXML después
        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/Dashboard.fxml")
        );

        Scene scene = new Scene(root, 1040, 700);

        // Debug: verificar que se cargaron
        javafx.scene.text.Font f = javafx.scene.text.Font.loadFont(
                getClass().getResourceAsStream("/fonts/Lexend-Bold.ttf"), 14);
        System.out.println("Fuente cargada: " + (f != null ? f.getName() : "NULL - archivo no encontrado"));

        // Configurar la ventana
        stage.setTitle("GYMBROT");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);

        // Ícono de la app (pon un archivo icon.png en src/main/resources/)
        try {
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icon.png"))
            );
        } catch (Exception e) {
            // Sin ícono si no existe el archivo, no rompe la app
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}