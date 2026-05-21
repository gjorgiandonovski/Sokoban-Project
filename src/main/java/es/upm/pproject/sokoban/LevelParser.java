package es.upm.pproject.sokoban;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import es.upm.pproject.sokoban.GameObjects.Box;
import es.upm.pproject.sokoban.GameObjects.GoalPosition;
import es.upm.pproject.sokoban.GameObjects.Player;
import es.upm.pproject.sokoban.GameObjects.Wall;

public final class LevelParser {
    private static final char WALL = '+';
    private static final char GOAL = '*';
    private static final char BOX = '#';
    private static final char PLAYER = 'W';
    private static final char EMPTY = ' ';

    private LevelParser() {
    }

    public static Board parseResource(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Level file name cannot be empty");
        }

        InputStream stream = LevelParser.class.getClassLoader().getResourceAsStream(fileName);
        if (stream == null) {
            throw new IllegalArgumentException("Level file not found: " + fileName);
        }

        return parse(stream, fileName);
    }

    public static Board parse(InputStream stream, String sourceName) {
        if (stream == null) {
            throw new IllegalArgumentException("Level input cannot be null");
        }

        String levelSource = sourceName == null || sourceName.trim().isEmpty() ? "level input" : sourceName;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, levelSource);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read level file: " + levelSource, exception);
        }
    }

    private static Board parse(BufferedReader reader, String sourceName) throws IOException {
        String levelName = reader.readLine();
        String dimensions = reader.readLine();
        if (levelName == null || dimensions == null) {
            throw new IllegalArgumentException("Level file is incomplete: " + sourceName);
        }
        if (levelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Level name cannot be empty: " + sourceName);
        }

        int[] boardDimensions = parseDimensions(dimensions, sourceName);
        int rows = boardDimensions[0];
        int columns = boardDimensions[1];
        Board board = new Board(rows, columns);
        LevelCounts counts = new LevelCounts();

        for (int row = 0; row < rows; row++) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("Level has fewer rows than expected: " + sourceName);
            }
            validateRowLength(line, columns, sourceName, row);
            parseRow(board, line, sourceName, row, counts);
        }

        if (reader.readLine() != null) {
            throw new IllegalArgumentException("Level has more rows than expected: " + sourceName);
        }

        validateObjectCounts(sourceName, counts);
        return board;
    }

    private static int[] parseDimensions(String dimensions, String sourceName) {
        String[] dimensionParts = dimensions.trim().split("\\s+");
        if (dimensionParts.length != 2) {
            throw new IllegalArgumentException("Invalid dimensions in " + sourceName);
        }

        try {
            int rows = Integer.parseInt(dimensionParts[0]);
            int columns = Integer.parseInt(dimensionParts[1]);
            if (rows <= 0 || columns <= 0) {
                throw new IllegalArgumentException("Level dimensions must be positive in " + sourceName);
            }
            return new int[] { rows, columns };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid dimensions in " + sourceName, exception);
        }
    }

    private static void validateRowLength(String line, int columns, String sourceName, int row) {
        if (line.length() != columns) {
            throw new IllegalArgumentException(
                "Invalid row length in " + sourceName + " at row " + (row + 1)
            );
        }
    }

    private static void parseRow(Board board, String line, String sourceName, int row, LevelCounts counts) {
        for (int column = 0; column < line.length(); column++) {
            Pair position = new Pair(column, row);
            char tile = line.charAt(column);
            switch (tile) {
                case WALL:
                    board.addTerrain(position, new Wall());
                    break;
                case GOAL:
                    counts.goals++;
                    board.addTerrain(position, new GoalPosition());
                    break;
                case BOX:
                    counts.boxes++;
                    board.addActor(position, new Box());
                    break;
                case PLAYER:
                    counts.players++;
                    board.addActor(position, new Player());
                    break;
                case EMPTY:
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Invalid character in " + sourceName + " at row " + (row + 1)
                            + ", column " + (column + 1)
                    );
            }
        }
    }

    private static void validateObjectCounts(String sourceName, LevelCounts counts) {
        if (counts.players != 1) {
            throw new IllegalArgumentException(sourceName + " must have exactly one warehouse man");
        }
        if (counts.boxes < 1 || counts.goals < 1) {
            throw new IllegalArgumentException(sourceName + " must have at least one box and one goal");
        }
        if (counts.boxes != counts.goals) {
            throw new IllegalArgumentException(sourceName + " must have the same number of boxes and goals");
        }
    }

    private static final class LevelCounts {
        private int boxes;
        private int goals;
        private int players;
    }
}
