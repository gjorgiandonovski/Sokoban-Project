package es.upm.pproject.sokoban.model.dto;

public interface IObject {
    Type type();

    @Override
    String toString();

    boolean onGoalPos();

    void setOnGoalPos(boolean onGoalPos);
}
