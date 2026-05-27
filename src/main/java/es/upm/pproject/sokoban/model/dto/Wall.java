package es.upm.pproject.sokoban.model.dto;

public class Wall implements IObject {
    @Override
    public Type type() {
        return Type.WALL;
    }

    @Override
    public String toString() {
        return "+";
    }

    @Override
    public boolean onGoalPos() {
        return false;
    }

    @Override
    public void setOnGoalPos(boolean onGoalPos) {
    }
}
