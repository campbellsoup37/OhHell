package client;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import core.Card;
import core.OhHellCore;
import core.Player;
import graphics.OhcGraphicsTools;
import graphics.OhcTextField;

public class GameClient extends JFrame {
    private static final long serialVersionUID = 1L;
    
    //////Dev Options //////////////////
    private final boolean aiHelpOptionEnabled = false;
    private final boolean stopperOptionEnabled = false;
    private final boolean botsOnlyOptionEnabled = true;
    private final boolean devSpeedOptionEnabled = true;
    private final boolean pingOptionEnabled = true;
    private final boolean fpsOptionEnabled = true;
    private final boolean lowGraphicsOptionEnabled = true;
    private final boolean antialiasingOptionEnabled = true;
    ///////////////////////////////////
    
    private final String version = "0.1.5.4";
    private boolean updateChecked = false;
    private String newVersion;
    
    private String username = "";
    private String hostName = "localhost";
    private String port = "6066";
    
    private Point windowLocationMenu = new Point(0, 0);
    private Dimension windowSizeMenu = new Dimension(685, 540);
    private boolean windowMaximizedMenu = false;
    private Point windowLocationInGame = new Point(0, 0);
    private Dimension windowSizeInGame = new Dimension(1200, 800);
    private boolean windowMaximizedInGame = false;

    private boolean connected = false;
    private Socket socket;
    private ClientReadThread readThread;
    private ClientWriteThread writeThread;
    
    private NotificationFrame notificationFrame;
    
    // Menu bar
    private JMenuBar menuBar = new JMenuBar();
    private JMenu optionsItem = new JMenu("Options");
    private JCheckBox soundOption = new JCheckBox("Sound");
    private JMenu aiSpeedOption = new JMenu("AI play speed");
    private JSlider aiSpeedOptionSlider = new JSlider(JSlider.HORIZONTAL, 0, 900, 500);
    private JCheckBox pingOption = new JCheckBox("Show ping");
    private JCheckBox fpsOption = new JCheckBox("Show FPS");
    private JMenu devOptionsItem = new JMenu("Dev options");
    private JCheckBox aiHelpOption = new JCheckBox("AI help");
    private JCheckBox stopperOption = new JCheckBox("Stoppers");
    private JCheckBox botsOnlyOption = new JCheckBox("Bots only");
    private JCheckBox devSpeedOption = new JCheckBox("Fast animation");
    private JCheckBox lowGraphicsOption = new JCheckBox("Low graphics");
    private JCheckBox antialiasingOption = new JCheckBox("Anti-aliasing");
    private JMenuItem backOption = new JMenuItem("Back to menu");
    
    private boolean stopperSelected = false;
    private long pingTime = 0;
    private long currentPing = 0;
    
    // MAIN_MENU
    private OhcCanvas mainMenuCanvas;
    
    // SINGLE_PLAYER_MENU
    private OhcCanvas singlePlayerCanvas;
    
    private int numRobots = 4;
    private OhHellCore core = new OhHellCore(false);
    private SinglePlayerPlayer spPlayer;
    
    // LOGIN_MENU
    private OhcCanvas loginCanvas;
    
    // IN_GAME
    private GameCanvas canvas;
    
    private List<ClientPlayer> players = new ArrayList<>();
    private List<ClientPlayer> kibitzers = new ArrayList<>();
    private ClientPlayer myPlayer;
    private List<int[]> rounds = new ArrayList<>();
    private int roundNumber = 0;
    
    private Random random = new Random();
    
    private ClientState state;
    private OhcCanvas stateCanvas;
    
    public GameClient() {}
    
    public ClientState getClientState() {
        return state;
    }
    
    public void changeState(ClientState newState) {
        OhcCanvas oldStateCanvas = stateCanvas;
        
        switch (newState) {
        case MAIN_MENU:
            setLocation(windowLocationMenu);
            setMinimumSize(new Dimension(685, 500));
            setSize(windowSizeMenu);
            setMaximized(windowMaximizedMenu);
            setResizable(false);
            stateCanvas = mainMenuCanvas;
            break;
        case SINGLE_PLAYER_MENU:
            setLocation(windowLocationMenu);
            setMinimumSize(new Dimension(685, 500));
            setSize(windowSizeMenu);
            setMaximized(windowMaximizedMenu);
            setResizable(false);
            stateCanvas = singlePlayerCanvas;
            break;
        case IN_SINGLE_PLAYER_GAME:
            setLocation(windowLocationInGame);
            setMinimumSize(new Dimension(1200, 800));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setResizable(true);
            stateCanvas = canvas;
            break;
        case SINGLE_PLAYER_POST_GAME:
            break;
        case LOGIN_MENU:
            setMinimumSize(new Dimension(685, 500));
            setSize(windowSizeMenu);
            setMaximized(windowMaximizedMenu);
            setResizable(false);
            stateCanvas = loginCanvas;
            myPlayer = null;
            break;
        case IN_MULTIPLAYER_GAME:
            setLocation(windowLocationInGame);
            setMinimumSize(new Dimension(1200, 800));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setResizable(true);
            stateCanvas = canvas;
            break;
        case MULTIPLAYER_POST_GAME:
            break;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (oldStateCanvas == null || oldStateCanvas != stateCanvas) {
                    resizeCanvas();
                    stateCanvas.setVisible(true);
                    if (oldStateCanvas != null) {
                        oldStateCanvas.setVisible(false);
                    }
                }
            }
        });
        
        state = newState;
    }
    
    public void resizeCanvas() {
        if (stateCanvas != null) {
            stateCanvas.setBounds(
                    0, 0,
                    getContentPane().getWidth(),
                    getContentPane().getHeight()
                    );
        }
    }
    
    public void setMaximized(boolean m) {
        if (m) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(getExtendedState() & (~JFrame.MAXIMIZED_BOTH));
        }
    }
    
    public void addPlayers(List<ClientPlayer> players) {
        boolean anyDc = false;
        
        for (ClientPlayer player : players) {
            if (player.isKibitzer()) {
                kibitzers.add(player);
            } else {
                this.players.add(player);
                player.setIndex(this.players.size() - 1);
                anyDc = anyDc || player.isDisconnected();
            }
            if (player.getId().equals(username)) {
                myPlayer = player;
            }
        }

        final boolean anyDcF = anyDc;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                canvas.updatePlayerCalcs();
                
                if (state == ClientState.IN_MULTIPLAYER_GAME && anyDcF) {
                    canvas.setConnectionStatusOnTimer(false);
                }
            }
        });
    }
    
    public void removePlayer(String id) {
        int toRemove = players.size();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(id)) {
                toRemove = i;
            }
            /*if (i > toRemove) {
                players.get(i).setIndex(i - 1);
            }*/
        }
        if (toRemove < players.size()) {
            players.remove(toRemove);
        } else {
            toRemove = kibitzers.size();
            for (int i = 0; i < kibitzers.size(); i++) {
                if (kibitzers.get(i).getId().equals(id)) {
                    toRemove = i;
                }
            }
            kibitzers.remove(toRemove);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                canvas.updatePlayerCalcs();
            }
        });
    }
    
    public void updatePlayers(List<ClientPlayer> newPlayers) {
        boolean anyDc = false;
        
        for (ClientPlayer newPlayer : newPlayers) {
            for (ClientPlayer oldPlayer : players) {
                if (oldPlayer.getId().equals(newPlayer.getId())) {
                    oldPlayer.setName(newPlayer.getName());
                    oldPlayer.setId(newPlayer.getId());
                    oldPlayer.setIndex(newPlayer.getIndex());
                    oldPlayer.setHost(newPlayer.isHost());
                    oldPlayer.setDisconnected(newPlayer.isDisconnected());
                    oldPlayer.setKicked(newPlayer.isKicked());
                    oldPlayer.setKibitzer(newPlayer.isKibitzer());
                    anyDc = anyDc || newPlayer.isDisconnected();
                }
            }
        }
        
        players.sort((p1, p2) -> (int) Math.signum(p1.getIndex() - p2.getIndex()));

        final boolean anyDcF = anyDc;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                canvas.updatePlayerCalcs();
                
                if (state == ClientState.IN_MULTIPLAYER_GAME) {
                    canvas.setConnectionStatusOnTimer(!anyDcF);
                }
            }
        });
        
        if (state == ClientState.IN_MULTIPLAYER_GAME) {
            canvas.setConnectionStatusOnTimer(!anyDc);
        }
    }
    
    public void sendIdToServer() {
        sendCommandToServer("ID:" + "STRING " + username.length() + ":" + username);
    }
    
    public void toggleKibitzer() {
        sendCommandToServer("KIBITZER:" + !myPlayer.isKibitzer());
    }
    
    public void updateRounds(List<int[]> rounds, int roundNumber) {
        this.rounds.clear();
        for (int[] round : rounds) {
            this.rounds.add(round);
        }
        this.roundNumber = roundNumber;
    }
    
    public boolean isOneRound() {
        return roundNumber < rounds.size() && rounds.get(roundNumber)[1] == 1;
    }
    
    public List<int[]> getRounds() {
        return rounds;
    }
    
    public int[] thisRound() {
        return rounds.get(roundNumber);
    }
    
    public void reconnectAs(int index) {
        sendCommandToServer("RECONNECT:" + index);
    }
    
    public void voteKick(int index) {
        sendCommandToServer("VOTEKICK:" + index);
    }
    
    public void startSinglePlayerGame() {
        players.clear();
        kibitzers.clear();
        
        spPlayer = new SinglePlayerPlayer(this);
        spPlayer.setId(username);
        spPlayer.setKibitzer(botsOnlyOption.isSelected());
        spPlayer.commandAddPlayers(Arrays.asList(spPlayer), null);
        
        List<Player> players = new ArrayList<>();
        List<Player> kibitzers = new ArrayList<>();
        (botsOnlyOption.isSelected() ? kibitzers : players).add(spPlayer);
        core.setPlayers(players);
        core.setKibitzers(kibitzers);
        
        spPlayer.setCore(core);
        canvas.reset();
        core.startGame(numRobots, false, null, 0);
    }
    
    public void startGame() {
        roundNumber = 0;
        canvas.setState(GameState.BIDDING);

        for (ClientPlayer player : players) {
            player.reset();
        }
        if (myPlayer.isKibitzer()) {
            myPlayer.setIndex(random.nextInt(players.size()));
        }
        
        switch (state) {
        case SINGLE_PLAYER_MENU:
        case IN_SINGLE_PLAYER_GAME:
        case SINGLE_PLAYER_POST_GAME:
            changeState(ClientState.IN_SINGLE_PLAYER_GAME);
            break;
        case MULTIPLAYER_POST_GAME:
            changeState(ClientState.IN_MULTIPLAYER_GAME);
            break;
        default:
            break;
        }
    }
    
    public List<ClientPlayer> getPlayers() {
        return players;
    }
    
    public ClientPlayer getMyPlayer() {
        return myPlayer;
    }
    
    public void restartRound() {
        for (ClientPlayer player : players) {
            player.setBid(0);
            player.setTaken(0);
            player.resetTrick();
            if (player.getBids().size() >= roundNumber + 1) {
                player.getBids().remove(roundNumber);
            }
        }

        canvas.showMessageOnTimer("Someone was kicked. Redealing this round.");
    }
    
    public void notify(String text) {
        if (notificationFrame != null) {
            notificationFrame.close();
            notificationFrame = null;
        }
        notificationFrame = new NotificationFrame(text);
        notificationFrame.setLocationRelativeTo(this);
        notificationFrame.execute();
    }
    
    public void setHand(int index, List<Card> hand) {
        canvas.setHandOnTimer(index, hand);
    }
    
    public void setTrump(Card card) {
        canvas.setTrumpOnTimer(card);
    }
    
    public void setDealerLeader(int dealer, int leader) {
        canvas.setDealerOnTimer(dealer);
        canvas.setLeaderOnTimer(leader);
    }
    
    public void bid(int index) {
        canvas.setBiddingOnTimer(index);
    }
    
    public void play(int index) {
        canvas.setPlayingOnTimer(index);
    }
    
    public int getRobotDelay() {
        return 1000 - aiSpeedOptionSlider.getValue();
    }
    
    public void bidReport(int index, int bid) {
        canvas.setBidOnTimer(index, bid, players.get(index).isHuman() ? 0 : getRobotDelay());
        if (stopperSelected) {
            canvas.addStopper();
        }
    }
    
    public void playReport(int index, Card card) {
        canvas.setPlayOnTimer(index, card, players.get(index).isHuman() ? 0 : getRobotDelay());
        canvas.animateCardPlay(index);
        if (stopperSelected) {
            canvas.addStopper();
        }
    }
    
    public void trickWinnerReport(int index) {
        canvas.animateTrickTake(index);
    }
    
    public void reportScores(List<Integer> scores) {
        if (!myPlayer.isKibitzer()) {
            canvas.showResultMessageOnTimer();
        }
        canvas.setScoresOnTimer(scores);
        canvas.clearRoundOnTimer();
        if (stopperSelected) {
            canvas.addStopper();
        }
    }
    
    public void incrementRoundNumber() {
        roundNumber++;
    }
    
    public void chat(String text) {
        canvas.chat(text);
    }
    
    public void setStatePlayer(int index, boolean hasBid, int bid, int taken, Card lastTrick, Card trick) {
        ClientPlayer player = players.get(index);
        player.setHasBid(hasBid);
        player.setBid(bid);
        player.setTaken(taken);
        player.setLastTrick(lastTrick);
        player.setTrick(trick);
        if (!trick.isEmpty()) {
            player.setTimerStarted(true);
        }
    }
    
    public void setStatePlayerBids(int index, List<Integer> bids) {
        players.get(index).setBids(bids);
    }
    
    public void setStatePlayerScores(int index, List<Integer> scores) {
        players.get(index).setScores(scores);
    }
    
    public void execute() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            setTitle("Oh Hell");
            
            setIconImage(OhcGraphicsTools.loadImage("resources/icon/cw.png", this));
            
            BufferedImage tableImg = OhcGraphicsTools.loadImage("resources/client/table.jpg", this);
            
            aiSpeedOption.add(aiSpeedOptionSlider);

            aiSpeedOption.setPreferredSize(new Dimension(150, 25));
            optionsItem.add(aiSpeedOption);
            
            optionsItem.add(soundOption);
            
            backOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (state) {
                    case SINGLE_PLAYER_MENU:
                        changeState(ClientState.MAIN_MENU);
                        break;
                    case IN_SINGLE_PLAYER_GAME:
                    case SINGLE_PLAYER_POST_GAME:
                        leaveGame();
                        break;
                    case IN_MULTIPLAYER_GAME:
                    case MULTIPLAYER_POST_GAME:
                        leaveGame();
                        break;
                    default:
                        break;
                    }
                }
            });
            optionsItem.add(backOption);
            
            aiHelpOption.setEnabled(aiHelpOptionEnabled);
            aiHelpOption.setPreferredSize(new Dimension(150, 25));
            aiHelpOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.setAiHelp(aiHelpOption.isSelected());
                }
            });
            devOptionsItem.add(aiHelpOption);
            
            stopperOption.setEnabled(stopperOptionEnabled);
            stopperOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopperSelected = stopperOption.isSelected();
                }
            });
            devOptionsItem.add(stopperOption);
            
            botsOnlyOption.setEnabled(botsOnlyOptionEnabled);
            devOptionsItem.add(botsOnlyOption);
            
            devSpeedOption.setEnabled(devSpeedOptionEnabled);
            devOptionsItem.add(devSpeedOption);
            
            pingOption.setEnabled(pingOptionEnabled);
            devOptionsItem.add(pingOption);

            fpsOption.setEnabled(fpsOptionEnabled);
            devOptionsItem.add(fpsOption);
            
            lowGraphicsOption.setEnabled(lowGraphicsOptionEnabled);
            devOptionsItem.add(lowGraphicsOption);
            
            antialiasingOption.setSelected(true);
            antialiasingOption.setEnabled(antialiasingOptionEnabled);
            devOptionsItem.add(antialiasingOption);

            menuBar.add(optionsItem);
            menuBar.add(devOptionsItem);
            setJMenuBar(menuBar);
            
            loadConfigFile();
            
            mainMenuCanvas = new OhcCanvas(this) {
                private static final long serialVersionUID = 1L;

                @Override
                public void initialize() {
                    setBackground(tableImg);
                    
                    CanvasButton spButton = new CanvasButton("Single Player") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 230;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void click() {
                            changeState(ClientState.SINGLE_PLAYER_MENU);
                        }
                    };
                    
                    CanvasButton mpButton = new CanvasButton("Multiplayer") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 330;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void click() {
                            changeState(ClientState.LOGIN_MENU);
                        }
                    };
                    
                    CanvasButton updateButton = new CanvasButton("") {
                        @Override
                        public int x() {
                            return 450;
                        }
                        
                        @Override
                        public int y() {
                            return 450;
                        }
                        
                        @Override
                        public int width() {
                            return 150;
                        }
                        
                        @Override
                        public int height() {
                            return 20;
                        }
                        
                        @Override
                        public String text() {
                            if (!updateChecked) {
                                return "Check for update";
                            } else if (updateChecked && newVersion.equals(version)) {
                                return "Version up to date";
                            } else {
                                return "Download update";
                            }
                        }
                        
                        @Override
                        public boolean alert() {
                            return updateChecked && !newVersion.equals(version);
                        }
                        
                        @Override
                        public void click() {
                            updatePressed();
                        }
                    };
                    
                    setInteractables(Arrays.asList(Arrays.asList(spButton, mpButton, updateButton)));
                }
                
                @Override
                public boolean isShown() {
                    return state == ClientState.MAIN_MENU;
                }
                
                @Override
                public void customPaint(Graphics graphics) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                    OhcGraphicsTools.drawBox(graphics, 200, 80, 285, 320, 20);
                    
                    graphics.setFont(OhcGraphicsTools.fontTitle);
                    OhcGraphicsTools.drawStringJustified(graphics, "Oh Hell", 342, 100, 1, 2);
                    
                    graphics.setFont(OhcGraphicsTools.fontBold);
                    OhcGraphicsTools.drawStringJustified(graphics, "v" + version, 610, 460, 0, 1);
                    
                    graphics.setFont(OhcGraphicsTools.font);
                }
            };
            
            singlePlayerCanvas = new OhcCanvas(this) {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void initialize() {
                    setBackground(tableImg);
                    
                    CanvasButton minusButton = new CanvasButton("-") {
                        @Override
                        public int x() {
                            return 350;
                        }
                        
                        @Override
                        public int y() {
                            return 160;
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
                        public void click() {
                            numRobots = Math.max(0, numRobots - 1);
                        }
                    };
                    
                    CanvasButton plusButton = new CanvasButton("+") {
                        @Override
                        public int x() {
                            return 400;
                        }
                        
                        @Override
                        public int y() {
                            return 160;
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
                        public void click() {
                            numRobots = Math.min(6, numRobots + 1);
                        }
                    };
                    
                    CanvasButton startButton = new CanvasButton("Start") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 230;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void click() {
                            startSinglePlayerGame();
                        }
                    };
                    
                    CanvasButton backButton = new CanvasButton("Back") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 330;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void click() {
                            changeState(ClientState.MAIN_MENU);
                        }
                    };
                    
                    setInteractables(Arrays.asList(Arrays.asList(minusButton, plusButton, startButton, backButton)));
                }
                
                @Override
                public boolean isShown() {
                    return state == ClientState.SINGLE_PLAYER_MENU;
                }
                
                @Override
                public void customPaint(Graphics graphics) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                    OhcGraphicsTools.drawBox(graphics, 200, 80, 285, 320, 20);
                    
                    graphics.setFont(OhcGraphicsTools.fontBold);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, "Robots:", 240, 170, 0, 1);
                    OhcGraphicsTools.drawStringJustified(graphics, numRobots + "", 385, 170, 1, 1);
                }
            };
            
            loginCanvas = new OhcCanvas(this) {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void initialize() {
                    setBackground(tableImg);
                    
                    OhcTextField usernameJField = new OhcTextField("Username");
                    usernameJField.setText(username);
                    CanvasEmbeddedSwing usernameField = new CanvasEmbeddedSwing(usernameJField, this) {
                        @Override
                        public int x() {
                            return 320;
                        }
                        
                        @Override
                        public int y() {
                            return 100;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void paint(Graphics graphics) {
                            username = usernameJField.getText();
                            super.paint(graphics);
                        }
                    };
                    
                    OhcTextField serverJField = new OhcTextField("Server address");
                    serverJField.setText(hostName);
                    CanvasEmbeddedSwing serverField = new CanvasEmbeddedSwing(serverJField, this) {
                        @Override
                        public int x() {
                            return 320;
                        }
                        
                        @Override
                        public int y() {
                            return 150;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void paint(Graphics graphics) {
                            hostName = serverJField.getText();
                            super.paint(graphics);
                        }
                    };

                    OhcTextField portJField = new OhcTextField("Server address");
                    portJField.setText(port);
                    CanvasEmbeddedSwing portField = new CanvasEmbeddedSwing(portJField, this) {
                        @Override
                        public int x() {
                            return 320;
                        }
                        
                        @Override
                        public int y() {
                            return 200;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void paint(Graphics graphics) {
                            port = portJField.getText();
                            super.paint(graphics);
                        }
                    };
                    
                    CanvasButton startButton = new CanvasButton("Connect") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 280;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void click() {
                            username = ((OhcTextField) usernameField.getComponent()).getText();
                            connect();
                        }
                    };
                    
                    CanvasButton backButton = new CanvasButton("Back") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 330;
                        }
                        
                        @Override
                        public int width() {
                            return 151;
                        }
                        
                        @Override
                        public int height() {
                            return 40;
                        }
                        
                        @Override
                        public void click() {
                            changeState(ClientState.MAIN_MENU);
                        }
                    };
                    
                    setInteractables(Arrays.asList(Arrays.asList(usernameField, serverField, portField, startButton, backButton)));
                }
                
                @Override
                public boolean isShown() {
                    return state == ClientState.LOGIN_MENU;
                }
                
                @Override
                public void customPaint(Graphics graphics) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                    OhcGraphicsTools.drawBox(graphics, 200, 80, 285, 320, 20);

                    graphics.setFont(OhcGraphicsTools.fontBold);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, "Username:", 230, 120, 0, 1);
                    OhcGraphicsTools.drawStringJustified(graphics, "Server:", 230, 170, 0, 1);
                    OhcGraphicsTools.drawStringJustified(graphics, "Port:", 230, 220, 0, 1);
                }
            };
            
            canvas = new GameCanvas(this);
            canvas.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    canvas.mouseWheeled(e.getWheelRotation());
                }
            });
            canvas.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}

                @Override
                public void keyPressed(KeyEvent e) {}

                @Override
                public void keyReleased(KeyEvent e) {
                    canvas.keyPressed(e.getKeyCode());
                }
            });
            canvas.setBackground(tableImg);
            
            for (OhcCanvas c : Arrays.asList(
                    mainMenuCanvas, 
                    singlePlayerCanvas, 
                    loginCanvas,
                    canvas
                    )) {
                c.setFocusable(true);
                c.setVisible(false);
                add(c);
                c.setLayout(null);
                c.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {}
                    
                    @Override
                    public void mouseEntered(MouseEvent arg0) {}
                    
                    @Override
                    public void mouseExited(MouseEvent arg0) {}

                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            c.mousePressed(e.getX(), e.getY());
                        } else if (e.getButton() == MouseEvent.BUTTON3 && stopperSelected) {
                            //menuCanvas.removeStopper();
                        }
                        c.grabFocus();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            c.mouseReleased(e.getX(), e.getY());
                        }
                    }
                });
                c.addMouseMotionListener(new MouseMotionListener() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        c.mouseMoved(e.getX(), e.getY());
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        c.mouseMoved(e.getX(), e.getY());
                    }
                });
            }
            
            setVisible(true);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    saveConfigFile();
                    if (connected) {
                        close();
                    }
                }
            });
            addWindowStateListener(new WindowStateListener() {
                @Override
                public void windowStateChanged(WindowEvent e) {
                    if (state == ClientState.IN_SINGLE_PLAYER_GAME
                            || state == ClientState.IN_MULTIPLAYER_GAME
                            || state == ClientState.SINGLE_PLAYER_POST_GAME
                            || state == ClientState.MULTIPLAYER_POST_GAME) {
                        windowMaximizedInGame = (e.getNewState() & JFrame.MAXIMIZED_BOTH) != 0;
                    }
                    resizeCanvas();
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (state == ClientState.IN_SINGLE_PLAYER_GAME
                            || state == ClientState.IN_MULTIPLAYER_GAME
                            || state == ClientState.SINGLE_PLAYER_POST_GAME
                            || state == ClientState.MULTIPLAYER_POST_GAME) {
                        windowSizeInGame = getSize();
                    }
                    resizeCanvas();
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    if (state == ClientState.IN_SINGLE_PLAYER_GAME
                            || state == ClientState.IN_MULTIPLAYER_GAME
                            || state == ClientState.SINGLE_PLAYER_POST_GAME
                            || state == ClientState.MULTIPLAYER_POST_GAME) {
                        windowLocationInGame = getLocationOnScreen();
                    } else {
                        windowLocationMenu = getLocationOnScreen();
                    }
                }
            });
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            changeState(ClientState.MAIN_MENU);
            checkForUpdates();
            
            /*ClientPlayer myPlayer = new ClientPlayer();
            myPlayer.setName("test");
            updatePlayersList(Arrays.asList(myPlayer), 0);*/
            //canvas.reset();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void leaveGame() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.stopGame();
        case SINGLE_PLAYER_POST_GAME:
            changeState(ClientState.SINGLE_PLAYER_MENU);
            break;
        case IN_MULTIPLAYER_GAME:
        case MULTIPLAYER_POST_GAME:
            if (connected) {
                disconnect();
            }
            changeState(ClientState.LOGIN_MENU);
            break;
        default:
            break;
        }
    }
    
    public String getOvlProb(int index) {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            return String.format("%.2f", spPlayer.getOvlProb(index));
        default:
            return "";
        }
    }
    
    public String getAiBid() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            int bid = spPlayer.getAiBid();
            return bid == -1 ? "" : bid + "";
        default:
            return "";
        }
    }
    
    public String getAiPlay() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            Card card = spPlayer.getAiPlay();
            return card == null ? "" : card + "";
        default:
            return "";
        }
    }
    
    public void goToPostGame() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            changeState(ClientState.SINGLE_PLAYER_POST_GAME);
            break;
        case IN_MULTIPLAYER_GAME:
            changeState(ClientState.MULTIPLAYER_POST_GAME);
            break;
        default:
            break;
        }
    }
    
    public void rename(String text) {
        if (text.isEmpty()) {
            notify("Name cannot be empty.");
        } else {
            sendCommandToServer("RENAME:STRING " + text.length() + ":" + text);
        }
    }
    
    public void readyPressed(int numRobots, boolean doubleDeck) {
        int numPlayers = (int) players.stream().filter(ClientPlayer::isHuman).count() + numRobots;
        if (myPlayer != null && myPlayer.isHost()) {
            if (numRobots > 0 && doubleDeck) {
                notify("Double deck with robots is not yet supported.");
            } else if (numPlayers <= 1) {
                notify("Not enough players.");
            } else {
                players.removeIf(p -> !p.isHuman());
                sendCommandToServer("START:" + numRobots + ":" + doubleDeck);
            }
        } else {
            notify("You are not the host.");
        }
    }
    
    public void close() {
        sendCommandToServer("CLOSE");
    }
    
    public void sendCommandToServer(String text) {
        writeThread.write(text);
    }
    
    public void makeBid(int bid) {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.incomingBid(spPlayer, bid);
            break;
        case IN_MULTIPLAYER_GAME:
            sendCommandToServer("BID:" + bid);
            break;
        default:
            break;
        }
    }
    
    public void makePlay(Card card) {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.incomingPlay(spPlayer, card);
            break;
        case IN_MULTIPLAYER_GAME:
            sendCommandToServer("PLAY:" + card);
            break;
        default:
            break;
        }
    }
    
    public void undoBid() {
        if (state == ClientState.IN_MULTIPLAYER_GAME) {
            sendCommandToServer("UNDOBID");
        }
    }
    
    public void undoBidReport(int index) {
        canvas.removeBidOnTimer(index);
        canvas.showMessageOnTimer(players.get(index).getName() + " is changing their bid.");
    }
    
    public void makeClaim() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.processClaim(spPlayer);
            break;
        case IN_MULTIPLAYER_GAME:
            sendCommandToServer("CLAIM");
            break;
        default:
            break;
        }
    }
    
    public void claimReport(int index) {
        canvas.claimReportOnTimer(index);
    }
    
    public void sendClaimResponse(String response) {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.processClaimResponse(spPlayer, response.equals("ACCEPT"));
            break;
        case IN_MULTIPLAYER_GAME:
            sendCommandToServer("CLAIMRESPONSE:" + response);
            break;
        default:
            break;
        }
    }
    
    public void claimResult(boolean accept) {
        canvas.claimResultOnTimer(accept);
        canvas.showMessageOnTimer("Claim " + (accept ? "accepted" : "refused") + ".");
    }
    
    public void pokePlayer() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            
            break;
        case IN_MULTIPLAYER_GAME:
            sendCommandToServer("POKE");
            break;
        default:
            break;
        }
    }
    
    public void bePoked() {
        canvas.bePokedOnTimer();
    }
    
    public void sendChat(String text) {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.sendChat(text);
            break;
        case IN_MULTIPLAYER_GAME:
            String command = "CHAT:STRING " 
                    + text.toCharArray().length + ":" 
                    + text;
            sendCommandToServer(command);
            break;
        default:
            break;
        }
    }
    
    public void connect() {
        if (!connected) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostName, Integer.parseInt(port)), 10000);
                
                players.clear();
                kibitzers.clear();
                
                writeThread = new ClientWriteThread(socket, this);
                writeThread.start();
                
                readThread = new ClientReadThread(socket, this);
                readThread.start();
                
                connected = true;
                canvas.reset();
                changeState(ClientState.IN_MULTIPLAYER_GAME);
                startPing();
            } catch (UnknownHostException e) {
                notify("That host does not exist.");
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                notify("Connection timed out.");
                e.printStackTrace();
            } catch (IOException e) {
                notify("Unable to connect to specified port.");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                notify("Invalid port.");
                e.printStackTrace();
            }
        }
    }
    
    public void disconnect() {
        readThread.disconnect();
        sendCommandToServer("CLOSE");
        connected = false;
        writeThread.disconnect();
    }
    
    public void reconnect(LinkedList<String> parsedContent) {
        ReconnectFrame reconnectFrame = new ReconnectFrame(parsedContent, this);
        reconnectFrame.setLocationRelativeTo(this);
        reconnectFrame.execute();
    }
    
    public void startPing() {
        pingTime = System.nanoTime();
        sendCommandToServer("PING");
    }
    
    public void endPing() {
        currentPing = System.nanoTime() - pingTime;
        Timer timer = new Timer(1000, null);
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                startPing();
                timer.stop();
            }
        });
        timer.start();
    }
    
    public double getPing() {
        return (double) currentPing / 1000000;
    }
    
    public boolean showPingSelected() {
        return pingOption.isSelected();
    }
    
    public void getKicked() {
        notify("You were disconnected from the server.");
        readThread.disconnect();
        connected = false;
        writeThread.disconnect();
        changeState(ClientState.LOGIN_MENU);
    }
    
    public void setPostGameTrumps(List<Card> trumps) {
        canvas.setTrumps(trumps);
    }
    
    public void addPostGameTakens(int index, List<Integer> takens) {
        players.get(index).setTakens(takens);
    }
    
    public void addPostGameHand(int index, List<Card> hand) {
        players.get(index).addPostGameHand(hand);
    }
    
    public void postGame() {
        canvas.showPostGameOnTimer();
    }
    
    public boolean soundSelected() {
        return soundOption.isSelected();
    }
    
    public boolean devSpeedSelected() {
        return devSpeedOption.isSelected();
    }
    
    public boolean lowGraphicsSelected() {
        return lowGraphicsOption.isSelected();
    }
    
    public boolean antialiasingSelected() {
        return antialiasingOption.isSelected();
    }
    
    public boolean showFpsSelected() {
        return fpsOption.isSelected();
    }
    
    public void loadConfigFile() {
        try {
            BufferedReader configReader = new BufferedReader(new FileReader("OhHellConfig.txt"));
            for (String line = configReader.readLine(); line != null; line = configReader.readLine()) {
                String[] split = line.split("=");
                if (split.length == 2) {
                    String variable = split[0].trim();
                    String value = split[1].trim();
                    if (variable.equals("windowlocationmenu")) {
                        String[] xy = value.split(",");
                        windowLocationMenu = new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    } else if (variable.equals("windowlocationingame")) {
                        String[] xy = value.split(",");
                        windowLocationInGame = new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    } else if (variable.equals("windowsizeingame")) {
                        String[] xy = value.split(",");
                        windowSizeInGame = new Dimension(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    } else if (variable.equals("windowmaximizedingame")) {
                        windowMaximizedInGame = value.equals("true");
                    } else if (variable.equals("recentusername")) {
                        username = value;
                    } else if (variable.equals("recentaddress")) {
                        hostName = value;
                    } else if (variable.equals("recentport")) {
                        port = value;
                    } else if (variable.equals("playsound")) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                soundOption.setSelected(value.equals("true"));
                            }
                        });
                    } else if (variable.equals("aidelay")) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                aiSpeedOptionSlider.setValue(1000 - Integer.parseInt(value));
                            }
                        });
                    }
                }
            }
            configReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Config file not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveConfigFile() {
        try {
            BufferedWriter configWriter = new BufferedWriter(new FileWriter("OhHellConfig.txt"));
            configWriter.write("windowlocationmenu = " + (int) windowLocationMenu.getX() + "," + (int) windowLocationMenu.getY() + "\n");
            configWriter.write("windowlocationingame = " + (int) windowLocationInGame.getX() + "," + (int) windowLocationInGame.getY() + "\n");
            configWriter.write("windowsizeingame = " + windowSizeInGame.width + "," + windowSizeInGame.height + "\n");
            configWriter.write("windowmaximizedingame = " + windowMaximizedInGame + "\n");
            configWriter.write("recentusername = " + username + "\n");
            configWriter.write("recentaddress = " + hostName + "\n");
            configWriter.write("recentport = " + port + "\n");
            configWriter.write("playsound = " + soundOption.isSelected() + "\n");
            configWriter.write("aidelay = " + getRobotDelay() + "\n");
            configWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void checkForUpdates() {
        try {
            BufferedReader versionReader = new BufferedReader(
                    new InputStreamReader(
                            new URL("https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/version")
                            .openStream()));
            newVersion = versionReader.readLine();
            versionReader.close();
            updateChecked = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void updatePressed() {
        if (!updateChecked) {
            checkForUpdates();
        } else {
            try {
                dispose();
                String path = new File(GameClient.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI()).getParent();
                Runtime.getRuntime().exec("java -jar " + path + "/updater.jar " + newVersion);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        GameClient client = new GameClient();
        client.execute();
    }
}