package com.lp.ligaportugalzone.bootstrap;

import com.lp.ligaportugalzone.player.Player;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the FBref CSV export into {@link Player} instances.
 *
 * <p>The export has a few quirks this parser has to deal with:
 * <ul>
 *   <li>fields may be quoted because they contain commas ({@code "1,620"}, {@code "MF,FW"});</li>
 *   <li>FBref repeats the header row every 25 rows, so a header can appear anywhere in the file;</li>
 *   <li>age is exported as {@code years-days} ({@code "32-011"}) and only the year part is kept.</li>
 * </ul>
 */
@Component
public class PlayerCsvParser {

    /** Number of columns the FBref export is expected to have. */
    static final int EXPECTED_COLUMNS = 15;

    /** Splits on commas that are not inside a quoted field. */
    private static final String CSV_SPLIT_PATTERN = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    /** Value of the first column on a header row. */
    private static final String HEADER_MARKER = "Player";

    /**
     * Reads every data row of the CSV. The caller owns the stream and is responsible for closing it.
     *
     * @throws IOException              if the stream cannot be read
     * @throws IllegalArgumentException if a row does not have {@link #EXPECTED_COLUMNS} columns
     */
    public List<Player> parse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        List<Player> players = new ArrayList<>();
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            if (line.isBlank()) {
                continue;
            }

            String[] columns = line.split(CSV_SPLIT_PATTERN, -1);

            if (columns.length != EXPECTED_COLUMNS) {
                throw new IllegalArgumentException(
                        "Line %d has %d columns, expected %d: %s"
                                .formatted(lineNumber, columns.length, EXPECTED_COLUMNS, line));
            }

            // FBref repeats the header every 25 rows; skipping only the first line is not enough.
            if (HEADER_MARKER.equals(clean(columns[0]))) {
                continue;
            }

            players.add(toPlayer(columns));
        }

        return players;
    }

    private Player toPlayer(String[] columns) {
        Player player = new Player();

        player.setName(clean(columns[0]));
        player.setNation(clean(columns[1]));
        player.setTeam(clean(columns[2]));
        player.setPosition(clean(columns[3]));

        player.setAge(parseInt(columns[4]));
        player.setMp(parseInt(columns[5]));
        player.setStarts(parseInt(columns[6]));
        player.setMin(parseInt(columns[7]));

        player.setGls(parseInt(columns[8]));
        player.setAst(parseInt(columns[9]));
        player.setPk(parseInt(columns[10]));
        player.setCrdY(parseInt(columns[11]));
        player.setCrdR(parseInt(columns[12]));

        // These two columns are named Gls/Ast in the export but hold Gls/90 and Ast/90.
        // See CLAUDE.md, "xG data does not exist".
        player.setGoalsPer90(parseDouble(columns[13]));
        player.setAssistsPer90(parseDouble(columns[14]));

        return player;
    }

    private String clean(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "").trim();
    }

    /**
     * Reads an integer, tolerating thousands separators and the {@code years-days} age format.
     * Returns {@code 0} for anything unparseable.
     */
    private Integer parseInt(String value) {
        String cleaned = clean(value);

        if (cleaned.contains("-")) {
            String[] parts = cleaned.split("-");
            cleaned = parts.length > 0 ? parts[0] : "";
        }

        cleaned = cleaned.replace(",", "").replace(".", "");

        if (cleaned.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Reads a decimal, returning {@code 0.0} for anything unparseable. */
    private Double parseDouble(String value) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
