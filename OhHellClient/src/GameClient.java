import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

public class GameClient extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private boolean connected = false;
    private boolean gameStarted = false;
    private Socket socket;
    private String hostName = "localhost";
    private int port = 6066;
    private String name = "";
    
    private ClientReadThread readThread;
    private ClientWriteThread writeThread;

    private long currentCommandId = 0;
    
    private NotificationFrame notificationFrame;
    private DisconnectFrame dcFrame;
    
    private JMenuBar menuBar = new JMenuBar();
    private JMenu optionsItem = new JMenu("Options");
    private JCheckBox soundOption = new JCheckBox("Sound");
    
    private JLabel hostLabel = new JLabel("Host name:");
    private JTextField hostField = new JTextField(hostName);
    private JLabel portLabel = new JLabel("Port:");
    private JTextField portField = new JTextField(port+"");
    private JButton connectButton = new JButton("Connect");
    private JLabel nameLabel = new JLabel("Name:");
    private JTextField nameField = new JTextField();
    private JButton nameButton = new JButton("Change name");
    private JCheckBox kibitzerCheckBox = new JCheckBox("Join as kibitzer");
    private JButton readyButton = new JButton("Start!");
    private DefaultListModel<String> playersListModel = new DefaultListModel<String>();
    private JList<String> playersJList = new JList<String>(playersListModel);
    private JScrollPane playersScrollPane = new JScrollPane(playersJList);
    private GameCanvas canvas;
    private JButton backButton = new JButton("Back");
    private JTextArea chatArea = new JTextArea();
    private JScrollPane chatScrollPane = new JScrollPane(chatArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private JTextField chatField = new JTextField();
    private JButton showButton = new JButton("Show");
    
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    private Player myPlayer;
    private List<int[]> rounds = new ArrayList<int[]>();
    private int roundNumber = 0;
    
    private Random random = new Random();
    
    public GameClient() {}
    
    public void receiveCommand(String line) {
        String[] splitLine = line.split(":", 3);
        long id = Long.parseLong(splitLine[0]);
        String command = splitLine[1];
        String content = "";
        if (splitLine.length >= 3) {
            content = splitLine[2];
        }
        
        sendCommand("CONFIRMED:" + id);
        if (currentCommandId == id) {
            return;
        }
        
        currentCommandId = id;
        LinkedList<String> parsedContent = parseCommandContent(content);
        
        if (command.equals("UPDATEPLAYERS")) {
            updatePlayersList(parsedContent);
        } else if (command.equals("UPDATEROUNDS")) {
            updateRounds(parsedContent);
        } else if (command.equals("KICK")) {
            getKicked();
        } else if (command.equals("START")) {
            startGame();
        } else if (command.equals("REDEAL")) {
            restartRound(parsedContent);
        } else if (command.equals("DEAL")) {
            setHand(parsedContent);
        } else if (command.equals("TRUMP")) {
            setTrump(new Card(parsedContent.get(0)));
        } else if (command.equals("BID")) {
            bid(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("BIDREPORT")) {
            bidReport(Integer.parseInt(parsedContent.get(0)), 
                    Integer.parseInt(parsedContent.get(1)));
        } else if (command.equals("PLAY")) {
            play(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("PLAYREPORT")) {
            playReport(Integer.parseInt(parsedContent.get(0)), 
                    new Card(parsedContent.get(1)));
        } else if (command.equals("TRICKWINNER")) {
            trickWinnerReport(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("REPORTSCORES")) {
            reportScores(parsedContent);
        } else if (command.equals("FINALSCORES")) {
            finalScores(parsedContent);
        } else if (command.equals("RECONNECT")) {
            ReconnectFrame reconnectFrame = new ReconnectFrame(parsedContent, this);
            reconnectFrame.setLocationRelativeTo(this);
            reconnectFrame.execute();
        } else if (command.equals("STATEDEALERLEADER")) {
            canvas.setDealer(Integer.parseInt(parsedContent.get(0)));
            canvas.setLeaderOnTimer(Integer.parseInt(parsedContent.get(1)));
        } else if (command.equals("STATEPLAYER")) {
            Player player = players.get(Integer.parseInt(parsedContent.get(0)));
            player.setBid(Integer.parseInt(parsedContent.get(1)));
            player.setTaken(Integer.parseInt(parsedContent.get(2)));
            player.setLastTrick(new Card(parsedContent.get(3)));
            player.setTrick(new Card(parsedContent.get(4)));
        } else if (command.equals("STATEPLAYERBIDS")) {
            Player player = players.get(Integer.parseInt(parsedContent.get(0)));
            player.setBids(parsedContent.subList(1, parsedContent.size()).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        } else if (command.equals("STATEPLAYERSCORES")) {
            Player player = players.get(Integer.parseInt(parsedContent.get(0)));
            player.setScores(parsedContent.subList(1, parsedContent.size()).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        } else if (command.equals("CHAT")) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    chatArea.setText(chatArea.getText() + parsedContent.get(0) + "\n");
                }
            });
        }
    }
    
    public LinkedList<String> parseCommandContent(String content) {
        if (content.isEmpty()) {
            return new LinkedList<String>();
        } else {
            String piece = "";
            String rest = "";
            if (content.startsWith("STRING ")) {
                int startIndex = content.indexOf(":") + 1;
                int length = Integer.parseInt(content.substring(7, startIndex - 1));
                piece = content.substring(startIndex, startIndex + length);
                if (content.length() > startIndex + length) {
                    rest = content.substring(startIndex + length + 1);
                }
            } else {
                String[] split = content.split(":", 2);
                piece = split[0];
                if (split.length >= 2) {
                    rest = split[1];
                }
            }
            LinkedList<String> out = parseCommandContent(rest);
            out.addFirst(piece);
            return out;
        }
    }
    
    public void updatePlayersList(LinkedList<String> parsedContent) {
        playersListModel.clear();
        if (!gameStarted) {
            players.clear();
        }
        kibitzers.clear();

        boolean anyDc = false;
        
        int params = 6;
        int numPlayers = parsedContent.size() / params;
        for (int i = 0; i < numPlayers; i++) {
            String iname = parsedContent.get(params * i);
            boolean ihost = parsedContent.get(params * i + 1).equals("true");
            boolean idced = parsedContent.get(params * i + 2).equals("true");
            boolean ikicked = parsedContent.get(params * i + 3).equals("true");
            if (idced && !ikicked) {
                anyDc = true;
            }
            boolean ikibitzer = parsedContent.get(params * i + 4).equals("true");
            boolean imy = parsedContent.get(params * i + 5).equals("true");
            Player player = new Player();
            playersListModel.addElement(
                    iname
                    + (ihost ? " (host)" : "")
                    + (ikibitzer ? " (kibitzer)" : ""));
            if (!ikibitzer) {
                if (!gameStarted) {
                    players.add(player);
                } else {
                    player = players.get(i);
                }
            } else {
                kibitzers.add(player);
            }
            player.setIndex(i);
            player.setName(iname);
            player.setHost(ihost);
            player.setDisconnected(idced);
            player.setKicked(ikicked);
            player.setKibitzer(ikibitzer);
            if (imy) {
                myPlayer = player;
                name = iname;
                if (nameField.getText().isEmpty()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            nameField.setText(iname);
                        }
                    });
                }
            }
        }
        
        if (anyDc && !myPlayer.isKibitzer()) {
            if (dcFrame == null) {
                dcFrame = new DisconnectFrame(this);
                dcFrame.setLocationRelativeTo(this);
                dcFrame.execute();
            }
            dcFrame.setDcList(
                    players.stream()
                        .filter(p -> p.isDisconnected() && !p.isKicked())
                        .map(Player::getName)
                        .collect(Collectors.toList()));
        } else {
            if (dcFrame != null) {
                dcFrame.close();
                dcFrame = null;
            }
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
    
    public void updateRounds(LinkedList<String> parsedContent) {
        rounds = new ArrayList<int[]>();
        roundNumber = Integer.parseInt(parsedContent.remove());
        
        int params = 2;
        int numRounds = parsedContent.size() / params;
        for (int i = 0; i < numRounds; i++) {
            int[] round = {Integer.parseInt(parsedContent.get(params * i)),
                    Integer.parseInt(parsedContent.get(params * i + 1))};
            rounds.add(round);
        }
    }
    
    public boolean isOneRound() {
        return rounds.get(roundNumber)[1] == 1;
    }
    
    public List<int[]> getRounds() {
        return rounds;
    }
    
    public void reconnectAs(int index) {
        sendCommand("RECONNECT:" + index);
    }
    
    public void voteKick(int index) {
        sendCommand("VOTEKICK:" + index);
    }
    
    public void startGame() {
        gameStarted = true;
        roundNumber = 0;
        
        hostLabel.setVisible(false);
        hostField.setVisible(false);
        portLabel.setVisible(false);
        portField.setVisible(false);
        connectButton.setVisible(false);
        playersScrollPane.setVisible(false);
        nameLabel.setVisible(false);
        nameField.setVisible(false);
        nameButton.setVisible(false);
        kibitzerCheckBox.setVisible(false);
        readyButton.setVisible(false);
        backButton.setVisible(false);
        chatScrollPane.setVisible(true);
        chatField.setVisible(true);

        setMinimumSize(new Dimension(1200, 800));
        setSize(1200, 800);
        setResizable(true);

        for (Player player : players) {
            player.reset();
        }
        if (myPlayer.isKibitzer()) {
            myPlayer.setIndex(random.nextInt(players.size()));
        }
        canvas.reset();
        
        canvas.setVisible(true);
        canvas.repaint();
    }
    
    public void backToLobby() {
        hostLabel.setVisible(true);
        hostField.setVisible(true);
        portLabel.setVisible(true);
        portField.setVisible(true);
        connectButton.setVisible(true);
        playersScrollPane.setVisible(true);
        nameLabel.setVisible(true);
        nameField.setVisible(true);
        nameButton.setVisible(true);
        kibitzerCheckBox.setVisible(true);
        readyButton.setVisible(true);

        canvas.setVisible(false);
        backButton.setVisible(false);
        chatScrollPane.setVisible(false);
        chatField.setVisible(false);

        setMinimumSize(new Dimension(685, 500));
        setSize(685, 500);
        setResizable(false);
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public Player getMyPlayer() {
        return myPlayer;
    }
    
    public void restartRound(LinkedList<String> content) {
        for (Player player : players) {
            player.setBid(0);
            player.setTaken(0);
            player.resetTrick();
            if (player.getBids().size() >= roundNumber + 1) {
                player.getBids().remove(roundNumber);
            }
        }
        canvas.repaint();

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
    
    public void setHand(LinkedList<String> parsedContent) {
        int index = Integer.parseInt(parsedContent.get(0));
        canvas.setHandOnTimer(index, 
                parsedContent.subList(1, parsedContent.size())
                .stream()
                .map(s -> new Card(s))
                .collect(Collectors.toList()));
    }
    
    public void setTrump(Card card) {
        canvas.setTrumpOnTimer(card);
        canvas.repaint();
    }
    
    public void bid(int index) {
        for (Player player : players) {
            player.setBidding(player.getIndex() == index ? 1 : 0);
        }
        for (Player player : players) {
            player.setPlaying(false);
        }
        canvas.repaint();
    }
    
    public void play(int index) {
        for (Player player : players) {
            player.setBidding(0);
        }
        for (Player player : players) {
            player.setPlaying(player.getIndex() == index);
        }
        canvas.repaint();
    }
    
    public void bidReport(int index, int bid) {
        players.get(index).addBid(bid);
        canvas.repaint();
    }
    
    public void playReport(int index, Card card) {
        players.get(index).setTrick(card);
        players.get(index).removeCard(card);
        canvas.animateCardPlay(index);
    }
    
    public void trickWinnerReport(int index) {
        canvas.animateTrickTake(index);
    }
    
    public void reportScores(LinkedList<String> scores) {
        for (int i = 0; i < players.size(); i++) {
            if (!scores.get(i).equals("-")) {
                players.get(i).addScore(Integer.parseInt(scores.get(i)));
            }
        }

        if (!myPlayer.isKibitzer()) {
            canvas.showResultMessageOnTimer();
        }
        roundNumber++;

        canvas.resetPlayersOnTimer();
        
        canvas.repaint();
    }
    
    public void execute() {
        try {
            setTitle("Oh Hell");
            //setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
            
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
            
            playersScrollPane.setBounds(350, 20, 300, 400);
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
                        sendCommand("KIBITZER:" + kibitzerCheckBox.isSelected());
                    }
                }
            });
            add(kibitzerCheckBox);
            
            readyButton.setBounds(120, 370, 150, 40);
            readyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (connected) {
                        readyPressed();
                    }
                }
            });
            add(readyButton);
            
            canvas = new GameCanvas(this);
            canvas.setVisible(false);
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
                public void mouseDragged(MouseEvent arg0) {}

                @Override
                public void mouseMoved(MouseEvent e) {
                    canvas.mouseMoved(e.getX(), e.getY());
                }
            });
            add(canvas);
            canvas.setLayout(null);
            
            backButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    backToLobby();
                }
            });
            canvas.add(backButton);
            backButton.setVisible(false);
            
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setEditable(false);
            canvas.add(chatScrollPane);
            chatScrollPane.setVisible(false);
            DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            
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
                            String command = "CHAT:STRING " 
                                    + text.toCharArray().length + ":" 
                                    + text;
                            sendCommand(command);
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
            chatField.setVisible(false);
            
            canvas.add(showButton);
            showButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showButton.setVisible(false);
                    canvas.doShowOneCard();
                    canvas.repaint();
                }
            });
            showButton.setVisible(false);
            
            menuBar.add(optionsItem);
            soundOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.setPlaySoundSelected(soundOption.isSelected());
                }
            });
            
            soundOption.setPreferredSize(new Dimension(80, 25));
            optionsItem.add(soundOption);
            setJMenuBar(menuBar);
            
            setSize(685, 500);
            setResizable(false);
            setVisible(true);
            
            addWindowListener(new WindowListener() {
                @Override
                public void windowActivated(WindowEvent arg0) {}
                
                @Override
                public void windowClosed(WindowEvent arg0) {}
                
                @Override
                public void windowDeactivated(WindowEvent arg0) {}
                
                @Override
                public void windowDeiconified(WindowEvent arg0) {}
                
                @Override
                public void windowIconified(WindowEvent arg0) {}
                
                @Override
                public void windowOpened(WindowEvent arg0) {}
                
                @Override
                public void windowClosing(WindowEvent e) {
                    if (connected) {
                        close();
                    }
                }
            });
            
            setDefaultCloseOperation(EXIT_ON_CLOSE);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void rename() {
        name = nameField.getText();
        sendCommand("RENAME:STRING " + name.length() + ":" + name);
    }
    
    public void readyPressed() {
        if (myPlayer.isHost()) {
            if (!players.isEmpty()) {
                sendCommand("START");
            } else {
                notify("There are no players.");
            }
        } else {
            notify("You are not the host.");
        }
    }
    
    public void close() {
        sendCommand("CLOSE");
    }
    
    public void sendCommand(String text) {
        writeThread.write(text);
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
    
    public void getKicked() {
        notify("You were disconnected from the server.");
        connected = false;
        writeThread.disconnect();
        playersListModel.clear();
        backToLobby();
    }
    
    public void finalScores(LinkedList<String> s) {
        gameStarted = false;
        canvas.setFinalScoresOnTimer(s);
    }
    
    public static void main(String[] args) {
        GameClient client = new GameClient();
        client.execute();
    }
}