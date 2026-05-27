package es.upm.pproject.sokoban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import es.upm.pproject.sokoban.controller.GameController;
import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.Box;
import es.upm.pproject.sokoban.model.dto.GoalPosition;
import es.upm.pproject.sokoban.model.dto.Player;
import es.upm.pproject.sokoban.model.dto.Type;
import es.upm.pproject.sokoban.model.dto.Wall;
import es.upm.pproject.sokoban.model.services.implementations.ServiceFactory;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.view.GameView;

public class AppTest {
    private final BoardService boardService = ServiceFactory.createBoardService();

    @Test
    public void loadsFirstLevelIntoBoard() {
        GameController controller = new GameController();
        Board board = controller.getBoard();

        assertNotNull(board);
        assertEquals(9, board.getRows());
        assertEquals(10, board.getColumns());
        assertEquals(new Pair(2, 5), boardService.findPlayer(board));
    }

    @Test
    public void usesProjectSymbolsForObjects() {
        assertEquals("+", new Wall().toString());
        assertEquals("W", new Player().toString());
        assertEquals("#", new Box().toString());
        assertEquals("*", new GoalPosition().toString());
    }

    @Test
    public void gameScreenShowsCurrentLevelScore() {
        GameController controller = new GameController();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GameView view = new GameView(controller, new PrintStream(output));

        view.render();

        assertTrue(output.toString().contains("Level score: 0"));
    }

    @Test
    public void levelScoreIncreasesOnlyAfterSuccessfulMoves() {
        GameController controller = new GameController();
        Board board = controller.getBoard();
        // Reset to a controlled state if needed, but here we just test movePlayer
        int initialScore = controller.getLevelScore();

        controller.movePlayer(new Pair(1, 0)); // Move player in level 1 (2,5) -> (3,5) is usually free
        assertTrue(controller.getLevelScore() > initialScore);
    }

    @Test
    public void restartShortcutReloadsCurrentLevelAndResetsScore() {
        GameController controller = new GameController();
        Board initialBoard = controller.getBoard();
        String initialRender = controller.getBoardService().render(initialBoard);
        
        controller.movePlayer(new Pair(1, 0));
        assertTrue(controller.getLevelScore() > 0);

        controller.restartLevel();

        assertEquals(0, controller.getLevelScore());
        assertEquals(initialRender, controller.getBoardService().render(controller.getBoard()));
    }

    @Test
    public void playerMovesHorizontallyAndVertically() {
        Board board = new Board(3, 3);
        boardService.addActor(board, new Pair(1, 1), new Player());

        assertTrue(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertEquals(new Pair(2, 1), boardService.findPlayer(board));

        assertTrue(boardService.tryMovePlayer(board, new Pair(0, 1)));
        assertEquals(new Pair(2, 2), boardService.findPlayer(board));
    }

    @Test
    public void playerCannotMoveDiagonally() {
        Board board = new Board(3, 3);
        boardService.addActor(board, new Pair(1, 1), new Player());

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 1)));
        assertEquals(new Pair(1, 1), boardService.findPlayer(board));
    }

    @Test
    public void playerCannotMoveThroughWalls() {
        Board board = new Board(3, 3);
        boardService.addActor(board, new Pair(1, 1), new Player());
        boardService.addTerrain(board, new Pair(2, 1), new Wall());

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertEquals(new Pair(1, 1), boardService.findPlayer(board));
    }

    @Test
    public void playerCannotLeaveBoard() {
        Board board = new Board(2, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());

        assertFalse(boardService.tryMovePlayer(board, new Pair(-1, 0)));
        assertFalse(boardService.tryMovePlayer(board, new Pair(0, -1)));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
    }

    @Test
    public void playerPushesBoxIntoFreeSquare() {
        Board board = new Board(1, 4);
        boardService.addActor(board, new Pair(0, 0), new Player());
        boardService.addActor(board, new Pair(1, 0), new Box());

        assertTrue(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertEquals(new Pair(1, 0), boardService.findPlayer(board));
        assertEquals(Type.BOX, boardService.get(board, 2, 0).type());
    }

    @Test
    public void playerCannotPushBoxIntoWall() {
        Board board = new Board(1, 4);
        boardService.addActor(board, new Pair(0, 0), new Player());
        boardService.addActor(board, new Pair(1, 0), new Box());
        boardService.addTerrain(board, new Pair(2, 0), new Wall());

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
        assertEquals(Type.BOX, boardService.get(board, 1, 0).type());
    }

    @Test
    public void playerCannotPushBoxIntoAnotherBox() {
        Board board = new Board(1, 4);
        boardService.addActor(board, new Pair(0, 0), new Player());
        boardService.addActor(board, new Pair(1, 0), new Box());
        boardService.addActor(board, new Pair(2, 0), new Box());

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
        assertEquals(Type.BOX, boardService.get(board, 1, 0).type());
        assertEquals(Type.BOX, boardService.get(board, 2, 0).type());
    }

    @Test
    public void pushedBoxOnGoalCompletesLevel() {
        Board board = new Board(1, 3);
        boardService.addActor(board, new Pair(0, 0), new Player());
        boardService.addActor(board, new Pair(1, 0), new Box());
        boardService.addTerrain(board, new Pair(2, 0), new GoalPosition());

        assertFalse(boardService.isSolved(board));
        assertTrue(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertTrue(boardService.get(board, 2, 0).onGoalPos());
        assertTrue(boardService.isSolved(board));
    }

    @Test
    public void levelIsNotSolvedWhenAGoalIsEmpty() {
        Board board = new Board(1, 2);
        boardService.addTerrain(board, new Pair(0, 0), new GoalPosition());
        boardService.addTerrain(board, new Pair(1, 0), new GoalPosition());
        boardService.addActor(board, new Pair(0, 0), new Box());

        assertFalse(boardService.isSolved(board));
    }
}
