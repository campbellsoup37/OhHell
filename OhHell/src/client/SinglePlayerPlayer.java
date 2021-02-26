package client;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import core.Card;
import core.OhHellCore;
import core.Player;
import core.RoundDetails;

public class SinglePlayerPlayer extends Player {
    private GameClient client;
    
    OhHellCore core;
    
    double[] ovlProbs;
    int aiBid;
    Card aiPlay;
    
    public SinglePlayerPlayer(GameClient client) {
        setName("Player");
        this.client = client;
    }
    
    public void setCore(OhHellCore core) {
        this.core = core;
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
    public void commandStart() {
        client.startGame();
    }

    @Override
    public void commandAddPlayers(List<? extends Player> players, List<? extends Player> kibitzers) {
        List<ClientPlayer> cPlayers = new ArrayList<>(
                (players != null ? players.size() : 0) 
                + (kibitzers != null ? kibitzers.size() : 0));
        if (players != null) {
            for (Player player : players) {
                cPlayers.add(convert(player));
            }
        }
        if (kibitzers != null) {
            for (Player kibitzer : kibitzers) {
                cPlayers.add(convert(kibitzer));
            }
        }
        
        client.addPlayers(cPlayers);
    }
    
    public ClientPlayer convert(Player player) {
        ClientPlayer cPlayer = new ClientPlayer();
        cPlayer.setName(player.getName());
        cPlayer.setId(player.getId());
        cPlayer.setIndex(player.getIndex());
        cPlayer.setHuman(player.isHuman());
        cPlayer.setHost(player.isHost());
        cPlayer.setDisconnected(player.isDisconnected());
        cPlayer.setKicked(player.isKicked());
        cPlayer.setKibitzer(player.isKibitzer());
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
                cPlayers.add(convert(player));
            }
        }
        
        client.updatePlayers(cPlayers);
    }

    /*@Override
    public void commandPlayersInfo(List<Player> players, List<Player> kibitzers, Player myPlayer) {
        List<ClientPlayer> newPlayers = new ArrayList<>();
        int myIndex = 0;
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            
            ClientPlayer clientPlayer = new ClientPlayer();
            newPlayers.add(clientPlayer);
            clientPlayer.setIndex(i);
            clientPlayer.setName(player.getName());
            clientPlayer.setHuman(player.isHuman());
            clientPlayer.setHost(true);
            clientPlayer.setDisconnected(false);
            clientPlayer.setKicked(false);
            clientPlayer.setKibitzer(false);
            if (player == myPlayer) {
                myIndex = i;
            }
        }
        for (int i = 0; i < kibitzers.size(); i++) {
            Player player = kibitzers.get(i);
            
            ClientPlayer clientPlayer = new ClientPlayer();
            newPlayers.add(clientPlayer);
            clientPlayer.setIndex(i);
            clientPlayer.setName(player.getName());
            clientPlayer.setHuman(player.isHuman());
            clientPlayer.setHost(true);
            clientPlayer.setDisconnected(false);
            clientPlayer.setKicked(false);
            clientPlayer.setKibitzer(true);
            if (player == myPlayer) {
                myIndex = i + players.size();
            }
        }
        
        client.updatePlayersList(newPlayers, myIndex);
    }*/
    
    public double getOvlProb(int index) {
        if (index >= ovlProbs.length) {
            return 0;
        } else {
            return ovlProbs[index];
        }
    }
    
    public void calculateOvls(boolean withBid) {
        /*ovlProbs = core.getAiKernel().getOvlPs(this);
        
        if (withBid) {
            aiBid = core.getAiKernel().getMyBid(ovlProbs);
        } else {
            aiBid = -1;
        }*/
    }
    
    public void calculateIvls() {
        //aiPlay = core.getAiKernel().getMyPlay(this);
    }
    
    public int getAiBid() {
        return aiBid;
    }
    
    public Card getAiPlay() {
        return aiPlay;
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
                .map(r -> new int[] {r.getDealer().getIndex(), r.getHandSize()})
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
        
        calculateOvls(true);
    }

    @Override
    public void commandRedeal() {}

    @Override
    public void commandBid(int index) {
        client.bid(index);
        
        if (index == getIndex()) {
            calculateOvls(true);
        }
    }

    @Override
    public void commandPlay(int index) {
        client.play(index);
        
        if (index == getIndex()) {
            calculateOvls(false);
            calculateIvls();
        }
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
