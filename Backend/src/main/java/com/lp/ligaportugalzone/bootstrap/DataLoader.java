package com.lp.ligaportugalzone.bootstrap;

import com.lp.ligaportugalzone.player.Player;
import com.lp.ligaportugalzone.player.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Seeds {@code player_data} from the bundled CSV on startup, but only when the table is empty.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    static final String CSV_RESOURCE = "players_primeira_liga.csv";

    private final PlayerRepository playerRepository;
    private final PlayerCsvParser csvParser;

    public DataLoader(PlayerRepository playerRepository, PlayerCsvParser csvParser) {
        this.playerRepository = playerRepository;
        this.csvParser = csvParser;
    }

    @Override
    public void run(String... args) {
        if (playerRepository.count() > 0) {
            log.info("Player table already populated, skipping CSV import.");
            return;
        }

        log.info("Player table is empty, importing {}", CSV_RESOURCE);
        try (InputStream inputStream = new ClassPathResource(CSV_RESOURCE).getInputStream()) {
            List<Player> players = csvParser.parse(inputStream);
            playerRepository.saveAll(players);
            log.info("Imported {} players from {}", players.size(), CSV_RESOURCE);
        } catch (Exception e) {
            // TODO: startup should fail instead of leaving an empty table. See CLAUDE.md,
            // "Errors are swallowed" — fixed together with the scraper in roadmap step 4.
            log.error("Failed to import {}", CSV_RESOURCE, e);
        }
    }
}
