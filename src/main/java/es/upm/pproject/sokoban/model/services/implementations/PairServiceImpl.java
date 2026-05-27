package es.upm.pproject.sokoban.model.services.implementations;

import es.upm.pproject.sokoban.model.dto.Pair;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;

class PairServiceImpl implements PairService {
    @Override
    public Pair add(Pair p1, Pair p2) {
        if (p1 == null || p2 == null) {
            return null;
        }
        return new Pair(p1.x() + p2.x(), p1.y() + p2.y());
    }

    @Override
    public boolean isCardinalDirection(Pair direction) {
        if (direction == null)
            return false;
        int distance = Math.abs(direction.x()) + Math.abs(direction.y());
        return distance == 1;
    }
}
