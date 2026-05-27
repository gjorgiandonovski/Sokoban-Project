package es.upm.pproject.sokoban.model.dto;

public class GoalPosition implements IObject {
    private static final long serialVersionUID = 1L;

    @Override
    public Type type() {
        return Type.GOALPOSITION;
    }

    @Override
    public String toString() {
        return "*";
    }

    @Override
    public boolean onGoalPos() {
        return false;
    }

    @Override
    public void setOnGoalPos(boolean onGoalPos) {
    }
}
