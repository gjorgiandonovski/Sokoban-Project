package es.upm.pproject.sokoban.model.services.interfaces;

import es.upm.pproject.sokoban.model.dto.Pair;

public interface PairService {
    Pair add(Pair p1, Pair p2);
    boolean isCardinalDirection(Pair direction);
}
