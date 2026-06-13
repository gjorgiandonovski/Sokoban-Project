package es.upm.pproject.sokoban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.Box;
import es.upm.pproject.sokoban.model.dto.GoalPosition;
import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Player;
import es.upm.pproject.sokoban.model.dto.Type;
import es.upm.pproject.sokoban.model.dto.Wall;
import es.upm.pproject.sokoban.model.services.implementations.ServiceFactory;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.LevelParserService;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;

public class ModelCoverageTest {
    private final BoardService boardService = ServiceFactory.createBoardService();
    private final PairService pairService = ServiceFactory.createPairService();
    private final LevelParserService levelParserService = ServiceFactory.createLevelParserService();

    @Test
    public void goalPositionAlwaysStaysOffGoal() {
        GoalPosition goal = new GoalPosition();

        assertEquals(Type.GOALPOSITION, goal.type());
        assertFalse(goal.onGoalPos());

        goal.setOnGoalPos(true);

        assertFalse(goal.onGoalPos());
    }

    @Test
    public void wallAlwaysStaysOffGoal() {
        Wall wall = new Wall();

        assertEquals(Type.WALL, wall.type());
        assertFalse(wall.onGoalPos());

        wall.setOnGoalPos(true);

        assertFalse(wall.onGoalPos());
    }

    @Test
    public void playerTracksGoalFlagState() {
        Player player = new Player();

        assertFalse(player.onGoalPos());
        player.setOnGoalPos(true);
        assertTrue(player.onGoalPos());
        player.setOnGoalPos(false);
        assertFalse(player.onGoalPos());
    }

    @Test
    public void pairSupportsEqualityHashCodeAndOrdering() {
        Pair pair = new Pair(2, 3);

        assertEquals(2, pair.x());
        assertEquals(3, pair.y());
        assertTrue(pair.equals(pair));
        assertFalse(pair.equals("pair"));
        assertFalse(pair.equals(new Pair(2, 4)));
        assertFalse(pair.equals(new Pair(4, 3)));
        assertEquals(new Pair(2, 3), pair);
        assertEquals(pair.hashCode(), new Pair(2, 3).hashCode());
        assertTrue(new Pair(0, 0).compareTo(new Pair(0, 1)) < 0);
        assertTrue(new Pair(0, 1).compareTo(new Pair(1, 1)) < 0);
        assertEquals(0, new Pair(1, 1).compareTo(new Pair(1, 1)));
    }

    @Test
    public void pairServiceHandlesNullsAndCardinalDirections() {
        assertNull(pairService.add(null, new Pair(1, 0)));
        assertNull(pairService.add(new Pair(1, 0), null));
        assertEquals(new Pair(3, 4), pairService.add(new Pair(1, 1), new Pair(2, 3)));

        assertFalse(pairService.isCardinalDirection(null));
        assertFalse(pairService.isCardinalDirection(new Pair(0, 0)));
        assertFalse(pairService.isCardinalDirection(new Pair(1, 1)));
        assertTrue(pairService.isCardinalDirection(new Pair(0, -1)));
    }

    @Test
    public void serviceFactoryCreatesExpectedServices() {
        ServiceFactory factory = new ServiceFactory();

        assertNotNull(factory);
        assertNotNull(ServiceFactory.createBoardService());
        assertNotNull(ServiceFactory.createPairService());
        assertNotNull(ServiceFactory.createLevelParserService());
    }

    @Test
    public void boardUsesDefaultLevelNameAndTracksState() {
        Board defaultNameBoard = new Board(null, 2, 3);
        Board blankNameBoard = new Board("   ", 1, 1);
        Board.MoveRecord moveRecord = new Board.MoveRecord(new Pair(0, 0), new Pair(1, 0), new Pair(1, 0), new Pair(2, 0));

        assertEquals("Level", defaultNameBoard.getLevelName());
        assertEquals("Level", blankNameBoard.getLevelName());
        defaultNameBoard.setPlayerPosition(new Pair(0, 1));
        defaultNameBoard.getMoveHistory().add(moveRecord);

        assertEquals(new Pair(0, 1), defaultNameBoard.getPlayerPosition());
        assertEquals(moveRecord, defaultNameBoard.getMoveHistory().get(0));
        assertEquals(new Pair(0, 0), moveRecord.getPlayerFrom());
        assertEquals(new Pair(1, 0), moveRecord.getPlayerTo());
        assertEquals(new Pair(1, 0), moveRecord.getBoxFrom());
        assertEquals(new Pair(2, 0), moveRecord.getBoxTo());
    }

    @Test
    public void boardRejectsNegativeDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new Board(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Board(1, -1));
    }

    @Test
    public void parseResourceRejectsBlankAndMissingNames() {
        assertThrows(IllegalArgumentException.class, () -> levelParserService.parseResource(null, boardService));
        assertThrows(IllegalArgumentException.class, () -> levelParserService.parseResource(" ", boardService));
        assertThrows(IllegalArgumentException.class, () -> levelParserService.parseResource("missing-level.txt", boardService));
    }

    @Test
    public void parseRejectsNullStreamAndBlankLevelName() {
        assertThrows(IllegalArgumentException.class, () -> levelParserService.parse(null, "missing.txt", boardService));
        assertThrows(IllegalArgumentException.class, () -> parse("\n1 1\n \n", "blank-name.txt"));
    }

    @Test
    public void parseUsesFallbackSourceNameWhenMissing() {
        Board boardFromNullSource = levelParserService.parse(level("Named\n1 3\nW#*\n"), null, boardService);
        Board boardFromBlankSource = levelParserService.parse(level("Named\n1 3\nW#*\n"), " ", boardService);

        assertEquals("Named", boardFromNullSource.getLevelName());
        assertEquals("Named", boardFromBlankSource.getLevelName());
    }

    @Test
    public void parseWrapsIoFailures() {
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> levelParserService.parse(failingStream, "broken.txt", boardService));

        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    public void parseRejectsEmptyInputAndMissingRows() {
        assertThrows(IllegalArgumentException.class, () -> parse("", "empty.txt"));
        assertThrows(IllegalArgumentException.class, () -> parse("Few rows\n2 1\nW\n", "few-rows.txt"));
    }

    @Test
    public void parseRejectsDimensionCountAndMissingObjects() {
        assertThrows(IllegalArgumentException.class, () -> parse("Bad dimensions\n2\n++\n", "bad-dimensions.txt"));
        assertThrows(IllegalArgumentException.class, () -> parse("No objects\n1 1\nW\n", "missing-objects.txt"));
        assertThrows(IllegalArgumentException.class, () -> parse("Zero columns\n1 0\n", "zero-columns.txt"));
    }

    @Test
    public void parseRejectsMismatchedBoxesAndGoalsWhenBothExist() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Mismatch\n2 3\n*W*\n # \n", "mismatch.txt"));
    }

    private Board parse(String level, String sourceName) {
        return levelParserService.parse(level(level), sourceName, boardService);
    }

    private InputStream level(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
