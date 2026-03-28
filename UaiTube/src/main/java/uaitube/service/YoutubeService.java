package uaitube.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class YoutubeService {

    private static final String BASE_PATH = "C:\\Users\\Gabriel\\Music";

    public void download(String url) throws IOException, InterruptedException {

        boolean isPlaylist = isPlaylist(url);

        String outputTemplate;

        if (isPlaylist) {
            System.out.println("📂 Detectado: PLAYLIST");
            outputTemplate = BASE_PATH + "\\%(playlist_title)s\\%(title)s.%(ext)s";
        } else {
            System.out.println("🎵 Detectado: MÚSICA");
            outputTemplate = BASE_PATH + "\\%(title)s.%(ext)s";
        }

        // 🔥 Monta comando dinamicamente
        List<String> command = new ArrayList<>();

        command.add("yt-dlp");
        command.add("-f"); command.add("bestaudio");
        command.add("-x");
        command.add("--audio-format"); command.add("mp3");
        command.add("-o"); command.add(outputTemplate);
        command.add("--embed-thumbnail");
        command.add("--add-metadata");
        command.add("--convert-thumbnails"); command.add("jpg");

        // 🔥 Só adiciona isso se NÃO for playlist
        if (!isPlaylist) {
            command.add("--no-playlist");
        }

        command.add(url);

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.inheritIO();

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Erro ao executar download");
        }

        System.out.println("\n✅ Download concluído!");
    }

    private boolean isPlaylist(String url) {
        return url.contains("playlist?list=");
    }
}