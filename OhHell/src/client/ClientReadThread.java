package client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import core.Card;

public class ClientReadThread extends Thread {
    private BufferedReader reader;
    private GameClient client;
    
    private boolean connected = true;

    private long currentCommandId = 0;
    
    public ClientReadThread(Socket socket, GameClient client) {
        this.client = client;
        
        try {
            reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(), "UTF8"));
        } catch (IOException e) {
            client.getKicked();
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        while (connected) {
            try {
                String command = reader.readLine();
                if (connected && command != null) {
                    receiveCommand(command);
                } else {
                    connected = false;
                }
            } catch (IOException e) {
                if (connected) {
                    client.getKicked();
                    connected = false;
                }
            }
        }
    }
    
    public void receiveCommand(String line) {
        String[] splitLine = line.split(":", 3);
        long id = Long.parseLong(splitLine[0]);
        String command = splitLine[1];
        String content = "";
        if (splitLine.length >= 3) {
            content = splitLine[2];
        }
        
        //client.sendCommandToServer("CONFIRMED:" + id);
        if (currentCommandId == id) {
            return;
        }
        
        currentCommandId = id;
        LinkedList<String> parsedContent = parseCommandContent(content);
        
        if (command.equals("UPDATEPLAYERS")) {
            List<ClientPlayer> newPlayers = new ArrayList<>();
            int myIndex = 0;
            
            int params = 7;
            int numPlayers = parsedContent.size() / params;
            for (int i = 0; i < numPlayers; i++) {
                String iname = parsedContent.get(params * i);
                boolean ihuman = parsedContent.get(params * i + 1).equals("true");
                boolean ihost = parsedContent.get(params * i + 2).equals("true");
                boolean idced = parsedContent.get(params * i + 3).equals("true");
                boolean ikicked = parsedContent.get(params * i + 4).equals("true");
                boolean ikibitzer = parsedContent.get(params * i + 5).equals("true");
                boolean imy = parsedContent.get(params * i + 6).equals("true");
                ClientPlayer player = new ClientPlayer();
                newPlayers.add(player);
                player.setIndex(i);
                player.setName(iname);
                player.setHuman(ihuman);
                player.setHost(ihost);
                player.setDisconnected(idced);
                player.setKicked(ikicked);
                player.setKibitzer(ikibitzer);
                if (imy) {
                    myIndex = i;
                }
            }
            
            client.updatePlayersList(newPlayers, myIndex);
        } else if (command.equals("UPDATEROUNDS")) {
            List<int[]> rounds = new ArrayList<>();
            int roundNumber = Integer.parseInt(parsedContent.remove());
            
            int params = 2;
            int numRounds = parsedContent.size() / params;
            for (int i = 0; i < numRounds; i++) {
                int[] round = {Integer.parseInt(parsedContent.get(params * i)),
                        Integer.parseInt(parsedContent.get(params * i + 1))};
                rounds.add(round);
            }
            
            client.updateRounds(rounds, roundNumber);
        } else if (command.equals("KICK")) {
            client.getKicked();
        } else if (command.equals("START")) {
            client.startGame();
        } else if (command.equals("REDEAL")) {
            client.restartRound();
        } else if (command.equals("DEAL")) {
            client.setHand(Integer.parseInt(parsedContent.get(0)), 
                    parsedContent.subList(1, parsedContent.size())
                    .stream()
                    .map(s -> new Card(s))
                    .collect(Collectors.toList()));
        } else if (command.equals("TRUMP")) {
            client.setTrump(new Card(parsedContent.get(0)));
        } else if (command.equals("BID")) {
            client.bid(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("BIDREPORT")) {
            client.bidReport(Integer.parseInt(parsedContent.get(0)), 
                    Integer.parseInt(parsedContent.get(1)));
        } else if (command.equals("PLAY")) {
            client.play(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("PLAYREPORT")) {
            client.playReport(Integer.parseInt(parsedContent.get(0)), 
                    new Card(parsedContent.get(1)));
        } else if (command.equals("TRICKWINNER")) {
            client.trickWinnerReport(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("UNDOBID")) {
            client.undoBidReport(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("CLAIMREQUEST")) {
            client.claimReport(Integer.parseInt(parsedContent.get(0)));
        } else if (command.equals("CLAIMRESULT")) {
            client.claimResult(parsedContent.get(0).equals("ACCEPT"));
        } else if (command.equals("REPORTSCORES")) {
            client.reportScores(parsedContent.stream()
                    .map(s -> s.equals("-") ? null : Integer.parseInt(s))
                    .collect(Collectors.toList()));
        } else if (command.equals("FINALSCORES")) {
            client.postGame();
        } else if (command.equals("POSTGAMETRUMPS")) {
            client.setPostGameTrumps(parsedContent
                    .stream()
                    .map(s -> new Card(s))
                    .collect(Collectors.toList()));
        } else if (command.equals("POSTGAMETAKENS")) {
            client.addPostGameTakens(
                    Integer.parseInt(parsedContent.get(0)),
                    parsedContent.subList(1, parsedContent.size())
                    .stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        } else if (command.equals("POSTGAMEHAND")) {
            client.addPostGameHand(
                    Integer.parseInt(parsedContent.get(0)), 
                    parsedContent.subList(1, parsedContent.size())
                    .stream()
                    .map(s -> new Card(s))
                    .collect(Collectors.toList()));
        } else if (command.equals("RECONNECT")) {
            client.reconnect(parsedContent);
        } else if (command.equals("STATEDEALERLEADER")) {
            client.setDealerLeader(Integer.parseInt(parsedContent.get(0)), 
                    Integer.parseInt(parsedContent.get(1)));
        } else if (command.equals("STATEPLAYER")) {
            client.setStatePlayer(Integer.parseInt(parsedContent.get(0)), 
                    Boolean.parseBoolean(parsedContent.get(1)),
                    Integer.parseInt(parsedContent.get(2)), 
                    Integer.parseInt(parsedContent.get(3)), 
                    new Card(parsedContent.get(4)), 
                    new Card(parsedContent.get(5)));
        } else if (command.equals("STATEPLAYERBIDS")) {
            client.setStatePlayerBids(Integer.parseInt(parsedContent.get(0)),
                    parsedContent.subList(1, parsedContent.size()).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        } else if (command.equals("STATEPLAYERSCORES")) {
            client.setStatePlayerScores(Integer.parseInt(parsedContent.get(0)),
                    parsedContent.subList(1, parsedContent.size()).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        } else if (command.equals("CHAT")) {
            client.chat(parsedContent.get(0));
        } else if (command.equals("POKE")) {
            client.bePoked();
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
    
    public void disconnect() {
        connected = false;
        interrupt();
    }
}