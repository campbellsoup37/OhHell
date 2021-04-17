package server;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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

import core.GameCoordinator;
import core.OhHellCore;
import core.Player;
import common.OhcButton;
import common.FileTools;
import common.OhcScrollPane;
import common.OhcTextField;

public class GameServer extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private String version;
    private boolean updateChecked = false;
    private String newVersion;
    
    private JLabel portLabel = new JLabel("Port:");
    private JTextField portField = new OhcTextField("Port");
    private JButton goButton = new OhcButton("Go");
    private OhcButton updateButton = new OhcButton("Check for update");
    private JTextArea logTextArea = new JTextArea();
    private JScrollPane logScrollPane = new OhcScrollPane(logTextArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private DefaultListModel<String> playersListModel = new DefaultListModel<>();
    private List<Player> playersInList = new ArrayList<>();
    private JList<String> playersJList = new JList<>(playersListModel);
    private JScrollPane playersScrollPane = new OhcScrollPane(playersJList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JButton dcButton = new OhcButton("DC Player");
    private JButton kickButton = new OhcButton("Kick Player");
    
    private JFrame popUpFrame = new JFrame();
    
    private class ServerGameCoordinator extends GameCoordinator {
        @Override
        public void notifyJoin(Player player, boolean rejoin) {
            logMessage("Player " + (rejoin ? "re" : "") + "connected: "
                    + player.getId() + " at "
                    + ((HumanPlayer) player).getThread().getSocket().getInetAddress());
        }
        
        @Override
        public void notifyRemoval(Player player, boolean kick) {
            logMessage("Player " + (kick ? "kicked" : "disconnected") + ": " 
                    + player.getName() + " at "
                    + ((HumanPlayer) player).getThread().getSocket().getInetAddress());
        }
        
        @Override
        public void updatePlayersList() {
            GameServer.this.updatePlayersList();
        }
        
        @Override
        public void reconnectPlayer(Player player1, Player player2) {
            HumanPlayer hPlayer1 = (HumanPlayer) player1;
            HumanPlayer hPlayer2 = (HumanPlayer) player2;
            
            hPlayer1.setThread(hPlayer2.getThread());
            hPlayer1.getThread().setPlayer(hPlayer1);
        }
    }
    private ServerGameCoordinator coordinator = new ServerGameCoordinator();
    
    private int port = -1;
    private ServerSocket serverSocket;
    private ConnectionFinder finder;
    
    public void execute(boolean deleteUpdater) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        
        setIconImage(FileTools.loadImage("resources/icon/cw.png", this));
        
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
        goButton.setPreferredSize(new Dimension(153, 40));
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPressed();
            }
        });
        northPanel.add(goButton);
        updateButton.setPreferredSize(new Dimension(200, 40));
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePressed();
            }
        });
        northPanel.add(updateButton);
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
                coordinator.forceRemovePlayer(
                        playersInList.get(playersJList.getSelectedIndex()), 
                        false);
            }
        });
        eastPanel.add(dcButton);
        kickButton.setPreferredSize(new Dimension(97, 40));
        kickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                coordinator.forceRemovePlayer(
                        playersInList.get(playersJList.getSelectedIndex()), 
                        true);
            }
        });
        eastPanel.add(kickButton);
        add(eastPanel, BorderLayout.EAST);

        BufferedReader versionReader = FileTools.getInternalFile("version", this);
        try {
            version = versionReader.readLine();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        setTitle("Oh Hell Server (v" + version + ")");
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                revalidate();
            }
        });
        
        if (deleteUpdater) {
            try {
                FileTools.deleteFile(getDirectory() + "/serverupdater.jar");
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        }
        checkForUpdates();
    }
    
    public void goPressed() {
        try {
            coordinator.startNewCore(new OhHellCore(true) {
                @Override
                public void stopGame() {
                    super.stopGame();
                    updatePlayersList();
                }
            });
            
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
    
    public void updatePlayersList() {
        playersListModel.clear();
        playersInList.clear();
        for (Player player : coordinator.getPlayers()) {
            if (player.isHuman()) {
                playersListModel.addElement(
                        player.getId()
                        + (player.isHost() ? " (host)" : "")
                        + (player.isDisconnected() && !player.isKicked() ? " (DCed)" : "")
                        + (player.isKicked() ? " (kicked)" : "")
                        + (player.isKibitzer() ? " (kibitzer)" : ""));
                playersInList.add(player);
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playersJList.updateUI();
            }
        });
    }

    public void connectPlayer(Socket socket) {
        PlayerThread thread = new PlayerThread(socket, this, coordinator);
        HumanPlayer player = new HumanPlayer(thread);
        thread.setPlayer(player);
        thread.start();
    }
    
    public void requestId(HumanPlayer player) {
        player.commandIdRequest(version);
    }
    
    public void logMessage(String s) {
        logTextArea.append(s + "\n");
    }
    
    public void checkForUpdates() {
        newVersion = FileTools.getCurrentVersion();
        updateChecked = true;
        if (newVersion.equals(version)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateButton.setText("Version up to date");
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateButton.setAlert(true);
                    updateButton.setText("Download v" + newVersion);
                }
            });
        }
    }
    
    public void updatePressed() {
        System.out.println("UPDATE BUTTON PRESSED");
        if (!updateChecked) {
            checkForUpdates();
        } else {
            try {
                String path = getDirectory() + "/serverupdater.jar";
                FileTools.downloadFile(
                        "https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/updater.jar", 
                        getDirectory() + "/serverupdater.jar");
                
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
                            "OhHellServer.jar",
                            getFileName()
                    }, false);
                    dispose();
                    System.exit(0);
                } else {
                    System.out.println("Error: Failed to download updater.");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getDirectory() throws URISyntaxException {
        return new File(GameServer.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).getParent();
    }
    
    public String getFileName() throws URISyntaxException {
        return new File(GameServer.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).getPath();
    }
    
    public static void main(String[] args) {
        boolean deleteUpdater = false;
        
        for (String arg : args) {
            if (arg.equals("-deleteupdater")) {
                deleteUpdater = true;
            }
        }
        
        GameServer server = new GameServer();
        server.execute(deleteUpdater);
    }
}