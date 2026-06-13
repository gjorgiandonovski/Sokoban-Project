package es.upm.pproject.sokoban.model.services.implementations;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.IObject;
import es.upm.pproject.sokoban.model.dto.Type;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;

class BoardServiceImpl implements BoardService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoardServiceImpl.class);
    private static final String OUTSIDE_BOARD_MESSAGE = "Position is outside the board";

    private final PairService pairService = ServiceFactory.createPairService();

    @Override
    public IObject get(Board board, int x, int y) {
        requireBoard(board);
        Pair position = new Pair(x, y);
        if (!isInside(board, position)) {
            return null;
        }
        return getObjectAt(board, position);
    }

    @Override
    public void addTerrain(Board board, Pair position, IObject obj) {
        requireBoard(board);
        if (obj == null) {
            return;
        }
        requireInside(board, position);
        if (obj.type() != Type.WALL && obj.type() != Type.GOALPOSITION) {
            throw new IllegalArgumentException("Terrain must be WALL or GOALPOSITION");
        }
        board.getTerrain().put(position, obj);
    }

    @Override
    public void addActor(Board board, Pair position, IObject obj) {
        requireBoard(board);
        if (obj == null) {
            return;
        }
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
        requireBoard(board);
        Pair playerPos = board.getPlayerPosition();
        if (playerPos == null) {
            LOGGER.debug("Cannot move player because the board has no player");
            return false;
        }
        if (!pairService.isCardinalDirection(direction)) {
            LOGGER.debug("Rejected non-cardinal direction {}", direction);
            return false;
        }

        Pair from = playerPos;
        Pair to = pairService.add(from, direction);
        if (!canEnter(board, to)) {
            return false;
        }

        IObject occupant = board.getActors().get(to);
        if (occupant == null) {
            return movePlayer(board, from, to);
        }

        if (occupant.type() != Type.BOX) {
            return false;
        }

        return pushBox(board, from, to, direction);
    }

    @Override
    public boolean undo(Board board) {
        requireBoard(board);
        if (board.getMoveHistory().isEmpty()) {
            return false;
        }
        Board.MoveRecord record = board.getMoveHistory().remove(board.getMoveHistory().size() - 1);

        moveActor(board, record.getPlayerTo(), record.getPlayerFrom());
        board.setPlayerPosition(record.getPlayerFrom());

        if (record.getBoxFrom() != null && record.getBoxTo() != null) {
            moveActor(board, record.getBoxTo(), record.getBoxFrom());
        }

        refreshMoveRecordFlags(board, record);

        LOGGER.debug("Undo restored player from {} to {}", record.getPlayerTo(), record.getPlayerFrom());
        return true;
    }

    @Override
    public boolean isSolved(Board board) {
        requireBoard(board);
        int goalCount = countGoals(board);
        if (goalCount == 0 || !allGoalsContainBoxes(board)) {
            return false;
        }

        int boxCount = countBoxesOnGoals(board);
        return goalCount == boxCount;
    }

    @Override
    public Pair findPlayer(Board board) {
        requireBoard(board);
        return board.getPlayerPosition();
    }

    @Override
    public String render(Board board) {
        requireBoard(board);
        int rows = board.getRows();
        int columns = board.getColumns();
        if (rows == 0 || columns == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(rows * (columns + 1));
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                appendRenderedCell(sb, board, new Pair(x, y));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private IObject getObjectAt(Board board, Pair position) {
        IObject actor = board.getActors().get(position);
        return actor != null ? actor : board.getTerrain().get(position);
    }

    private boolean movePlayer(Board board, Pair from, Pair to) {
        moveActor(board, from, to);
        board.setPlayerPosition(to);
        updateActorGoalFlag(board, to);
        updateActorGoalFlag(board, from);
        board.getMoveHistory().add(new Board.MoveRecord(from, to, null, null));
        LOGGER.debug("Player moved from {} to {}", from, to);
        return true;
    }

    private boolean pushBox(Board board, Pair playerFrom, Pair boxFrom, Pair direction) {
        Pair boxTo = pairService.add(boxFrom, direction);
        if (!canEnter(board, boxTo) || board.getActors().containsKey(boxTo)) {
            return false;
        }

        moveActor(board, boxFrom, boxTo);
        updateActorGoalFlag(board, boxTo);
        updateActorGoalFlag(board, boxFrom);

        moveActor(board, playerFrom, boxFrom);
        board.setPlayerPosition(boxFrom);
        updateActorGoalFlag(board, boxFrom);
        updateActorGoalFlag(board, playerFrom);

        board.getMoveHistory().add(new Board.MoveRecord(playerFrom, boxFrom, boxFrom, boxTo));
        LOGGER.debug("Player moved from {} to {} and pushed box to {}", playerFrom, boxFrom, boxTo);
        return true;
    }

    private void refreshMoveRecordFlags(Board board, Board.MoveRecord record) {
        updateActorGoalFlag(board, record.getPlayerFrom());
        updateActorGoalFlag(board, record.getPlayerTo());
        if (record.getBoxFrom() != null && record.getBoxTo() != null) {
            updateActorGoalFlag(board, record.getBoxFrom());
            updateActorGoalFlag(board, record.getBoxTo());
        }
    }

    private int countGoals(Board board) {
        int goalCount = 0;
        for (IObject terrain : board.getTerrain().values()) {
            if (terrain.type() == Type.GOALPOSITION) {
                goalCount++;
            }
        }
        return goalCount;
    }

    private boolean allGoalsContainBoxes(Board board) {
        for (Map.Entry<Pair, IObject> entry : board.getTerrain().entrySet()) {
            if (entry.getValue().type() != Type.GOALPOSITION) {
                continue;
            }

            IObject actor = board.getActors().get(entry.getKey());
            if (actor == null || actor.type() != Type.BOX) {
                return false;
            }
        }
        return true;
    }

    private int countBoxesOnGoals(Board board) {
        int boxCount = 0;
        for (Map.Entry<Pair, IObject> entry : board.getActors().entrySet()) {
            if (entry.getValue().type() != Type.BOX) {
                continue;
            }
            if (!isGoal(board, entry.getKey())) {
                return -1;
            }
            boxCount++;
        }
        return boxCount;
    }

    private void appendRenderedCell(StringBuilder builder, Board board, Pair position) {
        IObject cell = getObjectAt(board, position);
        builder.append(cell != null ? cell.toString() : " ");
    }

    private void moveActor(Board board, Pair from, Pair to) {
        IObject obj = board.getActors().remove(from);
        if (obj != null) {
            board.getActors().put(to, obj);
        }
    }

    private boolean canEnter(Board board, Pair position) {
        return isInside(board, position) && !isWall(board, position);
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
        if (actor == null) {
            return;
        }
        if (actor.type() != Type.PLAYER && actor.type() != Type.BOX) {
            return;
        }
        actor.setOnGoalPos(isGoal(board, position));
    }

    private boolean isInside(Board board, Pair position) {
        if (position == null) {
            return false;
        }
        return position.x() >= 0 && position.x() < board.getColumns()
                && position.y() >= 0 && position.y() < board.getRows();
    }

    private void requireInside(Board board, Pair position) {
        if (!isInside(board, position)) {
            throw new IllegalArgumentException(OUTSIDE_BOARD_MESSAGE);
        }
    }

    private void requireBoard(Board board) {
        Objects.requireNonNull(board, "board must not be null");
    }
}
