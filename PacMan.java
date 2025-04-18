import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;


public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x, y, width, height;
        Image image;
        int startX, startY;
        char direction = 'U';
        int velocityX = 0, velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize / 4;
            } else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize / 4;
            } else if (this.direction == 'L') {
                this.velocityX = -tileSize / 4;
                this.velocityY = 0;
            } else if (this.direction == 'R') {
                this.velocityX = tileSize / 4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image powerFoodImage, wallImage, blueGhostImage, orangeGhostImage, pinkGhostImage, redGhostImage, scaredGhostImage;
    private Image pacmanUpImage, pacmanDownImage, pacmanLeftImage, pacmanRightImage;

    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X        *        X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O  *    bpo    *   O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X        *        X",
        "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'};
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    boolean poweredUp = false;
    long powerUpEndTime = 0;

    // 👻 Blinking ghost state
    boolean showScaredState = false;
    long lastBlinkTime = 0;

    // 🍒 Fruit variables
    Block fruit = null;
    Image cherryImage;
    boolean fruitVisible = false;
    long fruitSpawnTime = 0;
    long fruitDuration = 10000; // 10 seconds
    long fruitCooldown = 20000; // every 20 seconds

     // 🎵 Music
    Clip bgmClip;

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        powerFoodImage = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        scaredGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();
        cherryImage = new ImageIcon(getClass().getResource("./cherry.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        loadMap();
        for (Block ghost : ghosts) {
            ghost.updateDirection(directions[random.nextInt(4)]);
        }

        gameLoop = new Timer(50, this);
        gameLoop.start();

         // 🎵 Play background music
         playMusic("./music.wav.wav");
    }
    public void playMusic(String fileName) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(getClass().getResource(fileName));
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char ch = tileMap[r].charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                if (ch == 'X') walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                else if (ch == 'b') ghosts.add(new Block(blueGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'o') ghosts.add(new Block(orangeGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'p') ghosts.add(new Block(pinkGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'r') ghosts.add(new Block(redGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'P') pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                else if (ch == ' ') foods.add(new Block(null, x + 14, y + 14, 4, 4));
                else if (ch == '*') foods.add(new Block(powerFoodImage, x + 8, y + 8, 16, 16));
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        long currentTime = System.currentTimeMillis();
        for (Block ghost : ghosts) {
            Image ghostToDraw = ghost.image;

            if (poweredUp) {
                if (currentTime > powerUpEndTime - 2000) {
                    if (currentTime - lastBlinkTime >= 200) {
                        lastBlinkTime = currentTime;
                        showScaredState = !showScaredState;
                    }
                    ghostToDraw = showScaredState ? scaredGhostImage : ghost.image;
                } else {
                    ghostToDraw = scaredGhostImage;
                }
            }

            g.drawImage(ghostToDraw, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }

        // 🍒 draw fruit if visible
        if (fruitVisible && fruit != null) {
            g.drawImage(fruit.image, fruit.x, fruit.y, fruit.width, fruit.height, null);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + score, tileSize / 2, tileSize / 2);
        } else {
            g.drawString("x" + lives + " Score: " + score, tileSize / 2, tileSize / 2);
        }
    }

    public void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // 🔁 Side warping for Pac-Man
        if (pacman.x < 0) pacman.x = boardWidth - pacman.width;
        else if (pacman.x + pacman.width > boardWidth) pacman.x = 0;

        if (poweredUp && System.currentTimeMillis() > powerUpEndTime) poweredUp = false;

        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                if (poweredUp) {
                    score += 100;
                    ghost.reset();
                    ghost.updateDirection(directions[random.nextInt(4)]);
                } else {
                    lives--;
                    if (lives == 0) {
                        gameOver = true;
                        return;
                    }
                    resetPositions();
                    break;
                }
            }

            if (ghost.y == tileSize * 9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }

            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;

            // 🔁 Side warping for ghosts
            if (ghost.x < 0) ghost.x = boardWidth - ghost.width;
            else if (ghost.x + ghost.width > boardWidth) ghost.x = 0;

            for (Block wall : walls) {
                if (collision(ghost, wall)) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    ghost.updateDirection(directions[random.nextInt(4)]);
                }
            }
        }

        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                if (food.image == powerFoodImage) {
                    poweredUp = true;
                    powerUpEndTime = System.currentTimeMillis() + 7000;
                }
                score += 10;
            }
        }
        foods.remove(foodEaten);

        // 🍒 Fruit logic
        long currentTime = System.currentTimeMillis();

        if (!fruitVisible && currentTime > fruitSpawnTime + fruitCooldown) {
            int fruitX = tileSize * (1 + random.nextInt(columnCount - 2));
            int fruitY = tileSize * (1 + random.nextInt(rowCount - 2));
            fruit = new Block(cherryImage, fruitX, fruitY, tileSize, tileSize);
            fruitVisible = true;
            fruitSpawnTime = currentTime;
        }

        if (fruitVisible && currentTime > fruitSpawnTime + fruitDuration) {
            fruitVisible = false;
            fruit = null;
        }

        if (fruitVisible && fruit != null && collision(pacman, fruit)) {
            score += 300;
            fruitVisible = false;
            fruit = null;
        }

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.updateDirection(directions[random.nextInt(4)]);
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) gameLoop.stop();
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
        }

        if (e.getKeyCode() == KeyEvent.VK_UP) pacman.updateDirection('U');
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) pacman.updateDirection('D');
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) pacman.updateDirection('L');
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) pacman.updateDirection('R');

        if (pacman.direction == 'U') pacman.image = pacmanUpImage;
        else if (pacman.direction == 'D') pacman.image = pacmanDownImage;
        else if (pacman.direction == 'L') pacman.image = pacmanLeftImage;
        else if (pacman.direction == 'R') pacman.image = pacmanRightImage;
    }
}