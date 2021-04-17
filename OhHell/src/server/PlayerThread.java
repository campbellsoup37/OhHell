package server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Random;

import core.Card;
import core.GameCoordinator;
import core.GameOptions;
import core.Player;

public class PlayerThread extends Thread {
    private Socket socket;
    private GameServer server;
    private GameCoordinator coordinator;
    private PrintWriter writer;
    private BufferedReader reader;
    private HumanPlayer player;
    
    private boolean running = true;
    
    private Random random = new Random();
    
    public PlayerThread(Socket socket, GameServer server, GameCoordinator coordinator) {
        this.socket = socket;
        this.server = server;
        this.coordinator = coordinator;
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(), "UTF8"));
            
            writer = new PrintWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(), "UTF8"), 
                        true);
            
            server.requestId(player);
            
            while (running) {
                String line = reader.readLine();
                if (line == null) {
                    continue;
                }
                
                String[] splitLine = line.split(":", 2);
                String command = splitLine[0];
                String content = "";
                if (splitLine.length >= 2) {
                    content = splitLine[1];
                }
                
                LinkedList<String> parsedContent = parseCommandContent(content);
                
                if (command.equals("ID")) {
                    coordinator.joinPlayer(player, parsedContent.get(0));
                } else if (command.equals("CLOSE")) {
                    handleDisconnect();
                    break;
                } else if (command.equals("STOP")) {
                    coordinator.requestEndGame(player);
                } else if (command.equals("KIBITZER")) {
                    coordinator.setKibitzer(player, parsedContent.get(0).equals("true"));
                } else if (command.equals("RENAME")) {
                    coordinator.renamePlayer(player, parsedContent.get(0));
                } else if (command.equals("RETEAM")) {
                    coordinator.reteamPlayer(
                            Integer.parseInt(parsedContent.get(0)), 
                            Integer.parseInt(parsedContent.get(1)));
                } else if (command.equals("RENAMETEAM")) {
                    if (!player.isKibitzer()) {
                        coordinator.renameTeam(player.getTeam(), parsedContent.get(0));
                    }
                } else if (command.equals("TEAMSCRAMBLE")) {
                    coordinator.scrambleTeams();
                } else if (command.equals("OPTIONS")) {
                    if (player.isHost()) {
                        coordinator.updateOptions(new GameOptions(parsedContent.get(0)));
                    }
                } else if (command.equals("START")) {
                    if (player.isHost()) {
                        GameOptions options = new GameOptions(parsedContent.get(0));
                        coordinator.startGame(options);
                    }
                } else if (command.equals("BID")) {
                    coordinator.makeBid(player, Integer.parseInt(parsedContent.get(0)));
                } else if (command.equals("PLAY")) {
                    coordinator.makePlay(player, new Card(parsedContent.get(0)));
                } else if (command.equals("UNDOBID")) {
                    coordinator.processUndoBid(player);
                } else if (command.equals("CLAIM")) {
                    coordinator.processClaim(player);
                } else if (command.equals("CLAIMRESPONSE")) {
                    coordinator.processClaimResponse(player, parsedContent.get(0).equals("true"));
                } else if (command.equals("VOTEKICK")) {
                    coordinator.addKickVote(Integer.parseInt(parsedContent.get(0)), player);
                } else if (command.equals("CHAT")) {
                    coordinator.sendChat(player, parsedContent.get(0), parsedContent.get(1));
                } else if (command.equals("POKE")) {
                    coordinator.pokePlayer();
                } else if (command.equals("PING")) {
                    player.ping();
                }
            }
        } catch(IOException e) {
//            e.printStackTrace();
            handleDisconnect();
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
                if (content.length() > startIndex+length) {
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
    
    public Socket getSocket() {
        return socket;
    }
    
    public void setPlayer(HumanPlayer player) {
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public void sendCommand(Command c) {
        if (socket.isClosed()) {
            return;
        }
        write(c.toString());
    }
    
    public void sendCommand(String text) {
        sendCommand(new Command(text, random.nextLong()));
    }
    
    public void write(String text) {
        writer.println(text);
    }
    
    public void handleDisconnect() {
        coordinator.disconnectPlayer(player);
        endThread();
    }
    
    public void endThread() {
        try {
            running = false;
            socket.close();
        } catch (IOException e) {}
    }
}