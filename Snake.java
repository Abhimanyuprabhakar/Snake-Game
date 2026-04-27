import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

// Enum to represent direction - demonstrates encapsulation of fixed constants
enum Direction {
    UP, DOWN, LEFT, RIGHT
}

// Interface demonstrating abstraction and polymorphism
interface Drawable {
    void draw(Graphics g);
}

// Snake class - demonstrates encapsulation (private fields, public methods)
class Snake implements Drawable {
    private List<Point> body;          // Snake body segments
    private Direction direction;       // Current moving direction
    private static final int CELL_SIZE = 30;

    public Snake(int startX, int startY) {
        body = new ArrayList<>();
        // Initial snake: 3 segments long, moving right
        body.add(new Point(startX, startY));         // Head
        body.add(new Point(startX - 1, startY));
        body.add(new Point(startX - 2, startY));
        direction = Direction.RIGHT;
    }

    // Getter for head position
    public Point getHead() {
        return body.get(0);
    }

    // Getter for entire body (immutable view)
    public List<Point> getBody() {
        return Collections.unmodifiableList(body);
    }

    // Change direction - prevents 180-degree turns
    public void changeDirection(Direction newDir) {
        if ((direction == Direction.UP && newDir == Direction.DOWN) ||
            (direction == Direction.DOWN && newDir == Direction.UP) ||
            (direction == Direction.LEFT && newDir == Direction.RIGHT) ||
            (direction == Direction.RIGHT && newDir == Direction.LEFT)) {
            return; // Can't reverse direction
        }
        direction = newDir;
    }

    // Calculate new head position without moving
    public Point getNewHead() {
        Point head = getHead();
        int x = head.x;
        int y = head.y;
        switch (direction) {
            case UP:    y--; break;
            case DOWN:  y++; break;
            case LEFT:  x--; break;
            case RIGHT: x++; break;
        }
        return new Point(x, y);
    }

    // Move the snake - demonstrates encapsulation of growth logic
    public void move(boolean grow) {
        Point newHead = getNewHead();
        body.add(0, newHead);          // Add new head
        if (!grow) {
            body.remove(body.size() - 1); // Remove tail if not growing
        }
    }

    // Check if a move to given head position is safe
    public boolean canMoveTo(Point newHead, boolean willEat, int boardWidth, int boardHeight) {
        // Wall collision check
        if (newHead.x < 0 || newHead.x >= boardWidth || newHead.y < 0 || newHead.y >= boardHeight) {
            return false;
        }
        // Self collision check
        for (int i = 0; i < body.size(); i++) {
            Point segment = body.get(i);
            if (segment.equals(newHead)) {
                // If we are going to eat, we keep the tail, so collision with head is allowed?
                // Actually newHead can't be equal to any existing segment unless it's the tail
                // and we are not eating (then tail will be removed).
                if (!willEat && i == body.size() - 1) {
                    continue; // Tail will be removed, so safe
                }
                return false; // Collision with body
            }
        }
        return true;
    }

    // Post-move self collision check (game over)
    public boolean checkSelfCollision() {
        Point head = getHead();
        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i))) {
                return true;
            }
        }
        return false;
    }

    // Implement draw() from Drawable interface - polymorphism
    @Override
    public void draw(Graphics g) {
        for (int i = 0; i < body.size(); i++) {
            Point p = body.get(i);
            if (i == 0) {
                g.setColor(Color.GREEN); // Head
            } else {
                g.setColor(Color.YELLOW); // Body
            }
            g.fillRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
        }
    }
}

// Food class - demonstrates encapsulation and polymorphism
class Food implements Drawable {
    private Point position;
    private static final int CELL_SIZE = 30;
    private Random random;

    public Food() {
        random = new Random();
        position = new Point(10, 10); // temporary
    }

    // Respawning logic with collision avoidance
    public void respawn(Snake snake, int boardWidth, int boardHeight) {
        List<Point> snakeBody = snake.getBody();
        boolean collision;
        do {
            collision = false;
            int x = random.nextInt(boardWidth);
            int y = random.nextInt(boardHeight);
            position = new Point(x, y);
            // Check if food spawns on snake
            for (Point segment : snakeBody) {
                if (segment.equals(position)) {
                    collision = true;
                    break;
                }
            }
        } while (collision);
    }

    public Point getPosition() {
        return position;
    }

    // Draw food
    @Override
    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(position.x * CELL_SIZE, position.y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
    }
}

// GamePanel - inherits from JPanel, implements ActionListener and KeyListener
// Demonstrates inheritance and event handling
class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int CELL_SIZE = 30;
    private static final int BOARD_WIDTH = 20;
    private static final int BOARD_HEIGHT = 20;
    private static final int GAME_UNIT = CELL_SIZE;
    private static final int DELAY = 120; // milliseconds

    private Snake snake;
    private Food food;
    private javax.swing.Timer timer;
    private int score;
    private boolean gameOver;
    private boolean gameStarted;

    public GamePanel() {
        setPreferredSize(new Dimension(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        initGame();
    }

    // Initialize game state
    private void initGame() {
        snake = new Snake(BOARD_WIDTH / 2, BOARD_HEIGHT / 2);
        food = new Food();
        food.respawn(snake, BOARD_WIDTH, BOARD_HEIGHT);
        score = 0;
        gameOver = false;
        gameStarted = true;
        if (timer != null) {
            timer.stop();
        }
        timer = new javax.swing.Timer(DELAY, this);
        timer.start();
    }

    // Game logic update - executed each timer tick
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            timer.stop();
            repaint();
            return;
        }
        updateGame();
        repaint();
    }

    private void updateGame() {
        Point newHead = snake.getNewHead();
        boolean willEat = newHead.equals(food.getPosition());

        // Check if move is safe
        if (!snake.canMoveTo(newHead, willEat, BOARD_WIDTH, BOARD_HEIGHT)) {
            gameOver = true;
            return;
        }

        // Perform the move
        snake.move(willEat);

        // Handle food eating
        if (willEat) {
            score++;
            food.respawn(snake, BOARD_WIDTH, BOARD_HEIGHT);
        }

        // Additional self-collision check (edge case)
        if (snake.checkSelfCollision()) {
            gameOver = true;
        }
    }

    // Drawing all components - demonstrates polymorphism via Drawable
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGrid(g);

        // Polymorphic draw calls
        snake.draw(g);
        food.draw(g);

        // Draw score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Score: " + score, 10, 30);

        // Game over message
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            String gameOverMsg = "Game Over! Press R to Restart";
            int msgWidth = metrics.stringWidth(gameOverMsg);
            g.drawString(gameOverMsg, (getWidth() - msgWidth) / 2, getHeight() / 2);
        }
    }

    private void drawGrid(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= BOARD_WIDTH; i++) {
            g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, getHeight());
            g.drawLine(0, i * CELL_SIZE, getWidth(), i * CELL_SIZE);
        }
    }

    // Key handling for snake control and restart
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                initGame();
                repaint();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                snake.changeDirection(Direction.UP);
                break;
            case KeyEvent.VK_DOWN:
                snake.changeDirection(Direction.DOWN);
                break;
            case KeyEvent.VK_LEFT:
                snake.changeDirection(Direction.LEFT);
                break;
            case KeyEvent.VK_RIGHT:
                snake.changeDirection(Direction.RIGHT);
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

// Main class - entry point
class SnakeGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake Game - OOP Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}