package com.lp.ligaportugalzone.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.lp.ligaportugalzone.player.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit test for the filtering rules. The repository is a mock returning a fixed list, so the
 * assertions are about the filters only.
 */
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    private static final Player PAVLIDIS = player("Vangelis Pavlidis", "gr GRE", "Benfica", "FW");
    private static final Player ZALAZAR = player("Rodrigo Zalazar", "uy URU", "Braga", "MF,FW");
    private static final Player ABBEY = player("Nelson Abbey", "eng ENG", "Rio Ave", "DF");
    private static final Player SOARES = player("Samuel Soares", "pt POR", "Benfica", "GK");

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private void givenPlayers() {
        given(playerRepository.findAll()).willReturn(List.of(PAVLIDIS, ZALAZAR, ABBEY, SOARES));
    }

    @Test
    void returnsEveryPlayerWhenNoFilterIsGiven() {
        givenPlayers();

        assertThat(playerService.findPlayers(null, null, null, null))
                .containsExactly(PAVLIDIS, ZALAZAR, ABBEY, SOARES);
    }

    @Test
    void filtersByTeamIgnoringCase() {
        givenPlayers();

        assertThat(playerService.findPlayers("benfica", null, null, null))
                .containsExactly(PAVLIDIS, SOARES);
    }

    @Test
    void filtersByCountryCodeBecauseNationIsStoredAsFlagPlusCode() {
        givenPlayers();

        // Nation is stored as "gr GRE", so the filter has to match the suffix.
        // CLAUDE.md tracks this as "nation filtering uses endsWith".
        assertThat(playerService.findPlayers(null, "gre", null, null))
                .containsExactly(PAVLIDIS);
    }

    @Test
    void matchesThePositionStringExactly() {
        givenPlayers();

        // Zalazar is "MF,FW" and is therefore NOT returned for "FW", even though the frontend
        // treats him as a forward. Known inconsistency to resolve in roadmap step 3.
        assertThat(playerService.findPlayers(null, null, "FW", null))
                .containsExactly(PAVLIDIS);
    }

    @Test
    void filtersByNameSubstringIgnoringCase() {
        givenPlayers();

        assertThat(playerService.findPlayers(null, null, null, "zala"))
                .containsExactly(ZALAZAR);
    }

    @Test
    void appliesEveryFilterTogether() {
        givenPlayers();

        assertThat(playerService.findPlayers("Benfica", "POR", "GK", "Soares"))
                .containsExactly(SOARES);
    }

    @Test
    void returnsAnEmptyListWhenNothingMatches() {
        givenPlayers();

        assertThat(playerService.findPlayers("Porto", null, null, null)).isEmpty();
    }

    @Test
    void readsTheWholeTableForEveryQuery() {
        givenPlayers();

        playerService.findPlayers("Benfica", null, null, null);

        // Documents CLAUDE.md issue "filtering happens in memory": even a single-team query
        // loads every row. Roadmap step 3 replaces this with a query.
        verify(playerRepository).findAll();
    }
}
