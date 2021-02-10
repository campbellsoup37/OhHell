package server;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import core.Card;
import core.OhHellCore;
import core.Player;
import core.Recorder;
import graphics.OhcButton;
import graphics.OhcGraphicsTools;
import graphics.OhcScrollPane;
import graphics.OhcTextField;

public class GameServer extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private final int robotDelay = 0;
    
    private JLabel portLabel = new JLabel("Port:");
    private JTextField portField = new OhcTextField("Port");
    private JButton goButton = new OhcButton("Go");
    private JCheckBox recordCheckBox = new JCheckBox("Record");
    private JTextArea logTextArea = new JTextArea();
    private JScrollPane logScrollPane = new OhcScrollPane(logTextArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private DefaultListModel<String> playersListModel = new DefaultListModel<>();
    private JList<String> playersJList = new JList<>(playersListModel);
    private JScrollPane playersScrollPane = new OhcScrollPane(playersJList,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JButton dcButton = new OhcButton("DC Player");
    private JButton kickButton = new OhcButton("Kick Player");
    
    private JFrame popUpFrame = new JFrame();
    
    private OhHellCore core;
    
    private int port;
    private ServerSocket serverSocket;
    private ConnectionFinder finder;
    
    private List<Player> players = new ArrayList<>();
    private List<Player> kibitzers = new ArrayList<>();
    
    private Random random = new Random();
    
    private Recorder recorder;
    
    public GameServer() {
        
    }
    
    public GameServer(int port) {
        this.port = port;
    }
    
    public void execute() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        setTitle("Oh Hell Server");
        
        setIconImage(OhcGraphicsTools.loadImage("resources/icon/cw.png", this));
        
        setSize(640, 480);
        setResizable(false);
        setLayout(new BorderLayout());
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout(3));
        northPanel.add(portLabel);
        portField.setPreferredSize(new Dimension(200, 40));
        portField.setText("6066");
        northPanel.add(portField);
        goButton.setPreferredSize(new Dimension(200, 40));
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPressed();
            }
        });
        northPanel.add(goButton);
        northPanel.add(recordCheckBox);
        add(northPanel, BorderLayout.NORTH);
        
        logTextArea.setEditable(false);
        add(logScrollPane);
        
        JPanel eastPanel = new JPanel();
        eastPanel.setPreferredSize(new Dimension(210, 400));
        eastPanel.setLayout(new FlowLayout(2));
        playersScrollPane.setPreferredSize(new Dimension(200, 320));
        eastPanel.add(playersScrollPane);
        dcButton.setPreferredSize(new Dimension(97, 40));
        dcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = playersJList.getSelectedIndex();
                if (index > -1) {
                    disconnectPlayer((HumanPlayer) players.get(index));
                }
            }
        });
        eastPanel.add(dcButton);
        kickButton.setPreferredSize(new Dimension(97, 40));
        kickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = playersJList.getSelectedIndex();
                if (index > -1) {
                    kickPlayer((HumanPlayer) players.get(index));
                }
            }
        });
        eastPanel.add(kickButton);
        add(eastPanel, BorderLayout.EAST);
        
        if (recording()) {
            recorder = new Recorder();
        }
        
        core = new OhHellCore(recording());
        core.setPlayers(players);
        core.setKibitzers(kibitzers);
    }
    
    public boolean recording() {
        return recordCheckBox.isSelected();
    }
    
    public void goPressed() {
        try {
            port = Integer.parseInt(portField.getText());
            if (serverSocket != null) {
                serverSocket.close();
            }
            serverSocket = new ServerSocket(port);
            finder = new ConnectionFinder(serverSocket, this);
            finder.start();
            logMessage("Server started on port " + port + ". Waiting for players.");
        } catch(NumberFormatException e1) {
            JOptionPane.showMessageDialog(popUpFrame, "Invalid port");
        } catch(IOException e1) {
            JOptionPane.showMessageDialog(popUpFrame, "Port unavailable");
        }
    }
    
    public boolean gameStarted() {
        return core.getGameStarted();
    }
    
    public void startGame(int robotCount, boolean doubleDeck) {
        core.startGame(robotCount, doubleDeck, null, robotDelay);
    }
    
    public void updatePlayersList() {
        playersListModel.clear();
        for (Player player : players) {
            if (player.isHuman()) {
                playersListModel.addElement(
                        player.getName()
                        + (player.isHost() ? " (host)" : "")
                        + (player.isDisconnected() && !player.isKicked() ? " (DCed)" : "")
                        + (player.isKicked() ? " (kicked)" : "")
                        + (player.isKibitzer() ? " (kibitzer)" : ""));
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playersJList.updateUI();
            }
        });
        
        //core.updatePlayersList();
    }

    public void connectPlayer(Socket socket) {
        String name = "Player " + random.nextInt(10000);
        logMessage("Player connected: " + name + " at " + socket.getInetAddress());
        
        // TODO Make a better fix for the issue of someone disconnecting
        // and then having a different address.
        List<Player> dcPlayersAtAddress = players.stream()
                .filter(p -> /*p.getThread().getSocket().getInetAddress().toString()
                        .equals(socket.getInetAddress().toString())
                        && */p.isDisconnected() && !p.isKicked())
                .collect(Collectors.toList());
        
        PlayerThread thread = new PlayerThread(socket, this, dcPlayersAtAddress);
        HumanPlayer player = new HumanPlayer(name, thread);
        thread.setPlayer(player);
        thread.start();
    }
    
    public void joinPlayer(HumanPlayer player, String id) {
        player.setName(id);
        player.setId(id);
        player.getThread().setName("Player thread: " + id);
        player.setJoined(true);
        
        boolean reconnect = false;
        for (Player p : players) {
            if (p.getId().equals(player.getId())) {
                players.remove(player);
                ((HumanPlayer) p).getThread().endThread();
                ((HumanPlayer) p).setThread(player.getThread());
                ((HumanPlayer) p).getThread().setPlayer(((HumanPlayer) p));
                ((HumanPlayer) p).setDisconnected(false);
                ((HumanPlayer) p).resetKickVotes();
                player = (HumanPlayer) p;
                
                for (Player p1 : players) {
                    if (p1 != player) {
                        p1.commandUpdatePlayers(Arrays.asList(player));
                    }
                }
                for (Player p1 : kibitzers) {
                    if (p1 != player) {
                        p1.commandUpdatePlayers(Arrays.asList(player));
                    }
                }
                
                reconnect = true;
                break;
            }
        }
        
        if (gameStarted()) {
            if (!reconnect) {
                player.setKibitzer(true);
                kibitzers.add(player);
                for (Player p : players) {
                    if (p != player) {
                        p.commandAddPlayers(null, Arrays.asList(player));
                    }
                }
                for (Player p : kibitzers) {
                    if (p != player) {
                        p.commandAddPlayers(null, Arrays.asList(player));
                    }
                }
            }
            player.commandAddPlayers(players, kibitzers);
            player.commandStart();
            core.sendFullGameState(player);
        } else {
            if (!reconnect) {
                players.add(player);
                if (players.size() == 1) {
                    player.setHost(true);
                }
                for (Player p : players) {
                    if (p != player) {
                        p.commandAddPlayers(Arrays.asList(player), null);
                    }
                }
                for (Player p : kibitzers) {
                    if (p != player) {
                        p.commandAddPlayers(Arrays.asList(player), null);
                    }
                }
            }
            player.commandAddPlayers(players, kibitzers);
        }
    }
    
    public void renamePlayer(Player player, String name) {
        player.setName(name);
        for (Player p : players) {
            p.commandUpdatePlayers(Arrays.asList(player));
        }
    }
    
    public void addKickVote(int index, HumanPlayer fromPlayer) {
        HumanPlayer player = (HumanPlayer) players.get(index);
        if (player.isDisconnected()) {
            player.addKickVote(fromPlayer);
            if (player.getNumberOfKickVotes() * 2
                    >= players.stream()
                    .filter(p -> !p.isDisconnected() && !p.isKicked() && p.isHuman())
                    .count()) { 
                kickPlayer(player);
            }
        }
    }
    
    public void disconnectPlayer(HumanPlayer player) {
        player.commandKick();
        player.getThread().endThread();
        removePlayer(player, false);
    }
    
    public void kickPlayer(HumanPlayer player) {
        player.commandKick();
        player.getThread().endThread();
        removePlayer(player, true);
    }
    
    public void setKibitzer(Player player, boolean kibitzer) {
        boolean wasKibitzer = player.isKibitzer();
        player.setKibitzer(kibitzer);
        if (!wasKibitzer && kibitzer) {
            for (Player p : players) {
                p.commandRemovePlayer(player);
            }
            for (Player p : kibitzers) {
                p.commandRemovePlayer(player);
            }
            for (Player p : players) {
                p.commandAddPlayers(null, Arrays.asList(player));
            }
            for (Player p : kibitzers) {
                p.commandAddPlayers(null, Arrays.asList(player));
            }
            players.remove(player);
            kibitzers.add(player);
        } else if (wasKibitzer && !kibitzer) {
            for (Player p : players) {
                p.commandAddPlayers(Arrays.asList(player), null);
            }
            for (Player p : kibitzers) {
                p.commandAddPlayers(Arrays.asList(player), null);
            }
            players.add(player);
            kibitzers.remove(player);
        }
    }
    
    public void pokePlayer() {
        core.pokePlayer();
    }
    
    public void logMessage(String s) {
        logTextArea.append(s + "\n");
    }
    
    public void removePlayer(HumanPlayer player, boolean kick) {
        logMessage("Player disconnected: " 
                + player.getName() + " at " + player.getThread().getSocket().getInetAddress());
        player.setKicked(kick);
        
        // Change host if necessary
        if (player.isHost() && players.stream().filter(Player::isHuman).count() > 1) {
            Player newHost = player;
            while (newHost == player && newHost.isHuman()) {
                newHost = players.get(random.nextInt(players.size()));
            }
            player.setHost(false);
            newHost.setHost(true);
            for (Player p : players) {
                p.commandUpdatePlayers(Arrays.asList(newHost));
            }
            for (Player p : kibitzers) {
                p.commandUpdatePlayers(Arrays.asList(newHost));
            }
        }
        
        // Remove if game hasn't started or player is kibitzer
        // Update otherwise
        if (!gameStarted() || !player.isJoined() || player.isKibitzer()) {
            (player.isKibitzer() ? kibitzers : players).remove(player);
            for (Player p : players) {
                if (p != player) {
                    p.commandRemovePlayer(player);
                }
            }
            for (Player p : kibitzers) {
                if (p != player) {
                    p.commandRemovePlayer(player);
                }
            }
        } else {
            for (Player p : players) {
                if (p != player) {
                    p.commandUpdatePlayers(Arrays.asList(player));
                }
            }
            for (Player p : kibitzers) {
                if (p != player) {
                    p.commandUpdatePlayers(Arrays.asList(player));
                }
            }
        }
        updatePlayersList();
        
        // Restart round if kicked
        if (kick && gameStarted() && !player.isKibitzer()) {
            if (recording()) {
                recorder.recordKick(player.getIndex());
            }
            core.updateRounds();
            core.restartRound();
        }
    }
    
    public void makeBid(Player player, int bid) {
        core.incomingBid(player, bid);
    }
    
    public void makePlay(Player player, Card card) {
        core.incomingPlay(player, card);
    }
    
    public void processUndoBid(Player player) {
        core.processUndoBid(player);
    }
    
    public void processClaim(Player player) {
        core.processClaim(player);
    }
    
    public void processClaimResponse(Player player, boolean accept) {
        core.processClaimResponse(player, accept);
    }
    
    public void sendChat(String text) {
        core.sendChat(text);
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer(-1);
        server.execute();
    }
}