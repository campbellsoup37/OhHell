package client;
import java.util.ArrayList;
import java.util.LinkedList;
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
        
        client.updatePlayersList(newPlayers, myIndex);
    }
    
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
            if (p != this) {
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
    public void commandFinalScores(List<Player> playersSorted) {
        LinkedList<String> finalScores = new LinkedList<>();
        for (Player p : playersSorted) {
            finalScores.add(p.getName());
            finalScores.add(p.getScore() + "");
        }
        client.finalScores(finalScores);
    }

    @Override
    public void commandChat(String text) {
        client.chat(text);
    }
}
