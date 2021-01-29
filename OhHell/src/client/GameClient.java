package client;
import java.awt.Component;
import java.awt.Dimension;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;

import core.Card;
import core.OhHellCore;
import core.Player;
import graphics.OhcButton;
import graphics.OhcGraphicsTools;
import graphics.OhcScrollPane;
import graphics.OhcTextField;

public class GameClient extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private final String version = "0.1.5.1";
    
    private boolean connected = false;
    private Socket socket;
    private String hostName = "localhost";
    private int port = 6066;
    private String name = "";
    
    private ClientReadThread readThread;
    private ClientWriteThread writeThread;
    
    private NotificationFrame notificationFrame;
    //private DisconnectFrame dcFrame;
    
    private Point windowLocationMenu = new Point(0, 0);
    private Dimension windowSizeMenu = new Dimension(685, 540);
    private boolean windowMaximizedMenu = false;
    private Point windowLocationInGame = new Point(0, 0);
    private Dimension windowSizeInGame = new Dimension(1200, 800);
    private boolean windowMaximizedInGame = false;
    
    private JMenuBar menuBar = new JMenuBar();
    private JMenu optionsItem = new JMenu("Options");
    private JCheckBox soundOption = new JCheckBox("Sound");
    private JMenu aiSpeedOption = new JMenu("AI play speed");
    private JSlider aiSpeedOptionSlider = new JSlider(JSlider.HORIZONTAL, 0, 900, 500);
    private JCheckBox fpsOption = new JCheckBox("Show FPS");
    private JMenu devOptionsItem = new JMenu("Dev options");
    private JCheckBox aiHelpOption = new JCheckBox("AI help");
    private JCheckBox stopperOption = new JCheckBox("Stoppers");
    private JCheckBox devSpeedOption = new JCheckBox("Fast animation");
    private JCheckBox lowGraphicsOption = new JCheckBox("Low graphics");
    private JMenuItem backOption = new JMenuItem("Back to menu");
    
    private boolean stopperSelected = false;
    
    // MAIN_MENU
    private JLabel titleLabel = new JLabel("Oh Hell", SwingConstants.CENTER);
    private JButton singlePlayerButton = new OhcButton("Single Player");
    private JButton multiplayerButton = new OhcButton("Multiplayer");
    private JLabel versionLabel = new JLabel("v" + version);
    private OhcButton updateButton = new OhcButton("Check for updates");
    private Component[] mainMenuComponents = {
                titleLabel, singlePlayerButton, multiplayerButton, versionLabel, updateButton
            };
    
    private boolean updateFound = false;
    private String newVersion;
    
    // SINGLE_PLAYER_MENU
    private JLabel spRobotsLabel = new JLabel("Robots:");
    private JSpinner spRobotsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 6, 1));
    private JButton spStartButton = new OhcButton("Start!");
    private JButton spBackButton = new OhcButton("Back");
    private Component[] singlePlayerMenuComponents = {
                spRobotsLabel, spRobotsSpinner, spStartButton, spBackButton
            };
    
    private OhHellCore core = new OhHellCore(false);
    private SinglePlayerPlayer spPlayer;
    
    // MULTIPLAYER_MENU
    private JLabel hostLabel = new JLabel("Host name:");
    private JTextField hostField = new OhcTextField("Host address");
    private JLabel portLabel = new JLabel("Port:");
    private JTextField portField = new OhcTextField("Port");
    private JButton connectButton = new OhcButton("Connect");
    private JLabel nameLabel = new JLabel("Name:");
    private OhcTextField nameField = new OhcTextField("Player name");
    private JButton nameButton = new OhcButton("Change name");
    private JCheckBox kibitzerCheckBox = new JCheckBox("Join as kibitzer");
    private JButton mpOptionsButton = new OhcButton("Game options");
    private JButton mpStartButton = new OhcButton("Start!");
    private JLabel playersLabel = new JLabel("Players:");
    private DefaultListModel<String> playersListModel = new DefaultListModel<>();
    private JList<String> playersJList = new JList<>(playersListModel);
    private JScrollPane playersScrollPane = new OhcScrollPane(playersJList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JButton mpBackButton = new OhcButton("Back");
    private Component[] multiplayerMenuComponents = {
                hostLabel, hostField, portLabel, portField, connectButton,
                nameLabel, nameField, nameButton, kibitzerCheckBox, mpOptionsButton,
                mpStartButton, playersLabel, playersJList, playersScrollPane, mpBackButton
            };
    
    private int mpNumRobots = 0;
    private boolean mpDoubleDeck = false;
    
    // IN_GAME
    private GameCanvas canvas = new GameCanvas(this);
    private JTextArea chatArea = new JTextArea();
    private JScrollPane chatScrollPane = new OhcScrollPane(chatArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private JTextField chatField = new OhcTextField("Enter text");
    private JButton showButton = new JButton("Show");
    private Component[] inGameComponents = {
                canvas, chatArea, chatScrollPane, chatField
            };
    
    // POST_GAME
    private JButton pgBackButton = new JButton("Back");
    private Component[] postGameComponents = {
                canvas, pgBackButton, chatArea, chatScrollPane, chatField
            };
    
    private Component[][] allComponents = {
                mainMenuComponents, singlePlayerMenuComponents, multiplayerMenuComponents, inGameComponents, postGameComponents
            };
    
    private List<ClientPlayer> players = new ArrayList<>();
    private List<ClientPlayer> kibitzers = new ArrayList<>();
    private ClientPlayer myPlayer;
    private List<int[]> rounds = new ArrayList<>();
    private int roundNumber = 0;
    
    private Random random = new Random();
    
    private ClientState state;
    
    public GameClient() {}
    
    public ClientState getClientState() {
        return state;
    }
    
    public void changeState(ClientState newState) {
        Component[] toShow = {};
        
        switch (newState) {
        case MAIN_MENU:
            toShow = mainMenuComponents;
            setLocation(windowLocationMenu);
            setMinimumSize(new Dimension(685, 500));
            setSize(windowSizeMenu);
            setMaximized(windowMaximizedMenu);
            setResizable(false);
            break;
        case SINGLE_PLAYER_MENU:
            name = "";
            toShow = singlePlayerMenuComponents;
            setLocation(windowLocationMenu);
            setMinimumSize(new Dimension(685, 500));
            setSize(windowSizeMenu);
            setMaximized(windowMaximizedMenu);
            setResizable(false);
            break;
        case IN_SINGLE_PLAYER_GAME:
            toShow = inGameComponents;
            setLocation(windowLocationInGame);
            setMinimumSize(new Dimension(1200, 800));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setResizable(true);
            break;
        case SINGLE_PLAYER_POST_GAME:
            toShow = postGameComponents;
            break;
        case MULTIPLAYER_MENU:
            toShow = multiplayerMenuComponents;
            setLocation(windowLocationMenu);
            setMinimumSize(new Dimension(685, 500));
            setSize(windowSizeMenu);
            setMaximized(windowMaximizedMenu);
            setResizable(false);
            break;
        case IN_MULTIPLAYER_GAME:
            toShow = inGameComponents;
            setLocation(windowLocationInGame);
            setMinimumSize(new Dimension(1200, 800));
            setSize(windowSizeInGame);
            setMaximized(windowMaximizedInGame);
            setResizable(true);
            break;
        case MULTIPLAYER_POST_GAME:
            toShow = postGameComponents;
            break;
        }
        
        for (Component[] compsList : allComponents) {
            if (compsList != toShow) {
                for (Component comp : compsList) {
                    comp.setVisible(false);
                }
            }
        }
        for (Component comp : toShow) {
            comp.setVisible(true);
        }
        state = newState;
    }
    
    public void setMaximized(boolean m) {
        if (m) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(getExtendedState() & (~JFrame.MAXIMIZED_BOTH));
        }
    }
    
    public void updatePlayersList(List<ClientPlayer> newPlayers, int myIndex) {
        playersListModel.clear();
        if (state != ClientState.IN_MULTIPLAYER_GAME) {
            players.clear();
        }
        kibitzers.clear();

        boolean anyDc = false;
        
        for (int i = 0; i < newPlayers.size(); i++) {
            ClientPlayer player = newPlayers.get(i);
            if (player.isDisconnected() && !player.isKicked()) {
                anyDc = true;
            }
            if (connected && player.isHuman()) {
                playersListModel.addElement(
                        player.getName()
                        + (player.isHost() ? " (host)" : "")
                        + (player.isKibitzer() ? " (kibitzer)" : ""));
            }
            if (!player.isKibitzer()) {
                if (state != ClientState.IN_MULTIPLAYER_GAME) {
                    players.add(player);
                } else {
                    ClientPlayer oldPlayer = players.get(i);
                    oldPlayer.setIndex(player.getIndex());
                    oldPlayer.setName(player.getName());
                    oldPlayer.setHost(player.isHost());
                    oldPlayer.setDisconnected(player.isDisconnected());
                    oldPlayer.setKicked(player.isKicked());
                    oldPlayer.setKibitzer(player.isKibitzer());
                    player = oldPlayer;
                }
            } else {
                kibitzers.add(player);
            }
            if (i == myIndex) {
                myPlayer = player;
                name = player.getName();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        nameField.setText(name);
                    }
                });
            }
        }
        
        /*if (anyDc && !myPlayer.isKibitzer()) {
            if (dcFrame == null) {
                dcFrame = new DisconnectFrame(this);
                dcFrame.setLocationRelativeTo(this);
                dcFrame.execute();
            }
            dcFrame.setDcList(
                    players.stream()
                        .filter(p -> p.isDisconnected() && !p.isKicked())
                        .map(ClientPlayer::getName)
                        .collect(Collectors.toList()));
        } else {
            if (dcFrame != null) {
                dcFrame.close();
                dcFrame = null;
            }
        }*/
        if (state == ClientState.IN_MULTIPLAYER_GAME) {
            canvas.setConnectionStatusOnTimer(!anyDc);
        }
        
        if (myPlayer.isKibitzer()) {
            kibitzerCheckBox.setSelected(true);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playersJList.updateUI();
            }
        });
    }
    
    public void updateRounds(List<int[]> rounds, int roundNumber) {
        this.rounds = rounds;
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
    
    public void startGame() {
        roundNumber = 0;

        for (ClientPlayer player : players) {
            player.reset();
        }
        if (myPlayer.isKibitzer()) {
            myPlayer.setIndex(random.nextInt(players.size()));
        }
        canvas.reset();
        
        switch(state) {
        case SINGLE_PLAYER_MENU:
            changeState(ClientState.IN_SINGLE_PLAYER_GAME);
            break;
        case MULTIPLAYER_MENU:
            changeState(ClientState.IN_MULTIPLAYER_GAME);
            break;
        case MULTIPLAYER_POST_GAME:
            changeState(ClientState.IN_MULTIPLAYER_GAME);
            break;
        default:
            break;
        }
        //canvas.repaint();
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
        //canvas.repaint();

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
        //canvas.repaint();
    }
    
    public void setDealerLeader(int dealer, int leader) {
        canvas.setDealerOnTimer(dealer);
        canvas.setLeaderOnTimer(leader);
    }
    
    public void bid(int index) {
        canvas.setBiddingOnTimer(index);
        //canvas.repaint();
    }
    
    public void play(int index) {
        canvas.setPlayingOnTimer(index);
        //canvas.repaint();
    }
    
    public int getRobotDelay() {
        return 1000 - aiSpeedOptionSlider.getValue();
    }
    
    public void bidReport(int index, int bid) {
        canvas.setBidOnTimer(index, bid, players.get(index).isHuman() ? 0 : getRobotDelay());
        //canvas.repaint();
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
        //canvas.deletePokeTimer();
        canvas.setScoresOnTimer(scores);
        canvas.clearRoundOnTimer();
        //canvas.repaint();
        if (stopperSelected) {
            canvas.addStopper();
        }
    }
    
    public void incrementRoundNumber() {
        roundNumber++;
    }
    
    public void chat(String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                chatArea.setText(chatArea.getText() + text + "\n");
            }
        });
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
            
            titleLabel.setFont(OhcGraphicsTools.fontTitle);
            titleLabel.setBounds(0, 100, 685, 52);
            add(titleLabel);
            
            singlePlayerButton.setBounds(267, 230, 151, 40);
            singlePlayerButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeState(ClientState.SINGLE_PLAYER_MENU);
                }
            });
            add(singlePlayerButton);
            
            multiplayerButton.setBounds(267, 330, 151, 40);
            multiplayerButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeState(ClientState.MULTIPLAYER_MENU);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            nameField.setText("");
                        }
                    });
                }
            });
            add(multiplayerButton);
            
            versionLabel.setBounds(610, 440, 100, 40);
            add(versionLabel);
            
            updateButton.setBounds(450, 450, 150, 20);
            updateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updatePressed();
                }
            });
            add(updateButton);
            
            spRobotsLabel.setBounds(256, 150, 200, 40);
            add(spRobotsLabel);
            
            spRobotsSpinner.setBounds(328, 150, 100, 40);
            add(spRobotsSpinner);
            
            spStartButton.setBounds(267, 250, 150, 40);
            GameClient thisClient = this;
            spStartButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int robotCount = (int) spRobotsSpinner.getValue();
                    spPlayer = new SinglePlayerPlayer(thisClient);
                    List<Player> players = new ArrayList<>();
                    players.add(spPlayer);
                    core.setPlayers(players);
                    spPlayer.setCore(core);
                    core.startGame(robotCount, false, null, 0);
                }
            });
            add(spStartButton);
            
            spBackButton.setBounds(267, 350, 150, 40);
            spBackButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeState(ClientState.MAIN_MENU);
                }
            });
            add(spBackButton);
            
            hostLabel.setBounds(20, 20, 200, 40);
            add(hostLabel);
            
            hostField.setBounds(120, 20, 200, 40);
            hostField.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent arg0) {}

                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        connect();
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {}
            });
            add(hostField);
            
            portLabel.setBounds(57, 70, 200, 40);
            add(portLabel);
            
            portField.setBounds(120, 70, 200, 40);
            portField.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent arg0) {}

                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        connect();
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {}
            });
            add(portField);
            
            connectButton.setBounds(120, 120, 150, 40);
            connectButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    connect();
                }
            });
            add(connectButton);
            
            playersLabel.setBounds(350, 20, 200, 40);
            add(playersLabel);
            
            playersScrollPane.setBounds(350, 70, 300, 320);
            add(playersScrollPane);
            
            nameLabel.setBounds(48, 200, 200, 40);
            add(nameLabel);
            
            nameField.setBounds(120, 200, 200, 40);
            nameField.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent arg0) {}
                
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (connected) {
                            rename();
                        }
                    }
                }
                
                public void keyTyped(KeyEvent e) {}
            });
            add(nameField);
            
            nameButton.setBounds(120, 250, 150, 40);
            nameButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (connected) {
                        rename();
                    }
                }
            });
            add(nameButton);
            
            kibitzerCheckBox.setBounds(120, 300, 150, 40);
            kibitzerCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (connected) {
                        sendCommandToServer("KIBITZER:" + kibitzerCheckBox.isSelected());
                    }
                }
            });
            add(kibitzerCheckBox);
            
            mpOptionsButton.setBounds(120, 350, 150, 40);
            mpOptionsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GameOptionsFrame mpOptionsFrame = new GameOptionsFrame(thisClient, mpNumRobots, mpDoubleDeck);
                    mpOptionsFrame.setLocationRelativeTo(thisClient);
                    mpOptionsFrame.execute();
                }
            });
            add(mpOptionsButton);
            
            mpStartButton.setBounds(120, 410, 150, 40);
            mpStartButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (connected) {
                        if (players.size() + mpNumRobots <= 1) {
                            thisClient.notify("There must be at least two players to start the game.");
                        } else if (mpNumRobots > 0 && mpDoubleDeck) {
                            thisClient.notify("Double deck with robots is currently unsupported.");
                        } else {
                            readyPressed();
                        }
                    }
                }
            });
            add(mpStartButton);
            
            mpBackButton.setBounds(500, 410, 150, 40);
            mpBackButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (connected) {
                        disconnect();
                    }
                    changeState(ClientState.MAIN_MENU);
                }
            });
            add(mpBackButton);
            
            canvas.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {}
                
                @Override
                public void mouseEntered(MouseEvent arg0) {}
                
                @Override
                public void mouseExited(MouseEvent arg0) {}

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        canvas.mousePressed(e.getX(), e.getY());
                    } else if (e.getButton() == MouseEvent.BUTTON3 && stopperSelected) {
                        canvas.removeStopper();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        canvas.mouseReleased(e.getX(), e.getY());
                    }
                }
            });
            canvas.addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    canvas.mouseMoved(e.getX(), e.getY());
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    canvas.mouseMoved(e.getX(), e.getY());
                }
            });
            add(canvas);
            canvas.setLayout(null);
            
            pgBackButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (state) {
                    case SINGLE_PLAYER_POST_GAME:
                        changeState(ClientState.SINGLE_PLAYER_MENU);
                        break;
                    case MULTIPLAYER_POST_GAME:
                        changeState(ClientState.MULTIPLAYER_MENU);
                        break;
                    default:
                        break;
                    }
                }
            });
            canvas.add(pgBackButton);

            canvas.add(chatScrollPane);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setEditable(false);
            DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            chatScrollPane.setVisible(false);
            
            canvas.add(chatField);
            chatField.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent arg0) {}

                @Override
                public void keyReleased(KeyEvent e) {
                    try {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER 
                                && !chatField.getText().trim().isEmpty()) {
                            String text = name + ": " + chatField.getText().trim();
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
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    chatField.setText("");
                                }
                            });
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {}
            });
            
            canvas.add(showButton);
            showButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showButton.setVisible(false);
                    canvas.doShowOneCard();
                    //canvas.repaint();
                }
            });
            showButton.setVisible(false);
            
            aiSpeedOption.add(aiSpeedOptionSlider);

            aiSpeedOption.setPreferredSize(new Dimension(150, 25));
            optionsItem.add(aiSpeedOption);
            
            optionsItem.add(soundOption);
            
            optionsItem.add(fpsOption);
            
            backOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (state) {
                    case SINGLE_PLAYER_MENU:
                        changeState(ClientState.MAIN_MENU);
                        break;
                    case MULTIPLAYER_MENU:
                        changeState(ClientState.MAIN_MENU);
                        break;
                    case IN_SINGLE_PLAYER_GAME:
                        core.stopGame();
                        changeState(ClientState.SINGLE_PLAYER_MENU);
                        break;
                    case SINGLE_PLAYER_POST_GAME:
                        changeState(ClientState.SINGLE_PLAYER_MENU);
                        break;
                    case IN_MULTIPLAYER_GAME:
                        if (connected) {
                            disconnect();
                        }
                        changeState(ClientState.MULTIPLAYER_MENU);
                        break;
                    case MULTIPLAYER_POST_GAME:
                        changeState(ClientState.MULTIPLAYER_MENU);
                        break;
                    default:
                        break;
                    }
                }
            });
            optionsItem.add(backOption);
            
            aiHelpOption.setEnabled(false);
            aiHelpOption.setPreferredSize(new Dimension(150, 25));
            aiHelpOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.setAiHelp(aiHelpOption.isSelected());
                }
            });
            devOptionsItem.add(aiHelpOption);
            
            stopperOption.setEnabled(false);
            stopperOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopperSelected = stopperOption.isSelected();
                }
            });
            devOptionsItem.add(stopperOption);
            
            devSpeedOption.setEnabled(false);
            devOptionsItem.add(devSpeedOption);
            
            devOptionsItem.add(lowGraphicsOption);

            menuBar.add(optionsItem);
            menuBar.add(devOptionsItem);
            setJMenuBar(menuBar);
            
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

            loadConfigFile();
            setVisible(true);
            changeState(ClientState.MAIN_MENU);
            checkForUpdates();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void pgBackPressed() {
        switch (state) {
        case SINGLE_PLAYER_POST_GAME:
            changeState(ClientState.SINGLE_PLAYER_MENU);
            break;
        case MULTIPLAYER_POST_GAME:
            changeState(ClientState.MULTIPLAYER_MENU);
            break;
        default:
            break;
        }
    }
    
    public void setMpNumRobots(int mpNumRobots) {
        this.mpNumRobots = mpNumRobots;
    }
    
    public void setMpDoubleDeck(boolean mpDoubleDeck) {
        this.mpDoubleDeck = mpDoubleDeck;
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
    
    public void rename() {
        String text = nameField.getText().trim();
        if (text.isEmpty()) {
            notify("Name cannot be empty.");
        } else {
            name = text;
            sendCommandToServer("RENAME:STRING " + name.length() + ":" + name);
        }
    }
    
    public void readyPressed() {
        if (myPlayer.isHost()) {
            if (!players.isEmpty() || mpNumRobots > 0) {
                sendCommandToServer("START:" + mpNumRobots + ":" + mpDoubleDeck);
            } else {
                notify("There are no players.");
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
    
    public void connect() {
        if (!connected) {
            try {
                hostName = hostField.getText();
                port = Integer.parseInt(portField.getText());
            } catch(Exception e1) {
                notify("Invalid port.");
            }
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostName, port), 10000);
                
                writeThread = new ClientWriteThread(socket, this);
                writeThread.start();
                
                readThread = new ClientReadThread(socket, this);
                readThread.start();
                
                connected = true;
            } catch (UnknownHostException e) {
                notify("That host does not exist.");
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                notify("Connection timed out.");
                e.printStackTrace();
            } catch (IOException e) {
                notify("Unable to connect to specified port.");
                e.printStackTrace();
            }
        }
    }
    
    public void disconnect() {
        readThread.disconnect();
        sendCommandToServer("CLOSE");
        connected = false;
        writeThread.disconnect();
        playersListModel.clear();
    }
    
    public void reconnect(LinkedList<String> parsedContent) {
        ReconnectFrame reconnectFrame = new ReconnectFrame(parsedContent, this);
        reconnectFrame.setLocationRelativeTo(this);
        reconnectFrame.execute();
    }
    
    public void getKicked() {
        notify("You were disconnected from the server.");
        disconnect();
        changeState(ClientState.MULTIPLAYER_MENU);
    }
    
    public void finalScores(List<String> s) {
        List<Integer> sToInts = new LinkedList<>();
        for (String score : s) {
            sToInts.add(Integer.parseInt(score));
        }
        canvas.setFinalScoresOnTimer(sToInts);
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
                    } else if (variable.equals("recentaddress")) {
                        hostName = value;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                hostField.setText(value);
                            }
                        });
                    } else if (variable.equals("recentport")) {
                        port = Integer.parseInt(value);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                portField.setText(value);
                            }
                        });
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
            String latestVersion = versionReader.readLine();
            versionReader.close();
            if (!latestVersion.equals(version)) {
                updateFound = true;
                newVersion = latestVersion;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateButton.setAlert(true);
                        updateButton.setText("Update available");
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateButton.setText("Version up to date");
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void updatePressed() {
        if (!updateFound) {
            checkForUpdates();
        } else {
            try {
                dispose();
                Runtime.getRuntime().exec("java -jar updater.jar " + newVersion);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        GameClient client = new GameClient();
        client.execute();
    }
}