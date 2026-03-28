package uaitube.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uaitube.service.YoutubeService;
import javafx.scene.image.Image;

import java.io.File;

public class UaiTubeUI extends Application {

    private String downloadPath = System.getProperty("user.home") + "\\Music";

    @Override
    public void start(Stage stage) {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext("uaitube");

        YoutubeService service = context.getBean(YoutubeService.class);

        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icon.png"))
        );

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f0f0f;");

        Label title = new Label("UaiTube");
        title.setStyle("-fx-text-fill: #ff3b3b; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label folderLabelDesc = new Label("Realize o download de Músicas ou PlayList completas");
        folderLabelDesc.setStyle("-fx-text-fill: #b3b3b3;");

        VBox header = new VBox(5, title);
        header.setStyle("-fx-alignment: center;");

        TextField urlField = new TextField();
        urlField.setPromptText("Cole a URL...");
        urlField.setStyle("-fx-background-color: #1f1f1f; -fx-text-fill: white;");

        // 🔴 PASTA
        Label folderLabel = new Label(downloadPath);
        folderLabel.setStyle("-fx-text-fill: #b3b3b3;");
        folderLabel.setMaxWidth(Double.MAX_VALUE);

        Button chooseFolder = new Button("📂 Pasta");
        styleButton(chooseFolder);

        chooseFolder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File file = chooser.showDialog(stage);
            if (file != null) {
                downloadPath = file.getAbsolutePath();
                folderLabel.setText(downloadPath);
            }
        });

        HBox folderBox = new HBox(10, chooseFolder, folderLabel);
        folderBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        // 🔴 STATUS
        Label info = new Label("Aguardando...");
        info.setStyle("-fx-text-fill: #b3b3b3;");

        // 🔥 NOVO: NOME DA MÚSICA
        Label musicName = new Label("-");
        musicName.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        // 🔴 PROGRESSO
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(400);
        progress.setStyle("-fx-accent: #ff3b3b;");

        // 🔴 LOG
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background:#181818; -fx-text-fill:white;");
        logArea.setPrefHeight(120);

        Button downloadBtn = new Button("⬇ Download");
        styleButton(downloadBtn);

        final String[] lastUrl = {""};

        Runnable resetUI = () -> Platform.runLater(() -> {
            progress.setProgress(0);
            info.setText("Aguardando...");
            logArea.clear();
            musicName.setText("-");
        });

        urlField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null &&
                !newValue.isEmpty() &&
                !newValue.equals(lastUrl[0])) {

                lastUrl[0] = newValue;
                resetUI.run();
            }
        });

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
                            p -> Platform.runLater(() -> progress.setProgress(p)),
                            line -> Platform.runLater(() -> {

                                logArea.appendText(line + "\n");

                                // 🔥 CAPTURA NOME DA MÚSICA
                                if (line.contains("[download] Destination:")) {
                                    String name = line.replace("[download] Destination:", "").trim();
                                    musicName.setText("🎵 " + name);
                                }
                            })
                    );

                    Platform.runLater(() -> info.setText("✅ Concluído"));

                } catch (Exception ex) {
                    Platform.runLater(() -> alert(ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(
                header,
                folderLabelDesc,
                urlField,
                folderBox,
                info,
                musicName, // 🔥 AQUI
                progress,
                downloadBtn,
                logArea
        );

        stage.setScene(new Scene(root, 500, 580));
        stage.setTitle("UaiTube");
        stage.show();
    }

    private void styleButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #ff3b3b;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;"
        );
    }

    private void alert(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(msg);
            a.show();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}