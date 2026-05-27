package es.upm.pproject.sokoban.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.IObject;
import es.upm.pproject.sokoban.model.services.implementations.ServiceFactory;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;
import es.upm.pproject.sokoban.model.services.interfaces.LevelParserService;

public class GameController {
    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_SCORE = 0;

    private final BoardService boardService;
    private final PairService pairService;
    private final LevelParserService levelParserService;
    private Board board;
    private int levelNumber;
    private int levelScore;
    private int globalScore;
    private boolean currentLevelScoreRecorded;

    public GameController() {
        this.boardService = ServiceFactory.createBoardService();
        this.pairService = ServiceFactory.createPairService();
        this.levelParserService = ServiceFactory.createLevelParserService();
        this.levelNumber = INITIAL_LEVEL;
        this.levelScore = INITIAL_SCORE;
        this.globalScore = INITIAL_SCORE;
        this.currentLevelScoreRecorded = false;
        this.board = loadLevel(levelNumber);
    }

    public void movePlayer(Pair direction) {
        if (direction != null) {
            boolean moved = boardService.tryMovePlayer(board, direction);
            if (moved) {
                clearCurrentLevelScoreRecord();
                levelScore++;
            }
        }
    }

    public void undoMove() {
        if (boardService.undo(board)) {
            clearCurrentLevelScoreRecord();
            levelScore = Math.max(INITIAL_SCORE, levelScore - 1);
        }
    }

    public void restartLevel() {
        clearCurrentLevelScoreRecord();
        this.board = loadLevel(levelNumber);
        this.levelScore = INITIAL_SCORE;
    }

    public void startNewGame() {
        this.levelNumber = INITIAL_LEVEL;
        this.levelScore = INITIAL_SCORE;
        this.globalScore = INITIAL_SCORE;
        this.currentLevelScoreRecorded = false;
        this.board = loadLevel(levelNumber);
    }

    public boolean nextLevel() {
        recordCurrentLevelScoreIfSolved();
        int nextLevelNumber = levelNumber + 1;
        try {
            Board nextBoard = loadLevel(nextLevelNumber);
            this.levelNumber = nextLevelNumber;
            this.board = nextBoard;
            this.levelScore = INITIAL_SCORE;
            this.currentLevelScoreRecorded = false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Board getBoard() {
        return board;
    }

    public String getLevelName() {
        return board.getLevelName();
    }

    public int getBoardRows() {
        return board.getRows();
    }

    public int getBoardColumns() {
        return board.getColumns();
    }

    public IObject getTerrainAt(int x, int y) {
        return board.getTerrain().get(new Pair(x, y));
    }

    public IObject getActorAt(int x, int y) {
        return board.getActors().get(new Pair(x, y));
    }

    public int getLevelScore() {
        return levelScore;
    }

    public int getGlobalScore() {
        if (!currentLevelScoreRecorded && isSolved()) {
            return globalScore + levelScore;
        }
        return globalScore;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isSolved() {
        return boardService.isSolved(board);
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public PairService getPairService() {
        return pairService;
    }

    public void handleInput(String input) {
        if (input == null || input.isEmpty() || input.length() != 1) {
            return;
        }

        char command = input.charAt(0);
        if (isRestartInput(command)) {
            restartLevel();
        } else if (isUndoInput(command)) {
            undoMove();
        } else {
            Pair direction = directionFromInput(command);
            movePlayer(direction);
        }
    }

    public Board loadLevel(int level) {
        String fileName = "level " + level + ".txt";
        return levelParserService.parseResource(fileName, boardService);
    }

    public Pair directionFromInput(char character) {
        switch (character) {
            case 'd': case 'D': return new Pair(1, 0);
            case 'a': case 'A': return new Pair(-1, 0);
            case 'w': case 'W': return new Pair(0, -1);
            case 's': case 'S': return new Pair(0, 1);
            default: return null;
        }
    }

    public boolean isRestartInput(char character) {
        return character == 'r' || character == 'R';
    }

    public boolean isUndoInput(char character) {
        return character == 'u' || character == 'U';
    }

    public void saveGame(Path saveFile) throws IOException {
        if (saveFile == null) {
            throw new IllegalArgumentException("Save file cannot be null");
        }

        SaveGameState state = new SaveGameState(
            levelNumber,
            board,
            levelScore,
            globalScore,
            currentLevelScoreRecorded
        );
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(saveFile))) {
            output.writeObject(state);
        }
    }

    public void loadGame(Path saveFile) throws IOException {
        if (saveFile == null) {
            throw new IllegalArgumentException("Save file cannot be null");
        }

        Object loadedObject;
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(saveFile))) {
            loadedObject = input.readObject();
        } catch (ClassNotFoundException exception) {
            throw new IOException("Saved game format is not supported", exception);
        }

        if (!(loadedObject instanceof SaveGameState)) {
            throw new IOException("Selected file is not a saved Sokoban game");
        }

        SaveGameState state = (SaveGameState) loadedObject;
        restore(state);
    }

    private void recordCurrentLevelScoreIfSolved() {
        if (!currentLevelScoreRecorded && isSolved()) {
            globalScore += levelScore;
            currentLevelScoreRecorded = true;
        }
    }

    private void clearCurrentLevelScoreRecord() {
        if (currentLevelScoreRecorded) {
            globalScore = Math.max(INITIAL_SCORE, globalScore - levelScore);
            currentLevelScoreRecorded = false;
        }
    }

    private void restore(SaveGameState state) throws IOException {
        if (state.board == null) {
            throw new IOException("Saved game does not contain a board");
        }
        if (state.levelNumber < INITIAL_LEVEL || state.levelScore < INITIAL_SCORE || state.globalScore < INITIAL_SCORE) {
            throw new IOException("Saved game contains invalid score or level values");
        }

        this.levelNumber = state.levelNumber;
        this.board = state.board;
        this.levelScore = state.levelScore;
        this.globalScore = state.globalScore;
        this.currentLevelScoreRecorded = state.currentLevelScoreRecorded;
    }

    private static final class SaveGameState implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int levelNumber;
        private final Board board;
        private final int levelScore;
        private final int globalScore;
        private final boolean currentLevelScoreRecorded;

        private SaveGameState(
            int levelNumber,
            Board board,
            int levelScore,
            int globalScore,
            boolean currentLevelScoreRecorded
        ) {
            this.levelNumber = levelNumber;
            this.board = board;
            this.levelScore = levelScore;
            this.globalScore = globalScore;
            this.currentLevelScoreRecorded = currentLevelScoreRecorded;
        }
    }
}
