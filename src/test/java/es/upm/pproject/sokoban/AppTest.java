package es.upm.pproject.sokoban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import es.upm.pproject.sokoban.GameObjects.Box;
import es.upm.pproject.sokoban.GameObjects.GoalPosition;
import es.upm.pproject.sokoban.GameObjects.Player;
import es.upm.pproject.sokoban.GameObjects.Type;
import es.upm.pproject.sokoban.GameObjects.Wall;

public class AppTest {

    @Test
    public void loadsFirstLevelIntoBoard() {
        Board board = GameMaster.createBoard();

        assertNotNull(board);
        assertEquals(9, board.getRows());
        assertEquals(10, board.getColumns());
        assertEquals(new Pair(2, 5), board.findPlayer());
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
        Board board = new Board(1, 1);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        GameMaster.printGameScreen(new PrintStream(output), board, 3);

        assertTrue(output.toString().contains("Level score: 3"));
    }

    @Test
    public void levelScoreIncreasesOnlyAfterSuccessfulMoves() {
        Board board = new Board(1, 2);
        board.addActor(new Pair(0, 0), new Player());
        int levelScore = 0;

        levelScore = GameMaster.updateLevelScore(levelScore, board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(1, levelScore);

        levelScore = GameMaster.updateLevelScore(levelScore, board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(1, levelScore);
    }

    @Test
    public void restartShortcutReloadsCurrentLevelAndResetsScore() {
        Board board = GameMaster.loadLevel(1);
        String initialBoard = board.toString();
        int levelScore = GameMaster.updateLevelScore(0, board.tryMovePlayer(new Pair(1, 0)));

        assertTrue(GameMaster.isRestartInput('r'));
        assertTrue(GameMaster.isRestartInput('R'));
        assertEquals(1, levelScore);

        Board restartedBoard = GameMaster.restartLevel(1);
        levelScore = GameMaster.restartLevelScore();

        assertEquals(0, levelScore);
        assertEquals(new Pair(2, 5), restartedBoard.findPlayer());
        assertEquals(initialBoard, restartedBoard.toString());
    }

    @Test
    public void playerMovesHorizontallyAndVertically() {
        Board board = new Board(3, 3);
        board.addActor(new Pair(1, 1), new Player());

        assertTrue(board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(new Pair(2, 1), board.findPlayer());

        assertTrue(board.tryMovePlayer(new Pair(0, 1)));
        assertEquals(new Pair(2, 2), board.findPlayer());
    }

    @Test
    public void playerCannotMoveDiagonally() {
        Board board = new Board(3, 3);
        board.addActor(new Pair(1, 1), new Player());

        assertFalse(board.tryMovePlayer(new Pair(1, 1)));
        assertEquals(new Pair(1, 1), board.findPlayer());
    }

    @Test
    public void playerCannotMoveThroughWalls() {
        Board board = new Board(3, 3);
        board.addActor(new Pair(1, 1), new Player());
        board.addTerrain(new Pair(2, 1), new Wall());

        assertFalse(board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(new Pair(1, 1), board.findPlayer());
    }

    @Test
    public void playerCannotLeaveBoard() {
        Board board = new Board(2, 2);
        board.addActor(new Pair(0, 0), new Player());

        assertFalse(board.tryMovePlayer(new Pair(-1, 0)));
        assertFalse(board.tryMovePlayer(new Pair(0, -1)));
        assertEquals(new Pair(0, 0), board.findPlayer());
    }

    @Test
    public void playerPushesBoxIntoFreeSquare() {
        Board board = new Board(1, 4);
        board.addActor(new Pair(0, 0), new Player());
        board.addActor(new Pair(1, 0), new Box());

        assertTrue(board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(new Pair(1, 0), board.findPlayer());
        assertEquals(Type.BOX, board.get(2, 0).type());
    }

    @Test
    public void playerCannotPushBoxIntoWall() {
        Board board = new Board(1, 4);
        board.addActor(new Pair(0, 0), new Player());
        board.addActor(new Pair(1, 0), new Box());
        board.addTerrain(new Pair(2, 0), new Wall());

        assertFalse(board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(new Pair(0, 0), board.findPlayer());
        assertEquals(Type.BOX, board.get(1, 0).type());
    }

    @Test
    public void playerCannotPushBoxIntoAnotherBox() {
        Board board = new Board(1, 4);
        board.addActor(new Pair(0, 0), new Player());
        board.addActor(new Pair(1, 0), new Box());
        board.addActor(new Pair(2, 0), new Box());

        assertFalse(board.tryMovePlayer(new Pair(1, 0)));
        assertEquals(new Pair(0, 0), board.findPlayer());
        assertEquals(Type.BOX, board.get(1, 0).type());
        assertEquals(Type.BOX, board.get(2, 0).type());
    }

    @Test
    public void pushedBoxOnGoalCompletesLevel() {
        Board board = new Board(1, 3);
        board.addActor(new Pair(0, 0), new Player());
        board.addActor(new Pair(1, 0), new Box());
        board.addTerrain(new Pair(2, 0), new GoalPosition());

        assertFalse(board.isSolved());
        assertTrue(board.tryMovePlayer(new Pair(1, 0)));
        assertTrue(board.get(2, 0).onGoalPos());
        assertTrue(board.isSolved());
    }

    @Test
    public void levelIsNotSolvedWhenAGoalIsEmpty() {
        Board board = new Board(1, 2);
        board.addTerrain(new Pair(0, 0), new GoalPosition());
        board.addTerrain(new Pair(1, 0), new GoalPosition());
        board.addActor(new Pair(0, 0), new Box());

        assertFalse(board.isSolved());
    }
}
