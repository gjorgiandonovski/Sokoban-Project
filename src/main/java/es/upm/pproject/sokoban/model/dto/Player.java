package es.upm.pproject.sokoban.model.dto;

public class Player implements IObject {
    private static final long serialVersionUID = 1L;

    private boolean onGoalPos;

    public Player() {
        onGoalPos = false;
    }

    @Override
    public Type type() {
        return Type.PLAYER;
    }

    @Override
    public String toString() {
        return "W";
    }

    @Override
    public boolean onGoalPos() {
        return onGoalPos;
    }

    @Override
    public void setOnGoalPos(boolean onGoalPos) {
        this.onGoalPos = onGoalPos;
    }
}
