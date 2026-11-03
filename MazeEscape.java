import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MAZE ESCAPE: THE ADVENTURE GAME
 * Fantasy-themed Java Swing maze game.
 *
 * Data Structures:
 * 1. 2D array - stores the maze.
 * 2. Custom linked Stack - stores previous player positions for Undo.
 * 3. Custom linked Queue - used by Breadth-First Search (BFS) for hints.
 *
 * Controls:
 * Arrow Keys = Move
 * U = Undo
 * H = Show shortest-path hint
 * R = Restart
 */
public class MazeEscape extends JFrame {

    private static final int WALL = 0;
    private static final int PATH = 1;
    private static final int EXIT = 2;

    private static final int TILE_SIZE = 35;

    private final int[][] maze = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0},
            {0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0},
            {0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0},
            {0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0},
            {0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    };

    private final int startRow = 1;
    private final int startCol = 1;
    private final int exitRow = 13;
    private final int exitCol = 13;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final MenuScreen menuScreen;
    private final InstructionsScreen instructionsScreen;
    private final GameScreen gameScreen;

    private int playerRow = startRow;
    private int playerCol = startCol;
    private int moves = 0;
    private boolean gameWon = false;
    private List<Point> hintPath = new ArrayList<Point>();

    private final PositionStack undoStack = new PositionStack();

    public MazeEscape() {
        setTitle("Maze Escape: The Adventure Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 760);
        setMinimumSize(new Dimension(820, 700));
        setLocationRelativeTo(null);
        setResizable(true);

        menuScreen = new MenuScreen();
        instructionsScreen = new InstructionsScreen();
        gameScreen = new GameScreen();

        mainPanel.add(menuScreen, "MENU");
        mainPanel.add(instructionsScreen, "INSTRUCTIONS");
        mainPanel.add(gameScreen, "GAME");

        setContentPane(mainPanel);
        showMenu();
    }

    private void showMenu() {
        cardLayout.show(mainPanel, "MENU");
    }

    private void showInstructions() {
        cardLayout.show(mainPanel, "INSTRUCTIONS");
    }

    private void startGame() {
        resetGame();
        cardLayout.show(mainPanel, "GAME");
        requestBoardFocus();
    }

    private void requestBoardFocus() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                gameScreen.board.requestFocusInWindow();
            }
        });
    }

    private void resetGame() {
        playerRow = startRow;
        playerCol = startCol;
        moves = 0;
        gameWon = false;
        hintPath.clear();
        undoStack.clear();
        gameScreen.refresh();
    }

    private void movePlayer(int rowChange, int colChange) {
        if (gameWon) {
            return;
        }

        int nextRow = playerRow + rowChange;
        int nextCol = playerCol + colChange;

        if (!isInsideMaze(nextRow, nextCol)) {
            return;
        }

        if (maze[nextRow][nextCol] == WALL) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        undoStack.push(playerRow, playerCol);
        playerRow = nextRow;
        playerCol = nextCol;
        moves++;
        hintPath.clear();

        gameScreen.refresh();

        if (playerRow == exitRow && playerCol == exitCol) {
            gameWon = true;
            gameScreen.refresh();
            showVictoryDialog();
        }
    }

    private boolean isInsideMaze(int row, int col) {
        return row >= 0 && row < maze.length
                && col >= 0 && col < maze[0].length;
    }

    private void undoMove() {
        if (gameWon) {
            return;
        }

        Position previous = undoStack.pop();
        if (previous == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        playerRow = previous.row;
        playerCol = previous.col;
        if (moves > 0) {
            moves--;
        }
        hintPath.clear();
        gameScreen.refresh();
        requestBoardFocus();
    }

    private void showHint() {
        if (gameWon) {
            return;
        }

        hintPath = findShortestPath();
        gameScreen.refresh();

        if (hintPath.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No path to the enchanted exit was found.",
                    "Ancient Map",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        requestBoardFocus();
    }

    /**
     * Breadth-First Search using the custom linked Queue.
     * The parent arrays are used to rebuild the shortest path.
     */
    private List<Point> findShortestPath() {
        List<Point> result = new ArrayList<Point>();

        int rows = maze.length;
        int cols = maze[0].length;

        boolean[][] visited = new boolean[rows][cols];
        int[][] parentRow = new int[rows][cols];
        int[][] parentCol = new int[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                parentRow[r][c] = -1;
                parentCol[r][c] = -1;
            }
        }

        PositionQueue queue = new PositionQueue();
        queue.enqueue(playerRow, playerCol);
        visited[playerRow][playerCol] = true;

        int[] rowMoves = {-1, 1, 0, 0};
        int[] colMoves = {0, 0, -1, 1};

        boolean found = false;

        while (!queue.isEmpty()) {
            Position current = queue.dequeue();

            if (current.row == exitRow && current.col == exitCol) {
                found = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nextRow = current.row + rowMoves[i];
                int nextCol = current.col + colMoves[i];

                if (isInsideMaze(nextRow, nextCol)
                        && maze[nextRow][nextCol] != WALL
                        && !visited[nextRow][nextCol]) {

                    visited[nextRow][nextCol] = true;
                    parentRow[nextRow][nextCol] = current.row;
                    parentCol[nextRow][nextCol] = current.col;
                    queue.enqueue(nextRow, nextCol);
                }
            }
        }

        if (!found) {
            return result;
        }

        int row = exitRow;
        int col = exitCol;

        while (!(row == playerRow && col == playerCol)) {
            result.add(new Point(col, row));

            int previousRow = parentRow[row][col];
            int previousCol = parentCol[row][col];

            if (previousRow == -1 || previousCol == -1) {
                return new ArrayList<Point>();
            }

            row = previousRow;
            col = previousCol;
        }

        Collections.reverse(result);
        return result;
    }

    private void showVictoryDialog() {
        String message = "QUEST COMPLETED!\n\n"
                + "The enchanted gate has opened.\n"
                + "Difficulty: HARD MODE\n"
                + "Total moves: " + moves + "\n\n"
                + "Well done, adventurer!";

        Object[] options = {"Play Again", "Main Menu"};

        int choice = JOptionPane.showOptionDialog(
                this,
                message,
                "Quest Completed!",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            startGame();
        } else if (choice == 1) {
            showMenu();
        }
    }

    // ------------------------------------------------------------
    // MENU SCREEN
    // ------------------------------------------------------------

    private class MenuScreen extends JPanel {

        MenuScreen() {
            setLayout(new GridBagLayout());
            setBackground(new Color(15, 24, 37));

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

            JLabel emblem = new JLabel("✦  THE ENCHANTED REALM  ✦");
            emblem.setAlignmentX(Component.CENTER_ALIGNMENT);
            emblem.setFont(new Font("Serif", Font.BOLD, 18));
            emblem.setForeground(new Color(224, 195, 119));

            JLabel title = new JLabel("MAZE ESCAPE");
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            title.setFont(new Font("Serif", Font.BOLD, 54));
            title.setForeground(new Color(245, 224, 169));

            JLabel subtitle = new JLabel("THE ADVENTURE GAME");
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            subtitle.setFont(new Font("Serif", Font.BOLD, 19));
            subtitle.setForeground(new Color(155, 207, 183));

            JLabel story = new JLabel(
                    "<html><div style='text-align:center;'>"
                            + "A deeper dungeon. More dead ends. One way out.<br>"
                            + "Find the enchanted gate and complete your quest."
                            + "</div></html>"
            );
            story.setAlignmentX(Component.CENTER_ALIGNMENT);
            story.setHorizontalAlignment(SwingConstants.CENTER);
            story.setFont(new Font("Serif", Font.PLAIN, 17));
            story.setForeground(new Color(218, 220, 208));

            JButton startButton = fantasyButton("✧  ENTER HARD MODE  ✧");
            JButton instructionsButton = fantasyButton("HOW TO PLAY");
            JButton exitButton = fantasyButton("LEAVE THE REALM");

            startButton.addActionListener(e -> startGame());
            instructionsButton.addActionListener(e -> showInstructions());
            exitButton.addActionListener(e -> {
                int choice = JOptionPane.showConfirmDialog(
                        MazeEscape.this,
                        "Are you sure you want to leave the realm?",
                        "Exit Game",
                        JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            });

            content.add(Box.createVerticalStrut(24));
            content.add(emblem);
            content.add(Box.createVerticalStrut(18));
            content.add(title);
            content.add(Box.createVerticalStrut(4));
            content.add(subtitle);
            content.add(Box.createVerticalStrut(22));
            content.add(story);
            content.add(Box.createVerticalStrut(38));
            content.add(startButton);
            content.add(Box.createVerticalStrut(13));
            content.add(instructionsButton);
            content.add(Box.createVerticalStrut(13));
            content.add(exitButton);
            content.add(Box.createVerticalStrut(22));

            add(content);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(12, 25, 39),
                    getWidth(), getHeight(), new Color(34, 48, 48)
            );
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(220, 190, 115, 65));
            for (int i = 0; i < 45; i++) {
                int x = (i * 97 + 31) % Math.max(1, getWidth());
                int y = (i * 61 + 17) % Math.max(1, getHeight());
                int size = (i % 3) + 2;
                g2.fillOval(x, y, size, size);
            }

            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(190, 158, 91, 130));
            g2.drawRoundRect(22, 22, getWidth() - 44, getHeight() - 44, 28, 28);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(190, 158, 91, 65));
            g2.drawRoundRect(31, 31, getWidth() - 62, getHeight() - 62, 22, 22);

            g2.dispose();
        }
    }

    // ------------------------------------------------------------
    // INSTRUCTIONS SCREEN
    // ------------------------------------------------------------

    private class InstructionsScreen extends JPanel {

        InstructionsScreen() {
            setLayout(new GridBagLayout());
            setBackground(new Color(17, 30, 39));

            JPanel card = new JPanel();
            card.setOpaque(true);
            card.setBackground(new Color(31, 48, 51));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(185, 154, 91), 2),
                    new EmptyBorder(28, 38, 28, 38)
            ));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JLabel heading = new JLabel("YOUR QUEST");
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            heading.setFont(new Font("Serif", Font.BOLD, 34));
            heading.setForeground(new Color(240, 215, 158));

            JLabel intro = new JLabel(
                    "<html><div style='text-align:center;'>"
                            + "Explore the larger, winding dungeon<br>"
                            + "and reach the glowing exit gate. Beware of dead ends!"
                            + "</div></html>"
            );
            intro.setAlignmentX(Component.CENTER_ALIGNMENT);
            intro.setFont(new Font("Serif", Font.PLAIN, 17));
            intro.setForeground(new Color(225, 228, 216));

            JLabel controls = new JLabel(
                    "<html><div style='line-height:1.8;'>"
                            + "<b>ARROW KEYS</b> &nbsp; Move your adventurer<br>"
                            + "<b>U</b> &nbsp; Undo your previous move<br>"
                            + "<b>H</b> &nbsp; Reveal the shortest path hint<br>"
                            + "<b>R</b> &nbsp; Restart the current maze<br><br>"
                            + "<b>GOLDEN PATH</b> &nbsp; Shortest route to the exit<br>"
                            + "<b>WALLS</b> &nbsp; Cannot be crossed<br>"
                            + "<b>EXIT GATE</b> &nbsp; Reach it to win the game"
                            + "</div></html>"
            );
            controls.setAlignmentX(Component.CENTER_ALIGNMENT);
            controls.setFont(new Font("SansSerif", Font.PLAIN, 15));
            controls.setForeground(new Color(220, 225, 212));

            JButton backButton = fantasyButton("RETURN TO MAIN MENU");
            backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            backButton.addActionListener(e -> showMenu());

            card.add(heading);
            card.add(Box.createVerticalStrut(15));
            card.add(intro);
            card.add(Box.createVerticalStrut(26));
            card.add(controls);
            card.add(Box.createVerticalStrut(28));
            card.add(backButton);

            add(card);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(13, 25, 36),
                    getWidth(), getHeight(), new Color(38, 57, 52)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------
    // GAME SCREEN
    // ------------------------------------------------------------

    private class GameScreen extends JPanel {

        private final JLabel movesLabel = new JLabel("MOVES: 0");
        private final JLabel statusLabel = new JLabel("HARD MODE: Find the enchanted exit!");
        private final MazeBoard board = new MazeBoard();

        GameScreen() {
            setLayout(new BorderLayout(12, 12));
            setBackground(new Color(17, 30, 38));
            setBorder(new EmptyBorder(15, 18, 15, 18));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JLabel title = new JLabel("✦  THE FORGOTTEN DUNGEON  ✦");
            title.setFont(new Font("Serif", Font.BOLD, 25));
            title.setForeground(new Color(239, 213, 157));

            JLabel difficultyLabel = new JLabel("DIFFICULTY: HARD");
            difficultyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            difficultyLabel.setForeground(new Color(232, 190, 112));
            difficultyLabel.setHorizontalAlignment(SwingConstants.CENTER);

            movesLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            movesLabel.setForeground(new Color(179, 220, 187));
            movesLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            header.add(title, BorderLayout.WEST);
            header.add(difficultyLabel, BorderLayout.CENTER);
            header.add(movesLabel, BorderLayout.EAST);

            JPanel boardHolder = new JPanel(new GridBagLayout());
            boardHolder.setOpaque(false);
            boardHolder.add(board);

            JPanel footer = new JPanel();
            footer.setOpaque(false);
            footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            statusLabel.setFont(new Font("Serif", Font.ITALIC, 16));
            statusLabel.setForeground(new Color(224, 203, 151));

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 7));
            controls.setOpaque(false);

            JButton hintButton = smallFantasyButton("✧ HINT (H)");
            JButton undoButton = smallFantasyButton("↶ UNDO (U)");
            JButton restartButton = smallFantasyButton("⟳ RESTART (R)");
            JButton menuButton = smallFantasyButton("MAIN MENU");

            hintButton.addActionListener(e -> showHint());
            undoButton.addActionListener(e -> undoMove());
            restartButton.addActionListener(e -> startGame());
            menuButton.addActionListener(e -> showMenu());

            controls.add(hintButton);
            controls.add(undoButton);
            controls.add(restartButton);
            controls.add(menuButton);

            JLabel legend = new JLabel(
                    "  ◆ Golden trail: shortest path     ● You: adventurer     ✦ Exit: enchanted gate"
            );
            legend.setAlignmentX(Component.CENTER_ALIGNMENT);
            legend.setFont(new Font("SansSerif", Font.PLAIN, 12));
            legend.setForeground(new Color(185, 199, 187));

            footer.add(statusLabel);
            footer.add(Box.createVerticalStrut(5));
            footer.add(controls);
            footer.add(legend);

            add(header, BorderLayout.NORTH);
            add(boardHolder, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }

        void refresh() {
            movesLabel.setText("MOVES: " + moves);
            if (gameWon) {
                statusLabel.setText("The ancient gate is open. Dungeon conquered!");
            } else if (!hintPath.isEmpty()) {
                statusLabel.setText("Follow the golden trail through the dungeon.");
            } else {
                statusLabel.setText("HARD MODE: Find the enchanted exit!");
            }
            board.repaint();
        }
    }

    // ------------------------------------------------------------
    // CUSTOM MAZE DRAWING
    // ------------------------------------------------------------

    private class MazeBoard extends JPanel implements KeyListener {

        MazeBoard() {
            setPreferredSize(new Dimension(
                    maze[0].length * TILE_SIZE,
                    maze.length * TILE_SIZE
            ));
            setFocusable(true);
            addKeyListener(this);
            setBackground(new Color(13, 23, 29));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int rows = maze.length;
            int cols = maze[0].length;

            // Soft shadow behind the maze.
            g2.setColor(new Color(0, 0, 0, 85));
            g2.fillRoundRect(7, 8, cols * TILE_SIZE + 5,
                    rows * TILE_SIZE + 5, 20, 20);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = c * TILE_SIZE;
                    int y = r * TILE_SIZE;

                    if (maze[r][c] == WALL) {
                        drawWall(g2, x, y);
                    } else {
                        drawFloor(g2, x, y);

                        if (isHintTile(r, c)) {
                            drawHintTile(g2, x, y);
                        }

                        if (r == exitRow && c == exitCol) {
                            drawExit(g2, x, y);
                        }
                    }
                }
            }

            drawPlayer(g2, playerCol * TILE_SIZE, playerRow * TILE_SIZE);

            g2.setColor(new Color(205, 174, 111));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(1, 1, cols * TILE_SIZE - 2,
                    rows * TILE_SIZE - 2, 13, 13);

            g2.dispose();
        }

        private void drawWall(Graphics2D g2, int x, int y) {
            g2.setPaint(new GradientPaint(
                    x, y, new Color(47, 67, 67),
                    x + TILE_SIZE, y + TILE_SIZE, new Color(23, 39, 43)
            ));
            g2.fillRoundRect(x + 2, y + 2, TILE_SIZE - 4,
                    TILE_SIZE - 4, 9, 9);

            g2.setColor(new Color(128, 151, 129, 90));
            g2.drawRoundRect(x + 5, y + 5, TILE_SIZE - 10,
                    TILE_SIZE - 10, 7, 7);

            g2.setColor(new Color(205, 186, 137, 35));
            g2.drawLine(x + 8, y + 12, x + 22, y + 12);
            g2.drawLine(x + 13, y + 23, x + 27, y + 23);
        }

        private void drawFloor(Graphics2D g2, int x, int y) {
            g2.setPaint(new GradientPaint(
                    x, y, new Color(94, 111, 81),
                    x + TILE_SIZE, y + TILE_SIZE, new Color(57, 78, 66)
            ));
            g2.fillRoundRect(x + 2, y + 2, TILE_SIZE - 4,
                    TILE_SIZE - 4, 8, 8);

            g2.setColor(new Color(209, 190, 133, 50));
            g2.drawRoundRect(x + 5, y + 5, TILE_SIZE - 10,
                    TILE_SIZE - 10, 6, 6);

            g2.setColor(new Color(222, 216, 171, 45));
            g2.fillOval(x + 9, y + 9, 3, 3);
            g2.fillOval(x + 23, y + 22, 2, 2);
        }

        private boolean isHintTile(int row, int col) {
            for (Point point : hintPath) {
                if (point.x == col && point.y == row) {
                    return true;
                }
            }
            return false;
        }

        private void drawHintTile(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(246, 195, 76, 155));
            g2.fillRoundRect(x + 9, y + 9, TILE_SIZE - 18,
                    TILE_SIZE - 18, 12, 12);

            g2.setColor(new Color(255, 235, 160, 210));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x + 12, y + 12, TILE_SIZE - 24,
                    TILE_SIZE - 24, 10, 10);
        }

        private void drawExit(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(255, 218, 112, 65));
            g2.fillOval(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);

            g2.setPaint(new GradientPaint(
                    x, y, new Color(255, 231, 150),
                    x + TILE_SIZE, y + TILE_SIZE, new Color(179, 119, 48)
            ));
            g2.fillRoundRect(x + 17, y + 8, TILE_SIZE - 34,
                    TILE_SIZE - 16, 18, 18);

            g2.setColor(new Color(255, 246, 199));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x + 17, y + 8, TILE_SIZE - 34,
                    TILE_SIZE - 16, 18, 18);

            g2.setColor(new Color(91, 60, 32));
            g2.fillOval(x + 34, y + 30, 5, 5);

            g2.setFont(new Font("Serif", Font.BOLD, 13));
            g2.setColor(new Color(255, 247, 206));
            g2.drawString("✦", x + 22, y + 24);
        }

        private void drawPlayer(Graphics2D g2, int x, int y) {
            int centerX = x + TILE_SIZE / 2;
            int centerY = y + TILE_SIZE / 2;

            // Glow
            g2.setColor(new Color(105, 222, 190, 75));
            g2.fillOval(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);

            // Cloak/body
            g2.setColor(new Color(34, 87, 92));
            int[] bodyX = {centerX - 15, centerX + 15, centerX};
            int[] bodyY = {centerY + 15, centerY + 15, centerY - 12};
            g2.fillPolygon(bodyX, bodyY, 3);

            // Head
            g2.setPaint(new GradientPaint(
                    centerX - 9, centerY - 17, new Color(255, 224, 171),
                    centerX + 9, centerY + 1, new Color(193, 132, 91)
            ));
            g2.fillOval(centerX - 9, centerY - 19, 18, 18);

            // Hood
            g2.setColor(new Color(49, 104, 82));
            g2.setStroke(new BasicStroke(3f));
            g2.drawArc(centerX - 11, centerY - 21, 22, 22, 0, 180);

            // Small staff
            g2.setColor(new Color(211, 176, 104));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(centerX + 13, centerY - 2, centerX + 17, centerY + 17);
            g2.setColor(new Color(148, 226, 206));
            g2.fillOval(centerX + 10, centerY - 7, 8, 8);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_UP) {
                movePlayer(-1, 0);
            } else if (key == KeyEvent.VK_DOWN) {
                movePlayer(1, 0);
            } else if (key == KeyEvent.VK_LEFT) {
                movePlayer(0, -1);
            } else if (key == KeyEvent.VK_RIGHT) {
                movePlayer(0, 1);
            } else if (key == KeyEvent.VK_U) {
                undoMove();
            } else if (key == KeyEvent.VK_H) {
                showHint();
            } else if (key == KeyEvent.VK_R) {
                startGame();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not needed.
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // Not needed.
        }
    }

    // ------------------------------------------------------------
    // CUSTOM LINKED STACK - UNDO FEATURE
    // ------------------------------------------------------------

    private static class PositionStack {

        private static class Node {
            Position data;
            Node next;

            Node(Position data) {
                this.data = data;
            }
        }

        private Node top;

        public void push(int row, int col) {
            Node newNode = new Node(new Position(row, col));
            newNode.next = top;
            top = newNode;
        }

        public Position pop() {
            if (isEmpty()) {
                return null;
            }

            Position removed = top.data;
            top = top.next;
            return removed;
        }

        public boolean isEmpty() {
            return top == null;
        }

        public void clear() {
            top = null;
        }
    }

    // ------------------------------------------------------------
    // CUSTOM LINKED QUEUE - BFS FEATURE
    // ------------------------------------------------------------

    private static class PositionQueue {

        private static class Node {
            Position data;
            Node next;

            Node(Position data) {
                this.data = data;
            }
        }

        private Node front;
        private Node rear;

        public void enqueue(int row, int col) {
            Node newNode = new Node(new Position(row, col));

            if (rear == null) {
                front = newNode;
                rear = newNode;
            } else {
                rear.next = newNode;
                rear = newNode;
            }
        }

        public Position dequeue() {
            if (isEmpty()) {
                return null;
            }

            Position removed = front.data;
            front = front.next;

            if (front == null) {
                rear = null;
            }

            return removed;
        }

        public boolean isEmpty() {
            return front == null;
        }
    }

    // Simple position object used by the custom Stack and Queue.
    private static class Position {
        int row;
        int col;

        Position(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    // ------------------------------------------------------------
    // BUTTON STYLES
    // ------------------------------------------------------------

    private JButton fantasyButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Serif", Font.BOLD, 16));
        button.setForeground(new Color(255, 241, 204));
        button.setBackground(new Color(42, 75, 62));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(199, 164, 96), 2),
                new EmptyBorder(12, 28, 12, 28)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(300, 52));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(67, 111, 79));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(42, 75, 62));
            }
        });

        return button;
    }

    private JButton smallFantasyButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(new Color(255, 241, 204));
        button.setBackground(new Color(42, 75, 62));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(178, 148, 91), 1),
                new EmptyBorder(9, 12, 9, 12)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(67, 111, 79));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(42, 75, 62));
            }
        });

        return button;
    }

    // ------------------------------------------------------------
    // MAIN METHOD
    // ------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName()
                    );
                } catch (Exception ignored) {
                    // Use the default Swing look and feel.
                }

                MazeEscape game = new MazeEscape();
                game.setVisible(true);
            }
        });
    }
}
