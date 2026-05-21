package es.upm.pproject.sokoban;

import java.io.PrintStream;
import java.util.Scanner;

public class GameMaster {

    public static void main(String[] args) {
        Board board = createBoard();
        int levelScore = 0;

        Scanner sc = new Scanner(System.in);
        while (true) {
            printGameScreen(System.out, board, levelScore);

            if (board.isSolved()) {
                break;
            }

            String input = sc.nextLine();
            if (input == null || input.isEmpty()) continue;
            if (input.length() != 1) continue;

            Pair direction = directionFromInput(input.charAt(0));
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
        return loadLevel(1);
    }

    static Board loadLevel(int levelNumber) {
        String fileName = "level " + levelNumber + ".txt";
        return LevelParser.parseResource(fileName);
    }
}
