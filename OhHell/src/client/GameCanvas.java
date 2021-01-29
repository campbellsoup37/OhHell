package client;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.sound.sampled.Clip;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import core.Card;
import graphics.OhcGraphicsTools;
import ml.BasicVector;
import ml.BootstrapAggregator;

public class GameCanvas extends JPanel {
    private static final long serialVersionUID = 1L;
    
    ////// Parameters /////////////////
    private final int cardSeparation = 40;
    private final double smallCardScale = 0.6667;
    private final double trumpCardScale = 1;
    private final double trickCardScale = 1;
    private final int handYOffset = 105;
    private final int takenXSeparation = 10;
    private final int takenYSeparation = 5;
    private final int lastTrickSeparation = 20;
    
    private final int scoreVSpacing = 20;
    private final int scoreMargin = 10;
    private final int trickStayTime = 1500;
    private final int animationTime = 150;
    private final int messageTime = 2000;
    
    private final int finalScoreOuterMargin = 50;
    private final int finalScoreInnerMargin = 10;
    private final int finalScoreListWidth = 200;
    private final int finalScoreBottomMargin = 300;
    
    private final int pokeWaitTime = 25000;
    ///////////////////////////////////
    
    private GameClient client;
    
    private List<ClientPlayer> players;
    private ClientPlayer myPlayer;

    private int maxHand;
    private double maxWid;
    
    private Card trump;
    
    private boolean paintHandMarker = true;
    private boolean paintBiddingMarker = true;
    private boolean paintTrumpMarker = true;
    private boolean paintPlayersMarker = true;
    private boolean paintMessageMarker = false;
    private boolean paintTakenMarker = true;
    private boolean paintTrickMarker = true;
    
    private String message = "";
    private String messageState = "UNBLOCKED";
    
    private List<CanvasButton> bidButtons = new LinkedList<>();
    private List<CanvasCard> cardInteractables = new LinkedList<>();
    private CanvasCard lastTrick;
    private List<CanvasButton> buttons = new LinkedList<>();
    private CanvasInteractable interactableMoused = null;
    private CanvasInteractable interactablePressed = null;
    
    private boolean trickTaken = false;
    
    private LinkedList<Timer> actionQueue = new LinkedList<>();
    private int cardJustPlayed = 0;
    private boolean animatingTaken = false;
    private double takenTimer = 1;
    
    private boolean canUndoBid = false;

    private int claimer = -1;
    
    private int dealer = -1;
    private int leader;
    
    private boolean showOneCard = false;

    private boolean end = false;
    private int[][] endscores;
    private CanvasScorePlot scorePlot;
    
    private String gameState = "PREGAME";
    
    private BufferedImage deckImg;
    private BufferedImage deckImgDark;
    private BufferedImage deckImgSmall;
    private BufferedImage deckImgSmallDark;
    private BufferedImage tableImg;
    private double cardHeight;
    private double cardWidth;
    private double cardWidthSmall;
    
    private boolean aiHelpSelected = false;
    private boolean stopped = false;
    private LinkedList<Timer> audioQueue = new LinkedList<>();
    private Clip cardPlayClip;
    private Clip pokeClip;
    
    //private LinkedList<Timer> pokeQueue = new LinkedList<>();
    private long pokeTime = 0;
    //private boolean canPoke = false;
    
    private Random random = new Random();
    
    private final int frameQueueSize = 100;
    private long currentTime = 0;
    private int framePointer = 0;
    private int frameTotalTime = 0;
    private int[] frameTimes = new int[frameQueueSize];
    
    public GameCanvas(GameClient client) {
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
        
        tableImg = OhcGraphicsTools.loadImage("resources/client/table.jpg", this);
        cardWidth = (double) deckImg.getWidth() / 9;
        cardHeight = (double) deckImg.getHeight() / 6;
        cardWidthSmall = cardWidth * smallCardScale;
        
        cardPlayClip = OhcGraphicsTools.loadSound("resources/client/Card play.wav", this);
        pokeClip = OhcGraphicsTools.loadSound("resources/client/shortpoke.wav", this);
    }
    
    public double getCardWidth(boolean small) {
        return small ? cardWidthSmall : cardWidth;
    }
    
    public double getCardHeight(boolean small) {
        return small ? cardWidthSmall * cardHeight / cardWidth : cardHeight;
    }
    
    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        
        Graphics2D graphics2 = OhcGraphicsTools.makeGraphics2D(graphics, false);
        graphics2.setFont(OhcGraphicsTools.font);
        
        maxHand = Math.max(maxHand, myPlayer.getHand().size());
        
        graphics2.drawImage(tableImg, 
                0, 0, 
                Math.max(getWidth(), tableImg.getWidth()), 
                Math.max(getHeight(), tableImg.getHeight()), 
                0, 0, 
                tableImg.getWidth(), tableImg.getHeight(), null);
        graphics2.setColor(Color.BLACK);

        if (paintBiddingMarker) {
            paintBidding(graphics2);
        }
        if (paintTrumpMarker) {
            paintTrump(graphics2);
        }
        if (paintPlayersMarker) {
            paintPlayers(graphics2);
        }
        if (aiHelpSelected) {
            paintAiHelp(graphics2);
        }
        paintScores(graphics2);
        if (paintTakenMarker) {
            paintTaken(graphics2);
        }
        if (paintTrickMarker) {
            paintTrick(graphics2);
        }
        if (paintMessageMarker) {
            paintMessage(graphics2);
        }
        if (end) {
            paintFinalScores(graphics2);
        }
        paintChat(graphics2);
        paintButtons(graphics2);
        
        if (currentTime == 0) {
            currentTime = System.currentTimeMillis();
        } else {
            long oldTime = currentTime;
            currentTime = System.currentTimeMillis();
            int newFrameTime = (int) (currentTime - oldTime);
            frameTotalTime += newFrameTime - frameTimes[framePointer];
            frameTimes[framePointer] = newFrameTime;
            framePointer = (framePointer + 1) % frameQueueSize;
            if (frameTimes[frameQueueSize - 1] != 0 && client.showFpsSelected()) {
                OhcGraphicsTools.drawStringJustified(graphics2, "FPS: " + String.format("%.2f", (double) 1000 * frameQueueSize / frameTotalTime), getWidth() - 450 - 100, 10, 0, 1);
            }
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                repaint();
            }
        });
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
        for (CanvasButton bidButton : bidButtons) {
            bidButton.paint(graphics);
        }
    }
    
    public void paintMessage(Graphics graphics) {
        OhcGraphicsTools.drawStringJustifiedBacked(graphics, message, (getWidth() - 450) / 2, getHeight() / 2);
    }
    
    public void paintTrick(Graphics graphics) {
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
    
    public void paintPlayers(Graphics graphics) {
        graphics.setColor(Color.BLACK);
        
        for (ClientPlayer player : players) {
            int x = player.getX();
            int y = player.getY();
            int pos = player.getJust();
            
            if (paintHandMarker) {
                if (player == myPlayer) {
                    for (CanvasCard canvasCard : cardInteractables) {
                        canvasCard.paint(graphics);
                    }
                } else {
                    int handSize = player.getHand().size();
                    int yOffset = 40;
                    int separation = 10;
                    for (int i = 0; i < handSize; i++) {
                        drawCard(graphics, 
                                player.getHand().get(i), 
                                (int)(x + i * separation 
                                        - (handSize - 1) * separation / 2 
                                        - (pos - 1) * maxWid / 2), 
                                y - yOffset, 
                                smallCardScale, 
                                true, 
                                false);
                    }
                }
            }
            
            if (player.getBidding() == 1 || player.isPlaying()) {
                graphics.setColor(new Color(255, 255, 0));
            } else {
                graphics.setColor(Color.WHITE);
            }
            graphics.fillRoundRect((int) (x - pos * maxWid / 2), y - 10, (int) maxWid, 20, 20, 20);
            
            graphics.setColor(Color.BLACK);
            graphics.drawRoundRect((int) (x - pos * maxWid / 2), y - 10, (int) maxWid, 20, 20, 20);
            if (player.isDisconnected()) {
                graphics.setColor(Color.GRAY);
            }
            if (player.isKicked()) {
                graphics.setColor(Color.RED);
            }
            OhcGraphicsTools.drawStringJustified(graphics, 
                    OhcGraphicsTools.fitString(graphics, player.getName(), maxWid), 
                    (int)(x - (pos - 1) * maxWid / 2), 
                    y, 
                    1, 1);
            
            if (player.hasBid()) {
                if (gameState.equals("BIDDING") || gameState.equals("PLAYING") && player.getBid() > player.getTaken()) {
                    graphics.setColor(Color.LIGHT_GRAY);
                } else if (player.getBid() == player.getTaken()) {
                    graphics.setColor(new Color(175, 255, 175));
                } else {
                    graphics.setColor(new Color(255, 175, 175));
                }
                graphics.fillOval((int) (x - pos * maxWid / 2) + 2, y - 8, 16, 16);
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, player.getBid() + "", 
                        (int) (x - pos* maxWid / 2) + 9, 
                        y, 
                        1, 1);
            }
            
            if (player.getIndex() == dealer) {
                graphics.setColor(Color.CYAN);
                graphics.fillOval((int) (x - (pos - 2) * maxWid / 2) - 19, y - 8, 16, 16);
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, "D", 
                        (int) (x - (pos - 2) * maxWid / 2) - 11, 
                        y, 
                        1, 1);
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
                
                if (isLastTrick && !animatingTaken) {
                    lastTrick.paint(graphics);
                } else {
                    drawCard(graphics, new Card(), x, y, smallCardScale, true, false);
                }
            }
        }
    }
    
    public void paintScores(Graphics graphics) {
        //graphics.setColor(Color.LIGHT_GRAY);
        //graphics.fillRect(getWidth() - 450, 0, 450, getHeight());
        
        List<int[]> rounds = client.getRounds();
        int numRounds = rounds.size();
        
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(
                getWidth() - (450 - scoreMargin), 
                scoreMargin, 
                450 - 2 * scoreMargin, 
                scoreVSpacing * (numRounds + 1) + 5,
                10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(
                getWidth() - (450 - scoreMargin), 
                scoreMargin, 
                450 - 2 * scoreMargin, 
                scoreVSpacing * (numRounds + 1) + 5, 
                10, 10);
        
        double wid = (double) (450 - 2 * scoreMargin - 50) / players.size();

        graphics.setColor(Color.BLACK);
        graphics.drawLine(
                getWidth() - (450 - scoreMargin - 45), 
                scoreMargin + scoreVSpacing, 
                getWidth() - scoreMargin - 5, 
                scoreMargin + scoreVSpacing);
        for (ClientPlayer player : players) {
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
                    players.get(round[0]).getName().substring(0, 1), 
                    getWidth() - (450 - scoreMargin - 5), 
                    scoreMargin + scoreVSpacing * (i + 2), 
                    0, 0);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    "" + round[1], 
                    getWidth() - (450 - scoreMargin - 25), 
                    scoreMargin + scoreVSpacing * (i + 2), 
                    0, 0);
        }
    
        for (ClientPlayer player : players) {
            int index = player.getIndex();
            for (int j = 0; j < player.getBids().size(); j++) {
                OhcGraphicsTools.drawStringJustified(graphics, 
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
                OhcGraphicsTools.drawStringJustified(graphics, 
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
                450 - 2 * scoreMargin + 1, 
                getHeight() - chatY - 40 - scoreMargin);
        getComponents()[2].setBounds(
                getWidth() - 450 + scoreMargin, 
                getHeight() - 35 - scoreMargin, 
                450 - 2 * scoreMargin + 1, 
                30);
    }
    
    public void paintFinalScores(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(
                finalScoreOuterMargin, 
                finalScoreOuterMargin, 
                getWidth() - 450 - 2 * finalScoreOuterMargin, 
                getHeight() - finalScoreBottomMargin, 
                10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(
                finalScoreOuterMargin, 
                finalScoreOuterMargin, 
                getWidth() - 450 - 2 * finalScoreOuterMargin, 
                getHeight() - finalScoreBottomMargin, 
                10, 10);
        
        graphics.drawRoundRect(
                finalScoreOuterMargin + finalScoreInnerMargin,
                finalScoreOuterMargin + finalScoreInnerMargin,
                finalScoreListWidth,
                getHeight() - finalScoreBottomMargin - 2 * finalScoreInnerMargin,
                10, 10);
        graphics.setFont(OhcGraphicsTools.fontBold);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Final scores", 
                finalScoreOuterMargin + finalScoreInnerMargin + finalScoreListWidth / 2, 
                finalScoreOuterMargin + finalScoreInnerMargin + 20,
                1, 1);
        graphics.setFont(OhcGraphicsTools.font);
        int points = Integer.MIN_VALUE;
        int place = 1;
        for (int i = 0; i < players.size(); i++) {
            if (endscores[i][1] != points) {
                points = endscores[i][1];
                place = i + 1;
            }
            graphics.setColor(CanvasScorePlot.colors[endscores[i][0]]);
            graphics.fillOval(
                    (int) (finalScoreOuterMargin + 2 * finalScoreInnerMargin - CanvasScorePlot.pointSize / 2), 
                    (int) (finalScoreOuterMargin + finalScoreInnerMargin + 30 + 15 * (i + 1) - CanvasScorePlot.pointSize / 2), 
                    (int) (CanvasScorePlot.pointSize),
                    (int) (CanvasScorePlot.pointSize));
            graphics.setColor(Color.BLACK);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    OhcGraphicsTools.fitString(
                            graphics, 
                            place + ". " 
                            + players.get(endscores[i][0]).getName(), 
                            0.8 * finalScoreListWidth - 2 * finalScoreInnerMargin),
                    finalScoreOuterMargin + 2 * finalScoreInnerMargin + finalScoreInnerMargin / 2, 
                    finalScoreOuterMargin + finalScoreInnerMargin + 30 + 15 * (i + 1), 
                    0, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    endscores[i][1] + "",
                    finalScoreOuterMargin + finalScoreInnerMargin + (int) (0.8 * finalScoreListWidth), 
                    finalScoreOuterMargin + finalScoreInnerMargin + 30 + 15 * (i + 1), 
                    1, 1);
        }
        
        scorePlot.paint(graphics);
    }
    
    public void paintButtons(Graphics graphics) {
        for (CanvasButton button : buttons) {
            button.paint(graphics);
        }
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
        
        OhcGraphicsTools.makeGraphics2D(graphics, !client.lowGraphicsSelected()).drawImage(img, 
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
        
        maxHand = Math.min(10, 51 / players.size());
        maxWid = (maxHand - 1) * 10 + cardWidthSmall;
        message = "";
        paintHandMarker = true;
        paintBiddingMarker = true;
        paintTrumpMarker = true;
        paintPlayersMarker = true;
        paintMessageMarker = false;
        paintTakenMarker = true;
        paintTrickMarker = true;
        end = false;
        
        setPlayerPositions();
        makePlot();
        resetInteractables();
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
                });
            }
        }
    }
    
    public void makePlot() {
        scorePlot = new CanvasScorePlot() {
            @Override
            public int x() {
                return finalScoreOuterMargin
                        + 2 * finalScoreInnerMargin
                        + finalScoreListWidth;
            }
            
            @Override
            public int y() {
                return finalScoreOuterMargin
                        + finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return getWidth() - 450 - x()
                        - finalScoreOuterMargin
                        - finalScoreInnerMargin;
            }
            
            @Override
            public int height() {
                return getHeight() - y()
                        + finalScoreOuterMargin
                        - finalScoreBottomMargin
                        - 2 * finalScoreInnerMargin
                        - 30;
            }
            
            @Override
            public boolean isShown() {
                return end;
            }
        };
    }
    
    public void resetInteractables() {
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
                return canUndoBid
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
                return !end;
            }
            
            @Override
            public boolean isEnabled() {
                return currentTime - pokeTime >= pokeWaitTime
                        && (gameState.equals("BIDDING") || gameState.equals("PLAYING"))
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
                return !end;
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer.getIndex() == dealer 
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
                return !end;
            }
            
            @Override
            public boolean isEnabled() {
                return gameState.equals("PLAYING") && messageState.equals("UNBLOCKED") && actionQueue.isEmpty();
            }
            
            @Override
            public void click() {
                client.makeClaim();
            }
        });
        
        // Post-game back button
        buttons.add(new CanvasButton("Back") {
            @Override
            public int x() {
                return (getWidth() - 450) / 2 - 50;
            }
            
            @Override
            public int y() {
                return getHeight() - 100;
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
                return end;
            }
            
            @Override
            public void click() {
                client.pgBackPressed();
            }
        });
        
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
        
        // Kick buttons
        for (ClientPlayer player : players) {
            final ClientPlayer playerF = player;
            buttons.add(new CanvasButton("Kick") {
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
        
        // Plot buttons
        // Scores button
        buttons.add(new CanvasButton("Scores") {
            @Override
            public int x() {
                return scorePlot.x()
                        + scorePlot.width() / 2
                        - finalScoreInnerMargin / 2
                        - width();
            }
            
            @Override
            public int y() {
                return scorePlot.y()
                        + scorePlot.height()
                        + finalScoreInnerMargin;
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
                return end;
            }
            
            @Override
            public boolean isSelected() {
                return scorePlot.getSelectedTab() == 0;
            }
            
            @Override
            public void click() {
                scorePlot.selectTab(0);
            }
        });
        
        // Win %
        buttons.add(new CanvasButton("Win %") {
            @Override
            public int x() {
                return scorePlot.x()
                        + scorePlot.width() / 2
                        + finalScoreInnerMargin / 2;
            }
            
            @Override
            public int y() {
                return scorePlot.y()
                        + scorePlot.height()
                        + finalScoreInnerMargin;
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
                return end;
            }
            
            @Override
            public boolean isSelected() {
                return scorePlot.getSelectedTab() == 1;
            }
            
            @Override
            public void click() {
                scorePlot.selectTab(1);
            }
        });
    }
    
    public void makeBidInteractables() {
        bidButtons = new LinkedList<>();
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
        interactableMoused = null;
        interactablePressed = null;
        bidButtons = new LinkedList<>();
    }
    
    public void makeHandInteractables() {
        cardInteractables = new LinkedList<>();
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
                    interactableMoused = null;
                    interactablePressed = null;
                    cardInteractables.remove(this);
                    //setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        }
    }
    
    public void setLeader(int leader) {
        this.leader = leader;
    }
    
    public void mousePressed(int x, int y) {
        mouseMoved(x, y);
        if (interactableMoused != null) {
            interactableMoused.setPressed(true);
            interactablePressed = interactableMoused;
        }
    }
    
    public void mouseReleased(int x, int y) {
        if (actionQueue.isEmpty()) {
            if (interactableMoused != null && interactableMoused == interactablePressed) {
                CanvasInteractable relay = interactableMoused;
                interactableMoused = null;
                interactablePressed = null;
                relay.click();
            }
        }
    }
    
    public void mouseMoved(int x, int y) {
        boolean mouseChanged = false;
        interactableMoused = null;
        
        for (CanvasButton bidButton : bidButtons) {
            mouseChanged = checkIfInteractableMoused(bidButton, x, y) || mouseChanged;
        }
        
        for (CanvasCard canvasCard : cardInteractables) {
            mouseChanged = checkIfInteractableMoused(canvasCard, x, y) || mouseChanged;
        }
        
        mouseChanged = checkIfInteractableMoused(lastTrick, x, y) || mouseChanged;
        
        for (CanvasButton button : buttons) {
            mouseChanged = checkIfInteractableMoused(button, x, y) || mouseChanged;
        }
        
        mouseChanged = checkIfInteractableMoused(scorePlot, x, y) || mouseChanged;
        
        if (mouseChanged) {
            //repaint();
        }
    }
    
    public boolean checkIfInteractableMoused(CanvasInteractable inter, int x, int y) {
        boolean out = inter.updateMoused(x, y);
        if (inter.isMoused()) {
            if (interactableMoused != null && interactableMoused != inter) {
                interactableMoused.setMoused(false);
                interactableMoused.setPressed(false);
            }
            interactableMoused = inter;
        }
        return out;
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
    
    public void setAiHelp(boolean aiHelpSelected) {
        this.aiHelpSelected = aiHelpSelected;
        //repaint();
    }
    
    public void playSound(Clip clip) {
        double length = (double) clip.getFrameLength() / 44.1;
        new CanvasTimerEntry((long) length, this, audioQueue) {
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
    
    public void animateCardPlay(int index) {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : animationTime, this, actionQueue) {
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
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : trickStayTime, this, actionQueue) {
            @Override
            public void onLastAction() {
                for (ClientPlayer player : players) {
                    player.resetTrick();
                }
                lastTrick.setMoused(false);
                setLeader(index);
            }
        };
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : animationTime, this, actionQueue) {
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
    
    public void claimReportOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue) {
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
        new CanvasTimerEntry(0, this, actionQueue) {
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
                    cardInteractables = new LinkedList<>();
                }
                messageState = "UNBLOCKED";
                claimer = -1;
            }
        };
    }
    
    public void showMessageOnTimer(String text) {
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : messageTime, this, actionQueue) {
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
        new CanvasTimerEntry(client.devSpeedSelected() ? 1 : messageTime, this, actionQueue) {
            @Override
            public void onFirstAction() {
                if (myPlayer.getBid() == myPlayer.getTaken()) {
                    message = "You made your bid!";
                } else {
                    message = "You went down "
                            + Math.abs(myPlayer.getBid() - myPlayer.getTaken()) + ".";
                }
                paintMessageMarker = true;
                gameState = "ENDOFROUND";
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
        new CanvasTimerEntry(0, this, actionQueue) {
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
        new CanvasTimerEntry(0, this, actionQueue) {
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
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                dealer = dealerI;
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
    
    public void setBiddingOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setBidding(player.getIndex() == index ? 1 : 0);
                }
                for (ClientPlayer player : players) {
                    player.setPlaying(false);
                }
                gameState = "BIDDING";
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
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                for (ClientPlayer player : players) {
                    player.setBidding(0);
                }
                for (ClientPlayer player : players) {
                    player.setPlaying(player.getIndex() == index);
                }
                gameState = "PLAYING";
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
        new CanvasTimerEntry(delay, this, actionQueue) {
            @Override
            public void onLastAction() {
                players.get(index).addBid(bid);
                if (index == myPlayer.getIndex()) {
                    canUndoBid = true;
                } else {
                    canUndoBid = false;
                }
            }
        };
    }
    
    public void removeBidOnTimer(int index) {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onLastAction() {
                if (myPlayer.getBidding() != 0) {
                    removeBidInteractables();
                }
                
                for (ClientPlayer player : players) {
                    player.setBidding(0);
                    player.setPlaying(false);
                }
                
                players.get(index).removeBid();
                gameState = "BIDDING";
                if (myPlayer.getBidding() != 0) {
                    makeBidInteractables();
                }
                resetPokeTime();
            }
        };
    }
    
    public void setPlayOnTimer(int index, Card card, int delay) {
        new CanvasTimerEntry(delay, this, actionQueue) {
            @Override
            public void onLastAction() {
                players.get(index).setTrick(card);
                players.get(index).removeCard(card);
                canUndoBid = false;
            }
        };
    }
    
    public void setScoresOnTimer(List<Integer> scores) {
        new CanvasTimerEntry(0, this, actionQueue) {
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
    
    public void setFinalScoresOnTimer(List<Integer> scores) {
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
                endscores = new int[scores.size() / 2][2];
                int i = 0;
                for (Integer score : scores) {
                    endscores[i / 2][i % 2] = score;
                    i++;
                }
                client.goToPostGame();
                gameState = "POSTGAME";
                
                for (ClientPlayer player : players) {
                    List<Integer> scoresAug = new LinkedList<>();
                    scoresAug.add(0);
                    for (Integer score : player.getScores()) {
                        scoresAug.add(score);
                    }
                    scorePlot.addIntData(0, player.getName(), scoresAug);
                }
                
                boolean[] winners = new boolean[players.size()];
                int winningScore = Integer.MIN_VALUE;
                for (ClientPlayer player : players) {
                    winningScore = Math.max(winningScore, player.getScores().get(player.getScores().size() - 1));
                }
                for (ClientPlayer player : players) {
                    if (player.getScores().get(player.getScores().size() - 1) == winningScore) {
                        winners[player.getIndex()] = true;
                    }
                }
                
                List<List<Double>> probs = new ArrayList<>(players.size());
                for (int k = 0; k < players.size(); k++) {
                    probs.add(new ArrayList<>(players.get(0).getScores().size()));
                    probs.get(k).add(100D / players.size());
                }
                BootstrapAggregator winModel = new BootstrapAggregator("resources/models/wb" + players.size() + ".txt");
                for (int j = 0; j < players.get(0).getScores().size(); j++) {
                    double[] in = new double[players.size() + 1];
                    for (int k = 0; k < players.size(); k++) {
                        in[k] = (double) players.get(k).getScores().get(j);
                    }
                    in[players.size()] = (double) (players.get(0).getScores().size() - 1 - j);
                    double[] out = winModel.testValue(new BasicVector(in)).get(1).toArray();
                    for (int k = 0; k < players.size(); k++) {
                        probs.get(k).add(out[k] * 100);
                    }
                }
                for (int k = 0; k < players.size(); k++) {
                    if (winners[k]) {
                        probs.get(k).set(players.get(0).getScores().size(), 100D);
                    } else {
                        probs.get(k).set(players.get(0).getScores().size(), 0D);
                    }
                    scorePlot.addData(1, players.get(k).getName(), probs.get(k));
                }
                
                List<int[]> rounds = client.getRounds();
                List<String> ticks = new ArrayList<>(rounds.size());
                ticks.add("");
                for (int[] round : rounds) {
                    ticks.add(round[1] + "");
                }
                scorePlot.addTicks(0, ticks);
                scorePlot.addTicks(1, ticks);
            }
        };
    }
    
    public void setConnectionStatusOnTimer(boolean connected) {
        new CanvasTimerEntry(0, this, actionQueue) {
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
    
    /*public void deletePokeTimer() {
        canPoke = false;
        if (!pokeQueue.isEmpty()) {
            pokeQueue.removeFirst().stop();
        }
    }
    
    public void addPokeTimer() {
        deletePokeTimer();
        new CanvasTimerEntry(pokeTime, this, pokeQueue) {
            @Override
            public void onLastAction() {
                canPoke = true;
                //repaint();
            }
        };
    }*/
    
    public void resetPokeTime() {
        pokeTime = currentTime;
    }
    
    public void bePokedOnTimer() {
        new CanvasTimerEntry(0, this, actionQueue) {
            @Override
            public void onFirstAction() {
                //showMessageOnTimer("You've been poked.");
                playSound(pokeClip);
            }
        };
    }
    
    public void addStopper() {
        new CanvasTimerEntry(0, this, actionQueue) {
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