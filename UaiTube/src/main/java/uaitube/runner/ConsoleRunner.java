//package uaitube.runner;
//
//import java.util.Scanner;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import uaitube.service.YoutubeService;
//
//@Component
//public class ConsoleRunner implements CommandLineRunner {
//
//    private final YoutubeService service;
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
//        while (true) {
//            System.out.println("\n===== YOUTUBE DOWNLOADER =====");
//            System.out.println("Baixar música ou playlist");
//            System.out.print("Cole a URL: ");
//            String url = scanner.nextLine();
//
//            try {
//                service.download(url);
//            } catch (Exception e) {
//                System.out.println("Erro: " + e.getMessage());
//            }
//        }
//    }
//}
