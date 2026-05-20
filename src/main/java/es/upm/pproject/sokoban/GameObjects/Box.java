package es.upm.pproject.sokoban.GameObjects;

import es.upm.pproject.sokoban.GameObjects.Interfaces.IObject;

public class Box implements IObject {
    private boolean onGoalPos;

    public Box() {
        onGoalPos = false;
    }

    @Override
    public Type type() {
        return Type.BOX;
    }

    @Override
    public String toString() {
        return onGoalPos ? "*" : "$";
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