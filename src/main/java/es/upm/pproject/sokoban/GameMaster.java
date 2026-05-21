package es.upm.pproject.sokoban;

import java.io.PrintStream;
import java.util.Scanner;

public class GameMaster {
    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_SCORE = 0;

    public static void main(String[] args) {
        int levelNumber = INITIAL_LEVEL;
        Board board = loadLevel(levelNumber);
        int levelScore = INITIAL_SCORE;

        Scanner sc = new Scanner(System.in);
        while (true) {
            printGameScreen(System.out, board, levelScore);

            if (board.isSolved()) {
                break;
            }

            String input = sc.nextLine();
            if (input == null || input.isEmpty()) continue;
            if (input.length() != 1) continue;

            char command = input.charAt(0);
            if (isRestartInput(command)) {
                board = restartLevel(levelNumber);
                levelScore = restartLevelScore();
                continue;
            }

            Pair direction = directionFromInput(command);
            if (direction == null) {
                System.out.println("Please enter a valid input");
                continue;
            }

            levelScore = updateLevelScore(levelScore, board.tryMovePlayer(direction));
        }

        sc.close();
    }

    static void printGameScreen(PrintStream out, Board board, int levelScore) {
        out.print(board);
        out.println("Level score: " + levelScore);
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
