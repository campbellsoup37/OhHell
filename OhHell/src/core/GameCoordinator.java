package core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GameCoordinator {
    /*public static String commandDelimiter1 = ":";
    
    public abstract class Command {
        public class JoinPlayer extends Command {
            protected String type = "ID";
            
            private Player player;
            private String id;
            
            public JoinPlayer(Player player, String id) {
                this.player = player;
                this.id = id;
            }
            
            public JoinPlayer(String code) {
                
            }
            
            @Override
            public String getCode() {
                return TcpTools.encode(id);
            }
            
            @Override
            public void execute() {
                
            }
        }
        
        protected String type;
        protected long timestamp = System.nanoTime();
        
        public static Command decode(String code) {
            
        }
        
        @Override
        public String toString() {
            return type
                    + commandDelimiter1 + timestamp
                    + commandDelimiter1 + getCode();
        }
        
        public String getCode() {
            return "";
        }
        
        public void execute() {}
    }*/
    
    private OhHellCore core;
    
    private List<Player> players = new ArrayList<>();
    private List<Player> kibitzers = new ArrayList<>();
    private HashMap<Integer, Team> teams = new HashMap<>();
    private GameOptions options = new GameOptions();
    
    private Random random = new Random();
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public void startNewCore(OhHellCore core) {
        this.core = core;
        core.setPlayers(players);
        core.setKibitzers(kibitzers);
        core.setTeams(teams);
    }
    
    public void fixPlayerIndices() {
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setIndex(i);
        }
    }
    
    public boolean gameStarted() {
        return core.getGameStarted();
    }
    
    public void updateOptions(GameOptions options) {
        int dummiesToRemove = (int) players.stream().filter(p -> !p.isHuman()).count() - options.getNumRobots();
        if (dummiesToRemove < 0) {
            List<Player> newDummies = new LinkedList<>();
            for (int i = 0; i < -dummiesToRemove; i++) {
                Player newDummy = new DummyPlayer(random.nextLong());
                newDummy.setIndex(players.size() + i);
                newDummies.add(newDummy);
            }
            for (Player player : players) {
                player.commandAddPlayers(newDummies, null);
            }
            for (Player player : kibitzers) {
                player.commandAddPlayers(newDummies, null);
            }
            players.addAll(newDummies);
            updateTeams();
        } else if (dummiesToRemove > 0) {
            kickDummies(dummiesToRemove);
            updateTeams();
        }
        
        this.options = options;
        for (Player player : players) {
            player.commandUpdateOptions(options);
        }
        for (Player kibitzer : kibitzers) {
            kibitzer.commandUpdateOptions(options);
        }
    }
    
    public void kickDummies(int dummiesToRemove) {
        List<Player> toRemove = new LinkedList<>();
        for (Player player : players) {
            if (!player.isHuman()) {
                toRemove.add(player);
                for (Player p : players) {
                    p.commandRemovePlayer(player);
                }
                for (Player p : kibitzers) {
                    p.commandRemovePlayer(player);
                }
                if (toRemove.size() == dummiesToRemove) {
                    break;
                }
            }
        }
        players.removeAll(toRemove);
        fixPlayerIndices();
    }
    
    public void startGame(GameOptions options) {
        this.options = options;
        core.startGame(options, null);
    }
    
    public void joinPlayer(Player player, String id) {
        player.setName(id);
        player.setId(id);
        player.setJoined(true);
        
        boolean reconnect = false;
        for (Player p : players) {
            if (p.getId().equals(player.getId()) && !p.isKicked()) {
                
                p.commandKick();
                reconnectPlayer(p, player);
                p.setDisconnected(false);
                p.resetKickVotes();
                player = p;
                
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
        
        notifyJoin(player, reconnect);
        
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
            player.commandStart(options);
            core.sendFullGameState(player);
        } else {
            if (!reconnect) {
                players.add(player);
                player.setIndex(players.size() - 1);
                if (players.stream().filter(Player::isHuman).count()
                        + kibitzers.stream().filter(Player::isHuman).count() == 1) {
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
        
        player.commandUpdateOptions(options);
        updateTeams();
        
        updatePlayersList();
    }
    
    public void renamePlayer(Player player, String name) {
        player.setName(name);
        for (Player p : players) {
            p.commandUpdatePlayers(Arrays.asList(player));
        }
        for (Player p : kibitzers) {
            p.commandUpdatePlayers(Arrays.asList(player));
        }
    }
    
    public void reteamPlayers(HashMap<Player, Integer> map) {
        for (Player player : map.keySet()) {
            int team = map.get(player);
            if (team == -1) {
                Set<Integer> teams = new HashSet<>();
                for (Player p : players) {
                    if (p != player) {
                        teams.add(p.getTeam());
                    }
                }
                for (Player p : kibitzers) {
                    teams.add(p.getTeam());
                }
                int i = 0;
                while (teams.contains(i)) {
                    i++;
                }
                team = i;
            }
            player.setTeam(team);
        }
        
        for (Player p : players) {
            p.commandUpdatePlayers(new LinkedList<>(map.keySet()));
        }
        for (Player p : kibitzers) {
            p.commandUpdatePlayers(new LinkedList<>(map.keySet()));
        }
        
        updateTeams();
    }
    
    public void reteamPlayer(int index, int team) {
        HashMap<Player, Integer> map = new HashMap<>();
        map.put(players.get(index), team);
        reteamPlayers(map);
    }
    
    public void updateTeams() {
        Set<Integer> teamSet = players.stream().map(Player::getTeam).collect(Collectors.toSet());
        
        List<Integer> staleTeams = new LinkedList<>();
        for (int index : teams.keySet()) {
            if (!teamSet.contains(index)) {
                staleTeams.add(index);
            } else {
                teamSet.remove(index);
            }
        }
        for (int index : staleTeams) {
            teams.remove(index);
        }
        
        for (int index : teamSet) {
            Team team = new Team();
            team.setIndex(index);
            team.setName("Team " + (index + 1));
            teams.put(index, team);
        }
        
        for (Player p : players) {
            p.commandUpdateTeams(new LinkedList<>(teams.values()));
        }
        for (Player p : kibitzers) {
            p.commandUpdateTeams(new LinkedList<>(teams.values()));
        }
    }
    
    public void renameTeam(int teamNumber, String name) {
        Team team = teams.get(teamNumber);
        team.setName(name);
        updateTeams();
    }
    
    public void scrambleTeams() {
        List<Integer> properDivisors = new LinkedList<>();
        for (int i = 2; i < players.size(); i++) {
            if (players.size() % i == 0) {
                properDivisors.add(i);
            }
        }
        if (!properDivisors.isEmpty()) {
            int numTeams = properDivisors.get(random.nextInt(properDivisors.size()));
            int playersPerTeam = players.size() / numTeams;
            List<Player> playersToChoose = new ArrayList<>(players.size());
            playersToChoose.addAll(players);
            HashMap<Player, Integer> map = new HashMap<>();
            for (int i = 0; i < numTeams; i++) {
                for (int j = 0; j < playersPerTeam; j++) {
                    Player player = playersToChoose.remove(random.nextInt(playersToChoose.size()));
                    map.put(player, i);
                }
            }
            reteamPlayers(map);
        }
    }
    
    public void addKickVote(int index, Player fromPlayer) {
        Player player = players.get(index);
        if (player.isDisconnected()) {
            player.addKickVote(fromPlayer);
            if (player.getNumberOfKickVotes() * 2
                    >= players.stream()
                    .filter(p -> !p.isDisconnected() && !p.isKicked() && p.isHuman())
                    .count()) { 
                forceRemovePlayer(player, true);
            }
        }
    }
    
    public void setKibitzer(Player player, boolean kibitzer) {
        boolean wasKibitzer = player.isKibitzer();
        player.setKibitzer(kibitzer);
        if (!wasKibitzer && kibitzer) {
            players.remove(player);
            for (int i = player.getIndex(); i < players.size(); i++) {
                players.get(i).setIndex(i);
            }
            kibitzers.add(player);
        } else if (wasKibitzer && !kibitzer) {
            players.add(player);
            player.setIndex(players.size() - 1);
            kibitzers.remove(player);
            
            //kickExcessDummies();
        }

        for (Player p : players) {
            p.commandRemovePlayer(player);
        }
        for (Player p : kibitzers) {
            p.commandRemovePlayer(player);
        }
        for (Player p : players) {
            p.commandAddPlayers(Arrays.asList(player), null);
        }
        for (Player p : kibitzers) {
            p.commandAddPlayers(Arrays.asList(player), null);
        }
    }
    
    public void pokePlayer() {
        core.pokePlayer();
    }
    
    public void disconnectPlayer(Player player) {
        if (gameStarted() && player.isJoined() && !player.isKibitzer()) {
            removePlayer(player, false);
        } else {
            removePlayer(player, true);
        }
    }
    
    public void forceRemovePlayer(Player player, boolean kick) {
        player.commandKick();
        removePlayer(player, kick);
    }
    
    public void removePlayer(Player player, boolean kick) {
        if ((!players.contains(player)
                || player.isKicked() 
                || player.isDisconnected() && !kick)
                && !kibitzers.contains(player)) {
            return;
        }
        
        notifyRemoval(player, kick);
        player.setDisconnected(!kick);
        player.setKicked(kick);
        
        // Change host if necessary
        if (player.isHost()) {
            List<Player> potentialHosts = players.stream()
                    .filter(p -> p.isHuman() && !p.isDisconnected() && !p.isKicked())
                    .collect(Collectors.toList());
            if (!potentialHosts.isEmpty()) {
                Player newHost = potentialHosts.get(random.nextInt(potentialHosts.size()));
                player.setHost(false);
                newHost.setHost(true);
                for (Player p : players) {
                    p.commandUpdatePlayers(Arrays.asList(newHost));
                }
                for (Player p : kibitzers) {
                    p.commandUpdatePlayers(Arrays.asList(newHost));
                }
            }
        }
        
        // Remove if game hasn't started or player is kibitzer
        // Update otherwise
        if (!gameStarted() || !player.isJoined() || player.isKibitzer()) {
            if (!player.isKibitzer()) {
                players.remove(player);
                for (int i = player.getIndex(); i < players.size(); i++) {
                    players.get(i).setIndex(i);
                }
            } else {
                kibitzers.remove(player);
            }
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
        
        // Restart round if kicked
        if (kick && gameStarted() && !player.isKibitzer()) {
            core.reportKick(player.getIndex());
        }
        
        updateTeams();
        
        updatePlayersList();
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
    
    public void sendChat(Player sender, String recipient, String text) {
        core.sendChat(sender, recipient, text);
    }
    
    public void requestEndGame(Player player) {
        core.requestEndGame(player);
    }
    
    // Overridable callbacks
    
    public void notifyJoin(Player player, boolean rejoin) {}
    
    public void notifyRemoval(Player player, boolean kick) {}
    
    public void reconnectPlayer(Player player1, Player player2) {}
    
    public void updatePlayersList() {}
    
    public void startPing() {}
}
