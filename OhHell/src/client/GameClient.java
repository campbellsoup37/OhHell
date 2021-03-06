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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import core.Card;
import core.GameOptions;
import core.OhHellCore;
import core.Player;
import core.Recorder;
import common.FileTools;
import common.GraphicsTools;
import common.OhcTextField;

public class GameClient extends JFrame {
    private static final long serialVersionUID = 1L;
    
    //////Dev Options //////////////////
    private final boolean stopperOptionEnabled = false;
    private final boolean botsOnlyOptionEnabled = true;
    private final boolean devSpeedOptionEnabled = true;
    private final boolean pingOptionEnabled = true;
    private final boolean fpsOptionEnabled = true;
    private final boolean lowGraphicsOptionEnabled = true;
    private final boolean antialiasingOptionEnabled = true;
    
    public static final int maxPlayers = 10;
    public static final int robotDelay = 2000;
    ///////////////////////////////////
    
    private String version;
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
    private String saveFileLocation = "";

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
    
    // UPDATING
    private OhcCanvas updatingCanvas;
    
    // SINGLE_PLAYER_MENU
    private OhcCanvas singlePlayerCanvas;
    
    private int numRobots = 4;
    private boolean doubleDeck = false;
    private OhHellCore core = new OhHellCore(true);
    private SinglePlayerPlayer spPlayer;
    
    // LOGIN_MENU
    private OhcCanvas loginCanvas;
    
    // IN_GAME
    private GameCanvas canvas;
    
    private volatile List<ClientPlayer> players = new ArrayList<>();
    private volatile List<ClientPlayer> kibitzers = new ArrayList<>();
    private volatile ClientPlayer myPlayer;
    private volatile GameOptions options;
    private volatile List<int[]> rounds = new ArrayList<>();
    private int roundNumber = 0;
    
    private Random random = new Random();
    
    private ClientState state;
    private OhcCanvas stateCanvas;
    
    private List<String> postGameFile;
    private StringBuilder postGameFileBuilder;
    
    public GameClient() {}
    
    public ClientState getClientState() {
        return state;
    }
    
    public void changeState(ClientState newState) {
        OhcCanvas oldStateCanvas = stateCanvas;
        state = newState;
        
        if (oldStateCanvas == canvas) {
            canvas.clearActionQueue();
        }
        
        switch (newState) {
        case MAIN_MENU:
            setMinimumSize(new Dimension(685, 500));
            setMaximized(windowMaximizedMenu);
            setLocation(windowLocationMenu);
            setSize(windowSizeMenu);
            setResizable(false);
            stateCanvas = mainMenuCanvas;
            break;
        case UPDATING:
            setSize(new Dimension(200, 50));
            stateCanvas = updatingCanvas;
            break;
        case SINGLE_PLAYER_MENU:
            setMinimumSize(new Dimension(685, 500));
            setMaximized(windowMaximizedMenu);
            setLocation(windowLocationMenu);
            setSize(windowSizeMenu);
            setResizable(false);
            stateCanvas = singlePlayerCanvas;
            break;
        case IN_SINGLE_PLAYER_GAME:
            setMinimumSize(new Dimension(1200, 700));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setLocation(windowLocationInGame);
            setResizable(true);
            stateCanvas = canvas;
            break;
        case SINGLE_PLAYER_POST_GAME:
            break;
        case LOGIN_MENU:
            setMinimumSize(new Dimension(685, 500));
            setMaximized(windowMaximizedMenu);
            setSize(windowSizeMenu);
            setResizable(false);
            stateCanvas = loginCanvas;
            myPlayer = null;
            break;
        case IN_MULTIPLAYER_GAME:
            setMinimumSize(new Dimension(1200, 700));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setLocation(windowLocationInGame);
            setResizable(true);
            stateCanvas = canvas;
            break;
        case MULTIPLAYER_POST_GAME:
            break;
        case FILE_VIEWER:
            setMinimumSize(new Dimension(1200, 700));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setLocation(windowLocationInGame);
            setResizable(true);
            stateCanvas = canvas;
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
                anyDc = anyDc || player.isDisconnected() && !player.isKicked();
            }
            if (player.getId().equals(username)) {
                myPlayer = player;
                if (player.isKibitzer()) {
                    myPlayer.setIndex(this.players.isEmpty() ? 0 : random.nextInt(this.players.size()));
                }
            }
        }

        final boolean anyDcF = anyDc;
        canvas.updatePlayersOnTimer();
        
        if (state == ClientState.IN_MULTIPLAYER_GAME && anyDcF) {
            canvas.setConnectionStatusOnTimer(false);
        }
    }
    
    public void removePlayer(String id) {
        int toRemove = players.size();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(id)) {
                toRemove = i;
            }
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
        
        canvas.updatePlayersOnTimer();
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
                    anyDc = anyDc || newPlayer.isDisconnected() && !newPlayer.isKicked();
                }
            }
        }
        
        players.sort((p1, p2) -> (int) Math.signum(p1.getIndex() - p2.getIndex()));

        final boolean anyDcF = anyDc;
        canvas.updatePlayersOnTimer();
        
        if (state == ClientState.IN_MULTIPLAYER_GAME) {
            canvas.setConnectionStatusOnTimer(!anyDcF);
        }
        
        if (state == ClientState.IN_MULTIPLAYER_GAME) {
            canvas.setConnectionStatusOnTimer(!anyDc);
        }
    }
    
    public void sendIdToServer(String serverVersion) {
        if (!serverVersion.equals(version)) {
            notify("Your version (" + version + ") does not match the server version (" + serverVersion + ").");
        }
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
        if (roundNumber >= rounds.size()) {
            return new int[] {-1, -1};
        } else {
            return rounds.get(roundNumber);
        }
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
        changeState(ClientState.IN_SINGLE_PLAYER_GAME);
        
        GameOptions options = new GameOptions();
        options.setD(doubleDeck ? 2 : 1);
        options.setRobotDelay(devSpeedSelected() ? 0 : robotDelay);
        
        core.startGame(numRobots, options, null);
    }
    
    public void startGame(GameOptions options) {
        this.options = options;
        roundNumber = 0;
        postGameFileBuilder = new StringBuilder();

        canvas.initializingOnTimer(true);
        
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            canvas.pregameOnTimer(true);
            break;
        case IN_MULTIPLAYER_GAME:
            break;
        case LOGIN_MENU:
            // This occurs when reconnecting. Same behavior as starting from post-game.
        case MULTIPLAYER_POST_GAME:
            changeState(ClientState.IN_MULTIPLAYER_GAME);
            canvas.pregameOnTimer(true);
            break;
        default:
            throw new IllegalClientStateException("startGame() was called in state " + state);
        }
    }
    
    public GameOptions getGameOptions() {
        return options;
    }
    
    public List<ClientPlayer> getPlayers() {
        return players;
    }
    
    public List<ClientPlayer> getKibitzers() {
        return kibitzers;
    }
    
    public ClientPlayer getMyPlayer() {
        return myPlayer;
    }
    
    public void restartRound() {
        for (ClientPlayer player : players) {
            if (player.getBids().size() >= roundNumber + 1) {
                player.getBids().remove(roundNumber);
            }
        }

        canvas.showMessageOnTimer("Someone was kicked. Redealing this round.", false);
        canvas.clearRoundOnTimer();
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
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public void chat(String text) {
        canvas.chat(text);
    }
    
    public void setStatePlayer(int index, boolean hasBid, int bid, int taken, Card lastTrick, Card trick) {
        canvas.setStatePlayerOnTimer(index, hasBid, bid, taken, lastTrick, trick);
    }
    
    public void setStatePlayerBids(int index, List<Integer> bids) {
        canvas.setStatePlayerBidsOnTimer(index, bids);
    }
    
    public void setStatePlayerScores(int index, List<Integer> scores) {
        canvas.setStatePlayerScoresOnTimer(index, scores);
    }
    
    public void execute(boolean deleteUpdater) {
        try {
            GameClient thisClient = this;
            
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            setTitle("Oh Hell");
            
            setIconImage(FileTools.loadImage("resources/icon/cw.png", this));

            setSize(windowSizeMenu);
            
            BufferedImage tableImg = FileTools.loadImage("resources/client/tableimage.png", this);
            
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
                    case IN_MULTIPLAYER_GAME:
                    case MULTIPLAYER_POST_GAME:
                    case FILE_VIEWER:
                        leaveGame();
                        break;
                    default:
                        break;
                    }
                }
            });
            optionsItem.add(backOption);
            
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
                            return 220;
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
                            changeState(ClientState.LOGIN_MENU);
                        }
                    };
                    
                    CanvasButton openButton = new CanvasButton("View saved game") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 340;
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
                            JFileChooser chooser = new JFileChooser();
                            try {
                                if (saveFileLocation.isEmpty() || !(new File(saveFileLocation)).exists()) {
                                    chooser.setCurrentDirectory(new File(getDirectory()));
                                } else {
                                    chooser.setCurrentDirectory(new File(saveFileLocation));
                                }
                            } catch (URISyntaxException e1) {
                                e1.printStackTrace();
                            }
                            if (chooser.showOpenDialog(thisClient) == JFileChooser.APPROVE_OPTION) {
                                try {
                                    BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()));
                                    List<String> lines = new LinkedList<>();
                                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                                        lines.add(line);
                                    }
                                    reader.close();
                                    saveFileLocation = chooser.getCurrentDirectory().getPath();
                                    
                                    postGameFile = lines;
                                    changeState(ClientState.FILE_VIEWER);
                                    canvas.pregameOnTimer(false);
                                    canvas.loadPostGameOnTimer(lines);
                                } catch (FileNotFoundException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
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
                                return "Download v" + newVersion;
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
                    
                    setInteractables(Arrays.asList(Arrays.asList(spButton, mpButton, openButton, updateButton)));
                }
                
                @Override
                public boolean isShown() {
                    return state == ClientState.MAIN_MENU;
                }
                
                @Override
                public void customPaintFirst(Graphics graphics) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                    GraphicsTools.drawBox(graphics, 200, 80, 285, 320, 20);

                    graphics.setColor(Color.BLACK);
                    graphics.setFont(GraphicsTools.fontTitle);
                    GraphicsTools.drawStringJustified(graphics, "Oh Hell", 342, 100, 1, 2);
                    
                    graphics.setFont(GraphicsTools.fontBold);
                    GraphicsTools.drawStringJustified(graphics, "v" + version, 610, 460, 0, 1);
                    
                    graphics.setFont(GraphicsTools.font);
                }
            };
            
            updatingCanvas = new OhcCanvas(this) {
                private static final long serialVersionUID = 1L;

                @Override
                public void initialize() {
                    setBackground(tableImg);
                }
                
                @Override
                public boolean isShown() {
                    return state == ClientState.UPDATING;
                }
                
                @Override
                public void customPaintFirst(Graphics graphics) {
                    GraphicsTools.drawStringJustified(graphics, 
                            "Updating", getWidth() / 2, getHeight() / 2, 1, 1);
                }
            };
            
            singlePlayerCanvas = new OhcCanvas(this) {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void initialize() {
                    setBackground(tableImg);
                    
                    CanvasSpinner robotsSpinner = new CanvasSpinner(4) {
                        @Override
                        public int x() {
                            return getWidth() / 2 + 20;
                        }
                        
                        @Override
                        public int y() {
                            return 155;
                        }
                        
                        @Override
                        public int min() {
                            return 1;
                        }
                        
                        @Override
                        public int max() {
                            return maxPlayers - (botsOnlyOption.isSelected() ? 0 : 1);
                        }
                    };
                    
                    CanvasButton doubleDeckButton = new CanvasButton("") {
                        @Override
                        public int x() {
                            return getWidth() / 2 + 20;
                        }
                        
                        @Override
                        public int y() {
                            return 200;
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
                        public String text() {
                            return doubleDeck ? "x" : "";
                        }
                        
                        @Override
                        public void click() {
                            doubleDeck = !doubleDeck;
                        }
                    };
                    
                    CanvasButton startButton = new CanvasButton("Start") {
                        @Override
                        public int x() {
                            return 267;
                        }
                        
                        @Override
                        public int y() {
                            return 260;
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
                            numRobots = robotsSpinner.getValue();
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
                    
                    setInteractables(Arrays.asList(Arrays.asList(robotsSpinner, doubleDeckButton, startButton, backButton)));
                }
                
                @Override
                public boolean isShown() {
                    return state == ClientState.SINGLE_PLAYER_MENU;
                }
                
                @Override
                public void customPaintFirst(Graphics graphics) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                    GraphicsTools.drawBox(graphics, 200, 80, 285, 320, 20);
                    
                    graphics.setFont(GraphicsTools.fontBold);
                    graphics.setColor(Color.BLACK);
                    GraphicsTools.drawStringJustified(graphics, 
                            "Robots:", 
                            getWidth() / 2 - 20, 
                            170, 
                            2, 1);
                    GraphicsTools.drawStringJustified(graphics, 
                            "Double deck:", 
                            getWidth() / 2 - 20, 
                            210, 
                            2, 1);
                    graphics.setFont(GraphicsTools.font);
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
                public void customPaintFirst(Graphics graphics) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                    GraphicsTools.drawBox(graphics, 200, 80, 285, 320, 20);

                    graphics.setFont(GraphicsTools.fontBold);
                    graphics.setColor(Color.BLACK);
                    GraphicsTools.drawStringJustified(graphics, "Username:", 290, 120, 2, 1);
                    GraphicsTools.drawStringJustified(graphics, "Server:", 290, 170, 2, 1);
                    GraphicsTools.drawStringJustified(graphics, "Port:", 290, 220, 2, 1);
                }
            };
            
            canvas = new GameCanvas(this) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isShown() {
                    return state == ClientState.IN_SINGLE_PLAYER_GAME
                            || state == ClientState.SINGLE_PLAYER_POST_GAME
                            || state == ClientState.IN_MULTIPLAYER_GAME
                            || state == ClientState.MULTIPLAYER_POST_GAME
                            || state == ClientState.FILE_VIEWER;
                }
            };
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
                        c.mousePressed(e.getX(), e.getY(), e.getButton());
                        c.grabFocus();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        c.mouseReleased(e.getX(), e.getY(), e.getButton());
                    }
                });
                c.addMouseMotionListener(new MouseMotionListener() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        c.mouseMoved(e.getX(), e.getY(), true);
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        c.mouseMoved(e.getX(), e.getY(), false);
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
                            || state == ClientState.MULTIPLAYER_POST_GAME
                            || state == ClientState.FILE_VIEWER) {
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
                            || state == ClientState.MULTIPLAYER_POST_GAME
                            || state == ClientState.FILE_VIEWER) {
                        windowSizeInGame = getSize();
                    }
                    resizeCanvas();
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    if (state == ClientState.IN_SINGLE_PLAYER_GAME
                            || state == ClientState.IN_MULTIPLAYER_GAME
                            || state == ClientState.SINGLE_PLAYER_POST_GAME
                            || state == ClientState.MULTIPLAYER_POST_GAME
                            || state == ClientState.FILE_VIEWER) {
                        windowLocationInGame = getLocationOnScreen();
                    } else {
                        windowLocationMenu = getLocationOnScreen();
                    }
                }
            });
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            changeState(ClientState.MAIN_MENU);
            
            if (deleteUpdater) {
                FileTools.deleteFile(getDirectory() + "/clientupdater.jar");
            }
            BufferedReader versionReader = FileTools.getInternalFile("version", this);
            version = versionReader.readLine();
            checkForUpdates();
            
            initiatePaintLoop();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void initiatePaintLoop() {
        new Timer(5, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        stateCanvas.updateUI();
                    }
                });
            }
        }).start();
    }
    
    public void leaveGame() {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.stopGame();
        case SINGLE_PLAYER_POST_GAME:
            changeState(ClientState.MAIN_MENU);
            break;
        case IN_MULTIPLAYER_GAME:
        case MULTIPLAYER_POST_GAME:
            if (connected) {
                disconnect();
            }
            changeState(ClientState.MAIN_MENU);
            break;
        case FILE_VIEWER:
            changeState(ClientState.MAIN_MENU);
        default:
            break;
        }
    }
    
    public void requestEndGame() {
        if (state == ClientState.IN_SINGLE_PLAYER_GAME) {
            core.requestEndGame(spPlayer);
        } else {
            sendCommandToServer("STOP");
        }
    }
    
    public void endGame(String id) {
        canvas.endGameOnTimer();
        if (state == ClientState.IN_MULTIPLAYER_GAME) {
            canvas.showMessageOnTimer(id + " is ending the game.", true);
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
            if (numRobots > 0 && numPlayers >= 11) {
                notify("Playing with robots and more than 10 players is not yet supported.");
            } else if (numPlayers <= 1) {
                notify("Not enough players.");
            } else {
                GameOptions options = new GameOptions();
                options.setD(doubleDeck ? 2 : 1);
                options.setRobotDelay(devSpeedSelected() ? 0 : robotDelay);
                sendCommandToServer("START:" + numRobots + ":" + options);
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
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
            core.processUndoBid(spPlayer);
            break;
        case IN_MULTIPLAYER_GAME:
            sendCommandToServer("UNDOBID");
            break;
        default:
            break;
        }
    }
    
    public void undoBidReport(int index) {
        canvas.removeBidOnTimer(index);
        canvas.showMessageOnTimer(players.get(index).getName() + " is changing their bid.", false);
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
        canvas.showMessageOnTimer("Claim " + (accept ? "accepted" : "refused") + ".", false);
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
    
    public void sendChat(String recipient, String text) {
        switch (state) {
        case IN_SINGLE_PLAYER_GAME:
        case SINGLE_PLAYER_POST_GAME:
            core.sendChat(spPlayer, null, text);
            break;
        case IN_MULTIPLAYER_GAME:
        case MULTIPLAYER_POST_GAME:
            String command = "CHAT:"
                    + (recipient.isEmpty() ? "" : 
                        "STRING " + recipient.length() + ":" + recipient) + ":"
                    + "STRING " 
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
                canvas.pregameOnTimer(false);
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
    
    public String getUsername() {
        return username;
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
    
    public void receivePostGameFile(String file) {
        postGameFile = Arrays.asList(file.split(Recorder.splitPreceder + Recorder.lineDelimiterRegex));
        canvas.loadPostGameOnTimer(postGameFile);
    }
    
    public void receivePostGameFilePiece(String piece) {
        if (piece.isEmpty()) {
            receivePostGameFile(postGameFile.toString());
        } else {
            postGameFileBuilder.append(piece);
        }
    }
    
    public void savePostGame() {
        JFileChooser chooser = new JFileChooser();
        try {
            if (saveFileLocation.isEmpty() || !(new File(saveFileLocation)).exists()) {
                chooser.setCurrentDirectory(new File(getDirectory()));
            } else {
                chooser.setCurrentDirectory(new File(saveFileLocation));
            }
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        int suffix = 1;
        while (new File(chooser.getCurrentDirectory().getPath() + "/" + defaultFileName(suffix)).exists()) {
            suffix++;
        }
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getPath() + "/" + defaultFileName(suffix)));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile()));
                for (String line : postGameFile) {
                    writer.write(line + "\n");
                }
                writer.close();
                saveFileLocation = chooser.getCurrentDirectory().getPath();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    public String defaultFileName(int suffix) {
        return "game_" + LocalDate.now() + "_" + suffix + ".txt";
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
            BufferedReader configReader = new BufferedReader(new FileReader(getDirectory() + "/OhHellConfig.txt"));
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
                    } else if (variable.equals("savefilelocation")) {
                        saveFileLocation = value;
                    }
                }
            }
            configReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Config file not found.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    public void saveConfigFile() {
        try {
            BufferedWriter configWriter = new BufferedWriter(new FileWriter(getDirectory() + "/OhHellConfig.txt"));
            configWriter.write("windowlocationmenu = " + (int) windowLocationMenu.getX() + "," + (int) windowLocationMenu.getY() + "\n");
            configWriter.write("windowlocationingame = " + (int) windowLocationInGame.getX() + "," + (int) windowLocationInGame.getY() + "\n");
            configWriter.write("windowsizeingame = " + windowSizeInGame.width + "," + windowSizeInGame.height + "\n");
            configWriter.write("windowmaximizedingame = " + windowMaximizedInGame + "\n");
            configWriter.write("recentusername = " + username + "\n");
            configWriter.write("recentaddress = " + hostName + "\n");
            configWriter.write("recentport = " + port + "\n");
            configWriter.write("playsound = " + soundOption.isSelected() + "\n");
            configWriter.write("aidelay = " + getRobotDelay() + "\n");
            configWriter.write("savefilelocation = " + saveFileLocation);
            configWriter.close();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    public void checkForUpdates() {
        newVersion = FileTools.getCurrentVersion();
        updateChecked = true;
    }
    
    public void updatePressed() {
        changeState(ClientState.UPDATING);
        System.out.println("UPDATE BUTTON PRESSED");
        if (!updateChecked) {
            checkForUpdates();
        } else {
            try {
                String path = getDirectory() + "/clientupdater.jar";
                
                FileTools.downloadFile(
                        "https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/updater.jar", 
                        path);
                
                if (new File(path).exists()) {
                    if (FileTools.isUnix()) {
                        FileTools.runTerminalCommand(new String[] {
                                "chmod",
                                "777",
                                path
                        }, false);
                    }
                    FileTools.runTerminalCommand(new String[] {
                            FileTools.cmdJava(),
                            "-jar",
                            path,
                            newVersion,
                            "OhHellClient.jar",
                            getFileName()
                    }, false);
                    dispose();
                    System.exit(0);
                } else {
                    notify("Failed to download updater.");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getDirectory() throws URISyntaxException {
        return new File(GameClient.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).getParent();
    }
    
    public String getFileName() throws URISyntaxException {
        return new File(GameClient.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).getPath();
    }
    
    public static void main(String[] args) {
        boolean deleteUpdater = false;
        
        for (String arg : args) {
            if (arg.equals("-deleteupdater")) {
                deleteUpdater = true;
            } else if (arg.equals("-savesystemproperties")) {
                System.out.println("PRINTING SYSTEM PROPERTIES");
                try {
                    List<String> lines = new ArrayList<>();
                    java.util.Properties props = System.getProperties();
                    for (Object prop : props.entrySet()) {
                        lines.add(prop.toString());
                    }
                    Collections.sort(lines);
                    
                    BufferedWriter writer = new BufferedWriter(new FileWriter("systemproperties.txt"));
                    for (String line : lines) {
                        writer.append(line + "\n");
                    }
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        GameClient client = new GameClient();
        client.execute(deleteUpdater);
    }
}