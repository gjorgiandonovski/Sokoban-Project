package es.upm.pproject.sokoban;

import java.util.Scanner;

public class GameMaster {

    public static void main(String[] args) {
        Board board = createBoard();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println(board);

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

            board.tryMovePlayer(direction);
        }

        sc.close();
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

    private static Board createBoard() {
        return null;
    }
}