package es.upm.pproject.sokoban.view;

import java.io.PrintStream;
import es.upm.pproject.sokoban.controller.GameController;

public class GameView {
    private static final String CLEAR_SCREEN = "\033[2J\033[H";
    private static final String HIDE_CURSOR = "\033[?25l";
    private static final String SHOW_CURSOR = "\033[?25h";

    private final GameController controller;
    private final PrintStream out;

    public GameView(GameController controller, PrintStream out) {
        this.controller = controller;
        this.out = out;
    }

    public void render() {
        out.print(CLEAR_SCREEN);
        String renderedBoard = controller.getBoardService().render(controller.getBoard());
        out.print(renderedBoard);
        out.println("Level: " + controller.getLevelNumber() + " - " + controller.getLevelName());
        out.println("Level score: " + controller.getLevelScore());
        out.println("Global score: " + controller.getGlobalScore());
    }

    public void displayMessage(String message) {
        out.println(message);
    }

    public void hideCursor() {
        out.print(HIDE_CURSOR);
    }

    public void showCursor() {
        out.print(SHOW_CURSOR);
    }
}
