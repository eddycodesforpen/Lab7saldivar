import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.util.Random;

public class SpaceGame extends JFrame implements KeyListener, ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_WIDTH = 180;  // Bigger ship (was 150)
    private static final int PLAYER_HEIGHT = 180; // Bigger ship (was 150)
    private static final int OBSTACLE_WIDTH = 50;
    private static final int OBSTACLE_HEIGHT = 50;
    private static final int PROJECTILE_WIDTH = 12;
    private static final int PROJECTILE_HEIGHT = 20;
    private static final int PLAYER_SPEED = 8;
    private static final int BASE_OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;
    private static final int MAX_LEVEL = 5;

    private int score = 0;
    private int health = 100;
    private int timeLeft = 60;
    private int level = 1;
    private boolean shieldActive = false;
    private boolean shieldOnCooldown = false;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JLabel levelLabel;
    private JLabel shieldStatusLabel;
    private JLabel speedLabel;
    private JProgressBar healthBar;
    private Timer gameTimer;
    private Timer countdownTimer;
    private boolean isGameOver;
    private boolean isGameWin;

    private int playerX, playerY;
    private int projectileX, projectileY;
    private boolean isProjectileVisible;
    private boolean isFiring;

    private java.util.List<Obstacle> obstacles;
    private java.util.List<HealthPowerUp> healthPowerUps; // Only health power-ups
    private java.util.List<Star> stars;
    private java.util.List<Explosion> explosions;

    private BufferedImage playerImage;
    private BufferedImage[] obstacleSprites; // Will load from obstacles.png
    private BufferedImage healthPowerUpImage;
    private Random random;

    // Sound clips
    private Clip fireSound;
    private Clip collisionSound;
    private Clip explosionSound;
    private Clip powerUpSound;
    private Clip gameOverSound;
    private Clip shieldSound;
    private Clip levelUpSound;
    private Clip backgroundMusic;

    public SpaceGame() {
        setTitle("SPACE GAME");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(null);

        random = new Random();
        loadResources();
        loadSounds();

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        gamePanel.setBounds(0, 80, WIDTH, HEIGHT - 80);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);
        gamePanel.requestFocus();

        // Create labels at the top
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setBounds(10, 10, 100, 25);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));

        timerLabel = new JLabel("Time: 60");
        timerLabel.setBounds(160, 10, 100, 25);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));

        levelLabel = new JLabel("Level: 1");
        levelLabel.setBounds(300, 10, 100, 25);
        levelLabel.setForeground(Color.YELLOW);
        levelLabel.setFont(new Font("Arial", Font.BOLD, 14));

        shieldStatusLabel = new JLabel("Shield: Ready");
        shieldStatusLabel.setBounds(480, 10, 150, 25);
        shieldStatusLabel.setForeground(Color.CYAN);
        shieldStatusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        speedLabel = new JLabel("Speed: " + getCurrentObstacleSpeed());
        speedLabel.setBounds(650, 10, 140, 25);
        speedLabel.setForeground(Color.ORANGE);
        speedLabel.setFont(new Font("Arial", Font.BOLD, 11));

        // Health Bar
        healthBar = new JProgressBar(0, 100);
        healthBar.setValue(100);
        healthBar.setStringPainted(true);
        healthBar.setString(health + "%");
        healthBar.setForeground(Color.GREEN);
        healthBar.setBackground(Color.DARK_GRAY);
        healthBar.setBounds(10, 45, 200, 20);
        healthBar.setFont(new Font("Arial", Font.BOLD, 11));

        JLabel healthLabel = new JLabel("HEALTH:");
        healthLabel.setBounds(10, 35, 60, 15);
        healthLabel.setForeground(Color.WHITE);
        healthLabel.setFont(new Font("Arial", Font.BOLD, 11));

        add(healthLabel);
        add(scoreLabel);
        add(timerLabel);
        add(levelLabel);
        add(shieldStatusLabel);
        add(speedLabel);
        add(healthBar);
        add(gamePanel);

        // Center the bigger ship
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 60;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isGameWin = false;
        isFiring = false;

        obstacles = new java.util.ArrayList<>();
        healthPowerUps = new java.util.ArrayList<>();
        stars = new java.util.ArrayList<>();
        explosions = new java.util.ArrayList<>();

        // Initialize stars with random colors
        for (int i = 0; i < 200; i++) {
            int red = random.nextInt(155) + 100;
            int green = random.nextInt(155) + 100;
            int blue = random.nextInt(155) + 100;
            stars.add(new Star(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(3) + 1, new Color(red, green, blue)));
        }

        gameTimer = new Timer(20, this);
        gameTimer.start();

        countdownTimer = new Timer(1000, e -> {
            if (!isGameOver && !isGameWin) {
                timeLeft--;
                timerLabel.setText("Time: " + timeLeft);
                if (timeLeft <= 0) {
                    if (level < MAX_LEVEL) {
                        levelUp();
                    } else {
                        isGameWin = true;
                        gameTimer.stop();
                        countdownTimer.stop();
                        playGameOverSound();
                        stopBackgroundMusic();
                        gamePanel.repaint();
                    }
                }
            }
        });
        countdownTimer.start();

        playBackgroundMusic();
    }

    private int getCurrentObstacleSpeed() {
        return BASE_OBSTACLE_SPEED + (level - 1) * 2;
    }

    private void loadResources() {
        // Load player image from Ship.png (Piskel image)
        try {
            File shipFile = new File("Ship.png");
            if (shipFile.exists()) {
                BufferedImage originalImage = ImageIO.read(shipFile);
                Image scaledImage = originalImage.getScaledInstance(PLAYER_WIDTH, PLAYER_HEIGHT, Image.SCALE_SMOOTH);
                playerImage = new BufferedImage(PLAYER_WIDTH, PLAYER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = playerImage.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();
                System.out.println("Loaded Ship.png");
            } else {
                InputStream inputStream = getClass().getResourceAsStream("/Ship.png");
                if (inputStream != null) {
                    BufferedImage originalImage = ImageIO.read(inputStream);
                    Image scaledImage = originalImage.getScaledInstance(PLAYER_WIDTH, PLAYER_HEIGHT, Image.SCALE_SMOOTH);
                    playerImage = new BufferedImage(PLAYER_WIDTH, PLAYER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = playerImage.createGraphics();
                    g2d.drawImage(scaledImage, 0, 0, null);
                    g2d.dispose();
                    System.out.println("Loaded Ship.png from classpath");
                } else {
                    System.out.println("Ship.png not found, creating default ship");
                    createDefaultShip();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading Ship.png: " + e.getMessage());
            createDefaultShip();
        }

        // Load obstacles from obstacles.png (sprite sheet with multiple asteroids)
        try {
            File obstaclesFile = new File("obstacles.png");
            if (obstaclesFile.exists()) {
                BufferedImage spriteSheet = ImageIO.read(obstaclesFile);
                System.out.println("Loaded obstacles.png - Sheet size: " + spriteSheet.getWidth() + "x" + spriteSheet.getHeight());

                // Calculate how many sprites fit in the sheet (assuming horizontal layout)
                int cols = spriteSheet.getWidth() / OBSTACLE_WIDTH;
                int rows = spriteSheet.getHeight() / OBSTACLE_HEIGHT;
                int totalSprites = cols * rows;

                // Load up to 4 obstacle types
                obstacleSprites = new BufferedImage[Math.min(4, totalSprites)];
                for (int i = 0; i < obstacleSprites.length; i++) {
                    int sx = (i % cols) * OBSTACLE_WIDTH;
                    int sy = (i / cols) * OBSTACLE_HEIGHT;
                    obstacleSprites[i] = spriteSheet.getSubimage(sx, sy, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                    System.out.println("Loaded obstacle sprite " + i);
                }

                // If not enough sprites, generate the rest
                if (obstacleSprites.length < 4) {
                    BufferedImage[] fullSprites = new BufferedImage[4];
                    System.arraycopy(obstacleSprites, 0, fullSprites, 0, obstacleSprites.length);
                    for (int i = obstacleSprites.length; i < 4; i++) {
                        fullSprites[i] = createDefaultObstacle(i);
                    }
                    obstacleSprites = fullSprites;
                }
            } else {
                InputStream inputStream = getClass().getResourceAsStream("/obstacles.png");
                if (inputStream != null) {
                    BufferedImage spriteSheet = ImageIO.read(inputStream);
                    int cols = spriteSheet.getWidth() / OBSTACLE_WIDTH;
                    int rows = spriteSheet.getHeight() / OBSTACLE_HEIGHT;
                    int totalSprites = cols * rows;

                    obstacleSprites = new BufferedImage[Math.min(4, totalSprites)];
                    for (int i = 0; i < obstacleSprites.length; i++) {
                        int sx = (i % cols) * OBSTACLE_WIDTH;
                        int sy = (i / cols) * OBSTACLE_HEIGHT;
                        obstacleSprites[i] = spriteSheet.getSubimage(sx, sy, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                    }

                    if (obstacleSprites.length < 4) {
                        BufferedImage[] fullSprites = new BufferedImage[4];
                        System.arraycopy(obstacleSprites, 0, fullSprites, 0, obstacleSprites.length);
                        for (int i = obstacleSprites.length; i < 4; i++) {
                            fullSprites[i] = createDefaultObstacle(i);
                        }
                        obstacleSprites = fullSprites;
                    }
                } else {
                    System.out.println("obstacles.png not found, creating default obstacles");
                    createDefaultObstacles();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading obstacles.png: " + e.getMessage());
            createDefaultObstacles();
        }

        // Load health power-up image (from health.png or create default)
        try {
            File healthFile = new File("health.png");
            if (healthFile.exists()) {
                BufferedImage originalImage = ImageIO.read(healthFile);
                healthPowerUpImage = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = healthPowerUpImage.createGraphics();
                g2d.drawImage(originalImage.getScaledInstance(22, 22, Image.SCALE_SMOOTH), 0, 0, null);
                g2d.dispose();
            } else {
                InputStream inputStream = getClass().getResourceAsStream("/health.png");
                if (inputStream != null) {
                    BufferedImage originalImage = ImageIO.read(inputStream);
                    healthPowerUpImage = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = healthPowerUpImage.createGraphics();
                    g2d.drawImage(originalImage.getScaledInstance(22, 22, Image.SCALE_SMOOTH), 0, 0, null);
                    g2d.dispose();
                } else {
                    createDefaultHealthPowerUp();
                }
            }
        } catch (IOException e) {
            createDefaultHealthPowerUp();
        }
    }

    private void createDefaultShip() {
        playerImage = new BufferedImage(PLAYER_WIDTH, PLAYER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = playerImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Main body
        g2d.setColor(new Color(0, 100, 200));
        int[] xPoints = {PLAYER_WIDTH/2, 25, PLAYER_WIDTH-25};
        int[] yPoints = {25, PLAYER_HEIGHT-30, PLAYER_HEIGHT-30};
        g2d.fillPolygon(xPoints, yPoints, 3);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolygon(xPoints, yPoints, 3);

        // Cockpit
        g2d.setColor(new Color(0, 200, 255));
        g2d.fillOval(PLAYER_WIDTH/2 - 22, 35, 44, 44);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(PLAYER_WIDTH/2 - 12, 45, 24, 24);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(PLAYER_WIDTH/2 - 6, 52, 12, 12);

        // Wings
        g2d.setColor(new Color(0, 80, 160));
        int[] leftWing = {25, 5, 40};
        int[] leftWingY = {PLAYER_HEIGHT-45, PLAYER_HEIGHT-20, PLAYER_HEIGHT-20};
        g2d.fillPolygon(leftWing, leftWingY, 3);

        int[] rightWing = {PLAYER_WIDTH-25, PLAYER_WIDTH-5, PLAYER_WIDTH-40};
        int[] rightWingY = {PLAYER_HEIGHT-45, PLAYER_HEIGHT-20, PLAYER_HEIGHT-20};
        g2d.fillPolygon(rightWing, rightWingY, 3);

        // Engine
        g2d.setColor(Color.ORANGE);
        g2d.fillRect(PLAYER_WIDTH/2 - 15, PLAYER_HEIGHT-25, 30, 15);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(PLAYER_WIDTH/2 - 10, PLAYER_HEIGHT-22, 20, 10);
        g2d.setColor(Color.RED);
        g2d.fillRect(PLAYER_WIDTH/2 - 5, PLAYER_HEIGHT-18, 10, 6);

        g2d.dispose();
    }

    private void createDefaultObstacles() {
        obstacleSprites = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            obstacleSprites[i] = createDefaultObstacle(i);
        }
    }

    private BufferedImage createDefaultObstacle(int type) {
        BufferedImage img = new BufferedImage(OBSTACLE_WIDTH, OBSTACLE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch(type) {
            case 0: // Grey asteroid
                g2d.setColor(Color.GRAY);
                g2d.fillOval(0, 0, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                g2d.setColor(Color.DARK_GRAY);
                for (int j = 0; j < 8; j++) {
                    g2d.fillOval(random.nextInt(OBSTACLE_WIDTH), random.nextInt(OBSTACLE_HEIGHT), 6, 6);
                }
                break;
            case 1: // Brown asteroid
                g2d.setColor(new Color(139, 69, 19));
                g2d.fillOval(0, 0, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                g2d.setColor(new Color(101, 67, 33));
                for (int j = 0; j < 6; j++) {
                    g2d.fillOval(random.nextInt(OBSTACLE_WIDTH), random.nextInt(OBSTACLE_HEIGHT), 5, 5);
                }
                break;
            case 2: // Dark asteroid
                g2d.setColor(new Color(70, 70, 70));
                g2d.fillOval(0, 0, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                g2d.setColor(Color.BLACK);
                g2d.fillOval(10, 10, 8, 8);
                g2d.fillOval(30, 25, 6, 6);
                g2d.fillOval(15, 35, 7, 7);
                break;
            case 3: // Glowing asteroid
                g2d.setColor(new Color(100, 100, 150));
                g2d.fillOval(0, 0, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                g2d.setColor(new Color(0, 255, 255, 100));
                g2d.fillOval(5, 5, OBSTACLE_WIDTH-10, OBSTACLE_HEIGHT-10);
                break;
        }
        g2d.dispose();
        return img;
    }

    private void createDefaultHealthPowerUp() {
        healthPowerUpImage = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = healthPowerUpImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Red cross / heart for health
        g2d.setColor(Color.RED);
        g2d.fillRect(5, 2, 12, 18);
        g2d.fillRect(2, 5, 18, 12);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("+", 6, 17);

        g2d.dispose();
    }

    private void loadSounds() {
        fireSound = loadWavFile("fire.wav");
        collisionSound = loadWavFile("collision.wav");
        explosionSound = loadWavFile("explosion.wav");
        powerUpSound = loadWavFile("powerup.wav");
        gameOverSound = loadWavFile("gameover.wav");
        shieldSound = loadWavFile("shield.wav");
        levelUpSound = loadWavFile("levelup.wav");
        backgroundMusic = loadWavFile("background.wav");

        if (fireSound == null) fireSound = createBeepSound(1000, 50);
        if (collisionSound == null) collisionSound = createBeepSound(300, 200);
        if (explosionSound == null) explosionSound = createExplosionSound();
        if (powerUpSound == null) powerUpSound = createBeepSound(1500, 100);
        if (gameOverSound == null) gameOverSound = createGameOverSound();
        if (shieldSound == null) shieldSound = createBeepSound(800, 300);
        if (levelUpSound == null) levelUpSound = createBeepSound(2000, 200);
    }

    private void playLevelUpSound() {
        if (levelUpSound != null) {
            levelUpSound.setFramePosition(0);
            levelUpSound.start();
        }
    }

    private Clip loadWavFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            return loadClipFromFile(file);
        }
        File soundsFolderFile = new File("sounds/" + filename);
        if (soundsFolderFile.exists()) {
            return loadClipFromFile(soundsFolderFile);
        }
        URL resourceUrl = getClass().getResource("/sounds/" + filename);
        if (resourceUrl == null) {
            resourceUrl = getClass().getResource("/" + filename);
        }
        if (resourceUrl != null) {
            return loadClipFromURL(resourceUrl);
        }
        return null;
    }

    private Clip loadClipFromFile(File file) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private Clip loadClipFromURL(URL url) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private Clip createBeepSound(int frequency, int duration) {
        try {
            int sampleRate = 44100;
            double durationSeconds = duration / 1000.0;
            int numSamples = (int)(durationSeconds * sampleRate);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i / (sampleRate / frequency);
                short sample = (short)(Math.sin(angle) * 32767);
                audioData[i * 2] = (byte)(sample & 0xFF);
                audioData[i * 2 + 1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private Clip createExplosionSound() {
        try {
            int sampleRate = 44100;
            int duration = 300;
            int numSamples = sampleRate * duration / 1000;
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double decay = 1.0 - (double)i / numSamples;
                double noise = random.nextDouble() * 2 - 1;
                short sample = (short)(noise * 32767 * decay);
                audioData[i * 2] = (byte)(sample & 0xFF);
                audioData[i * 2 + 1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private Clip createGameOverSound() {
        try {
            int sampleRate = 44100;
            int duration = 500;
            int numSamples = sampleRate * duration / 1000;
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i / (sampleRate / 200);
                double decrease = 1.0 - (double)i / numSamples;
                short sample = (short)(Math.sin(angle) * 32767 * decrease);
                audioData[i * 2] = (byte)(sample & 0xFF);
                audioData[i * 2 + 1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private void playFireSound() {
        if (fireSound != null) {
            fireSound.setFramePosition(0);
            fireSound.start();
        }
    }

    private void playCollisionSound() {
        if (collisionSound != null) {
            collisionSound.setFramePosition(0);
            collisionSound.start();
        }
    }

    private void playExplosionSound() {
        if (explosionSound != null) {
            explosionSound.setFramePosition(0);
            explosionSound.start();
        }
    }

    private void playPowerUpSound() {
        if (powerUpSound != null) {
            powerUpSound.setFramePosition(0);
            powerUpSound.start();
        }
    }

    private void playGameOverSound() {
        if (gameOverSound != null) {
            gameOverSound.setFramePosition(0);
            gameOverSound.start();
        }
    }

    private void playShieldSound() {
        if (shieldSound != null) {
            shieldSound.setFramePosition(0);
            shieldSound.start();
        }
    }

    private void playBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.setFramePosition(0);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    private void activateShield() {
        if (!shieldActive && !shieldOnCooldown && !isGameOver && !isGameWin) {
            shieldActive = true;
            shieldOnCooldown = true;
            shieldStatusLabel.setText("Shield: ACTIVE!");
            shieldStatusLabel.setForeground(Color.GREEN);
            playShieldSound();

            Timer shieldTimer = new Timer(5000, e -> {
                shieldActive = false;
                shieldStatusLabel.setText("Shield: Cooldown");
                shieldStatusLabel.setForeground(Color.RED);
            });
            shieldTimer.setRepeats(false);
            shieldTimer.start();

            Timer cooldownTimer = new Timer(10000, e -> {
                shieldOnCooldown = false;
                shieldStatusLabel.setText("Shield: Ready");
                shieldStatusLabel.setForeground(Color.CYAN);
            });
            cooldownTimer.setRepeats(false);
            cooldownTimer.start();
        }
    }

    private void levelUp() {
        level++;
        timeLeft = 60;
        health = 100;

        healthBar.setValue(health);
        healthBar.setString(health + "%");
        healthBar.setForeground(Color.GREEN);
        levelLabel.setText("Level: " + level);
        timerLabel.setText("Time: " + timeLeft);
        speedLabel.setText("Speed: " + getCurrentObstacleSpeed());

        obstacles.clear();
        playLevelUpSound();

        String message = "LEVEL " + level + "!\n\n" +
                "Obstacle Speed: " + getCurrentObstacleSpeed() + "\n" +
                "Get ready!";

        JOptionPane.showMessageDialog(this, message, "Level Up!", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addExplosionEffect(int x, int y) {
        explosions.add(new Explosion(x, y, 10));
    }

    private void draw(Graphics g) {
        // Background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Stars with random colors
        for (Star star : stars) {
            g.setColor(star.color);
            g.fillRect(star.x, star.y, star.size, star.size);
            star.update();
        }

        // Explosion effects
        for (int i = 0; i < explosions.size(); i++) {
            Explosion exp = explosions.get(i);
            exp.update();
            g.setColor(new Color(255, 100, 0, exp.alpha));
            g.fillOval(exp.x, exp.y, exp.size, exp.size);
            if (exp.isFinished()) {
                explosions.remove(i);
                i--;
            }
        }

        // Draw player ship (bigger!)
        g.drawImage(playerImage, playerX, playerY, null);

        // Shield effect
        if (shieldActive) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(0, 255, 255, 100));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval(playerX - 20, playerY - 20, PLAYER_WIDTH + 40, PLAYER_HEIGHT + 40);
            g2d.setColor(new Color(0, 255, 255, 50));
            g2d.fillOval(playerX - 20, playerY - 20, PLAYER_WIDTH + 40, PLAYER_HEIGHT + 40);
        }

        // Projectile
        if (isProjectileVisible) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.YELLOW);
            g2d.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            g2d.setColor(Color.ORANGE);
            g2d.fillRect(projectileX + 3, projectileY, PROJECTILE_WIDTH - 6, PROJECTILE_HEIGHT);
            g2d.setColor(Color.RED);
            g2d.fillRect(projectileX + 4, projectileY + 4, PROJECTILE_WIDTH - 8, PROJECTILE_HEIGHT / 2);
        }

        // Draw asteroids from obstacles.png
        for (Obstacle obstacle : obstacles) {
            if (obstacle.type < obstacleSprites.length) {
                g.drawImage(obstacleSprites[obstacle.type], obstacle.x, obstacle.y, null);
            } else {
                // Fallback to first sprite
                g.drawImage(obstacleSprites[0], obstacle.x, obstacle.y, null);
            }
        }

        // Draw health power-ups only
        for (HealthPowerUp powerUp : healthPowerUps) {
            g.drawImage(healthPowerUpImage, powerUp.x, powerUp.y, null);
        }

        // Game messages
        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("GAME OVER!", WIDTH / 2 - 150, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Final Score: " + score, WIDTH / 2 - 80, HEIGHT / 2 + 50);
            g.drawString("Reached Level: " + level, WIDTH / 2 - 80, HEIGHT / 2 + 90);
            g.drawString("Press R to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 130);
        } else if (isGameWin) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("VICTORY!", WIDTH / 2 - 100, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Final Score: " + score, WIDTH / 2 - 80, HEIGHT / 2 + 50);
            g.drawString("Completed " + MAX_LEVEL + " Levels!", WIDTH / 2 - 100, HEIGHT / 2 + 90);
            g.drawString("Press R to Play Again", WIDTH / 2 - 100, HEIGHT / 2 + 130);
        }
    }

    private void update() {
        if (!isGameOver && !isGameWin) {
            int currentSpeed = getCurrentObstacleSpeed();
            double spawnRate = 0.02 + (level - 1) * 0.008;

            // Update obstacles
            for (int i = 0; i < obstacles.size(); i++) {
                Obstacle obstacle = obstacles.get(i);
                obstacle.y += currentSpeed;
                if (obstacle.y > HEIGHT) {
                    obstacles.remove(i);
                    i--;
                }
            }

            // Generate obstacles
            if (Math.random() < spawnRate) {
                int obstacleX = random.nextInt(WIDTH - OBSTACLE_WIDTH);
                int obstacleType = random.nextInt(obstacleSprites.length);
                obstacles.add(new Obstacle(obstacleX, 0, obstacleType));
            }

            // Generate health power-ups only
            if (Math.random() < 0.005) {
                int powerUpX = random.nextInt(WIDTH - 22);
                healthPowerUps.add(new HealthPowerUp(powerUpX, 0));
            }

            // Update health power-ups
            for (int i = 0; i < healthPowerUps.size(); i++) {
                HealthPowerUp powerUp = healthPowerUps.get(i);
                powerUp.y += 2;
                if (powerUp.y > HEIGHT) {
                    healthPowerUps.remove(i);
                    i--;
                }
            }

            // Update projectile
            if (isProjectileVisible) {
                projectileY -= PROJECTILE_SPEED;
                if (projectileY < 0) {
                    isProjectileVisible = false;
                }
            }

            // Collision with obstacles
            Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            for (int i = 0; i < obstacles.size(); i++) {
                Obstacle obstacle = obstacles.get(i);
                Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (playerRect.intersects(obstacleRect)) {
                    if (!shieldActive) {
                        health -= 20;
                        healthBar.setValue(health);
                        healthBar.setString(health + "%");

                        if (health <= 30) {
                            healthBar.setForeground(Color.RED);
                        } else if (health <= 60) {
                            healthBar.setForeground(Color.YELLOW);
                        }

                        playCollisionSound();
                        addExplosionEffect(obstacle.x, obstacle.y);
                        obstacles.remove(i);
                        i--;

                        if (health <= 0) {
                            isGameOver = true;
                            gameTimer.stop();
                            countdownTimer.stop();
                            stopBackgroundMusic();
                            playGameOverSound();
                            break;
                        }
                    } else {
                        playCollisionSound();
                        addExplosionEffect(obstacle.x, obstacle.y);
                        obstacles.remove(i);
                        i--;
                    }
                }
            }

            // Projectile collision with obstacles
            Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            for (int i = 0; i < obstacles.size(); i++) {
                Obstacle obstacle = obstacles.get(i);
                Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (projectileRect.intersects(obstacleRect)) {
                    obstacles.remove(i);
                    score += 10;
                    isProjectileVisible = false;
                    scoreLabel.setText("Score: " + score);
                    playExplosionSound();
                    addExplosionEffect(obstacle.x, obstacle.y);
                    break;
                }
            }

            // Health power-up collection (only health)
            for (int i = 0; i < healthPowerUps.size(); i++) {
                HealthPowerUp powerUp = healthPowerUps.get(i);
                Rectangle powerUpRect = new Rectangle(powerUp.x, powerUp.y, 22, 22);
                if (playerRect.intersects(powerUpRect)) {
                    health = Math.min(100, health + 25);
                    healthBar.setValue(health);
                    healthBar.setString(health + "%");

                    if (health <= 30) {
                        healthBar.setForeground(Color.RED);
                    } else if (health <= 60) {
                        healthBar.setForeground(Color.YELLOW);
                    } else {
                        healthBar.setForeground(Color.GREEN);
                    }

                    healthPowerUps.remove(i);
                    playPowerUpSound();
                    i--;
                }
            }
        }
    }

    private void restartGame() {
        score = 0;
        health = 100;
        timeLeft = 60;
        level = 1;
        shieldActive = false;
        shieldOnCooldown = false;
        isGameOver = false;
        isGameWin = false;

        scoreLabel.setText("Score: 0");
        timerLabel.setText("Time: 60");
        levelLabel.setText("Level: 1");
        shieldStatusLabel.setText("Shield: Ready");
        shieldStatusLabel.setForeground(Color.CYAN);
        speedLabel.setText("Speed: " + getCurrentObstacleSpeed());

        healthBar.setValue(100);
        healthBar.setString("100%");
        healthBar.setForeground(Color.GREEN);

        obstacles.clear();
        healthPowerUps.clear();
        explosions.clear();

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 60;

        gameTimer.start();
        countdownTimer.start();
        playBackgroundMusic();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameOver && !isGameWin) {
            update();
            gamePanel.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if ((isGameOver || isGameWin) && keyCode == KeyEvent.VK_R) {
            restartGame();
            gamePanel.repaint();
            return;
        }

        if (!isGameOver && !isGameWin) {
            if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
                playerX -= PLAYER_SPEED;
            } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
                playerX += PLAYER_SPEED;
            } else if (keyCode == KeyEvent.VK_CONTROL) {
                activateShield();
            } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
                isFiring = true;
                projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
                projectileY = playerY;
                isProjectileVisible = true;
                playFireSound();

                Timer fireTimer = new Timer(300, evt -> {
                    isFiring = false;
                });
                fireTimer.setRepeats(false);
                fireTimer.start();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    // Helper classes
    class Star {
        int x, y, size;
        Color color;
        Star(int x, int y, int size, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
        }
        void update() {
            y += 1;
            if (y > HEIGHT) {
                y = 0;
                x = random.nextInt(WIDTH);
            }
        }
    }

    class Obstacle {
        int x, y, type;
        Obstacle(int x, int y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    class HealthPowerUp {
        int x, y;
        HealthPowerUp(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Explosion {
        int x, y, size, alpha, life;
        Explosion(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.alpha = 255;
            this.life = 10;
        }
        void update() {
            life--;
            alpha = life * 25;
            size += 2;
        }
        boolean isFinished() {
            return life <= 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpaceGame().setVisible(true));
    }
}