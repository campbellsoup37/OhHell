package client;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import core.Card;
import core.GameOptions;
import core.Player;
import core.RoundDetails;
import core.Team;

public class SinglePlayerPlayer extends Player {
    private GameClient client;
    
    public SinglePlayerPlayer(GameClient client) {
        setName("Player");
        this.client = client;
    }
    
    @Override
    public String realName() {
        return "Player";
    }
    
    @Override
    public boolean isHuman() {
        return true;
    }
    
    @Override
    public void commandStart(GameOptions options) {
        client.startGame(options);
    }

    @Override
    public void commandAddPlayers(List<? extends Player> players, List<? extends Player> kibitzers) {
        List<ClientPlayer> cPlayers = new ArrayList<>(
                (players != null ? players.size() : 0) 
                + (kibitzers != null ? kibitzers.size() : 0));
        if (players != null) {
            for (Player player : players) {
                cPlayers.add(convertPlayer(player));
            }
        }
        if (kibitzers != null) {
            for (Player kibitzer : kibitzers) {
                cPlayers.add(convertPlayer(kibitzer));
            }
        }
        
        client.addPlayers(cPlayers);
    }
    
    public ClientPlayer convertPlayer(Player player) {
        ClientPlayer cPlayer = new ClientPlayer();
        cPlayer.setName(player.getName());
        cPlayer.setId(player.getId());
        cPlayer.setIndex(player.getIndex());
        cPlayer.setHuman(player.isHuman());
        cPlayer.setHost(player.isHost() || player.isHuman());
        cPlayer.setDisconnected(player.isDisconnected());
        cPlayer.setKicked(player.isKicked());
        cPlayer.setKibitzer(player.isKibitzer());
        cPlayer.setTeam(player.getTeam());
        return cPlayer;
    }

    @Override
    public void commandRemovePlayer(Player player) {
        client.removePlayer(player.getId());
    }

    @Override
    public void commandUpdatePlayers(List<? extends Player> players) {
        List<ClientPlayer> cPlayers = new ArrayList<>(
                (players != null ? players.size() : 0));
        if (players != null) {
            for (Player player : players) {
                cPlayers.add(convertPlayer(player));
            }
        }
        
        client.updatePlayers(cPlayers);
    }
    
    @Override
    public void commandUpdateTeams(List<Team> teams) {
        client.updateTeams(
                teams.stream()
                .map(team -> convertTeam(team))
                .collect(Collectors.toList()));
    }
    
    public ClientTeam convertTeam(Team team) {
        ClientTeam cTeam = new ClientTeam();
        cTeam.setIndex(team.getIndex());
        cTeam.setName(team.getName());
        return cTeam;
    }
    
    @Override
    public void commandUpdateOptions(GameOptions options) {
        client.updateGameOptions(options);
    }

    @Override
    public void commandStatePlayer(Player player) {}

    @Override
    public void commandDealerLeader(int dealer, int leader) {
        client.setDealerLeader(dealer, leader);
    }

    @Override
    public void commandUpdateRounds(List<RoundDetails> rounds, int roundNumber) {
        client.updateRounds(rounds.stream()
                .map(r -> new int[] {r.getDealer(), r.getHandSize()})
                .collect(Collectors.toList()), 
                roundNumber);
    }

    @Override
    public void commandDeal(List<Player> players, Card trump) {
        for (Player p : players) {
            if (p != this && !isKibitzer()) {
                client.setHand(p.getIndex(), 
                        p.getHand().stream()
                        .map(c -> new Card())
                        .collect(Collectors.toList()));
            } else {
                List<Card> handCopy = new ArrayList<>();
                for (Card c : p.getHand()) {
                    handCopy.add(c);
                }
                client.setHand(p.getIndex(), handCopy);
            }
        }
        client.setTrump(trump);
    }

    @Override
    public void commandRedeal() {}

    @Override
    public void commandBid(int index) {
        client.bid(index);
    }

    @Override
    public void commandPlay(int index) {
        client.play(index);
    }

    @Override
    public void commandBidReport(int index, int bid) {
        client.bidReport(index, bid);
    }

    @Override
    public void commandPlayReport(int index, Card card) {
        client.playReport(index, card);
    }

    @Override
    public void commandTrickWinner(int index, List<Card> trick) {
        client.trickWinnerReport(index);
    }
    
    @Override
    public void commandUndoBidReport(int index) {
        client.undoBidReport(index);
    }
    
    @Override
    public void commandClaimRequest(int index) {
        client.claimReport(index);
    }
    
    @Override
    public void commandClaimResult(boolean accept) {
        client.claimResult(accept);
    }

    @Override
    public void commandNewScores(List<Integer> scores) {
        client.reportScores(scores);
    }
    
    @Override
    public void commandPostGameTrumps(List<Card> trumps) {
        client.setPostGameTrumps(trumps);
    }
    
    @Override
    public void commandPostGameTakens(List<Player> players) {
        for (Player player : players) {
            client.addPostGameTakens(player.getIndex(), player.getTakens());
        }
    }
    
    @Override
    public void commandPostGameHands(List<Player> players) {
        for (Player player : players) {
            for (List<Card> hand : player.getHands()) {
                client.addPostGameHand(player.getIndex(), hand);
            }
        }
    }
    
    @Override
    public void commandPostGameFile(String file) {
        client.receivePostGameFile(file);
    }
    
    @Override
    public void commandEndGame(Player player) {
        client.endGame(player.getId());
    }

    @Override
    public void commandChat(String text) {
        client.chat(text);
    }
}
