package es.upm.pproject.sokoban;

import java.awt.GraphicsEnvironment;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import es.upm.pproject.sokoban.controller.GameController;
import es.upm.pproject.sokoban.view.GameView;
import es.upm.pproject.sokoban.view.SwingGameFrame;

public class App {
    public static void main(String[] args) {
        if (isConsoleMode(args) || GraphicsEnvironment.isHeadless()) {
            runConsole();
            return;
        }

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

        view.hideCursor();
        try {
            while (true) {
                view.render();

                if (controller.isSolved()) {
                    view.displayMessage("Congratulations! You completed level " + controller.getLevelNumber() + "!");
                    if (controller.nextLevel()) {
                        view.displayMessage("Now starting level " + controller.getLevelNumber() + "!");
                        continue;
                    } else {
                        view.displayMessage("No more levels available! You win the game!");
                        view.displayMessage("Global score: " + controller.getGlobalScore());
                        break;
                    }
                }

                String input = sc.nextLine();
                controller.handleInput(input);
            }
        } finally {
            view.showCursor();
            sc.close();
        }
    }
}
