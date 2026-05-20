package es.upm.pproject.sokoban;

import java.util.HashMap;
import java.util.Map;

import es.upm.pproject.sokoban.GameObjects.Type;
import es.upm.pproject.sokoban.GameObjects.Interfaces.IObject;

public class Board {
    private final Map<Pair, IObject> terrain;
    private final Map<Pair, IObject> actors;
    private final int rows;
    private final int columns;
    private Pair playerPosition;

    public Board() {
        this(0, 0);
    }

    public Board(int rows, int columns) {
        if (rows < 0 || columns < 0) {
            throw new IllegalArgumentException("Board dimensions cannot be negative");
        }
        this.terrain = new HashMap<>();
        this.actors = new HashMap<>();
        this.rows = rows;
        this.columns = columns;
    }

    public Board(HashMap<Pair, IObject> objects) {
        this(inferRows(objects), inferColumns(objects));
        if (objects == null) return;

        for (Map.Entry<Pair, IObject> entry : objects.entrySet()) {
            Pair pos = entry.getKey();
            IObject obj = entry.getValue();
            if (obj == null) continue;

            switch (obj.type()) {
                case WALL:
                case GOALPOSITION:
                    terrain.put(pos, obj);
                    break;
                case PLAYER:
                case BOX:
                    addActor(pos, obj);
                    break;
            }
        }

        syncOnGoalFlags();
    }

    public IObject get(int x, int y) {
        Pair p = new Pair(x, y);
        if (!isInside(p)) return null;
        IObject actor = actors.get(p);
        if (actor != null) return actor;
        return terrain.get(p);
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public Pair findPlayer() {
        return playerPosition;
    }

    public void addTerrain(Pair position, IObject obj) {
        if (obj == null) return;
        requireInside(position);
        if (obj.type() != Type.WALL && obj.type() != Type.GOALPOSITION) {
            throw new IllegalArgumentException("Terrain must be WALL or GOALPOSITION");
        }
        terrain.put(position, obj);
    }

    public void addActor(Pair position, IObject obj) {
        if (obj == null) return;
        requireInside(position);
        if (obj.type() != Type.PLAYER && obj.type() != Type.BOX) {
            throw new IllegalArgumentException("Actor must be PLAYER or BOX");
        }
        if (isWall(position)) {
            throw new IllegalArgumentException("Actors cannot be placed on walls");
        }
        if (actors.containsKey(position)) {
            throw new IllegalArgumentException("Only one actor can occupy a square");
        }
        if (obj.type() == Type.PLAYER) {
            if (playerPosition != null) {
                throw new IllegalArgumentException("The board can only have one player");
            }
            playerPosition = position;
        }
        actors.put(position, obj);
        updateActorGoalFlag(position);
    }

    public boolean tryMovePlayer(Pair direction) {
        if (playerPosition == null) return false;
        if (!isCardinalDirection(direction)) return false;

        Pair from = playerPosition;
        Pair to = from.add(direction);

        if (!isInside(to)) return false;
        if (isWall(to)) return false;

        IObject occupant = actors.get(to);
        if (occupant == null) {
            moveActor(from, to);
            playerPosition = to;
            updateActorGoalFlag(to);
            updateActorGoalFlag(from);
            return true;
        }

        if (occupant.type() != Type.BOX) return false;

        Pair boxTo = to.add(direction);
        if (!isInside(boxTo)) return false;
        if (isWall(boxTo)) return false;
        if (actors.containsKey(boxTo)) return false;

        moveActor(to, boxTo);
        updateActorGoalFlag(boxTo);
        updateActorGoalFlag(to);

        moveActor(from, to);
        playerPosition = to;
        updateActorGoalFlag(to);
        updateActorGoalFlag(from);

        return true;
    }

    public boolean isSolved() {
        int goalCount = 0;
        int boxCount = 0;

        for (Map.Entry<Pair, IObject> entry : terrain.entrySet()) {
            if (entry.getValue().type() == Type.GOALPOSITION) {
                goalCount++;
                IObject actor = actors.get(entry.getKey());
                if (actor == null || actor.type() != Type.BOX) {
                    return false;
                }
            }
        }

        for (Map.Entry<Pair, IObject> entry : actors.entrySet()) {
            IObject actor = entry.getValue();
            if (actor.type() != Type.BOX) continue;
            boxCount++;
            if (!isGoal(entry.getKey())) return false;
        }

        return goalCount > 0 && goalCount == boxCount;
    }

    private void moveActor(Pair from, Pair to) {
        IObject obj = actors.remove(from);
        if (obj != null) actors.put(to, obj);
    }

    private boolean isWall(Pair position) {
        IObject tile = terrain.get(position);
        return tile != null && tile.type() == Type.WALL;
    }

    private boolean isGoal(Pair position) {
        IObject tile = terrain.get(position);
        return tile != null && tile.type() == Type.GOALPOSITION;
    }

    private void updateActorGoalFlag(Pair position) {
        IObject actor = actors.get(position);
        if (actor == null) return;
        if (actor.type() != Type.PLAYER && actor.type() != Type.BOX) return;
        actor.setOnGoalPos(isGoal(position));
    }

    private void syncOnGoalFlags() {
        for (Pair pos : actors.keySet()) {
            updateActorGoalFlag(pos);
        }
    }

    private boolean isInside(Pair position) {
        if (position == null) return false;
        return position.x() >= 0 && position.x() < columns
            && position.y() >= 0 && position.y() < rows;
    }

    private void requireInside(Pair position) {
        if (!isInside(position)) {
            throw new IllegalArgumentException("Position is outside the board");
        }
    }

    private boolean isCardinalDirection(Pair direction) {
        if (direction == null) return false;
        int distance = Math.abs(direction.x()) + Math.abs(direction.y());
        return distance == 1;
    }

    private static int inferRows(HashMap<Pair, IObject> objects) {
        if (objects == null || objects.isEmpty()) return 0;
        int maxY = 0;
        for (Pair p : objects.keySet()) {
            maxY = Math.max(maxY, p.y());
        }
        return maxY + 1;
    }

    private static int inferColumns(HashMap<Pair, IObject> objects) {
        if (objects == null || objects.isEmpty()) return 0;
        int maxX = 0;
        for (Pair p : objects.keySet()) {
            maxX = Math.max(maxX, p.x());
        }
        return maxX + 1;
    }

    @Override
    public String toString() {
        if (rows == 0 || columns == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                Pair p = new Pair(x, y);
                IObject actor = actors.get(p);
                if (actor != null) {
                    sb.append(actor.toString());
                    continue;
                }

                IObject tile = terrain.get(p);
                if (tile != null) {
                    sb.append(tile.toString());
                } else {
                    sb.append(" ");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
