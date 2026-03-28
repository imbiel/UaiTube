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

import java.io.*;
import java.net.URI;
import java.awt.Desktop;

public class UaiTubeUI extends Application {

    private String downloadPath = getWindowsMusicFolder();

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

        // 🔴 HEADER
        Label title = new Label("UaiTube");
        title.setStyle("-fx-text-fill: #ff3b3b; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label desc = new Label("Baixe músicas e playlists do YouTube");
        desc.setStyle("-fx-text-fill: #b3b3b3;");

        VBox header = new VBox(5, title, desc);
        header.setAlignment(Pos.CENTER);

        Label infoURL = new Label("Cole aqui a URL do YouTube:");
        infoURL.setStyle("-fx-text-fill: #b3b3b3;");

        // 🔴 INPUT
        TextField urlField = new TextField();
        urlField.setPromptText("Cole a URL aqui");
        urlField.setStyle("-fx-background-color: #1f1f1f; -fx-text-fill: white;");

        // 🔴 PASTA
        Label folderLabel = new Label(downloadPath);
        folderLabel.setStyle("-fx-text-fill: #b3b3b3;");
        folderLabel.setMaxWidth(Double.MAX_VALUE);

        Button chooseFolder = new Button("📂 Pasta");
        styleButton(chooseFolder);

        chooseFolder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(new File(downloadPath));

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

        // 🔴 NOME
        Label musicName = new Label("-");
        musicName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // 🔴 PROGRESSO
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(400);
        progress.setStyle("-fx-accent: #ff3b3b;");

        // 🔴 BOTÃO
        Button downloadBtn = new Button("⬇ Download");
        styleButton(downloadBtn);

        Label infoLista = new Label("Músicas baixadas:");
        infoLista.setStyle("-fx-text-fill: #b3b3b3;");
        
        // 🔴 LISTA
        ObservableList<String> musicList = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(musicList);
        listView.setPrefHeight(150);
        listView.setStyle(
                "-fx-control-inner-background: #181818;" +
                "-fx-text-fill: white;"
        );

        Label infoLog = new Label("Informações de Log:");
        infoLog.setStyle("-fx-text-fill: #b3b3b3;");
        
        // 🔴 LOG
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle(
                "-fx-control-inner-background:#181818;" +
                "-fx-text-fill:white;"
        );

        final String[] lastUrl = {""};

        Runnable resetUI = () -> Platform.runLater(() -> {
            progress.setProgress(0);
            info.setText("Aguardando...");
            logArea.clear();
            musicName.setText("-");
            musicList.clear();
        });

        // 🔥 BUSCA AUTOMÁTICA
        urlField.textProperty().addListener((obs, oldValue, newValue) -> {

            if (newValue != null && !newValue.isEmpty() && !newValue.equals(lastUrl[0])) {

                lastUrl[0] = newValue;
                resetUI.run();

                info.setText("🔎 Buscando informações...");

                new Thread(() -> {

                    String titleMusic = service.getMusicTitle(newValue);
                    boolean isPlaylist = newValue.contains("playlist");

                    Platform.runLater(() -> {

                        if (isPlaylist) {
                            musicName.setText("📂 Playlist: " + titleMusic);
                        } else {
                            musicName.setText("🎧 Música: " + titleMusic);
                        }

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

                            p -> Platform.runLater(() -> progress.setProgress(p)),

                            line -> Platform.runLater(() -> {

                                logArea.appendText(line + "\n");

                                if (line.startsWith("after_move:")) {

                                    String name = line.replace("after_move:", "").trim();

                                    if (name.endsWith(".mp3")) {
                                        musicList.add("✔ " + name);
                                    }
                                }
                                else if (line.contains("Destination:") && line.contains(".mp3")) {

                                    String fullPath = line.substring(line.indexOf("Destination:") + 12).trim();
                                    String name = new File(fullPath).getName();

                                    musicList.add("✔ " + name);
                                }
                                else if (line.toLowerCase().contains("error")) {

                                    musicList.add("❌ Erro no download");
                                }
                            })
                    );

                    Platform.runLater(() -> info.setText("✅ Concluído"));

                } catch (Exception ex) {
                    Platform.runLater(() -> alert(ex.getMessage()));
                }
            }).start();
        });

        // 🔻 FOOTER (CRÉDITOS)
        Label footerText = new Label("UaiTube v1.0 • © 2026 • Gabriel Lirio • ");
        footerText.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11px;");

        Hyperlink githubLink = new Hyperlink("GitHub");
        githubLink.setStyle(
                "-fx-text-fill: #ff3b3b;" +
                "-fx-font-size: 11px;" +
                "-fx-cursor: hand;"
        );

        githubLink.setTooltip(new Tooltip("Abrir repositório"));

        githubLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(
                        new URI("https://github.com/imbiel/UaiTube")
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox footer = new HBox(5, footerText, githubLink);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // 🔴 LAYOUT
        root.getChildren().addAll(
                header,
                infoURL,
                urlField,
                folderBox,
                info,
                musicName,
                progress,
                downloadBtn,
                infoLista,
                listView,
                infoLog,
                logArea,
                footer
        );

        stage.setScene(new Scene(root, 520, 700));
        stage.setTitle("UaiTube");
        stage.show();
    }

    private String getWindowsMusicFolder() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-command",
                    "[environment]::getfolderpath('MyMusic')"
            );

            Process p = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );

            String result = reader.readLine();

            if (result != null && !result.isEmpty()) {
                return result;
            }

        } catch (Exception ignored) {}

        return System.getProperty("user.home") + "\\Music";
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