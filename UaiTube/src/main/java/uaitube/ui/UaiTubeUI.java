package uaitube.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;

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

        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icon.png"))
        );

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f0f0f;");

        Label title = new Label("UaiTube");
        title.setStyle("-fx-text-fill: #ff3b3b; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label desc = new Label("Baixe músicas e playlists do YouTube");
        desc.setStyle("-fx-text-fill: #b3b3b3;");

        VBox header = new VBox(5, title, desc);
        header.setAlignment(Pos.CENTER);

        TextField urlField = new TextField();
        urlField.setPromptText("Cole a URL do YouTube...");
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

        // 🔴 NOME ATUAL
        Label musicName = new Label("-");
        musicName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // 🔴 LISTA VISUAL DE DOWNLOADS
        ObservableList<String> musicList = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(musicList);
        listView.setPrefHeight(150);
        listView.setStyle(
                "-fx-control-inner-background: #181818;" +
                "-fx-text-fill: white;"
        );

        // 🔴 PROGRESSO
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(400);
        progress.setStyle("-fx-accent: #ff3b3b;");

        // 🔴 LOG
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-control-inner-background:#181818; -fx-text-fill:white;");

        Button downloadBtn = new Button("⬇ Download");
        styleButton(downloadBtn);

        final String[] lastUrl = {""};

        Runnable resetUI = () -> Platform.runLater(() -> {
            progress.setProgress(0);
            info.setText("Aguardando...");
            logArea.clear();
            musicName.setText("-");
            musicList.clear();
        });

        // 🔥 BUSCAR NOME AO COLAR URL
        urlField.textProperty().addListener((obs, oldValue, newValue) -> {

            if (newValue != null && !newValue.isEmpty() && !newValue.equals(lastUrl[0])) {

                lastUrl[0] = newValue;

                resetUI.run();

                Platform.runLater(() -> info.setText("🔎 Buscando informações..."));

                new Thread(() -> {
                    String titleMusic = service.getMusicTitle(newValue);

                    Platform.runLater(() -> {
                        musicName.setText("🎵 " + titleMusic);
                        info.setText("Pronto para download");
                    });
                }).start();
            }
        });

        // 🔥 DOWNLOAD
        downloadBtn.setOnAction(e -> {

            String url = urlField.getText();

            if (url.isEmpty()) {
                alert("Cole uma URL!");
                return;
            }

            info.setText(url.contains("playlist") ? "📂 Playlist..." : "🎵 Baixando...");

            new Thread(() -> {
                try {

                    service.downloadWithProgress(
                            url,
                            downloadPath,

                            // progresso
                            p -> Platform.runLater(() -> progress.setProgress(p)),

                            // log + lista visual
                            line -> Platform.runLater(() -> {

                                logArea.appendText(line + "\n");

                                // 🔥 CAPTURA NOME DA MÚSICA
                                if (line.contains("[download] Destination:")) {

                                    String name = line.replace("[download] Destination:", "").trim();

                                    musicList.add("⬇ " + name);
                                }

                                // 🔥 MARCAR COMO CONCLUÍDO
                                if (line.contains("100%")) {
                                    int lastIndex = musicList.size() - 1;
                                    if (lastIndex >= 0) {
                                        String current = musicList.get(lastIndex);
                                        musicList.set(lastIndex, "✔ " + current.substring(2));
                                    }
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
                urlField,
                folderBox,
                info,
                musicName,
                listView, // 🔥 NOVA LISTA VISUAL
                progress,
                downloadBtn,
                logArea
        );

        stage.setScene(new Scene(root, 520, 650));
        stage.setTitle("UaiTube");
        stage.show();
    }

    private void styleButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #ff3b3b;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;"
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