package es.upm.pproject.sokoban.model.dto;

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
        return "#";
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
