package es.upm.pproject.sokoban.model.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final Map<Pair, IObject> terrain;
    private final Map<Pair, IObject> actors;
    private final int rows;
    private final int columns;
    private Pair playerPosition;
    private final List<MoveRecord> moveHistory;

    public static class MoveRecord {
        private final Pair playerFrom;
        private final Pair playerTo;
        private final Pair boxFrom;
        private final Pair boxTo;

        public MoveRecord(Pair playerFrom, Pair playerTo, Pair boxFrom, Pair boxTo) {
            this.playerFrom = playerFrom;
            this.playerTo = playerTo;
            this.boxFrom = boxFrom;
            this.boxTo = boxTo;
        }

        public Pair getPlayerFrom() { return playerFrom; }
        public Pair getPlayerTo() { return playerTo; }
        public Pair getBoxFrom() { return boxFrom; }
        public Pair getBoxTo() { return boxTo; }
    }

    public Board(int rows, int columns) {
        if (rows < 0 || columns < 0) {
            throw new IllegalArgumentException("Board dimensions cannot be negative");
        }
        this.terrain = new HashMap<>();
        this.actors = new HashMap<>();
        this.rows = rows;
        this.columns = columns;
        this.moveHistory = new ArrayList<>();
    }

    public Map<Pair, IObject> getTerrain() { return terrain; }
    public Map<Pair, IObject> getActors() { return actors; }
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public Pair getPlayerPosition() { return playerPosition; }
    public void setPlayerPosition(Pair playerPosition) { this.playerPosition = playerPosition; }
    public List<MoveRecord> getMoveHistory() { return moveHistory; }
}
