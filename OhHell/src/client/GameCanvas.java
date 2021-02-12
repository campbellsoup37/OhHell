package client;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.sound.sampled.Clip;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

import core.Card;
import graphics.OhcGraphicsTools;
import graphics.OhcScrollPane;
import graphics.OhcTextField;

public class GameCanvas extends OhcCanvas {
    private static final long serialVersionUID = 1L;
    
    ////// Parameters /////////////////
    public static final int cardSeparation = 40;
    public static final double smallCardScale = 0.6667;
    public static final double trumpCardScale = 0.6667;
    public static final double trickCardScale = 1;
    public static final int handYOffset = 105;
    public static final int takenXSeparation = 10;
    public static final int takenYSeparation = 5;
    public static final int lastTrickSeparation = 20;
    
    public static final int scoreVSpacing = 20;
    public static final int scoreMargin = 10;
    public static final int bidStayTime = 1000;
    public static final int trickStayTime = 1500;
    public static final int animationTime = 150;
    public static final int messageTime = 2000;
    
    public static final int finalScoreMaxWidth = 900;
    public static final int finalScoreMaxHeight = 500;
    public static final int finalScoreOuterMargin = 25;
    public static final int finalScoreInnerMargin = 10;
    public static final int finalScoreListWidth = 200;
    public static final int finalScoreBottomMargin = 150;
    
    public static final double pointSize = 4;
    public static final Color[] colors = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.MAGENTA,
            Color.CYAN,
            Color.ORANGE,
            Color.PINK,
            Color.YELLOW,
            Color.DARK_GRAY
    };
    
    public static final int pokeWaitTime = 25000;
    ///////////////////////////////////
    
    private GameClient client;
    
    private List<ClientPlayer> players;
    private ClientPlayer myPlayer;

    private int maxHand;
    private double maxWid;
    
    private int numRobots = 0;
    private boolean doubleDeck = false;
    
    private Card trump;
    private List<Card> trumps;
    
    private boolean paintMessageMarker = false;
    
    private String message = "";
    private String messageState = "UNBLOCKED";
    
    private List<CanvasEmbeddedSwing> embeddedSwings = new LinkedList<>();
    private List<CanvasButton> bidButtons = new LinkedList<>();
    private List<CanvasCard> cardInteractables = new LinkedList<>();
    private CanvasCard lastTrick;
    private List<CanvasButton> kickButtons = new LinkedList<>();
    private List<CanvasButton> buttons = new LinkedList<>();

    private JTextArea chatArea = new JTextArea();
    
    private LinkedList<Timer> actionQueue = new LinkedList<>();
    private boolean performingAction = false;
    
    private boolean trickTaken = false;
    private int cardJustPlayed = 0;
    private boolean animatingTaken = false;
    private double takenTimer = 1;
    
    private boolean canUndoBid = false;

    private int claimer = -1;
    
    private int dealer = -1;
    private int leader;
    
    private boolean showOneCard = false;

    private CanvasPostGamePage postGamePage;
    private List<ClientPlayer> postGamePlayers;
    private List<int[]> postGameRounds;
    
    private GameState state = GameState.PREGAME;
    
    private BufferedImage deckImg;
    private BufferedImage deckImgDark;
    private BufferedImage deckImgSmall;
    private BufferedImage deckImgSmallDark;
    private double cardHeight;
    private double cardWidth;
    private double cardWidthSmall;
    
    private boolean aiHelpSelected = false;
    private boolean stopped = false;
    private LinkedList<Timer> audioQueue = new LinkedList<>();
    private Clip cardPlayClip;
    private Clip pokeClip;
    
    private long pokeTime = 0;
    
    private Random random = new Random();
    
    private final int frameQueueSize = 100;
    private long currentTime = 0;
    private int framePointer = 0;
    private double frameTotalTime = 0;
    private double[] frameTimes = new double[frameQueueSize];
    
    public GameCanvas(GameClient client) {
        super(client);
        this.client = client;
        
        deckImg = OhcGraphicsTools.loadImage("resources/client/deck2.png", this);
        deckImgDark = OhcGraphicsTools.loadImage("resources/client/deck2.png", this);
        Graphics didg = deckImgDark.getGraphics();
        didg.setColor(new Color(127, 127, 127, 64));
        didg.fillRect(0, 0, deckImg.getWidth(), deckImg.getHeight());
        
        deckImgSmall = OhcGraphicsTools.loadImage("resources/client/deck2small.png", this);
        deckImgSmallDark = OhcGraphicsTools.loadImage("resources/client/deck2small.png", this);
        Graphics disdg = deckImgSmallDark.getGraphics();
        disdg.setColor(new Color(127, 127, 127, 64));
        disdg.fillRect(0, 0, deckImg.getWidth(), deckImg.getHeight());
        
        cardWidth = (double) deckImg.getWidth() / 9;
        cardHeight = (double) deckImg.getHeight() / 6;
        cardWidthSmall = cardWidth * smallCardScale;
        
        cardPlayClip = OhcGraphicsTools.loadSound("resources/client/Card play.wav", this);
        pokeClip = OhcGraphicsTools.loadSound("resources/client/shortpoke.wav", this);
        
        players = new ArrayList<>();
    }
    
    public double getCardWidth(boolean small) {
        return small ? cardWidthSmall : cardWidth;
    }
    
    public double getCardHeight(boolean small) {
        return small ? cardWidthSmall * cardHeight / cardWidth : cardHeight;
    }
    
    @Override
    public void customPaint(Graphics graphics) {
        //maxHand = Math.max(maxHand, myPlayer.getHand().size());
        
        graphics.setColor(Color.BLACK);

        paintPregame(graphics);
        paintTrump(graphics);
        paintPlayers(graphics);
        paintScores(graphics);
        paintTaken(graphics);
        paintTrick(graphics);
        if (paintMessageMarker) {
            paintMessage(graphics);
        }
        
        if (currentTime == 0) {
            currentTime = System.nanoTime();
        } else {
            long oldTime = currentTime;
            currentTime = System.nanoTime();
            double newFrameTime = (double) (currentTime - oldTime) / 1000000;
            frameTotalTime += newFrameTime - frameTimes[framePointer];
            frameTimes[framePointer] = newFrameTime;
            framePointer = (framePointer + 1) % frameQueueSize;
            if (frameTimes[frameQueueSize - 1] != 0 && client.showFpsSelected()) {
                OhcGraphicsTools.drawStringJustified(graphics, "FPS: " + String.format("%.2f", (double) 1000 * frameQueueSize / frameTotalTime), getWidth() - 450 - 100, 10, 0, 1);
            }
        }
        if (client.showPingSelected()) {
            OhcGraphicsTools.drawStringJustified(graphics, "Ping: " + String.format("%.2f", client.getPing()) + "ms", getWidth() - 450 - 100, 25, 0, 1);
        }
        
        if (!performingAction && !actionQueue.isEmpty()) {
            performingAction = true;
            actionQueue.remove().start();
        }
    }
    
    public void paintPregame(Graphics graphics) {
        if (state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME) {
            graphics.setColor(new Color(255, 255, 255, 180));
            OhcGraphicsTools.drawBox(graphics, 
                    (getWidth() - 450) / 2 - 200, 
                    getHeight() / 2 - 150, 
                    400, 300, 20);
            graphics.setColor(new Color(255, 255, 255, 210));
            OhcGraphicsTools.drawBox(graphics, 
                    (getWidth() - 450) / 2 + 20, 
                    getHeight() / 2 - 15, 
                    80, 30, 20);
            graphics.setColor(Color.BLACK);
            graphics.setFont(OhcGraphicsTools.fontBold);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    "Name:", 
                    (getWidth() - 450) / 2 - 140, 
                    getHeight() / 2 - 105, 
                    2, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    "Join as kibitzer:", 
                    (getWidth() - 450) / 2 - 20, 
                    getHeight() / 2 - 60, 
                    2, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    "Robots:", 
                    (getWidth() - 450) / 2 - 20, 
                    getHeight() / 2, 
                    2, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    numRobots + "", 
                    (getWidth() - 450) / 2 + 60, 
                    getHeight() / 2, 
                    1, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    "Double deck:", 
                    (getWidth() - 450) / 2 - 20, 
                    getHeight() / 2 + 40, 
                    2, 1);
            graphics.setFont(OhcGraphicsTools.font);
        }
    }
    
    public void paintTrump(Graphics graphics) {
        if (state == GameState.PREGAME || state != GameState.POSTGAME && dealer > -1) {
            graphics.setColor(Color.BLACK);
            double scaleFix = 1;
            if (players.size() >= 6 && getHeight() < 600) {
                scaleFix = 0.75;
            }
            int x = state == GameState.PREGAME ? 50 : players.get(dealer).getTrumpX();
            int y = state == GameState.PREGAME ? 66 : players.get(dealer).getTrumpY();
            
            drawCard(graphics, new Card(), x - 4, y - 4, trumpCardScale * scaleFix, true, true);
            drawCard(graphics, new Card(), x - 2, y - 2, trumpCardScale * scaleFix, true, true);
            drawCard(graphics, state == GameState.PREGAME || trump == null ? new Card() : trump, x, y, trumpCardScale * scaleFix, true, false);
        }
    }
    
    public void paintBidding(Graphics graphics) {
        for (CanvasButton bidButton : bidButtons) {
            bidButton.paint(graphics);
        }
    }
    
    public void paintMessage(Graphics graphics) {
        OhcGraphicsTools.drawStringJustifiedBacked(graphics, message, (getWidth() - 450) / 2, getHeight() / 2);
    }
    
    public void paintTrick(Graphics graphics) {
        if (state != GameState.POSTGAME) {
            graphics.setColor(Color.BLACK);
            
            for (int i = 0; i < players.size(); i++) {
                int iRelToLeader = (leader + i) % players.size();
                int iRelToMe = (iRelToLeader - myPlayer.getIndex() + players.size()) % players.size();
                ClientPlayer player = players.get(iRelToLeader);
                if (!player.isKicked() && !player.getTrick().isEmpty()) {
                    if (player.getTrickRad() == -1) {
                        player.setTrickRad((int) (70 + 10 * random.nextDouble()));
                    }
                    
                    double startx = player.getX();
                    double starty = player.getY();
                    
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
                    if (player.timerStarted()) {
                        drawCard(graphics, player.getTrick(), x, y, trickCardScale, true, false);
                    }
                }
            }
        }
    }
    
    public void paintPlayers(Graphics graphics) {
        if (state != GameState.POSTGAME) {
            graphics.setColor(Color.BLACK);
            
            for (ClientPlayer player : players) {
                if (player.posNotSet()) {
                    continue;
                }
                
                int x = player.getX();
                int y = player.getY();
                int pos = player.getJust();
                
                // Hand if not my player
                if (state != GameState.PREGAME && player != myPlayer) {
                    int handSize = player.getHand().size();
                    int yOffset = 40;
                    int separation = 10;
                    for (int i = 0; i < handSize; i++) {
                        drawCard(graphics, 
                                player.getHand().get(i), 
                                (int) (x + i * separation 
                                        - (handSize - 1) * separation / 2 
                                        - (pos - 1) * maxWid / 2), 
                                y - yOffset, 
                                smallCardScale, 
                                true, 
                                false);
                    }
                }
                
                // Name plate
                if (state != GameState.PREGAME && (player.getBidding() == 1 || player.isPlaying()) 
                        || state == GameState.PREGAME && player.isHost()) {
                    graphics.setColor(new Color(255, 255, 0));
                } else if (player.isHuman()) {
                    graphics.setColor(Color.WHITE);
                } else {
                    graphics.setColor(new Color(210, 255, 255));
                }
                OhcGraphicsTools.drawBox(graphics, x - pos * maxWid / 2, y - 10, maxWid, 20, 20);
                
                // Name
                if (player.isDisconnected()) {
                    graphics.setColor(Color.GRAY);
                }
                if (player.isKicked()) {
                    graphics.setColor(Color.RED);
                }
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, player.getName(), maxWid), 
                        (int) (x - (pos - 1) * maxWid / 2), 
                        y, 
                        1, 1);
                
                if (state != GameState.PREGAME) {
                    // Bid chip
                    if (player.hasBid()) {
                        double startX = player.getBidX();
                        double startY = player.getBidY();
                        double endX = x - pos * maxWid / 2 + 10;
                        double endY = y;
                        
                        double bidX = startX * (1 - player.getBidTimer()) + endX * player.getBidTimer();
                        double bidY = startY * (1 - player.getBidTimer()) + endY * player.getBidTimer();
                        double radius = 50 * (1 - player.getBidTimer()) + 16 * player.getBidTimer();
                        
                        if (player.getBidTimer() < 1) {
                            graphics.setColor(new Color(255, 255, 255, 180));
                        } else if (state == GameState.BIDDING || state == GameState.PLAYING && player.getBid() > player.getTaken()) {
                            graphics.setColor(new Color(175, 175, 175, 180));
                        } else if (player.getBid() == player.getTaken()) {
                            graphics.setColor(new Color(175, 255, 175));
                        } else {
                            graphics.setColor(new Color(255, 175, 175));
                        }
                        graphics.fillOval((int) (bidX - radius / 2), (int) (bidY - radius / 2), (int) radius, (int) radius);
                        graphics.setColor(Color.BLACK);
                        if (player.getBidTimer() == 0) {
                            graphics.drawOval((int) (bidX - radius / 2), (int) (bidY - radius / 2), (int) radius, (int) radius);
                            graphics.setFont(OhcGraphicsTools.fontLargeBold);
                        }
                        OhcGraphicsTools.drawStringJustified(graphics, player.getBid() + "", 
                                bidX, 
                                bidY, 
                                1, 1);
                        graphics.setFont(OhcGraphicsTools.font);
                    }
                    
                    // Dealer chip
                    /*if (player.getIndex() == dealer) {
                        graphics.setColor(Color.CYAN);
                        graphics.fillOval((int) (x - (pos - 2) * maxWid / 2) - 19, y - 8, 16, 16);
                        graphics.setColor(Color.BLACK);
                        OhcGraphicsTools.drawStringJustified(graphics, "D", 
                                (int) (x - (pos - 2) * maxWid / 2) - 11, 
                                y, 
                                1, 1);
                    }*/
                }
            }
        }
    }
    
    public void paintAiHelp(Graphics graphics) {
        /*int handSize = myPlayer.getHand().size();
        int x = (getWidth() - 450) / 2;
        int y = getHeight() - 20;
        int pos = 1;
        int separation = cardSeparation;
        double maxWid = (maxHand - 1) * 10 + cardWidthSmall;
        
        String aiBid = client.getAiBid();
        String aiPlay = client.getAiPlay();
        
        for (int i = 0; i < handSize; i++) {
            int yOffset = handYOffset;
            if (myPlayer.isPlaying() && i == cardMoused && canPlayThis(i)) {
                yOffset = handYOffset + 10;
            }
            
            String prob = client.getOvlProb(i);
            graphics.setColor(new Color(0, 0, (int) (Double.parseDouble(prob) * 255)));
            drawStringJustifiedBold(graphics, prob, 
                    (int)(x + i * separation 
                    - (handSize - 1) * separation / 2 
                    - (pos - 1) * maxWid / 2 - (int) cardWidth / 2 + 25), 
                    y - yOffset + (int) cardHeight / 2 - 10,
                    1, 1);
            
            if (actionQueue.isEmpty() && !aiPlay.isEmpty() && myPlayer.getHand().get(i).equals(new Card(aiPlay))) {
                graphics.setColor(new Color(0, 0, 255));
                graphics.drawRect(
                        (int) (x + i * separation 
                                - (handSize - 1) * separation / 2 
                                - (pos - 1) * maxWid / 2 - cardWidth / 2), 
                        (int) (y - yOffset - cardHeight / 2), 
                        (int) (i < handSize - 1 ? separation : cardWidth), 
                        (int) cardHeight);
                graphics.drawRect(
                        (int) (x + i * separation 
                                - (handSize - 1) * separation / 2 
                                - (pos - 1) * maxWid / 2 - cardWidth / 2 - 1), 
                        (int) (y - yOffset - cardHeight / 2 - 1), 
                        (int) (i < handSize - 1 ? separation : cardWidth + 2), 
                        (int) (cardHeight + 2));
            }
        }
        
        if (actionQueue.isEmpty() && !aiBid.isEmpty() && myPlayer.getBidding() != 0) {
            int i = Integer.parseInt(aiBid);
            graphics.setColor(new Color(0, 0, 255));
            graphics.drawRect(
                    (getWidth() - 450) / 2 + i * 40 
                        - myPlayer.getHand().size() * 40 / 2 - 16, 
                    getHeight() - 210 - 16, 
                    30, 
                    30);
            graphics.drawRect(
                    (getWidth() - 450) / 2 + i * 40 
                        - myPlayer.getHand().size() * 40 / 2 - 17, 
                    getHeight() - 210 - 17, 
                    32, 
                    32);
        }*/
    }
    
    public void paintTaken(Graphics graphics) {
        if (state != GameState.POSTGAME) {
            for (ClientPlayer player : players) {
                for (int j = 0; j < player.getTaken(); j++) {
                    int takenX = player.getTakenX();
                    int takenY = player.getTakenY();
                    
                    boolean isLastTrick = player.getIndex() == leader && j == player.getTaken() - 1;
                    
                    double timer = 1;
                    if (isLastTrick) timer = takenTimer;
                    
                    int x = takenX + takenXSeparation * j;
                    int y = takenY + takenYSeparation * j;
                    if (animatingTaken && isLastTrick) {
                        x = (int) (timer * (takenX + takenXSeparation * j)
                                + (1 - timer) * (getWidth() - 450) / 2);
                        y = (int) (timer * (takenY + takenYSeparation * j)
                                + (1 - timer) * getHeight() / 2);
                    }
                    
                    if (!isLastTrick || animatingTaken) {
                        drawCard(graphics, new Card(), x, y, smallCardScale, true, false);
                    }
                }
            }
        }
    }
    
    public void paintScores(Graphics graphics) {
        if (state != GameState.PREGAME) {
            List<ClientPlayer> playersToShow;
            List<int[]> rounds;
            if (state == GameState.POSTGAME) {
                playersToShow = postGamePlayers;
                rounds = postGameRounds;
            } else {
                playersToShow = players;
                rounds = client.getRounds();
            }
            int numRounds = rounds.size();
            
            graphics.setColor(Color.WHITE);
            OhcGraphicsTools.drawBox(graphics, 
                    getWidth() - (450 - scoreMargin), 
                    scoreMargin, 
                    450 - 2 * scoreMargin, 
                    scoreVSpacing * (numRounds + 1) + 5,
                    10);
            
            double wid = (double) (450 - 2 * scoreMargin - 50) / playersToShow.size();

            graphics.setColor(Color.BLACK);
            graphics.drawLine(
                    getWidth() - (450 - scoreMargin - 45), 
                    scoreMargin + scoreVSpacing, 
                    getWidth() - scoreMargin - 5, 
                    scoreMargin + scoreVSpacing);
            for (ClientPlayer player : playersToShow) {
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
                if (player.isDisconnected()) {
                    graphics.setColor(Color.GRAY);
                }
                if (player.isKicked()) {
                    graphics.setColor(Color.RED);
                }
                
                if (player.equals(myPlayer)) {
                    graphics.setFont(OhcGraphicsTools.fontBold);
                } else {
                    graphics.setFont(OhcGraphicsTools.font);
                }
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, player.getName(), wid - 2), 
                        (int) (getWidth() - (450 - scoreMargin - 45) + index * wid + wid / 2), 
                        scoreMargin + 15, 
                        1, 0);
                graphics.setFont(OhcGraphicsTools.font);
            }
            
            graphics.setColor(Color.BLACK);
            for (int i = 0; i < numRounds; i++) {
                int[] round = rounds.get(i);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        playersToShow.get(round[0]).getName().substring(0, 1), 
                        getWidth() - (450 - scoreMargin - 5), 
                        scoreMargin + scoreVSpacing * (i + 2), 
                        0, 0);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        "" + round[1], 
                        getWidth() - (450 - scoreMargin - 25), 
                        scoreMargin + scoreVSpacing * (i + 2), 
                        0, 0);
            }
        
            for (ClientPlayer player : playersToShow) {
                int index = player.getIndex();
                for (int j = 0; j < player.getBids().size(); j++) {
                    graphics.setColor(new Color(200, 200, 200, 180));
                    graphics.fillOval(
                            (int) (getWidth() - (450 - scoreMargin - 45) + (index + 1) * wid - 18), 
                            scoreMargin + 5 + scoreVSpacing * (1 + j) + 2, 
                            16, 16);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, 
                            Integer.toString(player.getBids().get(j)), 
                            (int) (getWidth() - (450 - scoreMargin - 45) + (index + 1) * wid - 10), 
                            scoreMargin + scoreVSpacing * (2 + j), 
                            1, 0);
                }
                for (int j = 0; j < player.getScores().size(); j++) {
                    OhcGraphicsTools.drawStringJustified(graphics, 
                            Integer.toString(player.getScores().get(j)), 
                            (int) (getWidth() - (450 - scoreMargin - 45) + index * wid + 10), 
                            scoreMargin + scoreVSpacing * (2 + j), 
                            0, 0);
                }
            }
        }
    }
    
    public void chat(String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                chatArea.setText(chatArea.getText() + text + "\n");
            }
        });
    }
    
    public void drawCard(Graphics graphics, Card card, double x, double y, double scale, boolean small, 
            boolean dark) {
        drawCard(graphics, card, x, y, scale, small, dark, -1);
    }
    
    public void drawCard(Graphics graphics, Card card, double x, double y, double scale, boolean small, 
            boolean dark, int maxY) {
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
        
        if (maxY < 0) {
            maxY = (int) (y + ch1 * scale / 2);
        }
        maxY = (int) Math.min(maxY, y + ch1 * scale / 2);
        double diff = maxY - (y - ch1 * scale / 2);
        
        OhcGraphicsTools.makeGraphics2D(graphics, 
                client.antialiasingSelected(),
                !client.lowGraphicsSelected())
        .drawImage(img, 
                (int) (x - cw1 * scale / 2), (int) (y - ch1 * scale / 2), 
                (int) (x + cw1 * scale / 2), maxY, 
                (int) (col * cw1), (int) (row * ch1), 
                (int) ((col + 1) * cw1), (int) (row * ch1 + diff / scale), 
                null);
    }
    
    public void doShowOneCard() {
        showOneCard = true;
    }
    
    public void setState(GameState newState) {
        state = newState;
    }
    
    public void setPlayerPositions() {
        int cut1t = (players.size() - 1) / 3;
        int cut2t = 2 * cut1t;
        if ((players.size() - 1) % 3 != 0) {
            cut2t++;
        }
        if ((players.size() - 1) % 3 == 2) {
            cut1t++;
        }
        final int cut1 = cut1t;
        final int cut2 = cut2t;

        for (ClientPlayer player : players) {
            int index = (player.getIndex() 
                            - myPlayer.getIndex() 
                            + players.size() - 1) % players.size();
            if (index < cut1) {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return 10;
                    }

                    @Override
                    public int y() {
                        return getHeight() - getHeight() * (index + 1) / (cut1 + 1);
                    }
                    
                    @Override
                    public int trumpX() {
                        return x() + 140;
                    }
                    
                    @Override
                    public int trumpY() {
                        return y() - 130;
                    }

                    @Override
                    public int justification() {
                        return 0;
                    }

                    @Override
                    public int takenX() {
                        return x() + 20;
                    }

                    @Override
                    public int takenY() {
                        return y() + 50;
                    }
                    
                    @Override
                    public int bidX() {
                        return x() + 220;
                    }
                    
                    @Override
                    public int bidY() {
                        return y();
                    }
                });
            } else if (index < cut2) {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return (getWidth() - 450) * (index - cut1 + 1) / (cut2 - cut1 + 1);
                    }

                    @Override
                    public int y() {
                        return 85;
                    }
                    
                    @Override
                    public int trumpX() {
                        return x() - 120;
                    }
                    
                    @Override
                    public int trumpY() {
                        return y();
                    }

                    @Override
                    public int justification() {
                        return 1;
                    }

                    @Override
                    public int takenX() {
                        return x() + 110;
                    }

                    @Override
                    public int takenY() {
                        return y() - 35;
                    }
                    
                    @Override
                    public int bidX() {
                        return x();
                    }
                    
                    @Override
                    public int bidY() {
                        return y() + 65;
                    }
                });
            } else if (index < players.size() - 1) {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return getWidth() - 450 - 10;
                    }

                    @Override
                    public int y() {
                        return getHeight() * (index - cut2 + 1) / (players.size() - 1 - cut2 + 1);
                    }
                    
                    @Override
                    public int trumpX() {
                        return x() - 130;
                    }
                    
                    @Override
                    public int trumpY() {
                        return y() - 130;
                    }

                    @Override
                    public int justification() {
                        return 2;
                    }

                    @Override
                    public int takenX() {
                        return x() - 90;
                    }

                    @Override
                    public int takenY() {
                        return y() + 50;
                    }
                    
                    @Override
                    public int bidX() {
                        return x() - 220;
                    }
                    
                    @Override
                    public int bidY() {
                        return y();
                    }
                });
            } else {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return (getWidth() - 450) / 2;
                    }

                    @Override
                    public int y() {
                        return getHeight() - 20;
                    }
                    
                    @Override
                    public int trumpX() {
                        return x() - 320;
                    }
                    
                    @Override
                    public int trumpY() {
                        return y() - 80;
                    }

                    @Override
                    public int justification() {
                        return 1;
                    }

                    @Override
                    public int takenX() {
                        return x() + 260;
                    }

                    @Override
                    public int takenY() {
                        return y() - 50;
                    }
                    
                    @Override
                    public int bidX() {
                        return x();
                    }
                    
                    @Override
                    public int bidY() {
                        return y() - 250;
                    }
                });
            }
        }
    }
    
    public void resetKickButtons() {
        kickButtons.clear();
        for (ClientPlayer player : players) {
            final ClientPlayer playerF = player;
            kickButtons.add(new CanvasButton("Kick") {
                @Override
                public int x() {
                    return (int) (playerF.getX() + (1 - playerF.getJust()) * maxWid / 2 - 50);
                }
                
                @Override
                public int y() {
                    return playerF.getY() - 50;
                }
                
                @Override
                public int width() {
                    return 100;
                }
                
                @Override
                public int height() {
                    return 30;
                }
                
                @Override
                public boolean isShown() {
                    return playerF.isDisconnected() && !playerF.isKicked();
                }
                
                @Override
                public void click() {
                    client.voteKick(playerF.getIndex());
                }
            });
        }
    }
    
    public void resetInteractables() {
        for (CanvasEmbeddedSwing inter : embeddedSwings) {
            inter.dispose();
        }
        
        embeddedSwings = new LinkedList<>();
        bidButtons = new LinkedList<>();
        cardInteractables = new LinkedList<>();
        
        // Last Trick
        lastTrick = new CanvasCard(new Card(), smallCardScale, true, this) {
            public ClientPlayer player() {
                return players.get(leader);
            }
            
            @Override
            public int xCenter() {
                return player().getTakenX() + takenXSeparation * (player().getTaken() - 1);
            }
            
            @Override
            public int yCenter() {
                return player().getTakenY() + takenYSeparation * (player().getTaken() - 1);
            }
            
            @Override
            public boolean isShown() {
                return trickTaken && !animatingTaken;
            }
            
            @Override
            public boolean isEnabled() {
                return trickTaken;
            }
            
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                if (isMoused()) {
                    List<ClientPlayer> unkickedPlayers = players.stream()
                            .filter(p -> !p.isKicked())
                            .collect(Collectors.toList());
                    for (int k = 0; k < unkickedPlayers.size(); k++) {
                        int x0 = Math.min(
                                xCenter() + 50, 
                                (int) (getWidth() - 450 - lastTrickSeparation * (unkickedPlayers.size() - 1) - cardWidth * trickCardScale / 2 - 10));
                        int y0 = Math.max(
                                yCenter(), 
                                (int) (cardWidth * trickCardScale / 2 + 10));
                        
                        drawCard(graphics, 
                                unkickedPlayers.get(k).getLastTrick(), 
                                x0 + lastTrickSeparation * k, 
                                y0, 
                                trickCardScale, true, false);
                    }
                }
            }
        };
        
        // Buttons
        buttons = new LinkedList<>();
        
        // Pre-game interactables
        OhcTextField nameField = new OhcTextField("Name");
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    client.rename(nameField.getText());
                }
            }
        });
        embeddedSwings.add(new CanvasEmbeddedSwing(nameField, this) {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 - 130;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 - 120;
            }
            
            @Override
            public int width() {
                return 200;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
        });
        
        buttons.add(new CanvasButton("Change name") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 80;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 - 120;
            }
            
            @Override
            public int width() {
                return 100;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public void click() {
                client.rename(nameField.getText());
            }
        });
        
        buttons.add(new CanvasButton("") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 20;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 - 70;
            }
            
            @Override
            public int width() {
                return 20;
            }
            
            @Override
            public int height() {
                return 20;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public String text() {
                return myPlayer != null && myPlayer.isKibitzer() ? "x" : "";
            }
            
            @Override
            public void click() {
                client.toggleKibitzer();
            }
        });
        
        buttons.add(new CanvasButton("-") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 25;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 - 10;
            }
            
            @Override
            public int width() {
                return 20;
            }
            
            @Override
            public int height() {
                return 20;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }
            
            @Override
            public void click() {
                numRobots = Math.max(0, numRobots - 1);
            }
        });
        
        buttons.add(new CanvasButton("+") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 75;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 - 10;
            }
            
            @Override
            public int width() {
                return 20;
            }
            
            @Override
            public int height() {
                return 20;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }
            
            @Override
            public void click() {
                numRobots = Math.min(6, numRobots + 1);
            }
        });
        
        buttons.add(new CanvasButton("") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 20;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 + 30;
            }
            
            @Override
            public int width() {
                return 20;
            }
            
            @Override
            public int height() {
                return 20;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }
            
            @Override
            public String text() {
                return doubleDeck ? "x" : "";
            }
            
            @Override
            public void click() {
                doubleDeck = !doubleDeck;
            }
        });
        
        buttons.add(new CanvasButton("Start") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 - 160;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 + 90;
            }
            
            @Override
            public int width() {
                return 150;
            }
            
            @Override
            public int height() {
                return 40;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }
            
            @Override
            public void click() {
                client.readyPressed(numRobots, doubleDeck);
            }
        });
        
        buttons.add(new CanvasButton("Leave table") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 10;
            }
            
            @Override
            public int y() {
                return getHeight() / 2 + 90;
            }
            
            @Override
            public int width() {
                return 150;
            }
            
            @Override
            public int height() {
                return 40;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.PREGAME && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer != null;
            }
            
            @Override
            public void click() {
                client.leaveGame();
            }
        });
        
        // Undo bid button
        buttons.add(new CanvasButton("Undo bid") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 - 40;
            }
            
            @Override
            public int y() {
                return getHeight() - 210 - 15;
            }
            
            @Override
            public int width() {
                return 80;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state != GameState.PREGAME
                        && canUndoBid
                        && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME
                        && !myPlayer.isKibitzer();
            }
            
            @Override
            public void click() {
                client.undoBid();
                canUndoBid = false;
            }
        });
        
        // Poke button
        buttons.add(new CanvasButton("Poke") {
            @Override
            public int x() {
                return 20;
            }
            
            @Override
            public int y() {
                return getHeight() - 130;
            }
            
            @Override
            public int width() {
                return 80;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state != GameState.PREGAME && state != GameState.POSTGAME;
            }
            
            @Override
            public boolean isEnabled() {
                return currentTime - pokeTime >= (long) pokeWaitTime * 1000000
                        && (state == GameState.BIDDING || state == GameState.PLAYING)
                        && myPlayer.getBidding() == 0
                        && !myPlayer.isPlaying();//canPoke;
            }
            
            @Override
            public void click() {
                resetPokeTime();
                //addPokeTimer();
                client.pokePlayer();
                //repaint();
            }
        });
        
        // One-round show card button
        buttons.add(new CanvasButton("Show card") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 - 40;
            }
            
            @Override
            public int y() {
                return getHeight() - handYOffset - height() / 2;
            }
            
            @Override
            public int width() {
                return 80;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.BIDDING
                        && myPlayer.getIndex() == dealer 
                        && !myPlayer.isKibitzer() 
                        && client.isOneRound() 
                        && !showOneCard;
            }
            
            @Override
            public void click() {
                doShowOneCard();
                //repaint();
            }
        });
        
        // Claim button
        buttons.add(new CanvasButton("Claim") {
            @Override
            public int x() {
                return 20;
            }
            
            @Override
            public int y() {
                return getHeight() - 90;
            }
            
            @Override
            public int width() {
                return 80;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state != GameState.PREGAME && state != GameState.POSTGAME;
            }
            
            @Override
            public boolean isEnabled() {
                return state == GameState.PLAYING && messageState.equals("UNBLOCKED");
            }
            
            @Override
            public void click() {
                makeClaimOnTimer();
            }
        });
        
        // End game button
        buttons.add(new CanvasButton("End game") {
            @Override
            public int x() {
                return 20;
            }
            
            @Override
            public int y() {
                return getHeight() - 50;
            }
            
            @Override
            public int width() {
                return 80;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return state != GameState.PREGAME && state != GameState.POSTGAME;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer.isHost() || client.getClientState() == ClientState.IN_SINGLE_PLAYER_GAME;
            }
            
            @Override
            public void click() {
                requestEndGameOnTimer();
            }
        });
        
        // Post-game buttons
        int numPgButtons = 3;
        // Back to options (pregame)
        buttons.add(new CanvasButton("Back to lobby") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 
                        - (numPgButtons * width() + (numPgButtons - 1) * 20) / 2
                        + 0 * (width() + 20);
            }
            
            @Override
            public int y() {
                return getHeight() - finalScoreBottomMargin + 2 * finalScoreOuterMargin;
            }
            
            @Override
            public int width() {
                return 150;
            }
            
            @Override
            public int height() {
                return 40;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.POSTGAME;
            }
            
            /*@Override
            public boolean isEnabled() {
                return myPlayer.isHost() || client.getClientState() == ClientState.SINGLE_PLAYER_POST_GAME;
            }*/
            
            @Override
            public void click() {
                if (client.getClientState() == ClientState.SINGLE_PLAYER_POST_GAME) {
                    client.changeState(ClientState.SINGLE_PLAYER_MENU);
                } else if (client.getClientState() == ClientState.MULTIPLAYER_POST_GAME) {
                    client.changeState(ClientState.IN_MULTIPLAYER_GAME);
                    pregameOnTimer();
                    updatePlayersOnTimer();
                } else {
                    client.changeState(ClientState.MAIN_MENU);
                }
            }
        });
        
        // Leave
        buttons.add(new CanvasButton("Leave table") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 
                        - (numPgButtons * width() + (numPgButtons - 1) * 20) / 2
                        + 1 * (width() + 20);
            }
            
            @Override
            public int y() {
                return getHeight() - finalScoreBottomMargin + 2 * finalScoreOuterMargin;
            }
            
            @Override
            public int width() {
                return 150;
            }
            
            @Override
            public int height() {
                return 40;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.POSTGAME;
            }
            
            @Override
            public void click() {
                client.leaveGame();
            }
        });
        
        // Save game
        buttons.add(new CanvasButton("Save game") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 
                        - (numPgButtons * width() + (numPgButtons - 1) * 20) / 2
                        + 2 * (width() + 20);
            }
            
            @Override
            public int y() {
                return getHeight() - finalScoreBottomMargin + 2 * finalScoreOuterMargin;
            }
            
            @Override
            public int width() {
                return 150;
            }
            
            @Override
            public int height() {
                return 40;
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.POSTGAME;
            }
            
            @Override
            public void click() {
                client.savePostGame();
            }
        });
        
        // Bottom left buttons
        // Claim accept button
        buttons.add(new CanvasButton("Accept") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 - 125;
            }
            
            @Override
            public int y() {
                return getHeight() - 210 - 15;
            }
            
            @Override
            public int width() {
                return 100;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return messageState.equals("CLAIM");
            }
            
            @Override
            public void click() {
                client.sendClaimResponse("ACCEPT");
                messageState = "CLAIMWAITING";
                //repaint();
            }
        });
        
        // Claim refuse button
        buttons.add(new CanvasButton("Refuse") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 + 25;
            }
            
            @Override
            public int y() {
                return getHeight() - 210 - 15;
            }
            
            @Override
            public int width() {
                return 100;
            }
            
            @Override
            public int height() {
                return 30;
            }
            
            @Override
            public boolean isShown() {
                return messageState.equals("CLAIM");
            }
            
            @Override
            public void click() {
                client.sendClaimResponse("REFUSE");
                messageState = "CLAIMWAITING";
                //repaint();
            }
        });
        
        // Post-game page
        postGamePage = new CanvasPostGamePage() {
            @Override
            public int x() {
                return Math.max((getWidth() - 450) / 2 - finalScoreMaxWidth / 2, finalScoreOuterMargin);
            }
            
            @Override
            public int y() {
                return Math.max((getHeight() - finalScoreBottomMargin) / 2 - finalScoreMaxHeight / 2, finalScoreOuterMargin);
            }
            
            @Override
            public int width() {
                return Math.min(finalScoreMaxWidth, getWidth() - 450 - 2 * finalScoreOuterMargin);
            }
            
            @Override
            public int height() {
                return Math.min(finalScoreMaxHeight, getHeight() - finalScoreBottomMargin - finalScoreOuterMargin);
            }
            
            @Override
            public boolean isShown() {
                return state == GameState.POSTGAME && super.isShown();
            }
        };
        
        // Chat areas
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        CanvasEmbeddedSwing chatScrollPane = new CanvasEmbeddedSwing(
                new OhcScrollPane(chatArea,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                this) {
            @Override
            public int x() {
                return getWidth() - 450 + scoreMargin;
            }
            
            @Override
            public int y() {
                int numRounds = client.getRounds().size();
                return Math.max(
                        2 * scoreMargin + scoreVSpacing * (numRounds + 1) + 5, 
                        getHeight() - 300);
            }
            
            @Override
            public int width() {
                return 450 - 2 * scoreMargin + 1;
            }
            
            @Override
            public int height() {
                return getHeight() - y() - 40 - scoreMargin;
            }
        };
        
        JTextField chatJField = new OhcTextField("Enter text");
        chatJField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER 
                            && !chatJField.getText().trim().isEmpty()) {
                        String text = myPlayer.getName() + ": " + chatJField.getText().trim();
                        client.sendChat(text);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                chatJField.setText("");
                            }
                        });
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        CanvasEmbeddedSwing chatField = new CanvasEmbeddedSwing(
                chatJField,
                this) {
            @Override
            public int x() {
                return getWidth() - 450 + scoreMargin;
            }
            
            @Override
            public int y() {
                return getHeight() - 35 - scoreMargin;
            }
            
            @Override
            public int width() {
                return 450 - 2 * scoreMargin + 1;
            }
            
            @Override
            public int height() {
                return 30;
            }
        };
        
        embeddedSwings.add(chatScrollPane);
        embeddedSwings.add(chatField);
    
        setInteractables(Arrays.asList(
                embeddedSwings,
                Arrays.asList(lastTrick),
                bidButtons,
                cardInteractables,
                kickButtons,
                buttons,
                Arrays.asList(postGamePage)
                ));
    }
    
    public void makeBidInteractables() {
        bidButtons.clear();
        for (int i = 0; i <= myPlayer.getHand().size(); i++) {
            final int bid = i;
            bidButtons.add(new CanvasButton(bid + "") {
                @Override
                public int x() {
                    return (getWidth() - 450) / 2 + bid * 40 
                            - myPlayer.getHand().size() * 40 / 2 - 15;
                }
                
                @Override
                public int y() {
                    return getHeight() - 210 - 15;
                }
                
                @Override
                public int width() {
                    return 30;
                }
                
                @Override
                public int height() {
                    return 30;
                }
                
                @Override
                public boolean isEnabled() {
                    return !cannotBid(bid);
                }
                
                @Override
                public void click() {
                    myPlayer.setBidding(0);
                    client.makeBid(bid);
                    removeBidInteractables();
                    //setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        }
    }
    
    public void removeBidInteractables() {
        bidButtons.clear();
    }
    
    public void makeHandInteractables() {
        cardInteractables.clear();
        for (int i = 0; i < myPlayer.getHand().size(); i++) {
            cardInteractables.add(new CanvasCard(myPlayer.getHand().get(i), 1, false, this) {
                public int index() {
                    return myPlayer.getHand().indexOf(getCard());
                }
                
                @Override
                public int xCenter() {
                    return (getWidth() - 450) / 2
                            + index() * cardSeparation
                            - (myPlayer.getHand().size() - 1) * cardSeparation / 2;
                }
                
                @Override
                public int yCenter() {
                    return getHeight() - handYOffset;
                }
                
                @Override
                public int yPaintOffset() {
                    return isMoused() ? -10 : 0;
                }
                
                @Override
                public boolean isEnabled() {
                    return myPlayer.isPlaying() && canPlayThis(getCard());
                }
                
                @Override
                public boolean hidden() {
                    return myPlayer.getIndex() == dealer 
                            && !myPlayer.isKibitzer() 
                            && client.isOneRound() 
                            && !showOneCard;
                }
                
                @Override
                public void click() {
                    myPlayer.setPlaying(false);
                    cardJustPlayed = index();
                    client.makePlay(getCard());
                    cardInteractables.remove(this);
                    //setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        }
    }
    
    public void setLeader(int leader) {
        this.leader = leader;
    }
    
    public void mouseWheeled(int clicks) {
        if (state == GameState.POSTGAME) {
            postGamePage.wheel(clicks);
        }
    }
    
    public void keyPressed(int keyCode) {
        if (state == GameState.POSTGAME) {
            postGamePage.pressKey(keyCode);
        }
    }
    
    public boolean cannotBid(int bid) {
        int totalBid = players.stream()
                .filter(ClientPlayer::hasBid)
                .map(ClientPlayer::getBid)
                .reduce(0, (sofar, b) -> sofar + b);
        return (myPlayer.getIndex() == dealer) 
                && (totalBid + bid == myPlayer.getHand().size());
    }
    
    public boolean canPlayThis(Card card) {
        String ledSuit = players.get(leader).getTrick().getSuit();
        return players.get(leader).getTrick().isEmpty()
                || card.getSuit().equals(ledSuit)
                || myPlayer.getHand().stream().noneMatch(c -> c.getSuit().equals(ledSuit));
    }
    
    public void setTrumps(List<Card> trumps) {
        this.trumps = trumps;
    }
    
    public List<Card> getTrumps() {
        return trumps;
    }
    
    public void setAiHelp(boolean aiHelpSelected) {
        this.aiHelpSelected = aiHelpSelected;
        //repaint();
    }
    
    public void playSound(Clip clip) {
        double length = (double) clip.getFrameLength() / 44.1;
        new CanvasTimerEntry((long) length, this, audioQueue, false) {
            @Override
            public void onFirstAction() {
                clip.start();
            }
            
            @Override
            public void onLastAction() {
                clip.stop();
                clip.setFramePosition(0);
            }
        };
    }
    
    public LinkedList<Timer> getActionQueue() {
        return actionQueue;
    }
    
    public void setPerformingAction(boolean performingAction) {
        this.performingAction = performingAction;
    }
    
    public void updatePlayersOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (state.equals(GameState.PREGAME)) {
                    updatePlayers();
                }
            }
        };
    }
    
    public void updatePlayers() {
        players.clear();
        players.addAll(client.getPlayers());
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setIndex(i);
        }
        myPlayer = client.getMyPlayer();
        maxHand = Math.min(10, 51 / Math.max(players.size(), 1));
        maxWid = (maxHand - 1) * 10 + cardWidthSmall;
        setPlayerPositions();
        resetKickButtons();
    }
    
    public void pregameOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                pregame();
            }
        };
    }
    
    public void pregame() {
        state = GameState.PREGAME;
        message = "";
        paintMessageMarker = false;
        resetInteractables();
        //updatePlayers();
        trickTaken = false;
        animatingTaken = false;
        takenTimer = 1;
    }
    
    public void initializingOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                updatePlayers();
                //updatePlayersOnTimer();
                state = GameState.INITIALIZING;

                for (ClientPlayer player : players) {
                    player.reset();
                }
            }
        };
    }
    
    public void requestEndGameOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, true) {
            @Override
            public void onFirstAction() {
                client.requestEndGame();
            }
        };
    }
    
    public void endGameOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, true) {
            @Override
            public void onFirstAction() {
                actionQueue.clear();
                if (client.getClientState() == ClientState.IN_SINGLE_PLAYER_GAME) {
                    client.changeState(ClientState.SINGLE_PLAYER_MENU);
                } else {
                    client.changeState(ClientState.IN_MULTIPLAYER_GAME);
                    updatePlayers();
                }
                pregame();
            }
        };
    }
    
    public void animateBids() {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : animationTime, this, actionQueue, true) {
            @Override
            public void onAction() {
                double t = Math.min((double) this.getElapsedTime() / (client.devSpeedSelected() ? 1 : animationTime), 1);
                for (ClientPlayer player : players) {
                    player.setBidTimer(t);
                }
            }
        };
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : bidStayTime, this, actionQueue, true);
    }
    
    public void animateCardPlay(int index) {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : animationTime, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (client.soundSelected()) {
                    playSound(cardPlayClip);
                }
                players.get(index).setTrickTimer(0);
                players.get(index).setTimerStarted(true);
            }
            
            @Override
            public void onAction() {
                players.get(index).setTrickTimer(
                        Math.min((double) this.getElapsedTime() / (client.devSpeedSelected() ? 1 : animationTime), 1));
                //repaint();
            }
        };
    }
    
    public void animateTrickTake(int index) {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : trickStayTime, this, actionQueue, false) {
            @Override
            public void onLastAction() {
                for (ClientPlayer player : players) {
                    player.resetTrick();
                }
                lastTrick.setMoused(false);
                setLeader(index);
            }
        };
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : animationTime, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                players.get(index).incrementTaken();
                trickTaken = true;
                takenTimer = 0;
                animatingTaken = true;
            }
            
            @Override
            public void onAction() {
                takenTimer = Math.min((double) this.getElapsedTime() / (client.devSpeedSelected() ? 1 : animationTime), 1);
                //repaint();
            }
            
            @Override
            public void onLastAction() {
                animatingTaken = false;
            }
        };
    }
    
    public void makeClaimOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                client.makeClaim();
            }
        };
    }
    
    public void claimReportOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                claimer = index;
                if (index == myPlayer.getIndex()) {
                    message = "You claim the rest of the tricks.";
                    messageState = "ICLAIM";
                    client.sendClaimResponse("ACCEPT");
                } else {
                    message = players.get(index).getName() + " claims the rest of the tricks.";
                    messageState = "CLAIM";
                }
                paintMessageMarker = true;
            }
        };
    }
    
    public void claimResultOnTimer(boolean accept) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (accept) {
                    int remainingTricks = myPlayer.getHand().size();
                    if (!myPlayer.getTrick().isEmpty()) {
                        remainingTricks++;
                    }
                    for (int i = 0; i < remainingTricks; i++) {
                        players.get(claimer).incrementTaken();
                    }
                    for (ClientPlayer player : players) {
                        player.resetTrick();
                        player.setHand(new LinkedList<>());
                    }
                    cardInteractables.clear();
                }
                messageState = "UNBLOCKED";
                claimer = -1;
            }
        };
    }
    
    public void showMessageOnTimer(String text, boolean immediately) {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : messageTime, this, actionQueue, immediately) {
            @Override
            public void onFirstAction() {
                message = text;
                paintMessageMarker = true;
            }
            
            @Override
            public void onAction() {
                //repaint();
            }
            
            @Override
            public void onLastAction() {
                message = "";
                paintMessageMarker = false;
            }
        };
    }
    
    public void showResultMessageOnTimer() {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : messageTime, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (myPlayer.getBid() == myPlayer.getTaken()) {
                    message = "You made your bid!";
                } else {
                    message = "You went down "
                            + Math.abs(myPlayer.getBid() - myPlayer.getTaken()) + ".";
                }
                paintMessageMarker = true;
                state = GameState.ENDOFROUND;
            }
            
            @Override
            public void onAction() {
                //repaint();
            }
            
            @Override
            public void onLastAction() {
                message = "";
                paintMessageMarker = false;
            }
        };
    }
    
    public void clearRoundOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setHasBid(false);
                    player.setBid(0);
                    player.setTaken(0);
                }
                trickTaken = false;
            }
        };
    }
    
    public void setHandOnTimer(int index, List<Card> hand) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (index == dealer 
                        && index == myPlayer.getIndex() 
                        && !myPlayer.isKibitzer() 
                        && client.thisRound()[1] == 1) {
                    showOneCard = false;
                }
                players.get(index).setHand(hand);
                if (index == myPlayer.getIndex()) {
                    makeHandInteractables();
                }
            }
        };
    }
    
    public void setDealerOnTimer(int dealerI) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                dealer = dealerI;
            }
        };
    }
    
    public void setLeaderOnTimer(int leaderI) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                leader = leaderI;
            }
        };
    }
    
    public void setTrumpOnTimer(Card card) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                trump = card;
            }
        };
    }
    
    public void setBiddingOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setBidding(player.getIndex() == index ? 1 : 0);
                }
                for (ClientPlayer player : players) {
                    player.setPlaying(false);
                }
                state = GameState.BIDDING;
                if (myPlayer.getBidding() != 0) {
                    makeBidInteractables();
                    //deletePokeTimer();
                } else {
                    //addPokeTimer();
                }
                resetPokeTime();
            }
        };
    }
    
    public void setPlayingOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setBidding(0);
                }
                for (ClientPlayer player : players) {
                    player.setPlaying(player.getIndex() == index);
                }
                state = GameState.PLAYING;
                resetPokeTime();
                /*if (myPlayer.isPlaying()) {
                    deletePokeTimer();
                } else {
                    addPokeTimer();
                }*/
            }
        };
    }
    
    public void setBidOnTimer(int index, int bid, int delay) {
        new CanvasTimerEntry(delay, this, actionQueue, false) {
            @Override
            public void onLastAction() {
                players.get(index).addBid(bid);
                players.get(index).setBidTimer(0);
                if (index == myPlayer.getIndex()) {
                    canUndoBid = true;
                } else {
                    canUndoBid = false;
                }
                if (players.stream().filter(p -> !p.hasBid() && !p.isKicked()).count() == 0) {
                    animateBids();
                }
            }
        };
    }
    
    public void removeBidOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onLastAction() {
                if (myPlayer.getBidding() != 0) {
                    removeBidInteractables();
                }
                
                for (ClientPlayer player : players) {
                    player.setBidding(0);
                    player.setPlaying(false);
                    player.setBidTimer(0);
                }
                
                players.get(index).removeBid();
                state = GameState.BIDDING;
                if (myPlayer.getBidding() != 0) {
                    makeBidInteractables();
                }
                resetPokeTime();
            }
        };
    }
    
    public void setPlayOnTimer(int index, Card card, int delay) {
        new CanvasTimerEntry(delay, this, actionQueue, false) {
            @Override
            public void onLastAction() {
                players.get(index).setTrick(card);
                players.get(index).removeCard(card);
                canUndoBid = false;
            }
        };
    }
    
    public void setScoresOnTimer(List<Integer> scores) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                for (int i = 0; i < players.size(); i++) {
                    if (scores.get(i) != null) {
                        players.get(i).addScore(scores.get(i));
                    }
                }
                client.incrementRoundNumber();
            }
        };
    }
    
    public void showPostGameOnTimer() {
        GameCanvas thisCanvas = this;
        new CanvasTimerEntry(1, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setBidding(0);
                    player.setPlaying(false);
                    dealer = -1;
                }
                
                message = "Analyzing...";
                paintMessageMarker = true;
                client.goToPostGame();
                state = GameState.POSTGAME;
            }
            
            @Override
            public void onLastAction() {
                postGamePage.buildTabs(players, client.getRounds(), thisCanvas);
                
                message = "";
                paintMessageMarker = false;
            }
        };
    }
    
    public void loadPostGameOnTimer(List<String> lines) {
        GameCanvas thisCanvas = this;
        new CanvasTimerEntry(1, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setBidding(0);
                    player.setPlaying(false);
                    dealer = -1;
                }
                
                message = "Analyzing...";
                paintMessageMarker = true;
                client.goToPostGame();
            }
            
            @Override
            public void onLastAction() {
                state = GameState.POSTGAME;
                
                trumps = new ArrayList<>();
                postGamePlayers = new ArrayList<>();
                postGameRounds = new ArrayList<>();
                loadPostGameFromFile(lines, trumps, postGamePlayers, postGameRounds);
                
                postGamePage.buildTabs(postGamePlayers, postGameRounds, thisCanvas);
                
                message = "";
                paintMessageMarker = false;
            }
        };
    }
    
    public static void loadPostGameFromFile(List<String> lines, List<Card> trumps, List<ClientPlayer> players, List<int[]> rounds) {
        trumps.clear();
        players.clear();
        rounds.clear();
        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }
            
            String[] typeContent = line.split(";", 2);
            String type = typeContent[0];
            String[] content = typeContent[1].split(";");
            
            if (type.equals("decks")) {
                
            } else if (type.equals("players")) {
                int index = 0;
                for (String playerInfo : content) {
                    ClientPlayer player = new ClientPlayer();
                    String[] info = playerInfo.split(":");
                    player.setId(info[0]);
                    player.setName(info[1]);
                    player.setHuman(info[2].equals("human"));
                    player.setIndex(index);
                    players.add(player);
                    index++;
                }
            } else if (type.equals("round")) {
                rounds.add(new int[] {Integer.parseInt(content[0]), Integer.parseInt(content[1])});
            } else if (type.equals("hands")) {
                for (int j = 0; j < players.size(); j++) {
                    List<Card> hand = Arrays.stream(content[j].split(":"))
                            .map(s -> new Card(s))
                            .collect(Collectors.toList());
                    players.get(j).addPostGameHand(hand);
                }
            } else if (type.equals("trump")) {
                trumps.add(new Card(content[0]));
            } else if (type.equals("bids")) {
                for (int j = 0; j < players.size(); j++) {
                    String[] info = content[j].split(":");
                    players.get(j).addBid(Integer.parseInt(info[info.length - 1]));
                }
            } else if (type.equals("trick")) {
                
            } else if (type.equals("takens")) {
                for (int j = 0; j < players.size(); j++) {
                    players.get(j).addTaken(Integer.parseInt(content[j]));
                }
            } else if (type.equals("scores")) {
                for (int j = 0; j < players.size(); j++) {
                    players.get(j).addScore(Integer.parseInt(content[j]));
                }
            } else if (type.equals("final scores")) {
                for (int j = 0; j < players.size(); j++) {
                    String[] info = content[j].split(":");
                    players.get(j).setPlace(Integer.parseInt(info[1]));
                }
            } else if (type.equals("kick")) {
                
            }
        }
    }
    
    public void setConnectionStatusOnTimer(boolean connected) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (!connected) {
                    message = "Player(s) disconnected.";
                    messageState = "DISCONNECTED";
                    paintMessageMarker = true;
                } else if (players.stream().filter(p -> !p.isKicked() && p.isDisconnected()).count() == 0) {
                    message = "";
                    messageState = "UNBLOCKED";
                    paintMessageMarker = false;
                }
            }
        };
    }
    
    public void setStatePlayerOnTimer(int index, boolean hasBid, int bid, int taken, Card lastTrick, Card trick) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                ClientPlayer player = players.get(index);
                player.setHasBid(hasBid);
                player.setBid(bid);
                player.setTaken(taken);
                player.setLastTrick(lastTrick);
                player.setTrick(trick);
                if (!hasBid) {
                    for (ClientPlayer p : players) {
                        p.setBidTimer(0);
                    }
                }
                if (!trick.isEmpty()) {
                    player.setTimerStarted(true);
                }
            }
        };
    }
    
    public void setStatePlayerBidsOnTimer(int index, List<Integer> bids) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                players.get(index).setBids(bids);
            }
        };
    }
    
    public void setStatePlayerScoresOnTimer(int index, List<Integer> scores) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                players.get(index).setScores(scores);
            }
        };
    }
    
    public void resetPokeTime() {
        pokeTime = currentTime;
    }
    
    public void bePokedOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                //showMessageOnTimer("You've been poked.");
                playSound(pokeClip);
            }
        };
    }
    
    public void addStopper() {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                stopped = true;
            }
            
            @Override
            public void onAction() {
                if (stopped) {
                    setEndTime(System.currentTimeMillis() - getStartTime() + 1);
                } else {
                    setEndTime(0);
                }
            }
        };
    }
    
    public void removeStopper() {
        stopped = false;
    }
}