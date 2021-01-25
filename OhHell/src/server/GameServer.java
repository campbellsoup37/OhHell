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
        kickButton.setPreferredSize(new Dimension(200, 40));
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
        
        core.updatePlayersList();
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
    
    public void joinPlayer(HumanPlayer player) {
        player.setJoined(true);
        if (gameStarted()) {
            player.setKibitzer(true);
            kibitzers.add(player);
            updatePlayersList();
            player.commandStart();
            core.sendFullGameState(player);
        } else {
            players.add(player);
            if (players.size() == 1) {
                player.setHost(true);
            }
            updatePlayersList();
        }
    }
    
    public void reconnectPlayer(HumanPlayer player, HumanPlayer oldPlayer) {
        players.remove(player);
        oldPlayer.getThread().endThread();
        oldPlayer.setThread(player.getThread());
        oldPlayer.getThread().setPlayer(oldPlayer);
        oldPlayer.setDisconnected(false);
        oldPlayer.resetKickVotes();
        updatePlayersList();
        oldPlayer.commandStart();
        core.sendFullGameState(oldPlayer);
    }
    
    public void putPlayerInRightList(HumanPlayer player) {
        if (player.isKibitzer()) {
            players.remove(player);
            kibitzers.add(player);
        } else {
            kibitzers.remove(player);
            players.add(player);
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
    
    public void kickPlayer(HumanPlayer player) {
        player.commandKick();
        player.getThread().endThread();
        removePlayer(player);
    }
    
    public void pokePlayer() {
        core.pokePlayer();
    }
    
    public void logMessage(String s) {
        logTextArea.append(s + "\n");
    }
    
    public void removePlayer(HumanPlayer player) {
        logMessage("Player disconnected: " 
                + player.getName() + " at " + player.getThread().getSocket().getInetAddress());
        player.setKicked(true);
        while (player.isHost() && players.size() > 1) {
            player.setHost(false);
            players.get(random.nextInt(players.size())).setHost(true);
        }
        if (!gameStarted() || !player.isJoined()) {
            players.remove(player);
        }
        if (player.isKibitzer()) {
            kibitzers.remove(player);
        }
        updatePlayersList();
        
        if (gameStarted() && !player.isKibitzer()) {
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