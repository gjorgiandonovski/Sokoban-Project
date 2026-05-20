package es.upm.pproject.sokoban.GameObjects.Interfaces;
import es.upm.pproject.sokoban.GameObjects.Type;

public interface IObject {
    Type type();

    @Override
    String toString();

    boolean onGoalPos();

    void setOnGoalPos(boolean onGoalPos);
}