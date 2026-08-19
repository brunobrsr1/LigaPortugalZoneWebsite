package com.lp.ligaportugalzone.bootstrap;

import com.lp.ligaportugalzone.player.Player;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for the CSV parsing rules. The CSV is written inline so each test states
 * exactly which input produces which output — no database and no file on disk.
 */
class PlayerCsvParserTest {

    private static final String HEADER =
            "Player,Nation,Squad,Pos,Age,MP,Starts,Min,Gls,Ast,PK,CrdY,CrdR,Gls,Ast";

    private final PlayerCsvParser parser = new PlayerCsvParser();

    @Test
    void mapsEveryColumnOfADataRow() throws IOException {
        List<Player> players = parse(HEADER + "\n"
                + "Rodrigo Abascal,uy URU,Vitória Guimarães,DF,32-011,18,18,\"1,620\",1,2,0,3,0,0.06,0.11");

        assertThat(players).singleElement().satisfies(player -> {
            assertThat(player.getName()).isEqualTo("Rodrigo Abascal");
            assertThat(player.getNation()).isEqualTo("uy URU");
            assertThat(player.getTeam()).isEqualTo("Vitória Guimarães");
            assertThat(player.getPosition()).isEqualTo("DF");
            assertThat(player.getAge()).isEqualTo(32);
            assertThat(player.getMp()).isEqualTo(18);
            assertThat(player.getStarts()).isEqualTo(18);
            assertThat(player.getMin()).isEqualTo(1620);
            assertThat(player.getGls()).isEqualTo(1);
            assertThat(player.getAst()).isEqualTo(2);
            assertThat(player.getPk()).isZero();
            assertThat(player.getCrdY()).isEqualTo(3);
            assertThat(player.getCrdR()).isZero();
            assertThat(player.getGoalsPer90()).isEqualTo(0.06);
            assertThat(player.getAssistsPer90()).isEqualTo(0.11);
        });
    }

    @Test
    void keepsCommasThatBelongInsideAQuotedField() throws IOException {
        List<Player> players = parse(HEADER + "\n"
                + "Rodrigo Zalazar,uy URU,Braga,\"MF,FW\",26-166,16,12,\"1,115\",7,1,6,5,0,0.57,0.08");

        assertThat(players).singleElement().satisfies(player -> {
            assertThat(player.getPosition()).isEqualTo("MF,FW");
            assertThat(player.getMin()).isEqualTo(1115);
        });
    }

    @Test
    void keepsOnlyTheYearPartOfTheAge() throws IOException {
        List<Player> players = parse(HEADER + "\n"
                + "Umar Abubakar,ng NGA,Famalicão,FW,19-344,9,0,76,0,0,0,0,0,0,0");

        assertThat(players).singleElement()
                .extracting(Player::getAge)
                .isEqualTo(19);
    }

    @Test
    void skipsTheHeaderRowsFbrefRepeatsInTheMiddleOfTheFile() throws IOException {
        List<Player> players = parse(HEADER + "\n"
                + "Nelson Abbey,eng ENG,Rio Ave,DF,22-150,13,12,\"1,040\",0,0,0,2,0,0,0\n"
                + HEADER + "\n"
                + "Sabit Abdulai,gh GHA,Alverca,MF,26-259,15,14,\"1,068\",0,1,0,5,1,0,0.08");

        assertThat(players)
                .extracting(Player::getName)
                .containsExactly("Nelson Abbey", "Sabit Abdulai");
    }

    @Test
    void skipsBlankLines() throws IOException {
        List<Player> players = parse(HEADER + "\n"
                + "\n"
                + "Karem Zoabi,il ISR,Rio Ave,FW,19-267,3,1,59,0,0,0,1,0,0,0\n"
                + "   \n");

        assertThat(players).hasSize(1);
    }

    @Test
    void acceptsAnEmptyNation() throws IOException {
        List<Player> players = parse(HEADER + "\n"
                + "Julien Lomboto,,Rio Ave,DF,23-257,4,2,144,0,0,0,1,0,0,0");

        assertThat(players).singleElement()
                .extracting(Player::getNation)
                .isEqualTo("");
    }

    @Test
    void fallsBackToZeroForValuesItCannotRead() throws IOException {
        // Documents today's lenient behaviour: bad numbers become 0 rather than failing the import.
        // CLAUDE.md flags this as "errors are swallowed"; roadmap step 4 should make it fail loudly.
        List<Player> players = parse(HEADER + "\n"
                + "Broken Row,pt POR,Benfica,MF,n/a,n/a,n/a,n/a,-,n/a,n/a,n/a,n/a,n/a,n/a");

        assertThat(players).singleElement().satisfies(player -> {
            assertThat(player.getAge()).isZero();
            assertThat(player.getMp()).isZero();
            assertThat(player.getGls()).isZero();
            assertThat(player.getGoalsPer90()).isZero();
        });
    }

    @Test
    void rejectsRowsWithTheWrongNumberOfColumns() {
        String csv = HEADER + "\n"
                + "Nelson Abbey,eng ENG,Rio Ave,DF,22-150,13,12\n";

        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line 2")
                .hasMessageContaining("expected 15");
    }

    @Test
    void returnsAnEmptyListForAFileWithOnlyAHeader() throws IOException {
        assertThat(parse(HEADER)).isEmpty();
    }

    private List<Player> parse(String csv) throws IOException {
        try (InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            return parser.parse(in);
        }
    }
}
