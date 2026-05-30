package org.gymbrot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.gymbrot.service.MembresiaScheduler;

public class Main extends Application {

    public static StackPane     contentArea;
    public static Parent        titleBarNode;
    public static org.gymbrot.TitleBarController titleBarCtrl;

    private MembresiaScheduler membresiaScheduler;

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

        stage.initStyle(StageStyle.TRANSPARENT);

        FXMLLoader barLoader = new FXMLLoader(
                getClass().getResource("/fxml/TitleBar.fxml"));
        Parent titleBar = barLoader.load();
        titleBarCtrl = barLoader.getController();
        titleBarNode = titleBar;
        titleBarCtrl.init(stage);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #111316;");
        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);

        Parent loginView = FXMLLoader.load(
                getClass().getResource("/fxml/login.fxml"));
        contentArea.getChildren().add(loginView);

        VBox root = new VBox(titleBar, contentArea);
        root.setStyle("-fx-background-color: #111316; -fx-padding: 0; -fx-background-insets: 0;");

        Scene scene = new Scene(root, 1040, 700);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("/css/gymbrot.css").toExternalForm());

        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);

        try {
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.png"),
                            1024, 1024, true, true));
        } catch (Exception e) {
            System.out.println("Icono no encontrado: " + e.getMessage());
        }

        stage.setTitle("GYMBROT");

        // ── Scheduler de membresías ───────────────────────────────────────
        membresiaScheduler = new MembresiaScheduler();
        membresiaScheduler.iniciar();

        stage.show();
    }

    // ── Detener scheduler al cerrar la app ────────────────────────────────
    @Override
    public void stop() throws Exception {
        super.stop();
        if (membresiaScheduler != null) {
            membresiaScheduler.detener();
        }
    }

    // ── Navegación global ─────────────────────────────────────────────────
    private static String tituloParaRuta(String ruta) {
        return switch (ruta) {
            case "/fxml/Dashboard.fxml"          -> "Dashboard";
            case "/fxml/GestionClientes.fxml"    -> "Clientes";
            case "/fxml/GestionInstructores.fxml"-> "Instructores";
            case "/fxml/GestionMembresias.fxml"  -> "Planes";
            case "/fxml/GymbroAI.fxml"           -> "Gymbro AI";
            case "/fxml/ProgresoFisico.fxml"     -> "Progreso Físico";
            case "/fxml/GestionCitas.fxml"       -> "Citas";
            case "/fxml/NuevoCliente.fxml"       -> "Nuevo Cliente";
            case "/fxml/NuevoInstructor.fxml"    -> "Nuevo Instructor";
            default -> "";
        };
    }

    public static void navegarA(String rutaFxml, String tituloBarra) {
        try {
            Parent vista = FXMLLoader.load(
                    Main.class.getResource(rutaFxml));
            contentArea.getChildren().setAll(vista);
            titleBarNode.setVisible(true);
            titleBarNode.setManaged(true);
            if (titleBarCtrl != null) {
                titleBarCtrl.setTitulo(tituloBarra);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void navegarA(String rutaFxml) {
        navegarA(rutaFxml, tituloParaRuta(rutaFxml));
    }

    public static void main(String[] args) {
        launch(args);
    }
}