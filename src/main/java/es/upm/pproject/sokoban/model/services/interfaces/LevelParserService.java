package es.upm.pproject.sokoban.model.services.interfaces;

import java.io.InputStream;
import es.upm.pproject.sokoban.model.dto.Board;

public interface LevelParserService {
    Board parseResource(String fileName, BoardService boardService);
    Board parse(InputStream stream, String sourceName, BoardService boardService);
}
