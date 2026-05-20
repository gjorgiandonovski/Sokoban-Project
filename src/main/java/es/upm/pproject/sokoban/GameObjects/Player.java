package es.upm.pproject.sokoban.GameObjects;

import es.upm.pproject.sokoban.GameObjects.Interfaces.IObject;

public class Player implements IObject {
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
        return onGoalPos ? "+" : "@";
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
