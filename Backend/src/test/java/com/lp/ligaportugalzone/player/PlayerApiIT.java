package com.lp.ligaportugalzone.player;

import com.lp.ligaportugalzone.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.lp.ligaportugalzone.player.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the only endpoint: real HTTP request, real Spring context, real PostgreSQL.
 * Everything the unit tests mock away — Hibernate mapping, JSON serialisation, the servlet stack —
 * is exercised here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlayerApiIT extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void resetData() {
        playerRepository.deleteAll();
        playerRepository.saveAll(List.of(
                player("Vangelis Pavlidis", "gr GRE", "Benfica", "FW"),
                player("Rodrigo Zalazar", "uy URU", "Braga", "MF,FW"),
                player("Nelson Abbey", "eng ENG", "Rio Ave", "DF")));
    }

    @Test
    void returnsEveryPlayerWhenNoFilterIsGiven() {
        ResponseEntity<Player[]> response = get("/api/v1/players/data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(Player::getName)
                .containsExactlyInAnyOrder("Vangelis Pavlidis", "Rodrigo Zalazar", "Nelson Abbey");
    }

    @Test
    void filtersByTeamSlug() {
        ResponseEntity<Player[]> response = get("/api/v1/players/data?team=Rio-Ave");

        assertThat(response.getBody())
                .singleElement()
                .extracting(Player::getName)
                .isEqualTo("Nelson Abbey");
    }

    @Test
    void filtersByCountryCode() {
        ResponseEntity<Player[]> response = get("/api/v1/players/data?nation=GRE");

        assertThat(response.getBody())
                .singleElement()
                .extracting(Player::getName)
                .isEqualTo("Vangelis Pavlidis");
    }

    @Test
    void filtersByPartialName() {
        ResponseEntity<Player[]> response = get("/api/v1/players/data?name=zala");

        assertThat(response.getBody())
                .singleElement()
                .extracting(Player::getName)
                .isEqualTo("Rodrigo Zalazar");
    }

    @Test
    void persistsEveryStatisticColumn() {
        ResponseEntity<Player[]> response = get("/api/v1/players/data?name=Pavlidis");

        assertThat(response.getBody()).singleElement().satisfies(player -> {
            assertThat(player.getId()).isNotNull();
            assertThat(player.getMin()).isEqualTo(700);
            assertThat(player.getGls()).isEqualTo(3);
            assertThat(player.getGoalsPer90()).isEqualTo(0.39);
        });
    }

    private ResponseEntity<Player[]> get(String path) {
        return restTemplate.getForEntity(path, Player[].class);
    }
}
