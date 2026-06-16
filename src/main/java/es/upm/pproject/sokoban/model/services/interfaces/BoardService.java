package es.upm.pproject.sokoban.model.services.interfaces;

import es.upm.pproject.sokoban.model.dto.Board;
import es.upm.pproject.sokoban.model.dto.IObject;
import es.upm.pproject.sokoban.model.dto.Pair;

public interface BoardService {
    IObject get(Board board, int x, int y);
    void addTerrain(Board board, Pair position, IObject obj);
    void addActor(Board board, Pair position, IObject obj);
    boolean tryMovePlayer(Board board, Pair direction);
    boolean undo(Board board);
    boolean isSolved(Board board);
    Pair findPlayer(Board board);
    String render(Board board);
}
