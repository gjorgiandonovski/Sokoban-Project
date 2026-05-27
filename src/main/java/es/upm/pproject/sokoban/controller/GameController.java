package es.upm.pproject.sokoban.controller;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
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
}
