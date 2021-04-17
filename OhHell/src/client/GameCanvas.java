package client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sound.sampled.Clip;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;

import core.Card;
import core.GameOptions;
import core.Recorder;
import common.Constants;
import common.FileTools;
import common.GraphicsTools;
import common.OhcScrollPane;
import common.OhcTextField;

public class GameCanvas extends OhcCanvas {
    private static final long serialVersionUID = 1L;

    ////// Parameters /////////////////
    public static final int cardSeparation = 40;
    public static final double smallCardScale = 0.6667;
    public static final double trumpCardScale = 1;
    public static final double trickCardScale = 1;
    public static final int handYOffset = 105;
    public static final int takenXSeparation = 10;
    public static final int takenYSeparation = 5;
    public static final int lastTrickSeparation = 20;
    public static final int preselectedCardYOffset = 50;

    public static final int maxUndos = 2;
    
    public static final int scoreVSpacing = 20;
    public static final int scoreMargin = 10;
    public static final int bidStayTime = 1500;
    public static final int trickStayTime = 1500;
    public static final int animationTime = 150;
    public static final int messageTime = 2000;

    public static final int finalScoreMaxWidth = 900;
    public static final int finalScoreMaxHeight = 550;
    public static final int finalScoreOuterMargin = 25;
    public static final int finalScoreInnerMargin = 10;
    public static final int finalScoreListWidth = 200;
    public static final int finalScoreBottomMargin = 150;

    public static final int pokeWaitTime = 25000;

    public static final int chatAreaHeight = 250;
    public static final int maxChatLines = 200;
    ///////////////////////////////////

    private volatile GameClient client;

    private List<ClientPlayer> players;
    private List<ClientPlayer> playersScoreSorted;
    private ClientPlayer myPlayer;
    
    private HashMap<Integer, ClientTeam> teams;
    private List<ClientTeam> teamsIndexSorted;
    private List<ClientTeam> teamsScoreSorted;

    private double maxWid;

    private Card trump;
    private List<Card> trumps;

    private boolean paintMessageMarker = false;

    private String message = "";
    private String messageState = "UNBLOCKED";

    private List<CanvasEmbeddedSwing> embeddedSwings = new LinkedList<>();
    private List<CanvasButton> bidButtons = new LinkedList<>();
    private List<CanvasCard> cardInteractables = new LinkedList<>();
    private List<PlayerNamePlate> namePlates = new LinkedList<>();
    private CanvasCard lastTrick;
    private List<CanvasButton> kickButtons = new LinkedList<>();
    private List<CanvasInteractable> miscInteractables = new LinkedList<>();

    private int scoreWidth = 450;
    private double scoreChatDivision = 0.6;
    
    private JTextPane chatArea = new JTextPane();
    private String[] chatLines = new String[maxChatLines];
    private int chatLinePointer = 0;
    private List<String> myChatMemo = new ArrayList<>();
    private int myChatMemoPointer = 0;

    private LinkedList<Timer> actionQueue = new LinkedList<>();
    private boolean performingAction = false;

    private boolean trickTaken = false;
    private List<CanvasCard> preselectedCards = new ArrayList<>();
    private Set<CanvasCard> playedCards = new HashSet<>();
    private int cardJustPlayed = 0;
    private boolean animatingTaken = false;
    private double takenTimer = 1;

    private boolean canUndoBid = false;
    private boolean showBidDots = false;
    private long undoBidTimer = -1;
    private int bidCount = 0;

    private boolean claimBlocked = false;
    private int claimer = -1;

    private int dealer = -1;
    private int leader;

    private boolean showOneCard = false;

    private CanvasScoreSheet scoreSheet;
    
    private PostGamePage postGamePage;
    private List<ClientPlayer> postGamePlayers;
    private List<ClientPlayer> postGamePlayersScoreSorted;
    private HashMap<Integer, ClientTeam> postGameTeams;
    private List<ClientTeam> postGameTeamsIndexSorted;
    private List<ClientTeam> postGameTeamsScoreSorted;
    private List<int[]> postGameRounds;
    private GameOptions postGameOptions;

    private GameState state = GameState.PREGAME;

    private BufferedImage deckImg;
    private BufferedImage deckImgDark;
    private BufferedImage deckImgSmall;
    private BufferedImage deckImgSmallDark;
    private double cardHeight;
    private double cardWidth;
    private double cardHeightSmall;
    private double cardWidthSmall;

    private boolean stopped = false;
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

        deckImg = FileTools.loadImage("resources/client/deck2.png", this);
        deckImgSmall = FileTools.loadImage("resources/client/deck2small.png", this);

        cardWidth = (double) deckImg.getWidth() / 9;
        cardHeight = (double) deckImg.getHeight() / 6;
        cardWidthSmall = cardWidth * smallCardScale;
        cardHeightSmall = cardWidthSmall * cardHeight / cardWidth;
        
        deckImgDark = FileTools.loadImage("resources/client/deck2.png", this);
        Graphics didg = deckImgDark.getGraphics();
        deckImgSmallDark = FileTools.loadImage("resources/client/deck2small.png", this);
        Graphics disdg = deckImgSmallDark.getGraphics();
        didg.setColor(new Color(127, 127, 127, 67));
        disdg.setColor(new Color(127, 127, 127, 67));
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 6; j++) {
                didg.fillRoundRect(
                        (int) (cardWidth * i), 
                        (int) (cardHeight * j), 
                        (int) (cardWidth), 
                        (int) (cardHeight),
                        20, 20);
                disdg.fillRoundRect(
                        (int) (cardWidthSmall * i), 
                        (int) (cardHeightSmall * j), 
                        (int) (cardWidthSmall), 
                        (int) (cardHeightSmall), 
                        20, 20);
            }
        }

        cardPlayClip = FileTools.loadSound("resources/client/Card play.wav", this);
        pokeClip = FileTools.loadSound("resources/client/shortpoke.wav", this);

        players = new ArrayList<>();
        playersScoreSorted = new ArrayList<>();
        teams = new HashMap<>();
        teamsIndexSorted = new ArrayList<>();
        teamsScoreSorted = new ArrayList<>();
    }

    public double getCardWidth(boolean small) {
        return small ? cardWidthSmall : cardWidth;
    }

    public double getCardHeight(boolean small) {
        return small ? cardWidthSmall * cardHeight / cardWidth : cardHeight;
    }

    @Override
    public int backgroundCenterX() {
        return (getWidth() - scoreWidth) / 2;
    }

    @Override
    public int backgroundCenterY() {
        return getHeight() / 2;
    }

    @Override
    public void customPaintFirst(Graphics graphics) {
        graphics.setColor(Color.BLACK);

        paintTrump(graphics);
        paintPlayers(graphics);
        paintTaken(graphics);

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
                graphics.setColor(Color.BLACK);
                GraphicsTools.drawStringJustified(graphics,
                        "FPS: " + String.format("%.2f", (double) 1000 * frameQueueSize / frameTotalTime),
                        getWidth() - scoreWidth - 100, 10, 0, 1);
            }
        }
        if (client.showPingSelected()) {
            graphics.setColor(Color.BLACK);
            GraphicsTools.drawStringJustified(graphics, "Ping: " + String.format("%.2f", client.getPing()) + "ms",
                    getWidth() - scoreWidth - 100, 25, 0, 1);
        }

        if (!performingAction && !actionQueue.isEmpty()) {
            performingAction = true;
            actionQueue.remove().start();
        }
        
        // It is possible for a card to be added to preselectedCards twice, probably due to some 
        // multithreading issue. This is supposed to fix this when it happens.
        if (!preselectedCards.isEmpty() && playedCards.contains(preselectedCards.get(0))) {
            preselectedCards.remove(0);
        }
    }

    @Override
    public void customPaintLast(Graphics graphics) {
        paintTrick(graphics);
        if (paintMessageMarker) {
            paintMessage(graphics);
        }
    }

    public void paintTrump(Graphics graphics) {
        if (state == GameState.PREGAME || state != GameState.POSTGAME && dealer > -1) {
            graphics.setColor(Color.BLACK);
            double scaleFix = 1;
            if (players.size() >= 6 && getHeight() < 600) {
                scaleFix = 0.75;
            }
            int x = 50;// state == GameState.PREGAME ? 50 :
                       // players.get(dealer).getTrumpX();
            int y = 66;// state == GameState.PREGAME ? 66 :
                       // players.get(dealer).getTrumpY();

            drawCard(graphics, new Card(), x - 4, y - 4, trumpCardScale * scaleFix, true, true);
            drawCard(graphics, new Card(), x - 2, y - 2, trumpCardScale * scaleFix, true, true);
            drawCard(graphics, state == GameState.PREGAME || trump == null ? new Card() : trump, x, y,
                    trumpCardScale * scaleFix, true, false);
        }
    }

    public void paintBidding(Graphics graphics) {
        for (CanvasButton bidButton : bidButtons) {
            bidButton.paint(graphics);
        }
    }

    public void paintMessage(Graphics graphics) {
        GraphicsTools.drawStringJustifiedBacked(graphics, message, (getWidth() - scoreWidth) / 2, getHeight() / 2);
    }

    public void paintTrick(Graphics graphics) {
        if (state != GameState.PREGAME && state != GameState.POSTGAME) {
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < players.size(); i++) {
                int iRelToLeader = (leader + i) % players.size();
                int iRelToMe = (iRelToLeader - myPlayer.getIndex() + players.size()) % players.size();
                ClientPlayer player = players.get(iRelToLeader);
                if (!player.isKicked() && !player.getTrick().isEmpty()) {
                    if (player.getTrickRad() == -1) {
                        int baseTrickRad = players.size() >= 8 ? 110 : 70;
                        player.setTrickRad((int) (baseTrickRad + 10 * random.nextDouble()));
                    }

                    double startx = player.getX();
                    double starty = player.getY();

                    if (iRelToLeader == myPlayer.getIndex()) {
                        startx = (getWidth() - scoreWidth) / 2 + cardJustPlayed * cardSeparation
                                - (myPlayer.getHand().size()) * cardSeparation / 2;
                        starty = getHeight() - handYOffset;
                    }

                    double endx = (getWidth() - scoreWidth) / 2
                            - player.getTrickRad() * Math.sin(2 * Math.PI * iRelToMe / players.size());
                    double endy = getHeight() / 2 - 50
                            + player.getTrickRad() * Math.cos(2 * Math.PI * iRelToMe / players.size());

                    int x = (int) (player.getTrickTimer() * endx + (1 - player.getTrickTimer()) * startx);
                    int y = (int) (player.getTrickTimer() * endy + (1 - player.getTrickTimer()) * starty);
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
                        drawCard(graphics, player.getHand().get(i),
                                (int) (x + i * separation - (handSize - 1) * separation / 2 - (pos - 1) * maxWid / 2),
                                y - yOffset, smallCardScale, true, false);
                    }
                }
            }
            
            if (showBidDots && !myPlayer.isKibitzer()) {
                for (int i = 0; i < maxUndos + 1; i++) {
                    if (i < bidCount) {
                        graphics.setColor(Color.WHITE);
                    } else {
                        graphics.setColor(new Color(175, 175, 175));
                    }
                    graphics.fillOval(
                            (getWidth() - scoreWidth) / 2 
                                - (4 * (maxUndos + 1) + 10 * maxUndos) / 2 
                                + 14 * i, 
                            getHeight() - 210 - 15 - 40
                                - (!preselectedCards.isEmpty() ? preselectedCardYOffset + 20 : 0), 
                            4, 4);
                }
            }
        }
    }

    public void paintTaken(Graphics graphics) {
        if (state != GameState.PREGAME && state != GameState.POSTGAME) {
            for (ClientPlayer player : players) {
                for (int j = 0; j < player.getTaken(); j++) {
                    int takenX = player.getTakenX();
                    int takenY = player.getTakenY();

                    boolean isLastTrick = player.getIndex() == leader && j == player.getTaken() - 1;

                    double timer = 1;
                    if (isLastTrick)
                        timer = takenTimer;

                    int x = takenX + takenXSeparation * j;
                    int y = takenY + takenYSeparation * j;
                    if (animatingTaken && isLastTrick) {
                        x = (int) (timer * (takenX + takenXSeparation * j) + (1 - timer) * (getWidth() - scoreWidth) / 2);
                        y = (int) (timer * (takenY + takenYSeparation * j) + (1 - timer) * getHeight() / 2);
                    }

                    if (!isLastTrick || animatingTaken) {
                        drawCard(graphics, new Card(), x, y, smallCardScale, true, false);
                    }
                }
            }
        }
    }

    public void chat(String text) {
        addChatLine(formatForHtml(text));
        refreshChat();
    }

    public void addChatLine(String text) {
        chatLines[chatLinePointer] = text;
        chatLinePointer = (chatLinePointer + 1) % maxChatLines;
    }

    public void refreshChat() {
        StringBuilder htmlBuilder = new StringBuilder("<html><p style=\"font-family:'Arial'\">");
        for (int i = 0; i < maxChatLines; i++) {
            String line = chatLines[(chatLinePointer + i) % maxChatLines];
            if (line != null && !line.isEmpty()) {
                htmlBuilder.append(line + "<br />");
            }
        }
        htmlBuilder.append("</p></html>");
        String html = htmlBuilder.toString();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                chatArea.setText(html);
            }
        });
    }

    public static String formatForHtml(String text) {
        String[] words = text.split(" ");

        StringBuilder ans = new StringBuilder();
        for (String word : words) {
            if (Pattern.matches(Constants.urlRegex, word)) {
                ans.append(" <a href='" + word + "'>" + escapeHtml(word) + "</a>");
            } else {
                ans.append(" " + escapeHtml(word));
            }
        }
        String str = ans.toString();
        if (!text.isEmpty() && text.charAt(0) == '*') {
            str = "<i style=\"color:blue\">" + str + "</i>";
        }
        return str;
    }

    public static String escapeHtml(String text) {
        String ans = text;
        for (String[] rep : Constants.htmlReservedChars) {
            ans = ans.replace(rep[0], rep[1]);
        }
        return ans;
    }

    public void drawCard(Graphics graphics, Card card, double x, double y, double scale, boolean small, boolean dark) {
        drawCard(graphics, card, x, y, scale, small, dark, -1);
    }

    public void drawCard(Graphics graphics, Card card, double x, double y, double scale, boolean small, boolean dark,
            double maxY) {
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
            maxY = y + ch1 * scale / 2;
        }
        maxY = Math.min(maxY, y + ch1 * scale / 2);
        double diff = maxY - (y - ch1 * scale / 2);

        GraphicsTools.makeGraphics2D(graphics, client.antialiasingSelected(), !client.lowGraphicsSelected()).drawImage(
                img, (int) (x - cw1 * scale / 2), (int) (y - ch1 * scale / 2), (int) (x + cw1 * scale / 2), (int) maxY,
                (int) (col * cw1), (int) (row * ch1), (int) ((col + 1) * cw1), (int) (row * ch1 + diff / scale), null);
    }

    public void doShowOneCard() {
        showOneCard = true;
    }

    public void setState(GameState newState) {
        state = newState;
    }

    public GameState getState() {
        return state;
    }
    
    @Override
    public void rightClick(int x, int y) {
        preselectedCards.clear();
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
            int index = (player.getIndex() - myPlayer.getIndex() + players.size() - 1) % players.size();
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
                });
            } else if (index < cut2) {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return (getWidth() - scoreWidth) * (index - cut1 + 1) / (cut2 - cut1 + 1);
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
                });
            } else if (index < players.size() - 1) {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return getWidth() - scoreWidth - 10;
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
                });
            } else {
                player.setPos(new CanvasPlayerPosition() {
                    @Override
                    public int x() {
                        return (getWidth() - scoreWidth) / 2;
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
                });
            }
        }
    }

    
    public void resetNamePlates() {
        namePlates.clear();
        for (ClientPlayer player : players) {
            final ClientPlayer playerF = player;
            namePlates.add(new PlayerNamePlate(this, playerF) {
                @Override
                public boolean isEnabled() {
                    return state != GameState.PREGAME && state != GameState.POSTGAME
                            && currentTime - pokeTime >= (long) pokeWaitTime * 1000000
                            && (playerF.isPlaying() && state == GameState.PLAYING
                                    || state == GameState.BIDDING && playerF.getBidding() != 0)
                            && !myPlayer.isKibitzer() && client.getClientState() == ClientState.IN_MULTIPLAYER_GAME;
                }

                @Override
                public void click() {
                    if (isEnabled()) {
                        resetPokeTime();
                        client.pokePlayer();
                    }
                }
            });
        }
    }

    public double getMaxWid() {
        return maxWid;
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
    
    public int getScoreWidth() {
        return scoreWidth;
    }
    
    public int scoreHeight() {
        int numRounds = getRoundsForScoreSheet().size();
        return scoreVSpacing * (numRounds + 1 + (client.getGameOptions().isTeams() ? 1 : 0))
                + CanvasScoreSheet.lineV
                + 3 * CanvasScoreSheet.margin
                + CanvasScoreSheet.sortByHeight 
                + CanvasScoreSheet.bidInfoHeight;
    }

    public void resetInteractables() {
        for (CanvasEmbeddedSwing inter : embeddedSwings) {
            inter.dispose();
        }

        for (Component c : getComponents()) {
            remove(c);
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
                return trickTaken && state == GameState.PLAYING;
            }

            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                if (isMoused()) {
                    List<ClientPlayer> unkickedPlayers = players.stream().filter(p -> !p.isKicked())
                            .collect(Collectors.toList());
                    for (int k = 0; k < unkickedPlayers.size(); k++) {
                        int x0 = Math.min(xCenter() + 50,
                                (int) (getWidth() - scoreWidth - lastTrickSeparation * (unkickedPlayers.size() - 1)
                                        - cardWidth * trickCardScale / 2 - 10));
                        int y0 = Math.max(yCenter(), (int) (cardWidth * trickCardScale / 2 + 10));

                        drawCard(graphics, unkickedPlayers.get(k).getLastTrick(), x0 + lastTrickSeparation * k, y0,
                                trickCardScale, true, false);
                    }
                }
            }
        };

        // Buttons
        miscInteractables = new LinkedList<>();

        // Pre-game interactables
        PreGameMenu preGameMenu = new PreGameMenu(client, this) {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2
                        - PreGameMenu.menuWidth / 2
                        - (client.getGameOptions().isTeams() ? 
                                PreGameMenu.menuTeamGap 
                                + PreGameTeamPanel.teamWidth : 0) / 2;
            }

            @Override
            public int y() {
                return getHeight() / 2 - height() / 2;
            }

            @Override
            public int width() {
                return PreGameMenu.menuWidth;
            }

            @Override
            public int height() {
                return 340;
            }

            @Override
            public boolean isShown() {
                return state == GameState.PREGAME;
            }
        };
        
        PreGameTeamPanel preGameTeamPanel = new PreGameTeamPanel(client, this) {
            @Override
            public int x() {
                return preGameMenu.x() + preGameMenu.width() + PreGameMenu.menuTeamGap;
            }

            @Override
            public int y() {
                return getHeight() / 2 - height() / 2;
            }

            @Override
            public int width() {
                return PreGameTeamPanel.teamWidth;
            }

            @Override
            public boolean isShown() {
                return state == GameState.PREGAME 
                        && client.getGameOptions().isTeams();
            }
        };

        // Undo bid button
        miscInteractables.add(new CanvasButton("Undo bid") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 - 40;
            }

            @Override
            public int y() {
                return getHeight() - 210 - 15
                        - (!preselectedCards.isEmpty() ? preselectedCardYOffset + 20 : 0);
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
                        && !myPlayer.isKibitzer();
            }

            @Override
            public void click() {
                client.undoBid();
                canUndoBid = false;
            }

            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                if (isShown() && undoBidTimer != -1) {
                    double progress = Math.min((double) 
                            (System.currentTimeMillis() - undoBidTimer) / client.getGameOptions().getRobotDelay(), 
                            1);
                    graphics.setColor(new Color(175, 175, 175));
                    graphics.fillRect((int) (x() + 0.05 * width()), y() - 20, (int) (0.9 * width()), 3);
                    graphics.setColor(new Color(255, 255, 255));
                    graphics.fillRect((int) (x() + 0.05 * width()), y() - 20, (int) (0.9 * width() * progress), 3);
                }
            }
        });

        // One-round show card button
        miscInteractables.add(new CanvasButton("Show card") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 - 40;
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
                return state == GameState.BIDDING && myPlayer.getIndex() == dealer && !myPlayer.isKibitzer()
                        && client.isOneRound() && !showOneCard;
            }

            @Override
            public void click() {
                doShowOneCard();
            }
        });

        // Claim button
        miscInteractables.add(new CanvasButton("Claim") {
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
                return state == GameState.PLAYING && messageState.equals("UNBLOCKED") && !claimBlocked
                        && !myPlayer.isKibitzer();
            }

            @Override
            public void click() {
                makeClaimOnTimer();
            }
        });

        // End game button
        miscInteractables.add(new CanvasButton("End game") {
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
        miscInteractables.add(new CanvasButton("Back to lobby") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 - (numPgButtons * width() + (numPgButtons - 1) * 20) / 2
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

            @Override
            public void click() {
                if (client.getClientState() == ClientState.SINGLE_PLAYER_POST_GAME) {
                    client.changeState(ClientState.IN_SINGLE_PLAYER_GAME);
                    pregameOnTimer(false);
                    updatePlayersOnTimer();
                } else if (client.getClientState() == ClientState.MULTIPLAYER_POST_GAME) {
                    client.changeState(ClientState.IN_MULTIPLAYER_GAME);
                    pregameOnTimer(false);
                    updatePlayersOnTimer();
                } else {
                    client.changeState(ClientState.MAIN_MENU);
                }
            }
        });

        // Leave
        miscInteractables.add(new CanvasButton("Leave table") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 - (numPgButtons * width() + (numPgButtons - 1) * 20) / 2
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
        miscInteractables.add(new CanvasButton("Save game") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 - (numPgButtons * width() + (numPgButtons - 1) * 20) / 2
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
        miscInteractables.add(new CanvasButton("Accept") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 - 125;
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
            }
        });

        // Claim refuse button
        miscInteractables.add(new CanvasButton("Refuse") {
            @Override
            public int x() {
                return (getWidth() - scoreWidth) / 2 + 25;
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
            }
        });

        // Post-game page
        postGamePage = new PostGamePage() {
            @Override
            public int x() {
                return Math.max((getWidth() - scoreWidth) / 2 - finalScoreMaxWidth / 2, finalScoreOuterMargin);
            }

            @Override
            public int y() {
                return Math.max((getHeight() - finalScoreBottomMargin) / 2 - finalScoreMaxHeight / 2,
                        finalScoreOuterMargin);
            }

            @Override
            public int width() {
                return Math.min(finalScoreMaxWidth, getWidth() - scoreWidth - 2 * finalScoreOuterMargin);
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

        // Score sheet
        scoreSheet = new CanvasScoreSheet(this) {
            @Override
            public int x() {
                return getWidth() - (scoreWidth - scoreMargin);
            }

            @Override
            public int y() {
                return scoreMargin;
            }

            @Override
            public int width() {
                return scoreWidth - 2 * scoreMargin;
            }

            @Override
            public int height() {
                return Math.min(scoreHeight(), (int) (scoreChatDivision * getHeight() - scoreMargin * 2));
            }

            @Override
            public boolean isShown() {
                return state != GameState.PREGAME;
            }
        };

        // Chat areas
        chatArea.setContentType("text/html; charset=UTF-8");
        if (chatArea.getHyperlinkListeners().length == 0) {
            chatArea.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }
        chatArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        CanvasEmbeddedSwing chatScrollPane = new CanvasEmbeddedSwing(new OhcScrollPane(chatArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), this) {
            private boolean showing = false;

            @Override
            public int x() {
                return getWidth() - scoreWidth + scoreMargin;
            }

            @Override
            public int y() {
                return Math.max(getHeight() - chatAreaHeight - 30 - 5 - scoreMargin, (int) (scoreChatDivision * getHeight() + scoreMargin));
            }

            @Override
            public int width() {
                return scoreWidth - 2 * scoreMargin + 1;
            }

            @Override
            public int height() {
                return getHeight() - y() - 40 - scoreMargin;
            }

            @Override
            public boolean isShown() {
                boolean ans = client.getClientState() != ClientState.FILE_VIEWER;
                if (!showing && ans) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            chatArea.updateUI();
                        }
                    });
                    showing = true;
                } else if (showing && !ans) {
                    showing = false;
                }
                return ans;
            }
        };

        JTextField chatJField = new OhcTextField("Enter text");
        chatJField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    myChatMemoPointer = Math.max(0, myChatMemoPointer - 1);
                    if (!myChatMemo.isEmpty()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                chatJField.setText(myChatMemo.get(myChatMemoPointer));
                            }
                        });
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    myChatMemoPointer = Math.min(myChatMemo.size(), myChatMemoPointer + 1);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            chatJField.setText(
                                    myChatMemoPointer == myChatMemo.size() ? "" : myChatMemo.get(myChatMemoPointer));
                        }
                    });
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    String fieldText = chatJField.getText().trim();
                    if (e.getKeyCode() == KeyEvent.VK_ENTER && !fieldText.isEmpty()) {
                        if (fieldText.charAt(0) == '/') {
                            String[] commandContent = fieldText.substring(1).split(" ", 2);
                            String command = commandContent[0];
                            String content = commandContent.length > 1 ? commandContent[1].trim() : "";
                            
                            // Break into args
                            List<String> args = new ArrayList<>();
                            StringBuilder currentWord = new StringBuilder();
                            boolean inQuotes = false;
                            for (int i = 0; i <= content.length(); i++) {
                                if (i == content.length() 
                                        || content.charAt(i) == ' ' && !inQuotes) {
                                    args.add(currentWord.toString());
                                    currentWord = new StringBuilder();
                                } else if (content.charAt(i) == '"') {
                                    inQuotes = !inQuotes;
                                } else {
                                    currentWord.append(content.charAt(i));
                                }
                            }
                            
                            if (command.equals("bitcoin")) {
                                addChatLine("<b style=\"color:green\">BITCOIN MODE ENABLED $$</b>");
                                refreshChat();
                            } else if (command.equals("hotdog")) {
                                int total = players.stream().map(p -> (Integer) p.getBid()).reduce(0, (a, b) -> a + b);
                                int h = thisRound()[1];
                                if (total < h) {
                                    addChatLine("<b>Underbid by " + (h - total) + "</b>");
                                } else {
                                    addChatLine("<b>Overbid by " + (total - h) + "</b>");
                                }
                                refreshChat();
                            } else if (command.equals("kibitzers")) {
                                List<ClientPlayer> kibitzers = client.getKibitzers();
                                addChatLine("<b>Kibitzers:" + (kibitzers.size() == 0 ? " none" : "") + "</b>");
                                for (ClientPlayer kibitzer : kibitzers) {
                                    addChatLine("<b>" + kibitzer.getName() + " (" + kibitzer.getId() + ")</b>");
                                }
                                refreshChat();
                            } else if (command.equals("w") && commandContent.length == 2) {
                                if (args.size() >= 2) {
                                    String name = args.get(0);
                                    String message = args.subList(1, args.size()).stream()
                                            .reduce("", (a, b) -> a + " " + b);
                                    client.sendChat(name, message);
                                }
                            } else if (command.equals("renameteam")) { 
                                if (args.size() >= 1) {
                                    client.renameTeam(args.get(0));
                                }
                            } else if (!command.isEmpty()) {
                                addChatLine("<b style=\"color:red\">Invalid command</b>");
                                refreshChat();
                            }
                        } else {
                            client.sendChat("", fieldText);
                        }
                        myChatMemo.add(fieldText);
                        myChatMemoPointer = myChatMemo.size();
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
        CanvasEmbeddedSwing chatField = new CanvasEmbeddedSwing(chatJField, this) {
            @Override
            public int x() {
                return getWidth() - scoreWidth + scoreMargin;
            }

            @Override
            public int y() {
                return getHeight() - 35 - scoreMargin;
            }

            @Override
            public int width() {
                return scoreWidth - 2 * scoreMargin + 1;
            }

            @Override
            public int height() {
                return 30;
            }

            @Override
            public boolean isShown() {
                return chatScrollPane.isShown();
            }
        };
        embeddedSwings.add(chatScrollPane);
        embeddedSwings.add(chatField);
        
        // Vertical score divider
        miscInteractables.add(new CanvasDivider(false, scoreWidth) {
            @Override
            public int x() {
                return getWidth() - scoreWidth;
            }
            
            @Override
            public int y() {
                if (state == GameState.PREGAME) {
                    return chatScrollPane.y();
                } else {
                    return scoreMargin;
                }
            }
            
            @Override
            public int width() {
                return 3;
            }
            
            @Override
            public int height() {
                if (client.getClientState() == ClientState.FILE_VIEWER) {
                    return scoreSheet.height() - CanvasScoreSheet.bidInfoHeight;
                } else {
                    return getHeight() - y() - scoreMargin;
                }
            }
            
            @Override
            public double min() {
                return 400;
            }
            
            @Override
            public double max() {
                return 750;
            }
            
            @Override
            public boolean isShown() {
                return true;
            }
            
            @Override
            public void drag(int x, int y) {
                setValue(getWidth() - x);
            }
            
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                scoreWidth = (int) getValue();
            }
        });
        
        // Score-chat divider
        miscInteractables.add(new CanvasDivider(true, scoreChatDivision) {
            @Override
            public int x() {
                return getWidth() - scoreWidth + scoreMargin;
            }
            
            @Override
            public int y() {
                return (int) (scoreChatDivision * getHeight() - 1);
            }
            
            @Override
            public int width() {
                return scoreWidth - 2 * scoreMargin;
            }
            
            @Override
            public int height() {
                return 3;
            }
            
            @Override
            public double min() {
                if (chatScrollPane.isShown()) {
                    return (double) (getHeight() - chatAreaHeight - 30 - 5 - 2 * scoreMargin) / getHeight();
                } else {
                    return 1;
                }
            }
            
            @Override
            public double max() {
                if (chatScrollPane.isShown()) {
                    return Math.min(
                            (double) (scoreHeight() + 2 * scoreMargin) / getHeight(),
                            (double) (getHeight() - 30 - 2 * scoreMargin) / getHeight());
                } else {
                    return 1;
                }
            }
            
            @Override
            public boolean isShown() {
                if (chatScrollPane.isShown()) {
                    int numRounds = getRoundsForScoreSheet().size();
                    int height = scoreVSpacing * (numRounds + 1) + CanvasScoreSheet.lineV + 2 * CanvasScoreSheet.margin;
                    return height + chatAreaHeight + 30 + 4 * scoreMargin > getHeight();
                } else {
                    return false;
                }
            }
            
            @Override
            public void drag(int x, int y) {
                setValue((double) y / getHeight());
            }
            
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                scoreChatDivision = getValue();
            }
        });

        setInteractables(Arrays.asList(
                embeddedSwings, 
                Arrays.asList(preGameMenu, preGameTeamPanel),
                Arrays.asList(scoreSheet), 
                bidButtons, 
                cardInteractables,
                namePlates, 
                Arrays.asList(lastTrick), 
                kickButtons, 
                miscInteractables, 
                Arrays.asList(postGamePage)));
    }

    public void makeBidInteractables() {
        bidButtons.clear();
        for (int i = 0; i <= myPlayer.getHand().size(); i++) {
            final int bid = i;
            bidButtons.add(new CanvasButton(bid + "") {
                @Override
                public int x() {
                    return (getWidth() - scoreWidth) / 2 + bid * 40 - myPlayer.getHand().size() * 40 / 2 - 15;
                }

                @Override
                public int y() {
                    return getHeight() - 210 - 15
                            - (preselectedCards.isEmpty() ? 0 : preselectedCardYOffset + 20);
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
                @Override
                public int index() {
                    return myPlayer.getHand().indexOf(getCard());
                }

                @Override
                public int xCenter() {
                    return (getWidth() - scoreWidth) / 2 + index() * cardSeparation
                            - (myPlayer.getHand().size() - 1) * cardSeparation / 2;
                }

                @Override
                public int yCenter() {
                    return getHeight() - handYOffset 
                            - (preselectedCards.contains(this) ? preselectedCardYOffset : 0);
                }

                @Override
                public int yPaintOffset() {
                    return isMoused() ? -10 : 0;
                }

                @Override
                public boolean isEnabled() {
                    return state == GameState.BIDDING
                            && myPlayer.hasBid()
                            || state == GameState.PLAYING
                            && (!myPlayer.isPlaying() 
                                    || myPlayer.isPlaying() 
                                    && canPlayThis(getCard())
                                    && preselectedCards.isEmpty());
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
                    if (myPlayer.isPlaying() && myPlayer.getTrick().isEmpty()) {
                        if (preselectedCards.isEmpty()) {
                            playCard(this);
                        } else {
                            return;
                        }
                    } else {
                        int index = preselectedCards.indexOf(this);
                        if (index == -1) {
                            preselectedCards.add(this);
                        } else {
                            while (index < preselectedCards.size()) {
                                preselectedCards.remove(index);
                            }
                        }
                    }
                }
                
                @Override
                public boolean dark() {
                    return isMoused() || !preselectedCards.isEmpty() && !preselectedCards.contains(this);
                }
                
                @Override
                public Cursor mousedCursor() {
                    return new Cursor(Cursor.HAND_CURSOR);
                }
                
                @Override
                public void paint(Graphics graphics) {
                    int index = preselectedCards.indexOf(this);
                    if (index != -1 && preselectedCards.size() > 1) {
                        graphics.setColor(Color.BLUE);
                        graphics.setFont(GraphicsTools.fontBold);
                        GraphicsTools.drawStringJustified(graphics, (index + 1) + "", x() + 20, y() - 20, 1, 1);
                        graphics.setColor(Color.BLACK);
                        graphics.setFont(GraphicsTools.font);
                    }
                    super.paint(graphics);
                }
            });
        }
    }
    
    public void playCard(CanvasCard canvasCard) {
        if (canPlayThis(canvasCard.getCard())) {
            playedCards.add(canvasCard);
            myPlayer.setPlaying(false);
            cardJustPlayed = canvasCard.index();
            client.makePlay(canvasCard.getCard());
        } else {
            preselectedCards.clear();
        }
    }

    public List<? extends ClientPlayer> getPlayersForScoreSheet(String sortBy) {
        if (state == GameState.POSTGAME) {
            if (client.getGameOptions().isTeams() && sortBy.charAt(0) != '*') {
                if (sortBy.equals("Score")) {
                    return postGameTeamsScoreSorted;
                } else {
                    return postGameTeamsIndexSorted;
                }
            } else {
                if (sortBy.equals("Score")) {
                    return postGamePlayersScoreSorted;
                } else {
                    return postGamePlayers;
                }
            }
        } else {
            if (client.getGameOptions().isTeams() && sortBy.charAt(0) != '*') {
                if (sortBy.equals("Score")) {
                    return teamsScoreSorted;
                } else {
                    return teamsIndexSorted;
                }
            } else {
                if (sortBy.equals("Score")) {
                    return playersScoreSorted;
                } else {
                    return players;
                }
            }
        }
    }

    public List<int[]> getRoundsForScoreSheet() {
        if (state == GameState.POSTGAME) {
            return postGameRounds;
        } else {
            return client.getRounds();
        }
    }
    
    public int[] thisRound() {
        if (state == GameState.BIDDING 
                || state == GameState.PLAYING
                || state == GameState.ENDOFROUND) {
            return client.thisRound();
        } else {
            return new int[] {-1, -1};
        }
    }
    
    public GameOptions getGameOptions() {
        return client.getGameOptions();
    }

    public List<ClientPlayer> getPlayers() {
        return players;
    }

    public ClientPlayer getMyPlayer() {
        return myPlayer;
    }

    public void setLeader(int leader) {
        this.leader = leader;
    }

    public int getDealer() {
        return dealer;
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
        int totalBid = players.stream().filter(ClientPlayer::hasBid).map(ClientPlayer::getBid).reduce(0,
                (sofar, b) -> sofar + b);
        return (myPlayer.getIndex() == dealer) && (totalBid + bid == myPlayer.getHand().size());
    }

    public boolean canPlayThis(Card card) {
        String ledSuit = players.get(leader).getTrick().getSuit();
        return players.get(leader).getTrick().isEmpty() || card.getSuit().equals(ledSuit)
                || myPlayer.getHand().stream().noneMatch(c -> c.getSuit().equals(ledSuit));
    }

    public void setTrumps(List<Card> trumps) {
        this.trumps = trumps;
    }

    public List<Card> getTrumps() {
        return trumps;
    }

    public void playSound(Clip clip) {
        clip.stop();
        clip.setFramePosition(0);
        clip.start();

        // double length = (double) clip.getFrameLength() / 44.1;
        //
        // Timer timer = new Timer((int) length, null);
        // timer.addActionListener(new ActionListener() {
        // boolean first = true;
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // if (first) {
        // clip.stop();
        // clip.setFramePosition(0);
        // clip.start();
        // first = false;
        // }
        // }
        // });
        // timer.start();
        //
        // new CanvasTimerEntry((long) length, this, audioQueue, false) {
        // @Override
        // public void onFirstAction() {
        // clip.start();
        // }
        //
        // @Override
        // public void onLastAction() {
        // clip.stop();
        // clip.setFramePosition(0);
        // }
        // };
    }

    public LinkedList<Timer> getActionQueue() {
        return actionQueue;
    }
    
    public void clearActionQueue() {
        actionQueue.clear();
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
        playersScoreSorted.clear();
        playersScoreSorted.addAll(players);
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setIndex(i);
        }
        myPlayer = client.getMyPlayer();
        
        if (client.getGameOptions().isTeams()) {
            updateTeams();
        }
        
        maxWid = 9 * 10 + cardWidthSmall;
        setPlayerPositions();
        resetNamePlates();
        resetKickButtons();
    }
    
    public void updateTeams() {
        teams.clear();
        teams.putAll(client.getTeams());
        teamsIndexSorted.clear();
        teamsIndexSorted.addAll(teams.values());
        teamsIndexSorted.sort((t1, t2) -> (int) Math.signum(t1.getIndex() - t2.getIndex()));
        teamsScoreSorted.clear();
        teamsScoreSorted.addAll(teamsIndexSorted);
        teamsScoreSorted.sort((t1, t2) -> (int) Math.signum(t2.getScore() - t1.getScore()));
    }

    public void pregameOnTimer(boolean immediate) {
        new CanvasTimerEntry(0, this, actionQueue, immediate) {
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
        // updatePlayers();
        trickTaken = false;
        animatingTaken = false;
        takenTimer = 1;
        bidCount = 0;
        showBidDots = false;
        cardJustPlayed = 0;
        preselectedCards.clear();
        playedCards.clear();
        canUndoBid = false;
        undoBidTimer = -1;
        messageState = "UNBLOCKED";
        claimBlocked = false;
        claimer = -1;
        
        client.reportGameOptions(client.getGameOptions());
    }

    public void initializingOnTimer(boolean immediate) {
        new CanvasTimerEntry(0, this, actionQueue, immediate) {
            @Override
            public void onFirstAction() {
                updatePlayers();
                // updatePlayersOnTimer();
                state = GameState.INITIALIZING;

                for (ClientPlayer player : players) {
                    player.reset();
                }
                
                GameOptions options = client.getGameOptions();
                int maxH = options.getStartingH();
                maxWid = (maxH - 1) * 10 + cardWidthSmall;
                addChatLine("<b>" + players.size() + "-player game started" + (options.getD() == 2 ? " (double deck)" : "") + "</b>");
                refreshChat();
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
                    client.changeState(ClientState.IN_SINGLE_PLAYER_GAME);
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
                double t = Math.min((double) this.getElapsedTime() / (client.devSpeedSelected() ? 1 : animationTime),
                        1);
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
                claimBlocked = false;
            }

            @Override
            public void onAction() {
                takenTimer = Math.min((double) this.getElapsedTime() / (client.devSpeedSelected() ? 1 : animationTime),
                        1);
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
                claimer = myPlayer.getIndex();
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
                    preselectedCards.clear();
                    state = GameState.ENDOFROUND;
                } else if (claimer == myPlayer.getIndex()) {
                    claimBlocked = true;
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
                String pronoun = "You";
                ClientPlayer player = myPlayer;
                if (client.getGameOptions().isTeams()) {
                    pronoun = "Your team";
                    player = client.getTeams().get(myPlayer.getTeam());
                }
                
                if (player.getBid() == player.getTaken()) {
                    message = pronoun + " made it!";
                } else {
                    message = pronoun + " went down " + Math.abs(player.getBid() - player.getTaken()) + ".";
                }
                paintMessageMarker = true;
                state = GameState.ENDOFROUND;
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
                    player.resetTrick();
                }
                trickTaken = false;
            }
        };
    }

    public void setHandOnTimer(int index, List<Card> hand) {
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                if (index == dealer && index == myPlayer.getIndex() && !myPlayer.isKibitzer()
                        && thisRound()[1] == 1) {
                    showOneCard = false;
                }
                players.get(index).setHand(hand);
                if (index == myPlayer.getIndex()) {
                    makeHandInteractables();
                    playedCards.clear();
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
                    preselectedCards.clear();
                    makeBidInteractables();
                    bidCount = 0;
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
                
                if (index == myPlayer.getIndex() && !preselectedCards.isEmpty()) {
                    playCard(preselectedCards.remove(0));
                }
            }
        };
    }

    public void setBidOnTimer(int index, int bid, int delay) {
        long time = System.currentTimeMillis();
        new CanvasTimerEntry(delay, this, actionQueue, false) {
            @Override
            public void onLastAction() {
                players.get(index).addBid(bid);
                players.get(index).setBidTimer(0);
                if (players.stream().filter(p -> !p.hasBid() && !p.isKicked()).count() == 0) {
                    animateBids();
                }
                
                if (index == myPlayer.getIndex()) {
                    bidCount++;
                    showBidDots = true;
                    
                    if (bidCount < maxUndos + 1) {
                        canUndoBid = true;
                        if (!client.devSpeedSelected()) {
                            for (int i = 1; i < players.size(); i++) {
                                ClientPlayer nextPlayer = players.get((i + index) % players.size());
                                if (!nextPlayer.isKicked()) {
                                    if (!nextPlayer.isHuman()) {
                                        undoBidTimer = time;
                                    } else {
                                        undoBidTimer = -1;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    canUndoBid = false;
                    showBidDots = false;
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
                    preselectedCards.clear();
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
                if (!myPlayer.isKibitzer() && index == myPlayer.getIndex()) {
                    cardInteractables.remove(cardJustPlayed);
                    myPlayer.removeCard(cardJustPlayed);
                } else {
                    players.get(index).removeCard(card);
                }
                canUndoBid = false;
                showBidDots = false;
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
                playersScoreSorted.sort((p1, p2) -> (int) Math.signum(p2.getScore() - p1.getScore()));
                if (client.getGameOptions().isTeams()) {
                    teamsScoreSorted.sort((t1, t2) -> (int) Math.signum(t2.getScore() - t1.getScore()));
                }
                
                scoreSheet.autoScroll(client.getRoundNumber());
            }
        };
    }

    public void loadPostGameOnTimer(List<String> lines) {
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
                postGameTeams = new HashMap<>();
                postGameRounds = new ArrayList<>();
                postGameOptions = new GameOptions();
                try {
                    loadPostGameFromFile(lines, trumps, postGamePlayers, 
                            postGameRounds, postGameOptions, postGameTeams);
                    postGameOptions.setNumRobots(postGamePlayers.size());
                    client.updateGameOptions(postGameOptions);
                    simulateGame();
                } catch (Exception e) {
                    client.notify("Unable to load post-game page.");
                    e.printStackTrace();
                    state = GameState.PREGAME;
                }
                postGamePlayersScoreSorted = new ArrayList<>(postGamePlayers.size());
                postGamePlayersScoreSorted.addAll(postGamePlayers);
                postGamePlayersScoreSorted.sort((p1, p2) -> (int) Math.signum(p2.getScore() - p1.getScore()));
                postGameTeamsIndexSorted = new ArrayList<>(postGameTeams.size());
                postGameTeamsIndexSorted.addAll(postGameTeams.values());
                postGameTeamsIndexSorted.sort((t1, t2) -> (int) Math.signum(t1.getIndex() - t2.getIndex()));
                postGameTeamsScoreSorted = new ArrayList<>(postGameTeams.size());
                postGameTeamsScoreSorted.addAll(postGameTeamsIndexSorted);
                postGameTeamsScoreSorted.sort((t1, t2) -> (int) Math.signum(t2.getScore() - t1.getScore()));
            }
        };
    }

    public static void loadPostGameFromFile(List<String> lines, List<Card> trumps, List<ClientPlayer> players,
            List<int[]> rounds, GameOptions options, HashMap<Integer, ClientTeam> teams) {
        trumps.clear();
        players.clear();
        rounds.clear();
        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }

            String[] typeContent = line.split(Recorder.splitPreceder + Recorder.commandDelimiter1, 2);
            String type = typeContent[0];
            String[] content = typeContent[1].split(Recorder.splitPreceder + Recorder.commandDelimiter1);

            if (type.equals("decks")) {
                options.setD(Integer.parseInt(content[0]));
            } else if (type.equals("players")) {
                int index = 0;
                for (String playerInfo : content) {
                    ClientPlayer player = new ClientPlayer();
                    String[] info = playerInfo.split(Recorder.splitPreceder + Recorder.commandDelimiter2);
                    player.setId(Recorder.decodeString(info[0]));
                    player.setName(Recorder.decodeString(info[1]));
                    player.setHuman(info[2].equals("human"));
                    player.setIndex(index);
                    players.add(player);
                    index++;
                }
            } else if (type.equals("teams")) {
                options.setTeams(true);
                HashMap<Integer, List<ClientPlayer>> teamMap = new HashMap<>();
                int index = 0;
                for (String teamInfo : content) {
                    String[] info = teamInfo.split(Recorder.splitPreceder + Recorder.commandDelimiter2);
                    int teamNumber = Integer.parseInt(info[0]);
                    if (!teamMap.containsKey(teamNumber)) {
                        teamMap.put(teamNumber, new LinkedList<>());
                    }
                    teamMap.get(teamNumber).add(players.get(index));
                    index++;
                }
                
                for (Integer teamNumber : teamMap.keySet()) {
                    ClientTeam team = new ClientTeam();
                    team.setName("Team " + (teamNumber + 1));
                    team.setIndex(teamNumber);
                    team.setMembers(teamMap.get(teamNumber));
                    teams.put(teamNumber, team);
                }
            } else if (type.equals("teaminfo")) {
                for (String teamInfo : content) {
                    String[] info = teamInfo.split(Recorder.splitPreceder + Recorder.commandDelimiter2);
                    teams.get(Integer.parseInt(info[0])).setName(Recorder.decodeString(info[1]));
                }
            } else if (type.equals("round")) {
                rounds.add(new int[] { Integer.parseInt(content[0]), Integer.parseInt(content[1]), 0 });
            } else if (type.equals("hands")) {
                for (int j = 0; j < players.size(); j++) {
                    List<Card> hand = Arrays
                            .stream(content[j].split(Recorder.splitPreceder + Recorder.commandDelimiter2))
                            .map(s -> new Card(s)).collect(Collectors.toList());
                    players.get(j).addPostGameHand(hand);
                }
            } else if (type.equals("trump")) {
                trumps.add(new Card(content[0]));
            } else if (type.equals("bids")) {
                for (int j = 0; j < players.size(); j++) {
                    String[] info = content[j].split(Recorder.splitPreceder + Recorder.commandDelimiter2);
                    players.get(j).addBid(Integer.parseInt(info[info.length - 1]));
                }
            } else if (type.equals("trick")) {
                for (int j = 0; j < players.size(); j++) {
                    String[] info = content[j].split(Recorder.splitPreceder + Recorder.commandDelimiter2);
                    Card card = new Card(info[info.length - 1]);
                    if (!card.isEmpty()) {
                        players.get(j).addPostGamePlay(card, info[0].equals("1"), info[1].equals("1"));
                    }
                }
                rounds.get(rounds.size() - 1)[2]++;
            } else if (type.equals("claim")) {
                for (int j = 0; j < players.size(); j++) {
                    players.get(j).addPostGameClaim(j == Integer.parseInt(content[0]));
                }
                rounds.get(rounds.size() - 1)[2]++;
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
                    String[] info = content[j].split(Recorder.splitPreceder + Recorder.commandDelimiter2);
                    players.get(j).setPlace(Integer.parseInt(info[1]));
                }
            } else if (type.equals("kick")) {
                players.get(Integer.parseInt(content[0])).setKickedAtRound(rounds.size());
            }
        }
    }

    public void simulateGame() {
        new CanvasGameSimulator(postGamePlayers, postGameRounds, trumps) {
            @Override
            public void whenFinished() {
                buildPostGameTabsOnTimer();
            }
        }.simulate(postGameOptions);
    }

    public void buildPostGameTabsOnTimer() {
        GameCanvas thisCanvas = this;
        new CanvasTimerEntry(0, this, actionQueue, false) {
            @Override
            public void onFirstAction() {
                try {
                    postGamePage.buildTabs(postGamePlayers, postGameRounds, thisCanvas);
                } catch (Exception e) {
                    client.notify("Unable to load post-game page.");
                    e.printStackTrace();
                    state = GameState.PREGAME;
                }

                message = "";
                paintMessageMarker = false;
            }
        };
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
                if (taken > 0) {
                    trickTaken = true;
                }
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
                playersScoreSorted.sort((p1, p2) -> (int) Math.signum(p2.getScore() - p1.getScore()));
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
                // showMessageOnTimer("You've been poked.");
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