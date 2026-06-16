package es.upm.pproject.sokoban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
import es.upm.pproject.sokoban.model.services.interfaces.LevelParserService;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;
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
        assertTrue(output.toString().contains("Global score: 0"));
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
    public void savedGameRestoresBoardScoresAndUndoHistory(@TempDir Path tempDirectory) throws Exception {
        GameController controller = new GameController();
        String initialRender = controller.getBoardService().render(controller.getBoard());

        controller.movePlayer(new Pair(1, 0));
        String movedRender = controller.getBoardService().render(controller.getBoard());
        Path saveFile = tempDirectory.resolve("saved-game.sok");

        controller.saveGame(saveFile);

        GameController loadedController = new GameController();
        loadedController.loadGame(saveFile);

        assertEquals(controller.getLevelNumber(), loadedController.getLevelNumber());
        assertEquals(controller.getLevelName(), loadedController.getLevelName());
        assertEquals(1, loadedController.getLevelScore());
        assertEquals(controller.getGlobalScore(), loadedController.getGlobalScore());
        assertEquals(movedRender, loadedController.getBoardService().render(loadedController.getBoard()));

        loadedController.undoMove();

        assertEquals(1, loadedController.getLevelScore());
        assertEquals(initialRender, loadedController.getBoardService().render(loadedController.getBoard()));
    }

    @Test
    public void undoDoesNotReduceLevelScore() {
        GameController controller = new GameController();

        controller.movePlayer(new Pair(1, 0));
        controller.undoMove();

        assertEquals(1, controller.getLevelScore());
    }

    @Test
    public void globalScoreSumsOnlyCompletedLevels() {
        GameController controller = new GameController();

        controller.movePlayer(new Pair(1, 0));
        assertEquals(0, controller.getGlobalScore());

        assertTrue(controller.nextLevel());
        assertEquals(2, controller.getLevelNumber());
        assertEquals(0, controller.getGlobalScore());

        solveLevelTwo(controller);

        assertTrue(controller.isSolved());
        assertEquals(7, controller.getLevelScore());
        assertEquals(7, controller.getGlobalScore());

        assertTrue(controller.nextLevel());
        assertEquals(3, controller.getLevelNumber());
        assertEquals(0, controller.getLevelScore());
        assertEquals(7, controller.getGlobalScore());
    }

    @Test
    public void globalScoreIncludesLastCompletedLevelWhenNoMoreLevels() {
        GameController controller = new GameController();

        assertTrue(controller.nextLevel());
        solveLevelTwo(controller);
        assertTrue(controller.nextLevel());
        solveLevelThree(controller);

        assertTrue(controller.isSolved());
        assertEquals(17, controller.getLevelScore());
        assertEquals(24, controller.getGlobalScore());

        assertFalse(controller.nextLevel());
        assertEquals(3, controller.getLevelNumber());
        assertEquals(17, controller.getLevelScore());
        assertEquals(24, controller.getGlobalScore());
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
    public void controllerStartsAtFirstValidLevelWhenEarlierLevelIsInvalid() {
        PairService pairService = ServiceFactory.createPairService();
        LevelParserService parser = parserWithSequence(
                invalidLevel("level 1.txt", "level 1.txt must have exactly one warehouse man"),
                validLevel("level 2.txt", board("Recovered level")));

        GameController controller = new GameController(boardService, pairService, parser);

        assertEquals(2, controller.getLevelNumber());
        assertEquals("Recovered level", controller.getLevelName());
    }

    @Test
    public void nextLevelSkipsInvalidLevelFiles() {
        PairService pairService = ServiceFactory.createPairService();
        LevelParserService parser = parserWithSequence(
                validLevel("level 1.txt", board("Initial test level")),
                invalidLevel("level 2.txt", "Invalid dimensions in level 2.txt"),
                validLevel("level 3.txt", board("Recovered next level")));

        GameController controller = new GameController(boardService, pairService, parser);

        assertTrue(controller.nextLevel());
        assertEquals(3, controller.getLevelNumber());
        assertEquals("Recovered next level", controller.getLevelName());
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
    public void getReturnsNullOutsideBoard() {
        Board board = new Board(2, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());

        assertNull(boardService.get(board, -1, 0));
        assertNull(boardService.get(board, 2, 0));
        assertNull(boardService.get(board, 0, -1));
        assertNull(boardService.get(board, 0, 2));
    }

    @Test
    public void addTerrainIgnoresNullObjects() {
        Board board = new Board(2, 2);

        boardService.addTerrain(board, new Pair(1, 1), null);

        assertTrue(board.getTerrain().isEmpty());
    }

    @Test
    public void addTerrainRejectsActorObjects() {
        Board board = new Board(2, 2);

        assertThrows(IllegalArgumentException.class, () -> boardService.addTerrain(board, new Pair(1, 1), new Player()));
    }

    @Test
    public void addTerrainRejectsNullPosition() {
        Board board = new Board(2, 2);

        assertThrows(IllegalArgumentException.class, () -> boardService.addTerrain(board, null, new Wall()));
    }

    @Test
    public void addTerrainRejectsOutsideBoardPosition() {
        Board board = new Board(2, 2);

        assertThrows(IllegalArgumentException.class, () -> boardService.addTerrain(board, new Pair(2, 0), new Wall()));
    }

    @Test
    public void addActorIgnoresNullObjects() {
        Board board = new Board(2, 2);

        boardService.addActor(board, new Pair(1, 1), null);

        assertTrue(board.getActors().isEmpty());
        assertNull(board.getPlayerPosition());
    }

    @Test
    public void addActorRejectsTerrainObjects() {
        Board board = new Board(2, 2);

        assertThrows(IllegalArgumentException.class, () -> boardService.addActor(board, new Pair(1, 1), new Wall()));
    }

    @Test
    public void addActorRejectsWalls() {
        Board board = new Board(2, 2);
        boardService.addTerrain(board, new Pair(1, 1), new Wall());

        assertThrows(IllegalArgumentException.class, () -> boardService.addActor(board, new Pair(1, 1), new Player()));
    }

    @Test
    public void addActorRejectsOccupiedSquares() {
        Board board = new Board(2, 2);
        boardService.addActor(board, new Pair(1, 1), new Box());

        assertThrows(IllegalArgumentException.class, () -> boardService.addActor(board, new Pair(1, 1), new Player()));
    }

    @Test
    public void addActorRejectsSecondPlayer() {
        Board board = new Board(2, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());

        assertThrows(IllegalArgumentException.class, () -> boardService.addActor(board, new Pair(1, 1), new Player()));
    }

    @Test
    public void tryMovePlayerReturnsFalseWhenBoardHasNoPlayer() {
        Board board = new Board(2, 2);

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 0)));
    }

    @Test
    public void playerCannotMoveIntoNonBoxOccupant() {
        Board board = new Board(1, 3);
        boardService.addActor(board, new Pair(0, 0), new Player());
        board.getActors().put(new Pair(1, 0), new Player());

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 0)));
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
    public void undoAfterPushingBoxRestoresPlayerAndBox() {
        Board board = new Board(1, 4);
        boardService.addActor(board, new Pair(0, 0), new Player());
        boardService.addActor(board, new Pair(1, 0), new Box());

        assertTrue(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertTrue(boardService.undo(board));

        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
        assertEquals(Type.PLAYER, boardService.get(board, 0, 0).type());
        assertEquals(Type.BOX, boardService.get(board, 1, 0).type());
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
    public void playerCannotPushBoxOutsideBoard() {
        Board board = new Board(1, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());
        boardService.addActor(board, new Pair(1, 0), new Box());

        assertFalse(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
        assertEquals(Type.BOX, boardService.get(board, 1, 0).type());
    }

    @Test
    public void undoReturnsFalseWhenHistoryIsEmpty() {
        Board board = new Board(2, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());

        assertFalse(boardService.undo(board));
    }

    @Test
    public void undoAfterSimpleMoveRestoresOnlyPlayerPosition() {
        Board board = new Board(1, 3);
        boardService.addActor(board, new Pair(0, 0), new Player());

        assertTrue(boardService.tryMovePlayer(board, new Pair(1, 0)));
        assertTrue(boardService.undo(board));

        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
        assertNull(boardService.get(board, 1, 0));
    }

    @Test
    public void undoIgnoresIncompleteBoxMoveRecord() {
        Board board = new Board(1, 3);
        boardService.addActor(board, new Pair(1, 0), new Player());
        board.getMoveHistory().add(new Board.MoveRecord(new Pair(0, 0), new Pair(1, 0), null, new Pair(2, 0)));

        assertTrue(boardService.undo(board));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
    }

    @Test
    public void undoIgnoresBoxRecordWithoutDestination() {
        Board board = new Board(1, 3);
        boardService.addActor(board, new Pair(1, 0), new Player());
        board.getMoveHistory().add(new Board.MoveRecord(new Pair(0, 0), new Pair(1, 0), new Pair(2, 0), null));

        assertTrue(boardService.undo(board));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
    }

    @Test
    public void undoHandlesMoveRecordWithoutActorAtRecordedDestination() {
        Board board = new Board(1, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());
        board.getMoveHistory().add(new Board.MoveRecord(new Pair(0, 0), new Pair(1, 0), null, null));

        assertTrue(boardService.undo(board));
        assertEquals(new Pair(0, 0), boardService.findPlayer(board));
        assertEquals(Type.PLAYER, boardService.get(board, 0, 0).type());
        assertNull(boardService.get(board, 1, 0));
    }

    @Test
    public void undoIgnoresNonMovableActorsWhenRefreshingGoalFlags() {
        Board board = new Board(1, 2);
        boardService.addActor(board, new Pair(0, 0), new Player());
        board.getActors().put(new Pair(1, 0), new Wall());
        board.getMoveHistory().add(new Board.MoveRecord(new Pair(0, 0), new Pair(0, 0), new Pair(1, 0), new Pair(1, 0)));

        assertTrue(boardService.undo(board));
        assertEquals(Type.WALL, boardService.get(board, 1, 0).type());
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

    @Test
    public void levelIsNotSolvedWhenABoxIsOutsideAnyGoal() {
        Board board = new Board(1, 3);
        boardService.addTerrain(board, new Pair(0, 0), new GoalPosition());
        boardService.addActor(board, new Pair(0, 0), new Box());
        boardService.addActor(board, new Pair(1, 0), new Box());

        assertFalse(boardService.isSolved(board));
    }

    @Test
    public void levelIsNotSolvedWhenThereAreNoGoals() {
        Board board = new Board(1, 1);
        boardService.addActor(board, new Pair(0, 0), new Box());

        assertFalse(boardService.isSolved(board));
    }

    @Test
    public void levelIsNotSolvedWhenBoxIsOnNonGoalTerrain() {
        Board board = new Board(1, 3);
        boardService.addTerrain(board, new Pair(0, 0), new GoalPosition());
        board.getActors().put(new Pair(0, 0), new Box());
        board.getTerrain().put(new Pair(1, 0), new Wall());
        board.getActors().put(new Pair(1, 0), new Box());

        assertFalse(boardService.isSolved(board));
    }

    @Test
    public void renderReturnsEmptyStringForEmptyBoard() {
        assertEquals("", boardService.render(new Board(0, 0)));
    }

    @Test
    public void renderReturnsEmptyStringWhenBoardHasZeroColumns() {
        assertEquals("", boardService.render(new Board(1, 0)));
    }

    private void solveLevelTwo(GameController controller) {
        move(
                controller,
                new Pair(0, -1),
                new Pair(1, 0),
                new Pair(1, 0),
                new Pair(0, 1),
                new Pair(0, -1),
                new Pair(1, 0),
                new Pair(0, 1));
    }

    private void solveLevelThree(GameController controller) {
        move(
                controller,
                new Pair(0, -1),
                new Pair(1, 0),
                new Pair(1, 0),
                new Pair(0, 1),
                new Pair(0, 1),
                new Pair(0, -1),
                new Pair(0, -1),
                new Pair(1, 0),
                new Pair(1, 0),
                new Pair(0, 1),
                new Pair(0, 1),
                new Pair(0, -1),
                new Pair(0, -1),
                new Pair(1, 0),
                new Pair(1, 0),
                new Pair(0, 1),
                new Pair(0, 1));
    }

    private void move(GameController controller, Pair... directions) {
        for (Pair direction : directions) {
            controller.movePlayer(direction);
        }
    }

    private LevelParserService parserWithSequence(LevelSpec... specs) {
        return new LevelParserService() {
            @Override
            public Board parseResource(String fileName, BoardService ignoredBoardService) {
                for (LevelSpec spec : specs) {
                    if (!spec.fileName.equals(fileName)) {
                        continue;
                    }
                    if (spec.errorMessage != null) {
                        throw new IllegalArgumentException(spec.errorMessage);
                    }
                    return spec.board;
                }
                throw new IllegalArgumentException("Level file not found: " + fileName);
            }

            @Override
            public Board parse(InputStream stream, String sourceName, BoardService ignoredBoardService) {
                throw new UnsupportedOperationException("parse(InputStream, ...) is not used in this test");
            }
        };
    }

    private LevelSpec validLevel(String fileName, Board board) {
        return new LevelSpec(fileName, board, null);
    }

    private LevelSpec invalidLevel(String fileName, String errorMessage) {
        return new LevelSpec(fileName, null, errorMessage);
    }

    private Board board(String levelName) {
        Board board = new Board(levelName, 1, 1);
        boardService.addActor(board, new Pair(0, 0), new Player());
        return board;
    }

    private static final class LevelSpec {
        private final String fileName;
        private final Board board;
        private final String errorMessage;

        private LevelSpec(String fileName, Board board, String errorMessage) {
            this.fileName = fileName;
            this.board = board;
            this.errorMessage = errorMessage;
        }
    }
}
