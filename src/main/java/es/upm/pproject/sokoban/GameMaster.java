package es.upm.pproject.sokoban;

import java.io.PrintStream;
import java.util.Scanner;

public class GameMaster {
    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_SCORE = 0;

    private static final String CLEAR_SCREEN = "\033[2J\033[H";
    private static final String HIDE_CURSOR = "\033[?25l";
    private static final String SHOW_CURSOR = "\033[?25h";

    public static void main(String[] args) {
        int levelNumber = INITIAL_LEVEL;
        Board board = loadLevel(levelNumber);
        int levelScore = INITIAL_SCORE;

        Scanner sc = new Scanner(System.in);

        hideCursor(System.out);
        while (true) {
            printGameScreen(System.out, board, levelScore);

            if (board.isSolved()) {
                System.out.println("Congratulations! You completed level " + levelNumber + "!");
                levelNumber++;
                try {
                    board = loadLevel(levelNumber);
                    levelScore = INITIAL_SCORE;
                    System.out.println("Now starting level " + levelNumber + "!");
                    continue;
                } catch (Exception e) {
                    System.out.println("No more levels available! You win the game!");
                    break;
                }
            }

            String input = sc.nextLine();
            if (input == null || input.isEmpty())
                continue;
            if (input.length() != 1)
                continue;

            char command = input.charAt(0);
            if (isRestartInput(command)) {
                board = restartLevel(levelNumber);
                levelScore = restartLevelScore();
                continue;
            }

            if (isUndoInput(command)) {
                if (board.undo()) {
                    levelScore = Math.max(INITIAL_SCORE, levelScore - 1);
                }
                continue;
            }

            Pair direction = directionFromInput(command);
            if (direction == null) {
                System.out.println("Please enter a valid input");
                continue;
            }

            levelScore = updateLevelScore(levelScore, board.tryMovePlayer(direction));
        }

        showCursor(System.out);
        sc.close();
    }

    static void printGameScreen(PrintStream out, Board board, int levelScore) {
        out.print(CLEAR_SCREEN);
        out.print(board);
        out.println("Level score: " + levelScore);
    }

    static void hideCursor(PrintStream out){
        out.print(HIDE_CURSOR);
    }

    static void showCursor(PrintStream out){
        out.print(SHOW_CURSOR);
    }

    static int updateLevelScore(int levelScore, boolean moved) {
        if (moved) {
            return levelScore + 1;
        }
        return levelScore;
    }

    static Board restartLevel(int levelNumber) {
        return loadLevel(levelNumber);
    }

    static int restartLevelScore() {
        return INITIAL_SCORE;
    }

    static boolean isRestartInput(char character) {
        return character == 'r' || character == 'R';
    }

    static boolean isUndoInput(char character) {
        return character == 'u' || character == 'U';
    }

    private static Pair directionFromInput(char character) {
        switch (character) {
            case 'd':
                return new Pair(1, 0);
            case 'a':
                return new Pair(-1, 0);
            case 'w':
                return new Pair(0, -1);
            case 's':
                return new Pair(0, 1);
            case 'D':
                return new Pair(1, 0);
            case 'A':
                return new Pair(-1, 0);
            case 'W':
                return new Pair(0, -1);
            case 'S':
                return new Pair(0, 1);
            default:
                return null;
        }
    }

    static Board createBoard() {
        return loadLevel(INITIAL_LEVEL);
    }

    static Board loadLevel(int levelNumber) {
        String fileName = "level " + levelNumber + ".txt";
        return LevelParser.parseResource(fileName);
    }
}
