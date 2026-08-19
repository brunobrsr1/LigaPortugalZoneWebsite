package com.lp.ligaportugalzone.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.lp.ligaportugalzone.player.PlayerFixtures.player;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer slice: Spring starts only the MVC infrastructure and this controller, with
 * {@link PlayerService} replaced by a mock. It covers routing, query-parameter binding and
 * the JSON shape — no database involved.
 */
@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @Test
    void serialisesPlayersAsJson() throws Exception {
        given(playerService.findPlayers(null, null, null, null))
                .willReturn(List.of(player("Vangelis Pavlidis", "gr GRE", "Benfica", "FW")));

        mockMvc.perform(get("/api/v1/players/data"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Vangelis Pavlidis"))
                .andExpect(jsonPath("$[0].team").value("Benfica"))
                // The JPA entity is serialised directly, so the JSON keys are the field names.
                // CLAUDE.md tracks this as "the JPA entity is the API contract".
                .andExpect(jsonPath("$[0].gls").value(3))
                .andExpect(jsonPath("$[0].crdY").value(2))
                .andExpect(jsonPath("$[0].goalsPer90").value(0.39));
    }

    @Test
    void returnsAnEmptyArrayWhenNothingMatches() throws Exception {
        given(playerService.findPlayers(null, null, null, "nobody")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/players/data").param("name", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void passesNullFiltersWhenNoQueryParametersAreGiven() throws Exception {
        mockMvc.perform(get("/api/v1/players/data"))
                .andExpect(status().isOk());

        verify(playerService).findPlayers(null, null, null, null);
    }

    @Test
    void convertsTheTeamSlugBackIntoATeamNameBeforeFiltering() throws Exception {
        mockMvc.perform(get("/api/v1/players/data").param("team", "Vitória-Guimarães"))
                .andExpect(status().isOk());

        verify(playerService).findPlayers("Vitória Guimarães", null, null, null);
    }

    @Test
    void passesTheRemainingFiltersThroughUnchanged() throws Exception {
        mockMvc.perform(get("/api/v1/players/data")
                        .param("nation", "POR")
                        .param("position", "GK")
                        .param("name", "Soares"))
                .andExpect(status().isOk());

        verify(playerService).findPlayers(null, "POR", "GK", "Soares");
    }
}
