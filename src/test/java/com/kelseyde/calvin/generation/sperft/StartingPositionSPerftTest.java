package com.kelseyde.calvin.generation.sperft;

import lombok.Getter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Getter
@Disabled
public class StartingPositionSPerftTest extends SPerftTest {

    private final String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final String subFolder = "starting_position";

    @Test
    public void testDepthThree() {
        sPerft(3);
    }

    @Test
    public void testDepthFive() {
        sPerft(5);
    }

    @Test
    public void testDepthTen() {
        sPerft(10);
    }

    @Test
    public void testDepthFourteen() {
        sPerft(14);
    }

    @Test
    public void testDepthFifteen() {
        sPerft(15);
    }

    @Test
    public void testDepthSixteen() {
        sPerft(16);
    }

    @Test
    public void testDepthSeventeen() {
        sPerft(17);
    }

}
