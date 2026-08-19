package com.lp.ligaportugalzone.bootstrap;

import com.lp.ligaportugalzone.player.Player;
import com.lp.ligaportugalzone.player.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit test for the seeding decision. The repository and the parser are mocked, so this
 * checks the orchestration ("when do we import?") without touching a database or the CSV.
 */
@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerCsvParser csvParser;

    @InjectMocks
    private DataLoader dataLoader;

    @Test
    void importsTheCsvWhenTheTableIsEmpty() throws Exception {
        List<Player> parsed = List.of(new Player(), new Player());
        given(playerRepository.count()).willReturn(0L);
        given(csvParser.parse(any(InputStream.class))).willReturn(parsed);

        dataLoader.run();

        verify(playerRepository).saveAll(parsed);
    }

    @Test
    void doesNothingWhenTheTableAlreadyHasPlayers() throws Exception {
        given(playerRepository.count()).willReturn(489L);

        dataLoader.run();

        verifyNoInteractions(csvParser);
        verify(playerRepository, never()).saveAll(any());
    }

    @Test
    void swallowsParsingFailuresInsteadOfFailingStartup() throws Exception {
        given(playerRepository.count()).willReturn(0L);
        given(csvParser.parse(any(InputStream.class)))
                .willThrow(new IllegalArgumentException("Line 2 has 7 columns, expected 15"));

        // Documents today's behaviour: a broken CSV leaves the application up with an empty table.
        // Roadmap step 4 should turn this into a startup failure.
        assertThatCode(() -> dataLoader.run()).doesNotThrowAnyException();

        verify(playerRepository, never()).saveAll(any());
    }
}
