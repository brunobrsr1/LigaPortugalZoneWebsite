package com.lp.ligaportugalzone.bootstrap;

import com.lp.ligaportugalzone.player.Player;
import com.lp.ligaportugalzone.player.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final PlayerRepository playerRepository;

    public DataLoader(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (playerRepository.count() == 0) {
            System.out.println("--- BASE DE DADOS VAZIA. A CARREGAR CSV... ---");
            loadCsvData();
            System.out.println("--- IMPORTAÇÃO CONCLUÍDA ---");
        } else {
            System.out.println("--- DADOS JÁ EXISTEM. A SALTAR IMPORTAÇÃO PARA ARRANQUE RÁPIDO. ---");
        }
    }

    private void loadCsvData() {
        try {
            ClassPathResource resource = new ClassPathResource("players_primeira_liga.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            String line;
            boolean isHeader = true;
            List<Player> players = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                Player player = new Player();

                player.setName(clean(columns[0]));
                player.setNation(clean(columns[1]));
                player.setTeam(clean(columns[2]));
                player.setPosition(clean(columns[3]));

                player.setAge(parseInt(columns[4]));
                player.setMp(parseInt(columns[5]));
                player.setStarts(parseInt(columns[6]));

                String minClean = clean(columns[7]).replace(",", "");
                player.setMin(parseInt(minClean));

                player.setGls(parseInt(columns[8]));
                player.setAst(parseInt(columns[9]));
                player.setPk(parseInt(columns[10]));
                player.setCrdY(parseInt(columns[11]));
                player.setCrdR(parseInt(columns[12]));

                player.setGoalsPer90(parseDouble(columns[13]));
                player.setAssistsPer90(parseDouble(columns[14]));

                players.add(player);
            }

            playerRepository.saveAll(players);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERRO CRÍTICO NO CSV: " + e.getMessage());
        }
    }

    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\"", "").trim();
    }

    private Integer parseInt(String value) {
        String cleaned = clean(value);

        if (cleaned.contains("-")) {
            cleaned = cleaned.split("-")[0];
        }

        cleaned = cleaned.replace(",", "").replace(".", "");

        if (cleaned.isEmpty()) return 0;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double parseDouble(String value) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}