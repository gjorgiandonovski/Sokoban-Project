package es.upm.pproject.sokoban.model.services.implementations;

import java.util.Map;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.IObject;
import es.upm.pproject.sokoban.model.dto.Type;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;

class BoardServiceImpl implements BoardService {

    private final PairService pairService = ServiceFactory.createPairService();

    @Override
    public IObject get(Board board, int x, int y) {
        Pair p = new Pair(x, y);
        if (!isInside(board, p))
            return null;
        IObject actor = board.getActors().get(p);
        if (actor != null)
            return actor;
        return board.getTerrain().get(p);
    }

    @Override
    public void addTerrain(Board board, Pair position, IObject obj) {
        if (obj == null)
            return;
        requireInside(board, position);
        if (obj.type() != Type.WALL && obj.type() != Type.GOALPOSITION) {
            throw new IllegalArgumentException("Terrain must be WALL or GOALPOSITION");
        }
        board.getTerrain().put(position, obj);
    }

    @Override
    public void addActor(Board board, Pair position, IObject obj) {
        if (obj == null)
            return;
        requireInside(board, position);
        if (obj.type() != Type.PLAYER && obj.type() != Type.BOX) {
            throw new IllegalArgumentException("Actor must be PLAYER or BOX");
        }
        if (isWall(board, position)) {
            throw new IllegalArgumentException("Actors cannot be placed on walls");
        }
        if (board.getActors().containsKey(position)) {
            throw new IllegalArgumentException("Only one actor can occupy a square");
        }
        if (obj.type() == Type.PLAYER) {
            if (board.getPlayerPosition() != null) {
                throw new IllegalArgumentException("The board can only have one player");
            }
            board.setPlayerPosition(position);
        }
        board.getActors().put(position, obj);
        updateActorGoalFlag(board, position);
    }

    @Override
    public boolean tryMovePlayer(Board board, Pair direction) {
        Pair playerPos = board.getPlayerPosition();
        if (playerPos == null)
            return false;
        if (!pairService.isCardinalDirection(direction))
            return false;

        Pair from = playerPos;
        Pair to = pairService.add(from, direction);

        if (!isInside(board, to))
            return false;
        if (isWall(board, to))
            return false;

        IObject occupant = board.getActors().get(to);
        if (occupant == null) {
            moveActor(board, from, to);
            board.setPlayerPosition(to);
            updateActorGoalFlag(board, to);
            updateActorGoalFlag(board, from);
            board.getMoveHistory().add(new Board.MoveRecord(from, to, null, null));
            return true;
        }

        if (occupant.type() != Type.BOX)
            return false;

        Pair boxTo = pairService.add(to, direction);
        if (!isInside(board, boxTo))
            return false;
        if (isWall(board, boxTo))
            return false;
        if (board.getActors().containsKey(boxTo))
            return false;

        moveActor(board, to, boxTo);
        updateActorGoalFlag(board, boxTo);
        updateActorGoalFlag(board, to);

        moveActor(board, from, to);
        board.setPlayerPosition(to);
        updateActorGoalFlag(board, to);
        updateActorGoalFlag(board, from);

        board.getMoveHistory().add(new Board.MoveRecord(from, to, to, boxTo));
        return true;
    }

    @Override
    public boolean undo(Board board) {
        if (board.getMoveHistory().isEmpty())
            return false;
        Board.MoveRecord record = board.getMoveHistory().remove(board.getMoveHistory().size() - 1);

        if (record.getBoxFrom() != null && record.getBoxTo() != null) {
            moveActor(board, record.getBoxTo(), record.getBoxFrom());
            updateActorGoalFlag(board, record.getBoxFrom());
            updateActorGoalFlag(board, record.getBoxTo());
        }

        moveActor(board, record.getPlayerTo(), record.getPlayerFrom());
        board.setPlayerPosition(record.getPlayerFrom());
        updateActorGoalFlag(board, record.getPlayerFrom());
        updateActorGoalFlag(board, record.getPlayerTo());

        return true;
    }

    @Override
    public boolean isSolved(Board board) {
        int goalCount = 0;
        int boxCount = 0;

        for (Map.Entry<Pair, IObject> entry : board.getTerrain().entrySet()) {
            if (entry.getValue().type() == Type.GOALPOSITION) {
                goalCount++;
                IObject actor = board.getActors().get(entry.getKey());
                if (actor == null || actor.type() != Type.BOX) {
                    return false;
                }
            }
        }

        for (Map.Entry<Pair, IObject> entry : board.getActors().entrySet()) {
            IObject actor = entry.getValue();
            if (actor.type() != Type.BOX)
                continue;
            boxCount++;
            if (!isGoal(board, entry.getKey()))
                return false;
        }

        return goalCount > 0 && goalCount == boxCount;
    }

    @Override
    public Pair findPlayer(Board board) {
        return board.getPlayerPosition();
    }

    @Override
    public String render(Board board) {
        int rows = board.getRows();
        int columns = board.getColumns();
        if (rows == 0 || columns == 0)
            return "";

        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                Pair p = new Pair(x, y);
                IObject actor = board.getActors().get(p);
                if (actor != null) {
                    sb.append(actor.toString());
                    continue;
                }

                IObject tile = board.getTerrain().get(p);
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

    private void moveActor(Board board, Pair from, Pair to) {
        IObject obj = board.getActors().remove(from);
        if (obj != null)
            board.getActors().put(to, obj);
    }

    private boolean isWall(Board board, Pair position) {
        IObject tile = board.getTerrain().get(position);
        return tile != null && tile.type() == Type.WALL;
    }

    private boolean isGoal(Board board, Pair position) {
        IObject tile = board.getTerrain().get(position);
        return tile != null && tile.type() == Type.GOALPOSITION;
    }

    private void updateActorGoalFlag(Board board, Pair position) {
        IObject actor = board.getActors().get(position);
        if (actor == null)
            return;
        if (actor.type() != Type.PLAYER && actor.type() != Type.BOX)
            return;
        actor.setOnGoalPos(isGoal(board, position));
    }

    private boolean isInside(Board board, Pair position) {
        if (position == null)
            return false;
        return position.x() >= 0 && position.x() < board.getColumns()
                && position.y() >= 0 && position.y() < board.getRows();
    }

    private void requireInside(Board board, Pair position) {
        if (!isInside(board, position)) {
            throw new IllegalArgumentException("Position is outside the board");
        }
    }
}
