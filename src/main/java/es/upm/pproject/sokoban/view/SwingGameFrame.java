package es.upm.pproject.sokoban.view;

import static es.upm.pproject.sokoban.model.dto.Type.BOX;
import static es.upm.pproject.sokoban.model.dto.Type.GOALPOSITION;
import static es.upm.pproject.sokoban.model.dto.Type.PLAYER;
import static es.upm.pproject.sokoban.model.dto.Type.WALL;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.upm.pproject.sokoban.controller.GameController;
import es.upm.pproject.sokoban.model.dto.IObject;
import es.upm.pproject.sokoban.model.dto.Pair;

public class SwingGameFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(SwingGameFrame.class);

    private static final Pair UP = new Pair(0, -1);
    private static final Pair DOWN = new Pair(0, 1);
    private static final Pair LEFT = new Pair(-1, 0);
    private static final Pair RIGHT = new Pair(1, 0);
    private static final Dimension BUTTON_SIZE = new Dimension(92, 34);
    private static final String SAVE_EXTENSION = ".sok";

    private final GameController controller;
    private final BoardPanel boardPanel;
    private final JLabel levelLabel;
    private final JLabel levelScoreLabel;
    private final JLabel globalScoreLabel;
    private final JLabel statusLabel;
    private boolean gameCompleted;
    private boolean advancingLevel;
    private Timer levelAdvanceTimer;

    public SwingGameFrame(GameController controller) {
        super("Sokoban");
        this.controller = controller;
        this.boardPanel = new BoardPanel(controller);
        this.levelLabel = createInfoLabel();
        this.levelScoreLabel = createInfoLabel();
        this.globalScoreLabel = createInfoLabel();
        this.statusLabel = createStatusLabel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(640, 560));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);

        installKeyBindings();
        refresh("Ready");
        pack();
        setLocationRelativeTo(null);
        LOGGER.info("Swing game frame initialized");
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        gameMenu.add(createMenuItem("New Game", this::newGame));
        gameMenu.add(createMenuItem("Restart Current Level", this::restartLevel));
        gameMenu.add(createMenuItem("Undo Last Movement", this::undoMove));
        gameMenu.addSeparator();
        gameMenu.add(createMenuItem("Save Game", this::saveGame));
        gameMenu.add(createMenuItem("Open Saved Game", this::openSavedGame));
        gameMenu.addSeparator();
        gameMenu.add(createMenuItem("Close Application", this::closeApplication));
        menuBar.add(gameMenu);
        return menuBar;
    }

    private JMenuItem createMenuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(event -> action.run());
        return item;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new GridLayout(1, 3, 12, 0));
        header.setBorder(new EmptyBorder(14, 16, 10, 16));
        header.setBackground(new Color(245, 247, 250));
        header.add(levelLabel);
        header.add(levelScoreLabel);
        header.add(globalScoreLabel);
        return header;
    }

    private JPanel createControlPanel() {
        JPanel controls = new JPanel(new BorderLayout(12, 8));
        controls.setBorder(new EmptyBorder(10, 16, 14, 16));
        controls.setBackground(new Color(245, 247, 250));

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(createButton("Undo", this::undoMove));
        actions.add(createButton("Restart", this::restartLevel));

        controls.add(actions, BorderLayout.CENTER);
        controls.add(statusLabel, BorderLayout.SOUTH);
        return controls;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setPreferredSize(BUTTON_SIZE);
        button.setFocusPainted(false);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JLabel createInfoLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(213, 219, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        return label;
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        label.setBorder(new EmptyBorder(8, 0, 0, 0));
        return label;
    }

    private void installKeyBindings() {
        JComponent root = getRootPane();
        bind(root, KeyStroke.getKeyStroke("UP"), "moveUp", () -> movePlayer(UP));
        bind(root, KeyStroke.getKeyStroke("DOWN"), "moveDown", () -> movePlayer(DOWN));
        bind(root, KeyStroke.getKeyStroke("LEFT"), "moveLeft", () -> movePlayer(LEFT));
        bind(root, KeyStroke.getKeyStroke("RIGHT"), "moveRight", () -> movePlayer(RIGHT));
        bind(root, KeyStroke.getKeyStroke('w'), "moveW", () -> movePlayer(UP));
        bind(root, KeyStroke.getKeyStroke('s'), "moveS", () -> movePlayer(DOWN));
        bind(root, KeyStroke.getKeyStroke('a'), "moveA", () -> movePlayer(LEFT));
        bind(root, KeyStroke.getKeyStroke('d'), "moveD", () -> movePlayer(RIGHT));
        bind(root, KeyStroke.getKeyStroke('u'), "undo", this::undoMove);
        bind(root, KeyStroke.getKeyStroke('r'), "restart", this::restartLevel);
    }

    private void bind(JComponent component, KeyStroke keyStroke, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        component.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void movePlayer(Pair direction) {
        if (gameCompleted || advancingLevel || controller.isSolved()) {
            return;
        }

        int previousScore = controller.getLevelScore();
        controller.movePlayer(direction);
        if (controller.isSolved()) {
            refresh("Level completed. Loading next level...");
            scheduleNextLevel();
        } else if (controller.getLevelScore() > previousScore) {
            refresh("Moved");
        } else {
            refresh("Blocked");
        }
    }

    private void undoMove() {
        if (gameCompleted || advancingLevel) {
            return;
        }

        int previousScore = controller.getLevelScore();
        controller.undoMove();
        if (controller.getLevelScore() < previousScore) {
            refresh("Move undone");
        } else {
            refresh("Nothing to undo");
        }
    }

    private void newGame() {
        cancelPendingLevelAdvance();
        controller.startNewGame();
        gameCompleted = false;
        advancingLevel = false;
        LOGGER.info("New game requested from UI");
        refresh("New game started");
    }

    private void restartLevel() {
        cancelPendingLevelAdvance();
        controller.restartLevel();
        gameCompleted = false;
        advancingLevel = false;
        LOGGER.info("Restart requested from UI for level {}", controller.getLevelNumber());
        refresh("Level restarted");
    }

    private void saveGame() {
        JFileChooser fileChooser = createSaveFileChooser();
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = ensureSaveExtension(fileChooser.getSelectedFile());
        if (selectedFile == null) {
            showError("Could not save the game", new IllegalArgumentException("No save file was selected"));
            return;
        }
        try {
            controller.saveGame(selectedFile.toPath());
            LOGGER.info("Save requested from UI: {}", selectedFile);
            refresh("Game saved");
        } catch (IOException | RuntimeException exception) {
            showError("Could not save the game", exception);
        }
    }

    private void openSavedGame() {
        JFileChooser fileChooser = createSaveFileChooser();
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        cancelPendingLevelAdvance();
        try {
            controller.loadGame(fileChooser.getSelectedFile().toPath());
            gameCompleted = false;
            advancingLevel = false;
            LOGGER.info("Load requested from UI");
            refresh("Game loaded");
            if (controller.isSolved()) {
                scheduleNextLevel();
            }
        } catch (IOException | RuntimeException exception) {
            showError("Could not open the saved game", exception);
        }
    }

    private JFileChooser createSaveFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Sokoban saved games (*.sok)", "sok"));
        return fileChooser;
    }

    private File ensureSaveExtension(File file) {
        if (file == null || file.getName().toLowerCase().endsWith(SAVE_EXTENSION)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + SAVE_EXTENSION);
    }

    private void showError(String message, Exception exception) {
        LOGGER.error(message, exception);
        JOptionPane.showMessageDialog(
            this,
            message + ":\n" + exception.getMessage(),
            "Sokoban",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void closeApplication() {
        LOGGER.info("Closing application");
        cancelPendingLevelAdvance();
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void scheduleNextLevel() {
        cancelPendingLevelAdvance();
        advancingLevel = true;
        LOGGER.info("Scheduling next level transition");
        levelAdvanceTimer = new Timer(700, event -> advanceLevel());
        levelAdvanceTimer.setRepeats(false);
        levelAdvanceTimer.start();
    }

    private void advanceLevel() {
        advancingLevel = false;
        levelAdvanceTimer = null;
        if (controller.nextLevel()) {
            LOGGER.info("Next level shown in UI: {}", controller.getLevelNumber());
            refresh("Next level loaded");
        } else {
            gameCompleted = true;
            LOGGER.info("Game completed in UI with total score {}", controller.getGlobalScore());
            refresh("You won! Total score: " + controller.getGlobalScore());
            showGameCompletedDialog();
        }
    }

    private void showGameCompletedDialog() {
        int option = JOptionPane.showOptionDialog(
            this,
            "You won!\nTotal score: " + controller.getGlobalScore(),
            "Game completed",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[] { "Play Again", "Exit" },
            "Play Again"
        );

        if (option == JOptionPane.YES_OPTION) {
            newGame();
        } else {
            closeApplication();
        }
    }

    private void cancelPendingLevelAdvance() {
        if (levelAdvanceTimer != null) {
            levelAdvanceTimer.stop();
            levelAdvanceTimer = null;
        }
    }

    private void refresh(String status) {
        levelLabel.setText("Level " + controller.getLevelNumber() + ": " + controller.getLevelName());
        levelScoreLabel.setText("Level score: " + controller.getLevelScore());
        globalScoreLabel.setText("Game score: " + controller.getGlobalScore());
        statusLabel.setText(status);
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private static final class BoardPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private static final int TILE_SIZE = 52;
        private static final int MIN_TILE_SIZE = 24;
        private static final Color BACKGROUND = new Color(37, 42, 49);
        private static final Color FLOOR = new Color(234, 229, 217);
        private static final Color FLOOR_ALT = new Color(225, 220, 209);
        private static final Color GRID = new Color(204, 199, 190);
        private static final Color WALL_TILE = new Color(80, 88, 100);
        private static final Color WALL_DARK = new Color(55, 62, 72);
        private static final Color GOAL_MARK = new Color(54, 146, 109);
        private static final Color BOX_TILE = new Color(196, 128, 48);
        private static final Color BOX_ON_GOAL = new Color(53, 132, 94);
        private static final Color BOX_BORDER = new Color(116, 72, 29);
        private static final Color PLAYER_SKIN = new Color(236, 190, 143);
        private static final Color PLAYER_SHIRT = new Color(43, 114, 190);
        private static final Color PLAYER_PANTS = new Color(47, 64, 88);
        private static final Color PLAYER_OUTLINE = new Color(25, 72, 125);

        private final GameController controller;

        private BoardPanel(GameController controller) {
            this.controller = controller;
            setBackground(BACKGROUND);
            setBorder(new EmptyBorder(16, 16, 16, 16));
        }

        @Override
        public Dimension getPreferredSize() {
            int width = controller.getBoardColumns() * TILE_SIZE + 32;
            int height = controller.getBoardRows() * TILE_SIZE + 32;
            return new Dimension(Math.max(420, width), Math.max(360, height));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawBoard(g2);
            } finally {
                g2.dispose();
            }
        }

        private void drawBoard(Graphics2D g2) {
            int rows = controller.getBoardRows();
            int columns = controller.getBoardColumns();
            if (rows == 0 || columns == 0) {
                return;
            }

            int tile = Math.max(MIN_TILE_SIZE, Math.min(
                (getWidth() - 32) / columns,
                (getHeight() - 32) / rows
            ));
            tile = Math.min(TILE_SIZE, tile);

            int boardWidth = tile * columns;
            int boardHeight = tile * rows;
            int originX = (getWidth() - boardWidth) / 2;
            int originY = (getHeight() - boardHeight) / 2;

            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    int x = originX + column * tile;
                    int y = originY + row * tile;
                    drawTile(g2, x, y, tile, row, column);
                }
            }
        }

        private void drawTile(Graphics2D g2, int x, int y, int tile, int row, int column) {
            IObject terrain = controller.getTerrainAt(column, row);
            IObject actor = controller.getActorAt(column, row);

            drawFloor(g2, x, y, tile, row, column);
            if (terrain != null && terrain.type() == WALL) {
                drawWall(g2, x, y, tile);
            } else if (terrain != null && terrain.type() == GOALPOSITION) {
                drawGoal(g2, x, y, tile);
            }

            if (actor != null && actor.type() == BOX) {
                drawBox(g2, x, y, tile, actor.onGoalPos());
            } else if (actor != null && actor.type() == PLAYER) {
                drawPlayer(g2, x, y, tile);
            }
        }

        private void drawFloor(Graphics2D g2, int x, int y, int tile, int row, int column) {
            g2.setColor((row + column) % 2 == 0 ? FLOOR : FLOOR_ALT);
            g2.fillRect(x, y, tile, tile);
            g2.setColor(GRID);
            g2.drawRect(x, y, tile, tile);
        }

        private void drawWall(Graphics2D g2, int x, int y, int tile) {
            int inset = Math.max(2, tile / 18);
            g2.setColor(WALL_TILE);
            g2.fillRect(x + inset, y + inset, tile - inset * 2, tile - inset * 2);
            g2.setColor(WALL_DARK);
            g2.drawRect(x + inset, y + inset, tile - inset * 2, tile - inset * 2);
            g2.drawLine(x + inset, y + tile - inset * 2, x + tile - inset * 2, y + inset);
        }

        private void drawGoal(Graphics2D g2, int x, int y, int tile) {
            int size = Math.max(10, tile / 3);
            int centerX = x + tile / 2;
            int centerY = y + tile / 2;
            int[] pointsX = { centerX, centerX + size / 2, centerX, centerX - size / 2 };
            int[] pointsY = { centerY - size / 2, centerY, centerY + size / 2, centerY };
            g2.setColor(GOAL_MARK);
            g2.fillPolygon(pointsX, pointsY, pointsX.length);
        }

        private void drawBox(Graphics2D g2, int x, int y, int tile, boolean onGoal) {
            int pad = Math.max(5, tile / 8);
            int size = tile - pad * 2;
            int arc = Math.max(4, tile / 8);
            g2.setColor(onGoal ? BOX_ON_GOAL : BOX_TILE);
            g2.fillRoundRect(x + pad, y + pad, size, size, arc, arc);
            g2.setColor(BOX_BORDER);
            g2.drawRoundRect(x + pad, y + pad, size, size, arc, arc);
            g2.drawLine(x + pad + 5, y + pad + 5, x + pad + size - 5, y + pad + size - 5);
            g2.drawLine(x + pad + size - 5, y + pad + 5, x + pad + 5, y + pad + size - 5);
        }

        private void drawPlayer(Graphics2D g2, int x, int y, int tile) {
            int centerX = x + tile / 2;
            int head = Math.max(8, tile / 5);
            int headX = centerX - head / 2;
            int headY = y + Math.max(5, tile / 8);
            g2.setColor(PLAYER_SKIN);
            g2.fillOval(headX, headY, head, head);
            g2.setColor(PLAYER_OUTLINE);
            g2.drawOval(headX, headY, head, head);

            int bodyWidth = Math.max(12, tile / 4);
            int bodyHeight = Math.max(14, tile / 3);
            int bodyX = centerX - bodyWidth / 2;
            int bodyY = headY + head - 1;
            int arc = Math.max(4, tile / 10);
            g2.setColor(PLAYER_SHIRT);
            g2.fillRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, arc, arc);
            g2.setColor(PLAYER_OUTLINE);
            g2.drawRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, arc, arc);

            int armWidth = Math.max(4, tile / 12);
            int armHeight = Math.max(11, tile / 4);
            int armY = bodyY + Math.max(2, tile / 18);
            g2.setColor(PLAYER_SHIRT);
            g2.fillRoundRect(bodyX - armWidth + 1, armY, armWidth, armHeight, armWidth, armWidth);
            g2.fillRoundRect(bodyX + bodyWidth - 1, armY, armWidth, armHeight, armWidth, armWidth);

            int legWidth = Math.max(4, tile / 12);
            int legHeight = Math.max(9, tile / 5);
            int legY = bodyY + bodyHeight - 1;
            g2.setColor(PLAYER_PANTS);
            g2.fillRoundRect(centerX - legWidth - 1, legY, legWidth, legHeight, legWidth, legWidth);
            g2.fillRoundRect(centerX + 1, legY, legWidth, legHeight, legWidth, legWidth);
        }
    }
}
