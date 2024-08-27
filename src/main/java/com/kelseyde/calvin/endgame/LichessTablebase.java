package com.kelseyde.calvin.endgame;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelseyde.calvin.board.Board;
import com.kelseyde.calvin.board.Move;
import com.kelseyde.calvin.engine.EngineConfig;
import com.kelseyde.calvin.utils.FEN;
import com.kelseyde.calvin.utils.Notation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of {@link Tablebase} that uses the Lichess online API to make the tablebase probe.
 *
 * <p>The limitations of this implementation are that it must send an HTTP request, meaning it is relatively slow, and
 * that the Lichess API is request-limited, meaning that eventually the engine will hit its limit on requests and must
 * wait another minute before querying again.</p>
 */
public class LichessTablebase implements Tablebase {

    private static final Duration API_REQUEST_LIMIT_COOLDOWN = Duration.ofMinutes(1);

    private final EngineConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Instant lastLimitReached;

    /**
     * Constructs a new LichessTablebase instance with the provided engine configuration.
     *
     * @param config the engine configuration.
     */
    public LichessTablebase(EngineConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gets the best move from the Lichess tablebase for the given board position.
     *
     * @param board the current board position.
     * @return the best move as determined by the Lichess tablebase.
     * @throws TablebaseException if an error occurs while querying the tablebase.
     */
    @Override
    public Move getTablebaseMove(Board board) throws TablebaseException {
        try {
            HttpRequest request = buildRequest(board);
            HttpResponse<String> response = probeLichess(request);
            LichessTablebaseEntry tablebaseEntry = parseResponse(response);
            return parseBestMove(tablebaseEntry);
        } catch (URISyntaxException e) {
            throw new TablebaseException("Invalid URI syntax for Lichess tablebase request!", e);
        } catch (IOException | InterruptedException e) {
            throw new TablebaseException("Failed to probe Lichess tablebase!", e);
        }
    }

    /**
     * Checks if the tablebase can be probed within the given timeout.
     *
     * @param timeout the timeout in milliseconds.
     * @return true if the tablebase can be probed, false otherwise.
     */
    @Override
    public boolean canProbeTablebase(Duration timeout) {
        int overheadMs = 50;
        if (timeout.toMillis() - overheadMs <= config.getLichessTablebaseTimeoutMs()) {
            return false;
        }
        boolean canProbe = lastLimitReached == null || Instant.now().isAfter(lastLimitReached.plus(API_REQUEST_LIMIT_COOLDOWN));
        if (config.isLichessTablebaseDebugEnabled()) {
            System.out.println("Can probe Lichess tablebase? " + canProbe);
        }
        return canProbe;
    }

    /**
     * Builds the HTTP request to query the Lichess tablebase.
     *
     * @param board   the current board position.
     * @return the constructed HttpRequest.
     * @throws URISyntaxException if the URI syntax is invalid.
     */
    private HttpRequest buildRequest(Board board) throws URISyntaxException {
        String fen = FEN.toFEN(board).replace(" ", "_");
        String url = config.getLichessTablebaseBaseUrl() + "/standard?fen=" + fen;
        Duration timeout = Duration.ofMillis(config.getLichessTablebaseTimeoutMs());
        if (config.isLichessTablebaseDebugEnabled()) {
            System.out.println("Lichess url:" + url);
        }
        return HttpRequest.newBuilder().uri(new URI(url)).timeout(timeout).GET().build();
    }

    /**
     * Sends the HTTP request to the Lichess tablebase and returns the response.
     *
     * @param httpRequest the HTTP request to be sent.
     * @return the HTTP response from the Lichess tablebase.
     * @throws IOException          if an I/O error occurs.
     * @throws InterruptedException if the operation is interrupted.
     */
    private HttpResponse<String> probeLichess(HttpRequest httpRequest) throws IOException, InterruptedException {
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
        int status = httpResponse.statusCode();
        if (config.isLichessTablebaseDebugEnabled()) {
            System.out.printf("Lichess response: status %s, body %s%n", status, objectMapper.writeValueAsString(httpResponse.body()));
        }
        if (status == 429) {
            lastLimitReached = Instant.now();
            throw new TablebaseException("Lichess API request limit reached!");
        }
        if (status != 200) {
            throw new TablebaseException(String.format("Lichess tablebase probe failed with status %s!", status));
        }
        return httpResponse;
    }

    /**
     * Parses the JSON response from the Lichess tablebase into a LichessTablebaseEntry.
     *
     * @param response the HTTP response from the Lichess tablebase.
     * @return the parsed LichessTablebaseEntry.
     * @throws JsonProcessingException if an error occurs while parsing the JSON.
     */
    private LichessTablebaseEntry parseResponse(HttpResponse<String> response) throws JsonProcessingException {
        String json = response.body();
        return objectMapper.readValue(json, LichessTablebaseEntry.class);
    }

    /**
     * Parses the best move from the Lichess tablebase entry.
     *
     * @param tablebaseEntry the Lichess tablebase entry.
     * @return the best move as determined by the tablebase.
     * @throws TablebaseException if the response from the tablebase is empty.
     */
    private Move parseBestMove(LichessTablebaseEntry tablebaseEntry) {
        if (tablebaseEntry.moves().isEmpty()) {
            throw new TablebaseException("Lichess tablebase response was empty!");
        }
        LichessTablebaseMove bestMove = tablebaseEntry.moves().get(0);
        String uci = bestMove.uci();
        return Notation.fromCombinedNotation(uci);
    }

}
