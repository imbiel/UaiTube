package uaitube.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeService {

    public void downloadWithProgress(
            String url,
            String basePath,
            java.util.function.Consumer<Double> onProgress,
            java.util.function.Consumer<String> onLog
    ) throws IOException {

        String normalizedUrl = normalizeUrl(url);
        boolean isPlaylist = isPlaylist(normalizedUrl);

        String outputTemplate = isPlaylist
                ? basePath + "\\%(playlist_title)s\\%(title)s.%(ext)s"
                : basePath + "\\%(title)s.%(ext)s";

        List<String> command = buildCommand(normalizedUrl, outputTemplate, isPlaylist);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 🔥 Thread para ler saída do yt-dlp
        new Thread(() -> readProcessOutput(process, onProgress, onLog)).start();

        try {
            int exit = process.waitFor();

            if (exit != 0) {
                onLog.accept("❌ Erro no download (exit code " + exit + ")");
            } else {
                onProgress.accept(1.0);
                onLog.accept("✅ Download finalizado!");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onLog.accept("❌ Processo interrompido");
        }
    }

    // 🔧 Monta comando yt-dlp
    private List<String> buildCommand(String url, String outputTemplate, boolean isPlaylist) {

        List<String> command = new ArrayList<>();

        command.add("yt-dlp");
        command.add("-f"); command.add("bestaudio");
        command.add("-x");
        command.add("--audio-format"); command.add("mp3");

        command.add("-o"); command.add(outputTemplate);

        command.add("--embed-thumbnail");
        command.add("--add-metadata");
        command.add("--convert-thumbnails"); command.add("jpg");

        if (!isPlaylist) {
            command.add("--no-playlist");
        }

        command.add(url);

        return command;
    }

    // 🔧 Leitura do output (progresso real)
    private void readProcessOutput(
            Process process,
            java.util.function.Consumer<Double> onProgress,
            java.util.function.Consumer<String> onLog
    ) {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                onLog.accept(line);

                extractProgress(line, onProgress);
            }

        } catch (Exception e) {
            onLog.accept("Erro ao ler output: " + e.getMessage());
        }
    }

    // 🔧 Extrai porcentagem do yt-dlp
    private void extractProgress(String line, java.util.function.Consumer<Double> onProgress) {

        try {
            if (line.contains("[download]") && line.contains("%")) {

                int percentIndex = line.indexOf('%');

                String percentStr = line.substring(0, percentIndex);

                percentStr = percentStr.replaceAll("[^0-9.]", "");

                if (!percentStr.isEmpty()) {
                    double progress = Double.parseDouble(percentStr) / 100.0;
                    onProgress.accept(progress);
                }
            }
        } catch (Exception ignored) {
        }
    }

    // 🔧 Detecta playlist
    private boolean isPlaylist(String url) {
        return url.contains("playlist?list=");
    }

    // 🔧 Normaliza URL (remove parâmetros extras)
    private String normalizeUrl(String url) {

        if (url.contains("watch?v=")) {

            int index = url.indexOf("&");

            if (index != -1) {
                return url.substring(0, index);
            }
        }

        return url;
    }
}