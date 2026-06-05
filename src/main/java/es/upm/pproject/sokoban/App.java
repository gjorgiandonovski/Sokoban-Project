package es.upm.pproject.sokoban;

import java.awt.GraphicsEnvironment;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.upm.pproject.sokoban.controller.GameController;
import es.upm.pproject.sokoban.view.GameView;
import es.upm.pproject.sokoban.view.SwingGameFrame;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        if (isConsoleMode(args) || GraphicsEnvironment.isHeadless()) {
            LOGGER.info("Starting Sokoban in console mode");
            runConsole();
            return;
        }

        LOGGER.info("Starting Sokoban in Swing mode");
        SwingUtilities.invokeLater(() -> {
            SwingGameFrame frame = new SwingGameFrame(new GameController());
            frame.setVisible(true);
        });
    }

    private static boolean isConsoleMode(String[] args) {
        return args != null && args.length > 0 && "--console".equalsIgnoreCase(args[0]);
    }

    private static void runConsole() {
        GameController controller = new GameController();
        GameView view = new GameView(controller, System.out);
        Scanner sc = new Scanner(System.in);

        LOGGER.info("Console game loop started at level {}", controller.getLevelNumber());
        view.hideCursor();
        try {
            while (true) {
                view.render();

                if (controller.isSolved()) {
                    LOGGER.info("Level {} solved in {} moves", controller.getLevelNumber(), controller.getLevelScore());
                    view.displayMessage("Congratulations! You completed level " + controller.getLevelNumber() + "!");
                    if (controller.nextLevel()) {
                        LOGGER.info("Advancing to level {}", controller.getLevelNumber());
                        view.displayMessage("Now starting level " + controller.getLevelNumber() + "!");
                        continue;
                    } else {
                        LOGGER.info("Game completed with total score {}", controller.getGlobalScore());
                        view.displayMessage("No more levels available! You win the game!");
                        view.displayMessage("Global score: " + controller.getGlobalScore());
                        break;
                    }
                }

                String input = sc.nextLine();
                controller.handleInput(input);
            }
        } finally {
            LOGGER.info("Console game loop finished");
            view.showCursor();
            sc.close();
        }
    }
}
