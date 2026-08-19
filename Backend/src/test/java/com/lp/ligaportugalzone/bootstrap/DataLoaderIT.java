package com.lp.ligaportugalzone.bootstrap;

import com.lp.ligaportugalzone.player.Player;
import com.lp.ligaportugalzone.player.PlayerRepository;
import com.lp.ligaportugalzone.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real CSV through the real parser into a real PostgreSQL. Guards the one code path
 * that only ever runs on startup, where a silent failure would leave the site with no data.
 */
@SpringBootTest
class DataLoaderIT extends AbstractPostgresIT {

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void emptyTable() {
        playerRepository.deleteAll();
    }

    @Test
    void importsEveryDataRowOfTheBundledCsv() throws IOException {
        dataLoader.run();

        List<Player> players = playerRepository.findAll();

        assertThat(players).hasSize((int) expectedPlayerCount());
        assertThat(players).allSatisfy(player -> {
            assertThat(player.getName()).isNotBlank();
            assertThat(player.getTeam()).isNotBlank();
            assertThat(player.getMp()).isNotNull();
        });
    }

    @Test
    void doesNotStoreTheHeaderRowsFbrefRepeatsThroughTheFile() {
        dataLoader.run();

        assertThat(playerRepository.findAll())
                .noneMatch(player -> "Player".equals(player.getName()));
    }

    @Test
    void readsMinutesThatAreQuotedBecauseOfTheThousandsSeparator() {
        dataLoader.run();

        assertThat(playerRepository.findAll())
                .anyMatch(player -> player.getMin() > 1000);
    }

    @Test
    void doesNotImportAgainWhenTheTableIsAlreadyPopulated() {
        dataLoader.run();
        long afterFirstRun = playerRepository.count();

        dataLoader.run();

        assertThat(playerRepository.count()).isEqualTo(afterFirstRun);
    }

    /** Every non-blank line of the CSV that is not one of the repeated header rows. */
    private long expectedPlayerCount() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(DataLoader.CSV_RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("Player,"))
                    .count();
        }
    }
}
