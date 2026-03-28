package uaitube.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uaitube.service.YoutubeService;

import java.io.File;

public class UaiTubeUI extends Application {

    private String downloadPath = System.getProperty("user.home") + "\\Music";

    @Override
    public void start(Stage stage) {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext("uaitube");

        YoutubeService service = context.getBean(YoutubeService.class);

        // 🎨 Tema dark
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #121212;");

        Label title = new Label("UaiTube");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        TextField urlField = new TextField();
        urlField.setPromptText("Cole a URL...");
        urlField.setStyle("-fx-background-color: #282828; -fx-text-fill: white;");

        Label folderLabel = new Label("Pasta: " + downloadPath);
        folderLabel.setStyle("-fx-text-fill: #b3b3b3;");

        Button chooseFolder = new Button("📂 Pasta");
        styleButton(chooseFolder);

        chooseFolder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File file = chooser.showDialog(stage);
            if (file != null) {
                downloadPath = file.getAbsolutePath();
                folderLabel.setText("Pasta: " + downloadPath);
            }
        });

        Label info = new Label("Aguardando...");
        info.setStyle("-fx-text-fill: #b3b3b3;");

        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(400);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background:#181818; -fx-text-fill:white;");
        logArea.setPrefHeight(120);

        Button downloadBtn = new Button("⬇ Download");
        styleButton(downloadBtn);

        downloadBtn.setOnAction(e -> {

            String url = urlField.getText();

            if (url.isEmpty()) {
                alert("Cole uma URL!");
                return;
            }

            info.setText(url.contains("playlist") ? "📂 Playlist..." : "🎵 Música...");

            new Thread(() -> {
                try {

                    service.downloadWithProgress(
                            url,
                            downloadPath,

                            // progresso
                            p -> Platform.runLater(() -> progress.setProgress(p)),

                            // log
                            line -> Platform.runLater(() -> {
                                logArea.appendText(line + "\n");
                            })
                    );

                    Platform.runLater(() -> info.setText("✅ Concluído"));

                } catch (Exception ex) {
                    Platform.runLater(() -> alert(ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(
                title,
                urlField,
                chooseFolder,
                folderLabel,
                info,
                progress,
                downloadBtn,
                logArea
        );

        stage.setScene(new Scene(root, 500, 500));
        stage.setTitle("UaiTube");
        stage.show();
    }

    private void styleButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #1DB954;" +
                "-fx-text-fill: black;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;"
        );
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}