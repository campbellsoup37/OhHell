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
    
    private int port;
    private ServerSocket serverSocket;
    private ConnectionFinder finder;
    
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    
    private Random random = new Random();
    private Deck deck = new Deck();
    
    private boolean gameStarted = false;
    private String state = "";

    private Card trump;
    
    private List<RoundDetails> rounds;
    private int roundNumber;
    private int leader;
    private int turn;
    
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
                    kickPlayer(players.get(index));
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
    
    public void randomizePlayerOrder() {
        for (int i = players.size(); i > 0; i--) {
            int j = random.nextInt(i);
            Player player = players.remove(j);
            players.add(player);
        }
        updatePlayersList();
    }
    
    public int nextUnkicked(int index) {
        for (int i = 0; i < players.size(); i++) {
            int relativeI = (index + 1 + i) % players.size();
            if (!players.get(relativeI).isKicked()) {
                return relativeI;
            }
        }
        return index;
    }
    
    public void updatePlayersList() {
        if (players.stream().allMatch(p -> p.isKicked() || p.isDisconnected())) {
            stopGame();
        }
        
        playersListModel.clear();
        for (Player player : players) {
            playersListModel.addElement(
                player.getName()
                + (player.isHost() ? " (host)" : "")
                + (player.isDisconnected() && !player.isKicked() ? " (DCed)" : "")
                + (player.isKicked() ? " (kicked)" : "")
                + (player.isKibitzer() ? " (kibitzer)" : ""));
        }
        
        for (Player player : players) {
            player.getThread().sendCommand(playerInfoCommand(player));
        }
        for (Player player : kibitzers) {
            player.getThread().sendCommand(playerInfoCommand(player));
        }
    }
    
    public String playerInfoCommand(Player player) {
        return players.stream()
                .map(p -> 
                    "STRING " + p.getName().length() + ":"
                        + p.getName() + ":"
                        + p.isHost() + ":"
                        + p.isDisconnected() + ":"
                        + p.isKicked() + ":"
                        + p.isKibitzer() + ":"
                        + p.equals(player) + ":")
                .reduce("UPDATEPLAYERS:", (sofar, pString) -> sofar + pString)
            + kibitzers.stream()
                .map(p -> 
                    "STRING " + p.getName().length() + ":"
                        + p.getName() + ":"
                        + p.isHost() + ":"
                        + p.isDisconnected() + ":"
                        + p.isKicked() + ":"
                        + p.isKibitzer() + ":"
                        + p.equals(player) + ":")
                .reduce("", (sofar, pString) -> sofar + pString);
    }
    
    public void connectPlayer(Socket socket) {
        String name = "Player " + random.nextInt(10000);
        logMessage("Player connected: " + name + " at " + socket.getInetAddress());
        
        List<Player> dcPlayersAtAddress = players.stream()
                .filter(p -> p.getThread().getSocket().getInetAddress().toString()
                        .equals(socket.getInetAddress().toString())
                        && p.isDisconnected() && !p.isKicked())
                .collect(Collectors.toList());
        
        PlayerThread thread = new PlayerThread(socket, this, dcPlayersAtAddress);
        Player player = new Player(name, thread);
        thread.setPlayer(player);
        thread.start();
    }
    
    public void joinPlayer(Player player) {
        player.setJoined(true);
        if (gameStarted) {
            player.setKibitzer(true);
            kibitzers.add(player);
            updatePlayersList();
            player.getThread().sendCommand("START");
            sendFullGameState(player);
        } else {
            players.add(player);
            if (players.size() == 1) {
                player.setHost(true);
            }
            updatePlayersList();
        }
    }
    
    public void reconnectPlayer(Player player, Player oldPlayer) {
        players.remove(player);
        oldPlayer.getThread().endThread();
        oldPlayer.setThread(player.getThread());
        oldPlayer.getThread().setPlayer(oldPlayer);
        oldPlayer.setDisconnected(false);
        oldPlayer.resetKickVotes();
        updatePlayersList();
        oldPlayer.getThread().sendCommand("START");
        sendFullGameState(oldPlayer);
    }
    
    public void putPlayerInRightList(Player player) {
        if (player.isKibitzer()) {
            players.remove(player);
            kibitzers.add(player);
        } else {
            kibitzers.remove(player);
            players.add(player);
        }
    }
    
    public void addKickVote(int index, Player fromPlayer) {
        Player player = players.stream()
                .filter(p -> p.isDisconnected() && !p.isKicked())
                .collect(Collectors.toList())
                .get(index);
        player.addKickVote(fromPlayer);
        if (player.getNumberOfKickVotes() * 2
                >= players.stream().filter(p -> !p.isDisconnected() && !p.isKicked()).count()) { 
            kickPlayer(player);
        }
    }
    
    public void kickPlayer(Player player) {
        player.getThread().sendCommand("KICK");
        player.getThread().endThread();
        removePlayer(player);
    }
    
    public void sendFullGameState(Player player) {
        //updatePlayersList();
        updateRounds();
        giveHands(player);
        sendDealerLeader(player);
        for (Player p : players) {
            player.getThread().sendCommand("STATEPLAYER:" +
                    p.getIndex() + ":" + 
                    p.getBid() + ":" + 
                    p.getTaken() + ":" + 
                    p.getLastTrick() + ":" + 
                    p.getTrick() + ":");
            player.getThread().sendCommand(p.getBids().stream()
                    .map(bid -> bid + ":")
                    .reduce("STATEPLAYERBIDS:" + p.getIndex() + ":", 
                            (sofar, bid) -> sofar + bid));
            player.getThread().sendCommand(p.getScores().stream()
                    .map(score -> score + ":")
                    .reduce("STATEPLAYERSCORES:" + p.getIndex() + ":", 
                            (sofar, score) -> sofar + score));
        }
        communicateTurn();
    }
    
    public void sendDealerLeader(Player player) {
        player.getThread().sendCommand("STATEDEALERLEADER:" + 
                rounds.get(roundNumber).getDealer().getIndex() + ":" + leader);
    }
    
    public boolean getGameStarted() {
        return gameStarted;
    }
    
    public void startGame() {
        gameStarted = true;
        randomizePlayerOrder();

        recorder.start();
        recorder.recordPlayers(
                players.stream()
                .map(p -> p.getThread().getSocket().getInetAddress().toString())
                .collect(Collectors.toList()));
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setIndex(i);
            player.reset();
            player.getThread().sendCommand("START");
        }
        for (Player player : kibitzers) {
            player.getThread().sendCommand("START");
        }
        
        rounds = new ArrayList<RoundDetails>();
        roundNumber = 0;

        //rounds.add(new RoundDetails(1));
        //rounds.add(new RoundDetails(2));
        
        int maxHand = Math.min(10, 52 / players.size());
        for (int i = maxHand; i >= 2; i--) {
            rounds.add(new RoundDetails(i));
        }
        for (int i = 0; i < players.size(); i++) {
            rounds.add(new RoundDetails(1));
        }
        for (int i = 2; i <= maxHand; i++) {
            rounds.add(new RoundDetails(i));
        }
        updateRounds();
        
        deal();
    }
    
    public void updateRounds() {
        rounds.removeIf(r -> r.getHandSize() == 1 && r.getDealer().isKicked() && !r.isRoundOver());
        
        List<RoundDetails> remainingRounds = rounds.stream()
                .filter(r -> !r.isRoundOver())
                .collect(Collectors.toList());
        int dIndex = remainingRounds.get(0).getDealer().getIndex() - 1;
        
        for (RoundDetails r : remainingRounds) {
            dIndex = nextUnkicked(dIndex);
            r.setDealer(players.get(dIndex));
        }
        
        String command = rounds.stream()
                .map(r -> r.getDealer().getIndex() + ":" + r.getHandSize() + ":")
                .reduce("UPDATEROUNDS:" + roundNumber + ":", 
                        (sofar, cString) -> sofar + cString);
        
        for (Player player : players) {
            player.getThread().sendCommand(command);
        }
        for (Player player : kibitzers) {
            player.getThread().sendCommand(command);
        }
    }
    
    public void stopGame() {
        gameStarted = false;
        if (!gameStarted) {
            players.removeIf(p -> p.isKicked() || p.isDisconnected());
        }
        recorder.stop();
    }
    
    public void deal() {
        int handSize = rounds.get(roundNumber).getHandSize();
        List<List<Card>> hands = deck.deal(players.size(), handSize);
        
        for (int i = 0; i < hands.size() - 1; i++) {
            players.get(i).setHand(hands.get(i));
        }
        trump = hands.get(players.size()).get(0);
        
        recorder.recordTrump(trump);
        
        int dealer = rounds.get(roundNumber).getDealer().getIndex();
        
        turn = nextUnkicked(dealer);
        leader = turn;
        
        recorder.recordDealer(dealer);
        
        for (Player player : players) {
            player.resetBid();
            if (player.getBids().size() >= roundNumber + 1) {
                player.getBids().remove(roundNumber);
            }
            player.clearTrick();
            player.setTaken(0);
            giveHands(player);
            sendDealerLeader(player);
        }
        
        for (Player player : kibitzers) {
            giveHands(player);
            sendDealerLeader(player);
        }
        
        state = "BIDDING";
        communicateTurn();
    }
    
    public void giveHands(Player player) {
        for (Player p : players) {
            if (p.isKicked()) {
                player.getThread().sendCommand("DEAL:"+p.getIndex());
            } else {
                player.getThread().sendCommand(
                        p.getHand().stream()
                        .map(card -> 
                            (player.equals(p) || player.isKibitzer() ? card.toString() : "0") + ":")
                        .reduce("DEAL:" + p.getIndex() + ":", 
                                (sofar, cString) -> sofar + cString));
            }
        }
        player.getThread().sendCommand("TRUMP:" + trump.toString());
    }
    
    public void incomingBid(Player player, int bid) {
        player.addBid(bid);
        
        for (Player p : players) {
            p.getThread().sendCommand("BIDREPORT:" + player.getIndex() + ":" + bid);
        }
        for (Player p : kibitzers) {
            p.getThread().sendCommand("BIDREPORT:" + player.getIndex() + ":" + bid);
        }
        
        turn = nextUnkicked(turn);
        
        if (players.stream().filter(p -> !p.isKicked()).noneMatch(p -> !p.hasBid())) {
            recorder.recordBids(
                    players.stream()
                    .filter(p -> !p.isKicked())
                    .map(p -> (Integer) p.getBid())
                    .collect(Collectors.toList()));
            state = "PLAYING";
        }
        communicateTurn();
    }
    
    public void communicateTurn() {
        if (state.equals("BIDDING")) {
            for (Player player : players) {
                player.getThread().sendCommand("BID:" + turn);
            }
            for (Player player : kibitzers) {
                player.getThread().sendCommand("BID:" + turn);
            }
        }
        if (state.equals("PLAYING")) {
            for (Player player : players) {
                player.getThread().sendCommand("PLAY:" + turn);
            }
            for (Player player : kibitzers) {
                player.getThread().sendCommand("PLAY:" + turn);
            }
        }
    }
    
    public void incomingPlay(Player player, Card card) {
        player.setTrick(card);
        player.removeCard(card);
        for (Player p : players) {
            p.getThread().sendCommand("PLAYREPORT:" + player.getIndex() + ":" + card.toString());
        }
        for (Player p : kibitzers) {
            p.getThread().sendCommand("PLAYREPORT:" + player.getIndex() + ":" + card.toString());
        }
        
        turn = nextUnkicked(turn);
        
        if (players.stream().filter(p -> !p.isKicked()).anyMatch(p -> p.getTrick().isEmpty())) {
            communicateTurn();
        } else {
            turn = trickWinner();
            
            recorder.recordTrick(
                    players.stream()
                        .map(Player::getTrick)
                        .collect(Collectors.toList()),
                    turn);
            
            players.get(turn).incTaken();
            for (Player p : players) {
                p.getThread().sendCommand("TRICKWINNER:" + turn);
                p.resetTrick();
            }
            for (Player p : kibitzers) {
                p.getThread().sendCommand("TRICKWINNER:" + turn);
            }
            
            if (players.stream()
                    .map(Player::getTaken)
                    .reduce(0, (sofar, t) -> sofar + t)
                    < rounds.get(roundNumber).getHandSize()) {
                communicateTurn();
                leader = turn;
            } else {
                String command = "REPORTSCORES:";
                for (Player p : players) {
                    if (p.isKicked()) {
                        command += "-:";
                    } else {
                        int b = p.getBid();
                        int d = Math.abs(p.getTaken() - b);
                        if (d == 0) {
                            p.addScore(10 + b * b);
                        } else {
                            p.addScore(-5 * d * (d + 1) / 2);
                        }
                        command += p.getScore() + ":";
                    }
                }
                for (Player p : players) {
                    p.getThread().sendCommand(command);
                }
                for (Player p : kibitzers) {
                    p.getThread().sendCommand(command);
                }
                
                recorder.recordResults(
                        players.stream()
                        .map(p -> (Integer) (p.getTaken() - p.getBid()))
                        .collect(Collectors.toList()));
                
                rounds.get(roundNumber).setRoundOver();
                roundNumber++;
                if (roundNumber < rounds.size()) {
                    deal();
                } else {
                    List<Player> playersSorted = new ArrayList<Player>();
                    for (Player p : players) {
                        int i = 0;
                        while (i < playersSorted.size() 
                                && p.getScore() < playersSorted.get(i).getScore()) {
                            i++;
                        }
                        playersSorted.add(i, p);
                    }
                    String command2 = "FINALSCORES:";
                    for (Player p : playersSorted) {
                        command2 += "STRING " 
                                + p.getName().length() + ":" + p.getName() + ":" + p.getScore()+":";
                    }
                    for (Player p : players) {
                        p.getThread().sendCommand(command2);
                    }
                    for (Player p : kibitzers) {
                        p.getThread().sendCommand(command2);
                    }
                    stopGame();
                }
            }
        }
    }
    
    public void restartRound() {
        for (Player player : players) {
            player.getThread().sendCommand("REDEAL");
        }
        for (Player player : kibitzers) {
            player.getThread().sendCommand("REDEAL");
        }
        deal();
    }
    
    public int trickWinner() {
        int out = turn;
        for (Player player : players) { 
            if (player.getTrick().isGreaterThan(players.get(out).getTrick(), trump.getSuit())) {
                out = player.getIndex();
            }
        }
        return out;
    }
    
    public void logMessage(String s) {
        logTextArea.append(s + "\n");
    }
    
    public void removePlayer(Player player) {
        logMessage("Player disconnected: " 
                + player.getName() + " at " + player.getThread().getSocket().getInetAddress());
        player.setKicked(true);
        while (player.isHost() && players.size() > 1) {
            player.setHost(false);
            players.get(random.nextInt(players.size())).setHost(true);
        }
        if (!gameStarted || !player.isJoined()) {
            players.remove(player);
        }
        if (player.isKibitzer()) {
            kibitzers.remove(player);
        }
        updatePlayersList();
        
        if (gameStarted && !player.isKibitzer()) {
            recorder.recordKick(player.getIndex());
            updateRounds();
            restartRound();
        }
    }
    
    public void sendChat(String text) {
        for (Player player : players) {
            player.getThread().sendCommand("CHAT:STRING " + text.length() + ":" + text);
        }
        for (Player player : kibitzers) {
            player.getThread().sendCommand("CHAT:STRING " + text.length() + ":" + text);
        }
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer(-1);
        server.execute();
    }
}