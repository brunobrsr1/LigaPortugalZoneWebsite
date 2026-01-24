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
            System.out.println("--- BASE DE DADOS VAZIA. A INICIAR IMPORTAÇÃO DO CSV ---");
            loadCsvData();
            System.out.println("--- IMPORTAÇÃO CONCLUÍDA COM SUCESSO ---");
        } else {
            System.out.println("--- A Base de Dados já contém dados. Importação ignorada. ---");
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


                String[] columns = line.split(",");

                Player player = new Player();


                player.setName(columns[0].trim());
                player.setNation(columns[1].trim());
                player.setTeam(columns[2].trim());
                player.setPosition(columns[3].trim());

                player.setAge(parseInt(columns[4]));
                player.setMp(parseInt(columns[5]));
                player.setStarts(parseInt(columns[6]));

                String minClean = columns[7].replace(",", "").trim();
                player.setMin(parseInt(minClean));

                player.setGls(parseInt(columns[8]));
                player.setAst(parseInt(columns[9]));
                player.setPk(parseInt(columns[10]));
                player.setCrdY(parseInt(columns[11]));
                player.setCrdR(parseInt(columns[12]));

                // Estatísticas Avançadas (Double)
                player.setXg(parseDouble(columns[13]));
                player.setXag(parseDouble(columns[14]));

                players.add(player);
            }

            playerRepository.saveAll(players);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERRO AO CARREGAR CSV: " + e.getMessage());
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}