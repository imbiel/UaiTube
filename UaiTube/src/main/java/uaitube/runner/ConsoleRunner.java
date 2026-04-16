//package uaitube.runner;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import uaitube.service.YoutubeService;
//
//import java.io.File;
//import java.util.Scanner;
//
//@Component
//public class ConsoleRunner implements CommandLineRunner {
//
//    private final YoutubeService service;
//
//    // 🔥 PASTA FIXA
//    private static final String DEFAULT_PATH = "C:\\UaiTube";
//
//    public ConsoleRunner(YoutubeService service) {
//        this.service = service;
//    }
//
//    @Override
//    public void run(String... args) {
//
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.println("==================================");
//        System.out.println("        🎧 UaiTube CLI");
//        System.out.println("==================================");
//
//        // 🔥 GARANTE QUE A PASTA EXISTE
//        createDefaultFolder();
//
//        while (true) {
//
//            try {
//
//                System.out.print("\nCole a URL (ou 'sair'): ");
//                String url = scanner.nextLine();
//
//                if (url.equalsIgnoreCase("sair")) {
//                    System.out.println("Encerrando...");
//                    break;
//                }
//
//                if (url.isEmpty()) {
//                    System.out.println("❌ URL inválida!");
//                    continue;
//                }
//
//                // 🔍 BUSCA INFO
//                System.out.println("\n🔎 Buscando informações...");
//
//                String title = service.getMusicTitle(url);
//                boolean isPlaylist = url.contains("playlist");
//
//                if (isPlaylist) {
//                    System.out.println("📂 Playlist: " + title);
//                } else {
//                    System.out.println("🎵 Música: " + title);
//                }
//
//                System.out.println("\n📁 Pasta de destino: " + DEFAULT_PATH);
//                System.out.println("\n⬇ Iniciando download...\n");
//
//                service.downloadWithProgress(
//                        url,
//                        DEFAULT_PATH,
//
//                        // progresso
//                        progress -> {
//                            int percent = (int) (progress * 100);
//                            System.out.print("\rProgresso: " + percent + "%");
//                        },
//
//                        // log
//                        line -> {
//
//                            if (line.startsWith("after_move:")) {
//
//                                String name = line.replace("after_move:", "").trim();
//
//                                if (name.endsWith(".mp3")) {
//                                    System.out.println("\n✔ Baixado: " + name);
//                                }
//                            }
//                            else if (line.toLowerCase().contains("error")) {
//                                System.out.println("\n❌ Erro detectado!");
//                            }
//                        }
//                );
//
//                System.out.println("\n\n✅ Download concluído!");
//
//            } catch (Exception e) {
//                System.out.println("\n❌ Erro: " + e.getMessage());
//            }
//        }
//
//        scanner.close();
//    }
//
//    // 🔥 CRIA A PASTA AUTOMATICAMENTE
//    private void createDefaultFolder() {
//
//        File folder = new File(DEFAULT_PATH);
//
//        if (!folder.exists()) {
//
//            boolean created = folder.mkdirs();
//
//            if (created) {
//                System.out.println("📁 Pasta criada: " + DEFAULT_PATH);
//            } else {
//                System.out.println("⚠️ Não foi possível criar a pasta!");
//            }
//
//        } else {
//            System.out.println("📁 Pasta encontrada: " + DEFAULT_PATH);
//        }
//    }
//}