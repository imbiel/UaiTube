package uaitube.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uaitube.service.YoutubeService;

import java.io.File;

public class UaiTubeUI extends Application {

    private String downloadPath = System.getProperty("user.home") + "\\Music";

    @Override
    public void start(Stage stage) {

        // 🔹 Inicializa Spring
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext("uaitube");

        YoutubeService service = context.getBean(YoutubeService.class);

        // 🔹 Título
        Label title = new Label("🎧 UaiTube");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // 🔹 Campo URL
        TextField urlField = new TextField();
        urlField.setPromptText("Cole a URL do YouTube...");

        // 🔹 Pasta
        Label folderLabel = new Label("Pasta: " + downloadPath);

        Button chooseFolder = new Button("Selecionar Pasta");
        chooseFolder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Escolher pasta de download");

            File selected = chooser.showDialog(stage);

            if (selected != null) {
                downloadPath = selected.getAbsolutePath();
                folderLabel.setText("Pasta: " + downloadPath);
            }
        });

        // 🔹 Info
        Label infoLabel = new Label("Aguardando URL...");
        infoLabel.setStyle("-fx-text-fill: gray;");

        // 🔹 Barra de progresso
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);

        // 🔹 Botão download
        Button downloadBtn = new Button("⬇ Download");

        downloadBtn.setOnAction(e -> {

            String url = urlField.getText();

            if (url == null || url.isEmpty()) {
                showAlert("Cole uma URL válida!");
                return;
            }

            boolean isPlaylist = url.contains("playlist");

            if (isPlaylist) {
                infoLabel.setText("📂 Playlist detectada...");
            } else {
                infoLabel.setText("🎵 Música detectada...");
            }

            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        progressBar.setProgress(-1); // indeterminado
                    });

                    service.downloadWithPath(url, downloadPath);

                    Platform.runLater(() -> {
                        progressBar.setProgress(1);
                        infoLabel.setText("✅ Download concluído!");
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        progressBar.setProgress(0);
                        showAlert("Erro: " + ex.getMessage());
                    });
                }
            }).start();
        });

        // 🔹 Layout
        VBox layout = new VBox(15,
                title,
                urlField,
                chooseFolder,
                folderLabel,
                infoLabel,
                progressBar,
                downloadBtn
        );

        layout.setPadding(new Insets(20));

        // 🔹 Cena
        Scene scene = new Scene(layout, 450, 350);

        stage.setTitle("UaiTube");
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}