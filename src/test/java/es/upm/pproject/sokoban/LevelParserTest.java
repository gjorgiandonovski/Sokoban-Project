package es.upm.pproject.sokoban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.Type;
import es.upm.pproject.sokoban.model.services.implementations.ServiceFactory;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.LevelParserService;

public class LevelParserTest {
    private final BoardService boardService = ServiceFactory.createBoardService();
    private final LevelParserService levelParserService = ServiceFactory.createLevelParserService();

    @Test
    public void parsesValidLevelFile() {
        Board board = parse(
                "Simple\n"
                        + "3 5\n"
                        + "+++++\n"
                        + "+W#*+\n"
                        + "+++++\n");

        assertEquals(3, board.getRows());
        assertEquals(5, board.getColumns());
        assertEquals(new Pair(1, 1), boardService.findPlayer(board));
        assertEquals(Type.WALL, boardService.get(board, 0, 0).type());
        assertEquals(Type.PLAYER, boardService.get(board, 1, 1).type());
        assertEquals(Type.BOX, boardService.get(board, 2, 1).type());
        assertEquals(Type.GOALPOSITION, boardService.get(board, 3, 1).type());
    }

    @Test
    public void parsesBundledLevelResources() {
        assertEquals(9, levelParserService.parseResource("level 1.txt", boardService).getRows());
        assertEquals(7, levelParserService.parseResource("level 2.txt", boardService).getRows());
        assertEquals(7, levelParserService.parseResource("level 3.txt", boardService).getRows());
    }

    @Test
    public void rejectsIncompleteLevelFile() {
        assertThrows(IllegalArgumentException.class, () -> parse("Only a name\n"));
    }

    @Test
    public void rejectsInvalidDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Bad dimensions\n2x 5\n+++++\n+W#*+\n"));
    }

    @Test
    public void rejectsNonPositiveDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Empty board\n0 5\n"));
    }

    @Test
    public void rejectsWrongRowLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Wrong width\n2 5\n+++++\n+W#*++\n"));
    }

    @Test
    public void rejectsUnexpectedCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Bad symbol\n3 5\n+++++\n+W?*+\n+++++\n"));
    }

    @Test
    public void rejectsExtraRows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Too many rows\n2 5\n+++++\n+W#*+\n+++++\n"));
    }

    @Test
    public void rejectsLevelWithoutExactlyOnePlayer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("No player\n3 5\n+++++\n+ #*+\n+++++\n"));
    }

    @Test
    public void rejectsLevelWithDifferentBoxAndGoalCounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("Missing goal\n3 5\n+++++\n+W##+\n+++++\n"));
    }

    private Board parse(String level) {
        return levelParserService.parse(
                new ByteArrayInputStream(level.getBytes(StandardCharsets.UTF_8)),
                "test-level.txt",
                boardService);
    }
}
