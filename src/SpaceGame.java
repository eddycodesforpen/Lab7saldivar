import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class SpaceGame extends JFrame implements KeyListener {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;

    private static final int PLAYER_WIDTH = 60;
    private static final int PLAYER_HEIGHT = 60;

    private static final int OBSTACLE_WIDTH = 40;
    private static final int OBSTACLE_HEIGHT = 40;

    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 15;

    private static final int PLAYER_SPEED = 15;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;

    private int score = 0;

    private boolean shieldActive = false;
    private boolean shieldUsed = false;
    private boolean isGameOver = false;
    private boolean isProjectileVisible = false;
    private boolean isFiring = false;

    private int playerX, playerY;
    private int projectileX, projectileY;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private Timer timer;

    private List<Point> obstacles = new ArrayList<>();
    private List<Star> stars = new ArrayList<>();

    // Star class
    class Star {
        int x;
        int y;
        Color color;
        int size;

        public Star(int x, int y, Color color, int size) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
        }
    }

    public SpaceGame() {
        setTitle("Rat Space Defender");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Ask user for shield
        int choice = JOptionPane.showConfirmDialog(
                null,
                "Would you like a one-time shield?",
                "Shield Option",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            shieldActive = true;
        }

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 30;

        // Create static multicolor stars
        Color[] starColors = {
                Color.WHITE,
                Color.YELLOW,
                Color.CYAN,
                Color.PINK,
                Color.GREEN,
                Color.ORANGE
        };

        for (int i = 0; i < 80; i++) {
            Color randomColor =
                    starColors[(int)(Math.random() * starColors.length)];

            int size = (int)(Math.random() * 3) + 2;

            stars.add(new Star(
                    (int)(Math.random() * WIDTH),
                    (int)(Math.random() * HEIGHT),
                    randomColor,
                    size
            ));
        }

        gamePanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        gamePanel.add(scoreLabel);

        add(gamePanel);

        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        timer = new Timer(20, e -> {
            if (!isGameOver) {
                updateGame();
                gamePanel.repaint();
            }
        });

        timer.start();
    }

    private void drawGame(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Black background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw static stars
        for (Star star : stars) {
            g2.setColor(star.color);
            g2.fillOval(star.x, star.y, star.size, star.size);
        }

        // Draw rat ship
        drawRat(g2, playerX, playerY);

        // Draw shield
        if (shieldActive) {
            g2.setColor(Color.CYAN);
            g2.drawOval(playerX - 10, playerY - 10, 80, 80);
        }

        // Draw projectile
        if (isProjectileVisible) {
            g2.setColor(Color.YELLOW);
            g2.fillRect(projectileX, projectileY,
                    PROJECTILE_WIDTH,
                    PROJECTILE_HEIGHT);
        }

        // Draw cats
        for (Point obstacle : obstacles) {
            drawCat(g2, obstacle.x, obstacle.y);
        }

        // Game over
        if (isGameOver) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            g2.drawString("GAME OVER",
                    WIDTH / 2 - 100,
                    HEIGHT / 2);
        }
    }

    private void drawRat(Graphics2D g, int x, int y) {
        // Face
        g.setColor(Color.GRAY);
        g.fillOval(x, y, 60, 60);

        // Ears
        g.fillOval(x + 5, y - 10, 20, 20);
        g.fillOval(x + 35, y - 10, 20, 20);

        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(x + 15, y + 20, 8, 8);
        g.fillOval(x + 35, y + 20, 8, 8);

        // Nose
        g.setColor(Color.PINK);
        g.fillOval(x + 25, y + 35, 10, 10);

        // Whiskers
        g.setColor(Color.WHITE);
        g.drawLine(x + 5, y + 35, x + 20, y + 30);
        g.drawLine(x + 5, y + 40, x + 20, y + 40);

        g.drawLine(x + 55, y + 35, x + 40, y + 30);
        g.drawLine(x + 55, y + 40, x + 40, y + 40);
    }

    private void drawCat(Graphics2D g, int x, int y) {
        // Cat face
        g.setColor(Color.ORANGE);
        g.fillOval(x, y, 40, 40);

        // Cat ears
        int[] x1 = {x + 8, x + 15, x + 20};
        int[] y1 = {y + 5, y - 10, y + 10};
        g.fillPolygon(x1, y1, 3);

        int[] x2 = {x + 20, x + 25, x + 32};
        int[] y2 = {y + 10, y - 10, y + 5};
        g.fillPolygon(x2, y2, 3);

        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(x + 10, y + 15, 5, 5);
        g.fillOval(x + 25, y + 15, 5, 5);

        // Nose
        g.setColor(Color.PINK);
        g.fillOval(x + 17, y + 25, 6, 6);
    }

    private void updateGame() {

        // Move cats
        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += OBSTACLE_SPEED;

            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
                i--;
            }
        }

        // Spawn cats
        if (Math.random() < 0.03) {
            int x = (int)(Math.random() *
                    (WIDTH - OBSTACLE_WIDTH));
            obstacles.add(new Point(x, 0));
        }

        // Move projectile
        if (isProjectileVisible) {
            projectileY -= PROJECTILE_SPEED;

            if (projectileY < 0) {
                isProjectileVisible = false;
            }
        }

        Rectangle playerRect =
                new Rectangle(playerX, playerY,
                        PLAYER_WIDTH,
                        PLAYER_HEIGHT);

        // Cat collision with player
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle catRect =
                    new Rectangle(
                            obstacles.get(i).x,
                            obstacles.get(i).y,
                            OBSTACLE_WIDTH,
                            OBSTACLE_HEIGHT
                    );

            if (playerRect.intersects(catRect)) {

                if (shieldActive && !shieldUsed) {
                    shieldActive = false;
                    shieldUsed = true;
                    obstacles.remove(i);

                    JOptionPane.showMessageDialog(
                            null,
                            "Shield destroyed! One life saved."
                    );
                    break;
                } else {
                    isGameOver = true;
                }
            }
        }

        // Projectile collision with cats
        Rectangle projectileRect =
                new Rectangle(
                        projectileX,
                        projectileY,
                        PROJECTILE_WIDTH,
                        PROJECTILE_HEIGHT
                );

        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle catRect =
                    new Rectangle(
                            obstacles.get(i).x,
                            obstacles.get(i).y,
                            OBSTACLE_WIDTH,
                            OBSTACLE_HEIGHT
                    );

            if (projectileRect.intersects(catRect)) {
                obstacles.remove(i);
                score += 10;
                isProjectileVisible = false;
                break;
            }
        }

        scoreLabel.setText("Score: " + score);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
        }

        if (key == KeyEvent.VK_RIGHT &&
                playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        }

        if (key == KeyEvent.VK_SPACE && !isFiring) {
            isFiring = true;

            projectileX = playerX + PLAYER_WIDTH / 2;
            projectileY = playerY;

            isProjectileVisible = true;

            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    isFiring = false;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SpaceGame().setVisible(true);
        });
    }
}