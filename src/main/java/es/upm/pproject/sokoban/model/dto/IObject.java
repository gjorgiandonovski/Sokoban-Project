package es.upm.pproject.sokoban.model.dto;

import java.io.Serializable;

public interface IObject extends Serializable {
    Type type();

    @Override
    String toString();

    boolean onGoalPos();

    void setOnGoalPos(boolean onGoalPos);
}
