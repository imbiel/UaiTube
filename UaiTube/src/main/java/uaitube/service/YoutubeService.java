package uaitube.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class YoutubeService {
	
    // 🔥 USER AGENTS
	private static final List<String> USER_AGENTS = List.of(
	        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
	        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15",
	        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36",
	        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148",
	        "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
	);

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
        
        // 🔥 USER AGENT ROTATIVO
//        command.add("--user-agent");
//        command.add(getRandomUserAgent());

        if (isPlaylist) {
//            command.add("--sleep-interval"); command.add("1");
//            command.add("--max-sleep-interval"); command.add("5");
//
//            command.add("--retries"); command.add("10");
//            command.add("--fragment-retries"); command.add("10");
//
//            command.add("--concurrent-fragments"); command.add("1");
//            command.add("--limit-rate"); command.add("1M");
        	
            command.add("--parse-metadata");
            command.add("playlist_index:%(track_number)s");
            
            command.add("--replace-in-metadata");
            command.add("playlist_title");
            command.add("^Album - ");
            command.add("");
        }
        
        if (!isPlaylist) {
            command.add("--no-playlist");
        }
        
        command.add(url);

        return command;
    }

    private String getRandomUserAgent() {
        return USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));
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
    
    public String getMusicTitle(String url) {

        try {

            ProcessBuilder pb = new ProcessBuilder(
                    "yt-dlp",
                    "--dump-single-json",
                    "--no-playlist",
                    url
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            process.waitFor();

            String raw = output.toString();

            // 🔥 REMOVE WARNINGS → pega só JSON
            int jsonStart = raw.indexOf("{");

            if (jsonStart == -1) {
                return "Não identificado";
            }

            String json = raw.substring(jsonStart);

            // 🔥 EXTRAI TITLE
            String title = extract(json, "\"title\": \"", "\"");

            if (title == null || title.isEmpty()) {
                return "Não identificado";
            }

            return title;

        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao buscar música";
        }
    }

    // 🔥 MÉTODO AUXILIAR (evita split frágil)
    private String extract(String json, String start, String end) {
        try {
            int i = json.indexOf(start);
            if (i == -1) return null;

            int j = json.indexOf(end, i + start.length());
            if (j == -1) return null;

            return json.substring(i + start.length(), j);

        } catch (Exception e) {
            return null;
        }
    }
}