package es.upm.pproject.sokoban.model.services.implementations;
import es.upm.pproject.sokoban.model.services.interfaces.BoardService;
import es.upm.pproject.sokoban.model.services.interfaces.PairService;
import es.upm.pproject.sokoban.model.services.interfaces.LevelParserService;

public class ServiceFactory {
    public static BoardService createBoardService() {
        return new BoardServiceImpl();
    }

    public static PairService createPairService() {
        return new PairServiceImpl();
    }

    public static LevelParserService createLevelParserService() {
        return new LevelParserServiceImpl();
    }
}
