import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class GameServer extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private JLabel portLabel = new JLabel("Port:");
    private JTextField portField = new JTextField("6066");
    private JButton goButton = new JButton("Go");
    private JTextArea logTextArea = new JTextArea();
    private JScrollPane logScrollPane = new JScrollPane(logTextArea);
    private DefaultListModel<String> playersListModel = new DefaultListModel<String>();
    private JList<String> playersJList = new JList<String>(playersListModel);
    private JScrollPane playersScrollPane = new JScrollPane(playersJList);
    private JButton kickButton = new JButton("Kick Player");
    
    private JFrame popUpFrame = new JFrame();
    
    private OhHellCore core = new OhHellCore();
    
    private int port;
    private ServerSocket serverSocket;
    private ConnectionFinder finder;
    
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    
    private Random random = new Random();
    
    private Recorder recorder;
    
    public GameServer(int port) {
        this.port = port;
    }
    
    public void execute() {
        portLabel.setBounds(10, 10, 200, 40);
        add(portLabel);
        
        portField.setBounds(50, 10, 200, 40);
        add(portField);
        
        goButton.setBounds(300, 10, 200, 40);
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPressed();
            }
        });
        add(goButton);
        
        logScrollPane.setBounds(10, 60, 400, 370);
        logTextArea.setEditable(false);
        add(logScrollPane);
        
        playersScrollPane.setBounds(420, 60, 200, 320);
        add(playersScrollPane);
        
        kickButton.setBounds(420, 390, 200, 40);
        kickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = playersJList.getSelectedIndex();
                if (index > -1) {
                    kickPlayer((HumanPlayer) players.get(index));
                }
            }
        });
        add(kickButton);
        
        recorder = new Recorder();
        
        setSize(640, 480);
        setResizable(false);
        setLayout(null);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        core.execute(true);
        core.setPlayers(players);
        core.setKibitzers(kibitzers);
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
        core.startGame(robotCount, doubleDeck, null, null);
    }
    
    public void updatePlayersList() {
        playersListModel.clear();
        for (Player player : players) {
            playersListModel.addElement(
                player.getName()
                + (player.isHost() ? " (host)" : "")
                + (player.isDisconnected() && !player.isKicked() ? " (DCed)" : "")
                + (player.isKicked() ? " (kicked)" : "")
                + (player.isKibitzer() ? " (kibitzer)" : ""));
        }
        
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
        HumanPlayer player = (HumanPlayer) players.stream()
                .filter(p -> p.isDisconnected() && !p.isKicked())
                .collect(Collectors.toList())
                .get(index);
        player.addKickVote(fromPlayer);
        if (player.getNumberOfKickVotes() * 2
                >= players.stream()
                .filter(p -> !p.isDisconnected() && !p.isKicked() && p.isHuman())
                .count()) { 
            kickPlayer(player);
        }
    }
    
    public void kickPlayer(HumanPlayer player) {
        player.commandKick();
        player.getThread().endThread();
        removePlayer(player);
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
            recorder.recordKick(player.getIndex());
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
    
    public void sendChat(String text) {
        core.sendChat(text);
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer(-1);
        server.execute();
    }
}