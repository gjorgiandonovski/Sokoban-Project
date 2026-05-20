package es.upm.pproject.sokoban;

import java.util.HashMap;
import java.util.Map;

import es.upm.pproject.sokoban.GameObjects.Type;
import es.upm.pproject.sokoban.GameObjects.Interfaces.IObject;

public class Board {
    private final Map<Pair, IObject> terrain;
    private final Map<Pair, IObject> actors;
    private Pair playerPosition;

    public Board() {
        this.terrain = new HashMap<>();
        this.actors = new HashMap<>();
    }

    public Board(HashMap<Pair, IObject> objects) {
        this();
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
        IObject actor = actors.get(p);
        if (actor != null) return actor;
        return terrain.get(p);
    }

    public Pair findPlayer() {
        return playerPosition;
    }

    public void addTerrain(Pair position, IObject obj) {
        if (obj == null) return;
        if (obj.type() != Type.WALL && obj.type() != Type.GOALPOSITION) {
            throw new IllegalArgumentException("Terrain must be WALL or GOALPOSITION");
        }
        terrain.put(position, obj);
    }

    public void addActor(Pair position, IObject obj) {
        if (obj == null) return;
        if (obj.type() != Type.PLAYER && obj.type() != Type.BOX) {
            throw new IllegalArgumentException("Actor must be PLAYER or BOX");
        }
        if (obj.type() == Type.PLAYER) {
            playerPosition = position;
        }
        actors.put(position, obj);
    }

    public boolean tryMovePlayer(Pair direction) {
        if (playerPosition == null) return false;
        if (direction == null) return false;
        if (direction.x() == 0 && direction.y() == 0) return false;

        Pair from = playerPosition;
        Pair to = from.add(direction);

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
        boolean hasGoal = false;
        for (IObject tile : terrain.values()) {
            if (tile.type() == Type.GOALPOSITION) {
                hasGoal = true;
                break;
            }
        }
        if (!hasGoal) return false;

        for (Map.Entry<Pair, IObject> entry : actors.entrySet()) {
            IObject actor = entry.getValue();
            if (actor.type() != Type.BOX) continue;
            if (!isGoal(entry.getKey())) return false;
        }

        return true;
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

    @Override
    public String toString() {
        if (terrain.isEmpty() && actors.isEmpty()) return "";

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Pair p : terrain.keySet()) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        for (Pair p : actors.keySet()) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }

        StringBuilder sb = new StringBuilder();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
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
