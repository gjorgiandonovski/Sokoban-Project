package es.upm.pproject.sokoban.model.services.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.Box;
import es.upm.pproject.sokoban.model.dto.GoalPosition;
import es.upm.pproject.sokoban.model.dto.Player;
import es.upm.pproject.sokoban.model.dto.Wall;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.LevelParserService;

class LevelParserServiceImpl implements LevelParserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LevelParserServiceImpl.class);
    private static final char WALL = '+';
    private static final char GOAL = '*';
    private static final char BOX = '#';
    private static final char PLAYER = 'W';
    private static final char EMPTY = ' ';

    @Override
    public Board parseResource(String fileName, BoardService boardService) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Level file name cannot be empty");
        }

        InputStream stream = LevelParserServiceImpl.class.getClassLoader().getResourceAsStream(fileName);
        if (stream == null) {
            throw new IllegalArgumentException("Level file not found: " + fileName);
        }

        return parse(stream, fileName, boardService);
    }

    @Override
    public Board parse(InputStream stream, String sourceName, BoardService boardService) {
        if (stream == null) {
            throw new IllegalArgumentException("Level input cannot be null");
        }

        String levelSource = sourceName == null || sourceName.trim().isEmpty() ? "level input" : sourceName;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, levelSource, boardService);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read level file: " + levelSource, exception);
        }
    }

    private Board parse(BufferedReader reader, String sourceName, BoardService boardService) throws IOException {
        String levelName = reader.readLine();
        String dimensions = reader.readLine();
        if (levelName == null || dimensions == null) {
            throw new IllegalArgumentException("Level file is incomplete: " + sourceName);
        }
        if (levelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Level name cannot be empty: " + sourceName);
        }

        BoardDimensions boardDimensions = parseDimensions(dimensions, sourceName);
        int rows = boardDimensions.getRows();
        int columns = boardDimensions.getColumns();
        Board board = new Board(levelName, rows, columns);
        LevelCounts counts = new LevelCounts();

        for (int row = 0; row < rows; row++) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("Level has fewer rows than expected: " + sourceName);
            }
            validateRowLength(line, columns, sourceName, row);
            parseRow(board, line, sourceName, row, counts, boardService);
        }

        String trailingLine = reader.readLine();
        if (trailingLine != null) {
            throw new IllegalArgumentException("Level has more rows than expected: " + sourceName);
        }

        validateObjectCounts(sourceName, counts);
        LOGGER.info(
            "Parsed level {} with size {}x{} (players={}, boxes={}, goals={})",
            levelName,
            rows,
            columns,
            counts.getPlayers(),
            counts.getBoxes(),
            counts.getGoals()
        );
        return board;
    }

    private BoardDimensions parseDimensions(String dimensions, String sourceName) {
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
            return new BoardDimensions(rows, columns);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid dimensions in " + sourceName, exception);
        }
    }

    private void validateRowLength(String line, int columns, String sourceName, int row) {
        if (line.length() != columns) {
            throw new IllegalArgumentException(
                "Invalid row length in " + sourceName + " at row " + (row + 1)
            );
        }
    }

    private void parseRow(Board board, String line, String sourceName, int row, LevelCounts counts, BoardService boardService) {
        for (int column = 0; column < line.length(); column++) {
            Pair position = new Pair(column, row);
            char tile = line.charAt(column);
            switch (tile) {
                case WALL:
                    boardService.addTerrain(board, position, new Wall());
                    break;
                case GOAL:
                    counts.addGoal();
                    boardService.addTerrain(board, position, new GoalPosition());
                    break;
                case BOX:
                    counts.addBox();
                    boardService.addActor(board, position, new Box());
                    break;
                case PLAYER:
                    counts.addPlayer();
                    boardService.addActor(board, position, new Player());
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

    private void validateObjectCounts(String sourceName, LevelCounts counts) {
        if (counts.getPlayers() != 1) {
            throw new IllegalArgumentException(sourceName + " must have exactly one warehouse man");
        }
        if (counts.getBoxes() < 1 || counts.getGoals() < 1) {
            throw new IllegalArgumentException(sourceName + " must have at least one box and one goal");
        }
        if (counts.getBoxes() != counts.getGoals()) {
            throw new IllegalArgumentException(sourceName + " must have the same number of boxes and goals");
        }
    }

    private static final class BoardDimensions {
        private final int rows;
        private final int columns;

        private BoardDimensions(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
        }

        private int getRows() {
            return rows;
        }

        private int getColumns() {
            return columns;
        }
    }

    private static final class LevelCounts {
        private int boxes;
        private int goals;
        private int players;

        private void addBox() {
            boxes++;
        }

        private void addGoal() {
            goals++;
        }

        private void addPlayer() {
            players++;
        }

        private int getBoxes() {
            return boxes;
        }

        private int getGoals() {
            return goals;
        }

        private int getPlayers() {
            return players;
        }
    }
}
