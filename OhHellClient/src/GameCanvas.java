import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GameCanvas extends JPanel {
    private static final long serialVersionUID = 1L;
    
    ////// Parameters /////////////////
    private final Font font = new Font("Arial", Font.PLAIN, 13);
    private final Font fontBold = new Font("Arial", Font.BOLD, 13);
    private final int cardSeparation = 40;
    private final double smallCardScale = 0.6667;
    private final double trumpCardScale = 1;
    private final double trickCardScale = 1;
    private final int handYOffset = 85;
    private final int scoreVSpacing = 20;
    private final int scoreMargin = 10;
    private final int trickStayTime = 1500;
    private final int animationTime = 150;
    private final int messageTime = 2000;
    ///////////////////////////////////
    
    private GameClient client;
    
    private List<Player> players;
    private Player myPlayer;

    private int maxHand;
    
    private Card trump;
    
    private int currentClick = -1;
    private int bidMoused = -1;
    private int cardMoused = -1;
    private boolean lastTrickMoused = false;
    
    private boolean paintHandMarker = true;
    private boolean paintBiddingMarker = true;
    private boolean paintTrumpMarker = true;
    private boolean paintPlayersMarker = true;
    private boolean paintMessageMarker = false;
    private boolean paintTakenMarker = true;
    private boolean paintTrickMarker = true;
    
    private String message = "";
    
    private LinkedList<Timer> actionQueue = new LinkedList<Timer>();
    private int cardJustPlayed = 0;
    private boolean animatingTaken = false;
    private double takenTimer = 1;

    private int dealer = -1;
    private int leader;
    
    private boolean showOneCard = false;

    private boolean end = false;
    private String[] endscores;
    
    private BufferedImage deckImg;
    private BufferedImage deckImgDark;
    private BufferedImage deckImgSmall;
    private BufferedImage deckImgSmallDark;
    private BufferedImage tableImg;
    private double cardHeight;
    private double cardWidth;
    private double cardWidthSmall;
    
    private boolean playSoundSelected = false;
    private LinkedList<Timer> audioQueue = new LinkedList<Timer>();
    private Clip cardPlayClip;
    
    private Random random = new Random();
    
    public GameCanvas(GameClient client) {
        this.client = client;
        
        deckImg = loadImage("resources/deck2.png");
        deckImgDark = loadImage("resources/deck2.png");
        Graphics didg = deckImgDark.getGraphics();
        didg.setColor(new Color(127, 127, 127, 64));
        didg.fillRect(0, 0, deckImg.getWidth(), deckImg.getHeight());
        
        deckImgSmall = loadImage("resources/deck2small.png");
        deckImgSmallDark = loadImage("resources/deck2small.png");
        Graphics disdg = deckImgSmallDark.getGraphics();
        disdg.setColor(new Color(127, 127, 127, 64));
        disdg.fillRect(0, 0, deckImg.getWidth(), deckImg.getHeight());
        
        tableImg = loadImage("resources/table.jpg");
        cardWidth = (double) deckImg.getWidth() / 9;
        cardHeight = (double) deckImg.getHeight() / 6;
        cardWidthSmall = cardWidth * smallCardScale;
        
        cardPlayClip = loadSound("resources/Card play.wav");
    }
    
    public BufferedImage loadImage(String file) {
        BufferedImage img;
        URL imgurl = getClass().getResource("/" + file);
        try {
            if (imgurl != null) {
                img = ImageIO.read(imgurl);
            } else {
                img = ImageIO.read(new File(file));
            }
            return img;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Clip loadSound(String file) {
        try {
            Clip clip = AudioSystem.getClip();
            URL soundUrl = getClass().getResource("/" + file);
            if (soundUrl != null) {
                clip.open(AudioSystem.getAudioInputStream(soundUrl));
            } else {
                clip.open(AudioSystem.getAudioInputStream(new File(file)));
            }
            //playSound(clip);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(tableImg, 
                0, 0, 
                Math.max(getWidth()-450, tableImg.getWidth()), 
                Math.max(getHeight(), tableImg.getHeight()), 
                0, 0, 
                tableImg.getWidth(), tableImg.getHeight(), null);
        graphics.setColor(Color.BLACK);

        if (paintBiddingMarker) {
            paintBidding(graphics);
        }
        if (paintTrumpMarker) {
            paintTrump(graphics);
        }
        if (paintPlayersMarker) {
            paintPlayers(graphics);
        }
        if (paintMessageMarker) {
            paintMessage(graphics);
        }
        paintScores(graphics);
        if (paintTakenMarker) {
            paintTaken(graphics);
        }
        if (paintTrickMarker) {
            paintTrick(graphics);
        }
        paintChat(graphics);
        
        getComponents()[0].setBounds((getWidth() - 450) / 2 - 50, getHeight() - 100, 100, 30);
        getComponents()[3].setBounds((getWidth() - 450) / 2 + 100, getHeight() - 100, 70, 30);
        
        if (end) {
            paintFinalScores(graphics);
        }
        
    }
    
    public String fitString(Graphics graphics, String text, double wid) {
        graphics.setFont(fontBold);
        FontMetrics m = graphics.getFontMetrics();
        for (int i = 0; i < text.length(); i++) {
            String s0 = text.substring(0, text.length() - i).trim();
            if (m.stringWidth(s0) < wid) {
                return s0;
            }
        }
        return text.charAt(0) + "";
    }
    
    public void paintTrump(Graphics graphics) {
        graphics.setColor(Color.BLACK);
        double scaleFix = 1;
        if (players.size() >= 6 && getHeight() < 600) {
            scaleFix = 0.75;
        }
        drawCard(graphics, new Card(), 46, 62, trumpCardScale * scaleFix, true, true);
        drawCard(graphics, new Card(), 48, 64, trumpCardScale * scaleFix, true, true);
        drawCard(graphics, trump, 50, 66, trumpCardScale * scaleFix, true, false);
    }
    
    public void paintBidding(Graphics graphics) {
        if (actionQueue.isEmpty()) {
            graphics.setColor(Color.BLACK);
            if (myPlayer.getBidding() != 0) {
                for (int i = 0; i <= myPlayer.getHand().size(); i++) {
                    if (i == bidMoused && !cannotBid(i)) {
                        graphics.setColor(new Color(192, 192, 192));
                    } else if (!cannotBid(i)) {
                        graphics.setColor(Color.WHITE);
                    } else {
                        graphics.setColor(new Color(96, 96, 96));
                    }
                    graphics.fill3DRect(
                            (getWidth() - 450) / 2 + i * 40 
                                - myPlayer.getHand().size() * 40 / 2 - 15, 
                            getHeight() - 210 - 15, 
                            30, 
                            30, 
                            true);
                    graphics.setColor(Color.BLACK);
                    drawStringJustified(graphics, 
                            Integer.toString(i), 
                            (getWidth() - 450) / 2 + i * 40 - myPlayer.getHand().size() * 40 / 2, 
                            getHeight() - 210, 
                            1, 1);
                }
            }
        }
    }
    
    public void paintMessage(Graphics graphics) {
        drawStringJustifiedBacked(graphics, message, (getWidth() - 450) / 2, getHeight() / 2);
    }
    
    public void paintTrick(Graphics graphics) {
        graphics.setColor(Color.BLACK);
        
        for (int i = 0; i < players.size(); i++) {
            int iRelToLeader = (leader + i) % players.size();
            int iRelToMe = (iRelToLeader - myPlayer.getIndex() + players.size()) % players.size();
            Player player = players.get(iRelToLeader);
            if (!player.isKicked() && !player.getTrick().isEmpty()) {
                if (player.getTrickRad() == -1) {
                    player.setTrickRad((int) (70 + 10 * random.nextDouble()));
                }
                
                int[] pos = playerPos((iRelToMe + players.size() - 1)%players.size());
                double startx = pos[0];
                double starty = pos[1];
                
                if (iRelToLeader == myPlayer.getIndex()) {
                    startx = (getWidth() - 450) / 2
                            + cardJustPlayed * cardSeparation
                            - (myPlayer.getHand().size()) * cardSeparation / 2;
                    starty = getHeight() - handYOffset;
                }
                
                double endx = (getWidth() - 450) / 2
                        - player.getTrickRad()
                            * Math.sin(2 * Math.PI * iRelToMe / players.size());
                double endy = getHeight()/2 - 50
                        + player.getTrickRad() 
                            * Math.cos(2 * Math.PI * iRelToMe / players.size());
                
                int x = (int) (player.getTrickTimer() * endx
                        + (1 - player.getTrickTimer()) * startx);
                int y = (int) (player.getTrickTimer() * endy
                        + (1 - player.getTrickTimer()) * starty);
                drawCard(graphics, player.getTrick(), x, y, trickCardScale, true, false);
            }
        }
    }
    
    public void paintPlayers(Graphics graphics) {
        graphics.setColor(Color.BLACK);
        
        double maxWid = (maxHand - 1) * 10 + cardWidthSmall;
        for (Player player : players) {
            int[] ppos = playerPos((player.getIndex() 
                            - myPlayer.getIndex() 
                            + players.size() - 1) % players.size());
            int x = ppos[0];
            int y = ppos[1];
            int pos = ppos[2];
            
            if (paintHandMarker) {
                boolean myHand = player.equals(myPlayer);
                int handsize = player.getHand().size();
                for (int i = 0; i < handsize; i++) {
                    int yOffset = 40;
                    int separation = 10;
                    boolean dark = false;
                    if (myHand) {
                        yOffset = handYOffset;
                        separation = cardSeparation;
                        if (myPlayer.isPlaying() && i == cardMoused && canPlayThis(i)) {
                            yOffset = handYOffset + 10;
                            dark = true;
                        }
                    }
                    Card card = player.getHand().get(i);
                    if (myHand && myPlayer.getIndex() == dealer 
                            && !myPlayer.isKibitzer() 
                            && client.isOneRound() 
                            && !showOneCard) {
                        card = new Card();
                    }
                    drawCard(graphics, 
                            card, 
                            (int)(x + i * separation 
                                    - (handsize - 1) * separation / 2 
                                    - (pos - 1) * maxWid / 2), 
                            y-yOffset, 
                            myHand ? 1 : smallCardScale, 
                            !myHand, 
                            dark);
                }
            }
            
            if (player.getBidding()==1 || player.isPlaying()) {
                graphics.setColor(new Color(255, 255, 0));
            } else {
                graphics.setColor(Color.WHITE);
            }
            graphics.fillRoundRect((int) (x - pos * maxWid / 2), y - 10, (int) maxWid, 20, 20, 20);
            
            graphics.setColor(Color.BLACK);
            if (player.isDisconnected()) {
                graphics.setColor(Color.GRAY);
            }
            if (player.isKicked()) {
                graphics.setColor(Color.RED);
            }
            drawStringJustified(graphics, 
                    fitString(graphics, player.getName(), maxWid), 
                    (int)(x - (pos - 1) * maxWid / 2), 
                    y, 
                    1, 1);
            
            if (player.getIndex() == dealer) {
                graphics.setColor(Color.CYAN);
                graphics.fillOval((int) (x - (pos - 2) * maxWid / 2) - 19, y - 8, 16, 16);
                graphics.setColor(Color.BLACK);
                drawStringJustified(graphics, "D", 
                        (int) (x - (pos - 2) * maxWid / 2) - 11, 
                        y, 
                        1, 1);
            }
        }
    }
    
    public void paintTaken(Graphics graphics) {
        for (Player player : players) {
            for (int j = 0; j < player.getTaken(); j++) {
                int[] ppos = playerPos((player.getIndex() 
                        - myPlayer.getIndex() 
                        + players.size() - 1) % players.size());
                int takenX = ppos[3];
                int takenY = ppos[4];
                
                boolean isLastTrick = player.getIndex() == leader && j == player.getTaken()-1;
                boolean mouseOver = lastTrickMoused && isLastTrick;
                
                boolean dark = false;
                if (mouseOver) dark = true;
                
                double timer = 1;
                if (isLastTrick) timer = takenTimer;
                
                int x = takenX + 10 * j;
                int y = takenY + 5 * j;
                if (animatingTaken && isLastTrick) {
                    x = (int) (timer * (takenX + 10 * j)
                            + (1 - timer) * (getWidth() - 450) / 2);
                    y = (int) (timer * (takenY + 5 * j)
                            + (1 - timer) * getHeight()/2);
                }
                
                drawCard(graphics, new Card(), x, y, smallCardScale, true, dark);
                
                if (!animatingTaken && mouseOver) {
                    List<Player> unkickedPlayers = players.stream()
                            .filter(p -> !p.isKicked())
                            .collect(Collectors.toList());
                    for (int k = 0; k < unkickedPlayers.size(); k++) {
                        drawCard(graphics, 
                                unkickedPlayers.get(k).getLastTrick(), 
                                takenX + 10 * j + 50 + 20*k, 
                                takenY + 5 * j, 
                                1, true, false);
                    }
                }
            }
        }
    }
    
    public int[] playerPos(int index) {
        int cut1 = (players.size() - 1) / 3;
        int cut2 = 2 * cut1;
        if ((players.size() - 1) % 3 != 0) {
            cut2++;
        }
        if ((players.size() - 1) % 3 == 2) {
            cut1++;
        }
        
        int x;
        int y;
        int pos;
        int takenX;
        int takenY;
        if (index < cut1) {
            x = 10;
            y = getHeight() - getHeight() * (index + 1) / (cut1 + 1);
            pos = 0;
            takenX = x + 20;
            takenY = y + 50;
        } else if (index < cut2) {
            x = (getWidth() - 450) * (index - cut1 + 1) / (cut2 - cut1 + 1);
            y = 85;
            pos = 1;
            takenX = x + 110;
            takenY = y - 35;
        } else if (index < players.size() - 1) {
            x = getWidth() - 450 - 10;
            y = getHeight() * (index - cut2 + 1) / (players.size() - 1 - cut2 + 1);
            pos = 2;
            takenX = x - 90;
            takenY = y + 50;
        } else {
            x = (getWidth() - 450) / 2;
            y = getHeight() - 20;
            pos = 1;
            takenX = x + 260;
            takenY = y - 50;
        }
        return new int[] {x, y, pos, takenX, takenY};
    }
    
    public void paintScores(Graphics graphics) {
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(getWidth() - 450, 0, 450, getHeight());
        
        List<int[]> rounds = client.getRounds();
        int numRounds = rounds.size();
        
        graphics.setColor(Color.WHITE);
        graphics.fillRect(
                getWidth() - (450 - scoreMargin), 
                scoreMargin, 
                450 - 2 * scoreMargin, 
                scoreVSpacing * (numRounds + 1) + 5);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(
                getWidth() - (450 - scoreMargin), 
                scoreMargin, 
                450 - 2 * scoreMargin, 
                scoreVSpacing * (numRounds + 1) + 5);
        
        double wid = (double) (450 - 2 * scoreMargin - 50) / players.size();

        graphics.setColor(Color.BLACK);
        graphics.drawLine(
                getWidth() - (450 - scoreMargin - 45), 
                scoreMargin + scoreVSpacing, 
                getWidth() - scoreMargin - 5, 
                scoreMargin + scoreVSpacing);
        for (Player player : players) {
            int index = player.getIndex();
            graphics.setColor(Color.BLACK);
            if (index > 0) {
                graphics.drawLine(
                        (int) (getWidth() - (450 - scoreMargin - 45) + index * wid), 
                        scoreMargin + 5, 
                        (int) (getWidth() - (450 - scoreMargin - 45) + index * wid), 
                        scoreMargin + scoreVSpacing * (numRounds + 1));
            }
            
            graphics.setColor(Color.BLACK);
            if (player.isDisconnected()) graphics.setColor(Color.GRAY);
            if (player.isKicked()) graphics.setColor(Color.RED);
            
            if (player.equals(myPlayer)) {
                drawStringJustifiedBold(graphics, 
                        fitString(graphics, player.getName(), wid - 2), 
                        (int) (getWidth() - (450 - scoreMargin - 45) + index * wid + wid / 2), 
                        scoreMargin + 15, 
                        1, 0);
            } else {
                drawStringJustified(graphics, 
                        fitString(graphics, player.getName(), wid - 2), 
                        (int) (getWidth() - (450 - scoreMargin - 45) + index * wid + wid / 2), 
                        scoreMargin + 15, 
                        1, 0);
            }
        }
        
        graphics.setColor(Color.BLACK);
        for (int i = 0; i < numRounds; i++) {
            int[] round = rounds.get(i);
            drawStringJustified(graphics, 
                    players.get(round[0]).getName().substring(0, 1), 
                    getWidth() - (450 - scoreMargin - 5), 
                    scoreMargin + scoreVSpacing * (i + 2), 
                    0, 0);
            drawStringJustified(graphics, 
                    "" + round[1], 
                    getWidth() - (450 - scoreMargin - 25), 
                    scoreMargin + scoreVSpacing * (i + 2), 
                    0, 0);
        }
    
        for (Player player : players) {
            int index = player.getIndex();
            for (int j = 0; j < player.getBids().size(); j++) {
                drawStringJustified(graphics, 
                        Integer.toString(player.getBids().get(j)), 
                        (int) (getWidth() - (450 - scoreMargin - 45) + (index + 1) * wid - 10), 
                        scoreMargin + scoreVSpacing * (2 + j), 
                        1, 0);
                graphics.drawOval(
                        (int) (getWidth() - (450 - scoreMargin - 45) + (index + 1) * wid - 18), 
                        scoreMargin + 5 + scoreVSpacing * (1 + j) + 2, 
                        16, 16);
            }
            for (int j = 0; j < player.getScores().size(); j++) {
                drawStringJustified(graphics, 
                        Integer.toString(player.getScores().get(j)), 
                        (int) (getWidth() - (450 - scoreMargin - 45) + index * wid + 10), 
                        scoreMargin + scoreVSpacing * (2 + j), 
                        0, 0);
            }
        }
    }
    
    public void paintChat(Graphics graphics) {
        int numRounds = client.getRounds().size();
        int chatY = Math.max(
                2 * scoreMargin + scoreVSpacing * (numRounds + 1) + 5, 
                getHeight() - 300);
        getComponents()[1].setBounds(
                getWidth() - 450 + scoreMargin, 
                chatY, 
                450 - 2 * scoreMargin, 
                getHeight() - chatY - 40 - scoreMargin);
        getComponents()[2].setBounds(
                getWidth() - 450 + scoreMargin, 
                getHeight() - 35 - scoreMargin, 
                450 - 2 * scoreMargin, 
                30);
    }
    
    public void paintFinalScores(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(
                (getWidth() - 450) / 2 - 150, 
                getHeight() / 2 - (players.size() + 1) * 15 / 2 - 56, 
                300, 
                (players.size() + 1) * 15 + 100, 20, 20);
        
        graphics.setColor(Color.BLACK);
        drawStringJustifiedBold(graphics, 
                "Final scores: ", 
                (getWidth() - 450) / 2, 
                getHeight() / 2 - (players.size() + 1) * 15 / 2,
                1, 1);
        for (int i = 0; i < players.size(); i++) {
            drawStringJustified(graphics, 
                    fitString(
                            graphics, 
                            (i + 1) + ". " 
                            + endscores[2 * i], 
                            200) + ", " 
                        + endscores[2 * i + 1] 
                        + " points",
                    (getWidth() - 450) / 2, 
                    getHeight() / 2 - (players.size() + 1) * 15 / 2 + 15 * (i + 1), 
                    1, 1);
        }
    }
    
    public void drawStringJustified(Graphics graphics, String text, int x, int y, int posX, 
            int posY) {
        graphics.setFont(font);
        FontMetrics m = graphics.getFontMetrics();
        graphics.drawString(text, x - m.stringWidth(text) * posX / 2, y + m.getHeight() * posY / 3);
    }
    
    public void drawStringJustifiedBold(Graphics graphics, String text, int x, int y, int posX, 
            int posY) {
        graphics.setFont(fontBold);
        FontMetrics m = graphics.getFontMetrics();
        graphics.drawString(text, x - m.stringWidth(text) * posX / 2, y + m.getHeight() * posY / 3);
    }
    
    public void drawStringJustifiedBacked(Graphics graphics, String text, int x, int y) {
        graphics.setFont(font);
        FontMetrics m = graphics.getFontMetrics();
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(x - m.stringWidth(text) / 2 - 20, 
                y - m.getHeight() / 3 - 10, 
                m.stringWidth(text) + 40, 
                m.getHeight() + 20, 
                20, 20);
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x - m.stringWidth(text) / 2, y + m.getHeight() / 3);
    }
    
    public void drawCard(Graphics graphics, Card card, int x, int y, double scale, boolean small, 
            boolean dark) {
        int cardNumber = card.toNumber();
        int col = cardNumber % 9;
        int row = (cardNumber - col) / 9;
        
        BufferedImage img = deckImg;
        if (small && !dark) {
            img = deckImgSmall;
        } else if (!small & dark) {
            img = deckImgDark;
        } else if (small & dark) {
            img = deckImgSmallDark;
        }
        
        double cw1 = cardWidth;
        double ch1 = cardHeight;
        if (small) {
            cw1 = cardWidth * deckImgSmall.getWidth() / deckImg.getWidth();
            ch1 = cardHeight * deckImgSmall.getWidth() / deckImg.getWidth();
        }
        
        graphics.drawImage(img, 
                (int) (x - cw1 * scale / 2), (int) (y - ch1 * scale / 2), 
                (int) (x + cw1 * scale / 2), (int) (y + ch1 * scale / 2), 
                (int) (col * cw1), (int) (row * ch1), 
                (int) ((col + 1) * cw1), (int) ((row + 1) * ch1), 
                null);
    }
    
    public void doShowOneCard() {
        showOneCard = true;
    }
    
    public void reset() {
        players = client.getPlayers();
        myPlayer = client.getMyPlayer();
        trump = new Card();
        
        maxHand = Math.min(10, 52 / players.size());
        message = "";
        paintHandMarker = true;
        paintBiddingMarker = true;
        paintTrumpMarker = true;
        paintPlayersMarker = true;
        paintMessageMarker = false;
        paintTakenMarker = true;
        paintTrickMarker = true;
        end = false;
    }
    
    public void setDealer(int dealer) {
        this.dealer = dealer;
    }
    
    public void setLeader(int leader) {
        this.leader = leader;
    }
    
    public void mousePressed(int x, int y) {
        if (myPlayer.getBidding() != 0) {
            currentClick = bidClick(x, y);
        } else if (myPlayer.isPlaying()) {
            currentClick = cardClick(x, y);
        }
    }
    
    public void mouseReleased(int x, int y) {
        if (actionQueue.size() == 0) {
            if (myPlayer.getBidding() != 0) {
                int bid = bidClick(x, y);
                if (bid != -1 && !cannotBid(bid) && bid == currentClick) {
                    myPlayer.setBidding(0);
                    client.sendCommand("BID:" + bid);
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            } else if (myPlayer.isPlaying()) {
                int cardIndex = cardClick(x, y);
                if (cardIndex != -1 && canPlayThis(cardIndex) && cardIndex == currentClick) {
                    myPlayer.setPlaying(false);
                    cardJustPlayed = cardIndex;
                    Card card = myPlayer.getHand().get(cardIndex);
                    client.sendCommand("PLAY:" + card.toString());
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    getComponents()[3].setVisible(false);
                }
            }
        }
    }
    
    public void setPlaySoundSelected(boolean playSoundSelected) {
        this.playSoundSelected = playSoundSelected;
    }
    
    public void playSound(Clip clip) {
        double length = (double) clip.getFrameLength() / 44.1;
        clip.start();
        new CanvasTimerEntry((long) length, this, audioQueue) {
            @Override
            public void onLastAction() {
                clip.stop();
                clip.setFramePosition(0);
            }
        };
    }
    
    public void mouseMoved(int x, int y) {
        int oldBidMoused = bidMoused;
        int oldCardMoused = cardMoused;
        boolean oldLastTrickMoused = lastTrickMoused;
        
        if (myPlayer.getBidding() != 0) {
            bidMoused = bidClick(x, y);
        } else {
            bidMoused = -1;
        }
        if (myPlayer.isPlaying()) {
            cardMoused = cardClick(x, y);
        } else {
            cardMoused = -1;
        }
        
        lastTrickMoused = lastTrickClick(x, y);
        
        if ((bidMoused != -1 && !cannotBid(bidMoused))
                || (cardMoused != -1 && canPlayThis(cardMoused))) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        
        if (oldBidMoused != bidMoused 
                || oldCardMoused != cardMoused 
                || oldLastTrickMoused != lastTrickMoused) {
            repaint();
        }
    }
    
    public int bidClick(int x, int y) {
        for (int i = 0; i <= myPlayer.getHand().size(); i++) {
            int x1 = (getWidth() - 450) / 2
                    + i * 40
                    - myPlayer.getHand().size() * 40 / 2 - 15;
            int y1 = getHeight() - 210 - 15;
            if (x >= x1 && y >= y1 && x <= x1 + 30 && y <= y1 + 30) {
                return i;
            }
        }
        return -1;
    }
    
    public int cardClick(int x, int y) {
        for (int i = 0; i < myPlayer.getHand().size(); i++) {
            int cardX = (getWidth() - 450) / 2
                    + i * cardSeparation
                    - (myPlayer.getHand().size() - 1) * cardSeparation / 2;
            int cardY = getHeight() - handYOffset;
            int x1 = cardX - (int) (cardWidth / 2);
            int y1 = cardY - (int) (cardHeight / 2);
            int x2 = x1 + cardSeparation;
            int y2 = y1 + (int) cardHeight;
            if (i == myPlayer.getHand().size() - 1) {
                x2 = x1 + (int) cardWidth;
            }
            if (x >= x1 && y >= y1 && x <= x2 && y <= y2) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean lastTrickClick(int x, int y) {
        if (players.get(leader).getTaken() == 0) {
            return false;
        } else {
            int[] ppos = playerPos((leader - myPlayer.getIndex()
                    + players.size() - 1) % players.size());
            int takenX = ppos[3] + 10 * (players.get(leader).getTaken() - 1);
            int takenY = ppos[4] + 5 * (players.get(leader).getTaken() - 1);
            return (Math.abs(takenX - x) <= 0.2 * cardWidth) 
                    && (Math.abs(takenY - y) <= 0.2 * cardHeight);
        }
    }
    
    public boolean cannotBid(int bid) {
        int totalBid = players.stream()
                .map(p -> p.getBid())
                .reduce(0, (sofar, b) -> sofar + b);
        return (myPlayer.getIndex() == dealer) 
                && (totalBid + bid == myPlayer.getHand().size());
    }
    
    public boolean canPlayThis(int cardIndex) {
        String ledSuit = players.get(leader).getTrick().getSuit();
        return players.get(leader).getTrick().isEmpty()
                || myPlayer.getHand().get(cardIndex).getSuit().equals(ledSuit)
                || myPlayer.getHand().stream().noneMatch(c -> c.getSuit().equals(ledSuit));
    }
    
    public LinkedList<Timer> getactionQueue() {
        return actionQueue;
    }
    
    public void animateCardPlay(int index) {
        new CanvasTimerEntry(animationTime, this, actionQueue) {
            @Override
            public void onFirstAction() {
                if (playSoundSelected) {
                    playSound(cardPlayClip);
                }
                players.get(index).setTrickTimer(0);
            }
            
            @Override
            public void onAction() {
                players.get(index).setTrickTimer(
                        Math.min((double) this.getElapsedTime() / animationTime, 1));
                repaint();
            }
        };
    }
    
    public void animateTrickTake(int index) {
        new CanvasTimerEntry(trickStayTime, this, actionQueue) {
            @Override
            public void onLastAction() {
                for (Player player : players) {
                    player.resetTrick();
                    setLeader(index);
                }
            }
        };
        new CanvasTimerEntry(animationTime, this, actionQueue) {
            @Override
            public void onFirstAction() {
                players.get(index).incrementTaken();
                lastTrickMoused = false;
                takenTimer = 0;
                animatingTaken = true;
            }
            
            @Override
            public void onAction() {
                takenTimer = Math.min((double) this.getElapsedTime() / animationTime, 1);
                repaint();
            }
            
            @Override
            public void onLastAction() {
                animatingTaken = false;
            }
        };
    }
    
    public void showMessageOnTimer(String text) {
        new CanvasTimerEntry(messageTime, this, actionQueue) {
            @Override
            public void onFirstAction() {
                message = text;
                paintMessageMarker = true;
            }
            
            @Override
            public void onAction() {
                repaint();
            }
            
            @Override
            public void onLastAction() {
                message = "";
                paintMessageMarker = false;
            }
        };
    }
    
    public void showResultMessageOnTimer() {
        new CanvasTimerEntry(messageTime, this, actionQueue) {
            @Override
            public void onFirstAction() {
                if (myPlayer.getBid() == myPlayer.getTaken()) {
                    message = "You made your bid!";
                } else {
                    message = "You went down "
                            + Math.abs(myPlayer.getBid() - myPlayer.getTaken()) + ".";
                }
                paintMessageMarker = true;
            }
            
            @Override
            public void onAction() {
                repaint();
            }
            
            @Override
            public void onLastAction() {
                message = "";
                paintMessageMarker = false;
            }
        };
    }
    
    public void resetPlayersOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                for (Player player : players) {
                    player.setBid(0);
                    player.setTaken(0);
                }
            }
        };
    }
    
    public void setHandOnTimer(int index, List<Card> hand) {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                players.get(index).setHand(hand);
                if (index == dealer 
                        && index == myPlayer.getIndex() 
                        && !myPlayer.isKibitzer() 
                        && client.isOneRound()) {
                    getComponents()[3].setVisible(true);
                    showOneCard = false;
                }
            }
        };
    }
    
    public void setLeaderOnTimer(int leaderI) {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                leader = leaderI;
            }
        };
    }
    
    public void setTrumpOnTimer(Card card) {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                trump = card;
            }
        };
    }
    
    public void setFinalScoresOnTimer(LinkedList<String> scores) {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                paintHandMarker = false;
                paintBiddingMarker = false;
                paintTrumpMarker = false;
                paintPlayersMarker = false;
                paintMessageMarker = false;
                paintTakenMarker = false;
                paintTrickMarker = false;
                end = true;
                endscores = new String[scores.size()];
                for (int i = 0; i < scores.size(); i++) {
                    endscores[i] = scores.get(i);
                }
                getComponents()[0].setVisible(true);
            }
        };
    }
}