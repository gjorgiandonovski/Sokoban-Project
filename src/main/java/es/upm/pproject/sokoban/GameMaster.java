package es.upm.pproject.sokoban;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import es.upm.pproject.sokoban.GameObjects.Box;
import es.upm.pproject.sokoban.GameObjects.GoalPosition;
import es.upm.pproject.sokoban.GameObjects.Player;
import es.upm.pproject.sokoban.GameObjects.Wall;

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
        InputStream stream = GameMaster.class.getClassLoader().getResourceAsStream(fileName);
        if (stream == null) {
            throw new IllegalArgumentException("Level file not found: " + fileName);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String levelName = reader.readLine();
            String dimensions = reader.readLine();
            if (levelName == null || dimensions == null) {
                throw new IllegalArgumentException("Level file is incomplete: " + fileName);
            }

            String[] dimensionParts = dimensions.trim().split("\\s+");
            if (dimensionParts.length != 2) {
                throw new IllegalArgumentException("Invalid dimensions in " + fileName);
            }

            int rows = Integer.parseInt(dimensionParts[0]);
            int columns = Integer.parseInt(dimensionParts[1]);
            Board board = new Board(rows, columns);
            int boxes = 0;
            int goals = 0;
            int players = 0;

            for (int row = 0; row < rows; row++) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Level has fewer rows than expected: " + fileName);
                }
                if (line.length() != columns) {
                    throw new IllegalArgumentException("Invalid row length in " + fileName + " at row " + (row + 1));
                }

                for (int column = 0; column < columns; column++) {
                    Pair position = new Pair(column, row);
                    switch (line.charAt(column)) {
                        case '+':
                            board.addTerrain(position, new Wall());
                            break;
                        case '*':
                            goals++;
                            board.addTerrain(position, new GoalPosition());
                            break;
                        case '#':
                            boxes++;
                            board.addActor(position, new Box());
                            break;
                        case 'W':
                            players++;
                            board.addActor(position, new Player());
                            break;
                        case ' ':
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid character in " + fileName + " at row " + (row + 1));
                    }
                }
            }

            validateLevel(fileName, boxes, goals, players);
            return board;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read level file: " + fileName, exception);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid dimensions in " + fileName, exception);
        }
    }

    private static void validateLevel(String fileName, int boxes, int goals, int players) {
        if (players != 1) {
            throw new IllegalArgumentException(fileName + " must have exactly one warehouse man");
        }
        if (boxes < 1 || goals < 1) {
            throw new IllegalArgumentException(fileName + " must have at least one box and one goal");
        }
        if (boxes != goals) {
            throw new IllegalArgumentException(fileName + " must have the same number of boxes and goals");
        }
    }
}
