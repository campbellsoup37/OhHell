package client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.AiStrategyModule;
import core.Card;
import core.OhHellCore;
import core.Player;
import core.RoundDetails;
import strategyOI.AiStrategyModuleOI;
import strategyOI.ImmediateValueLearner;
import strategyOI.OverallValueLearner;

public class CanvasGameSimulator {
    public List<ClientPlayer> cPlayers;
    public List<int[]> rounds;
    public List<Card> trumps;
    
    public OhHellCore core;
    public List<Player> players;
    
    public CanvasGameSimulator(List<ClientPlayer> cPlayers, List<int[]> rounds, List<Card> trumps) {
        this.cPlayers = cPlayers;
        this.rounds = rounds;
        this.trumps = trumps;
    }
    
    public void simulate(boolean doubleDeck) {
        int D = doubleDeck ? 2 : 1;
        core = new OhHellCore(false) {
            @Override
            public void buildRounds(boolean doubleDeck) {
                for (int[] round : rounds) {
                    getRounds().add(new RoundDetails(round[1]));
                }
            }
            
            @Override
            public List<List<Card>> getNextHands() {
                List<List<Card>> hands = new ArrayList<>(cPlayers.size() + 1);
                for (ClientPlayer player : cPlayers) {
                    List<Card> hand = player.getHands().get(getRoundNumber());
                    List<Card> handCopy = new ArrayList<>(hand.size());
                    handCopy.addAll(hand);
                    hands.add(handCopy);
                }
                hands.add(Arrays.asList(trumps.get(getRoundNumber())));
                return hands;
            }
            
            @Override
            public void stopGame() {
                stopKernel();
                whenFinished();
            }
            
            @Override
            public void reloadAiStrategyModules(int N) {
                getAiKernel().reloadAiStrategyModules(N, createAiStrategyModules(N, D));
            }
        };
        players = new ArrayList<>(cPlayers.size());
        core.setPlayers(players);
        
        core.startGame(cPlayers.size(), doubleDeck, createAiStrategyModules(cPlayers.size(), D), 0);
    }
    
    public List<AiStrategyModule> createAiStrategyModules(int N, int D) {
        OverallValueLearner ovl = new OverallValueLearner("resources/models/" + "ovlN" + N + "D" + D + ".txt");
        ImmediateValueLearner ivl = new ImmediateValueLearner("resources/models/" + "ivlN" + N + "D" + D + ".txt");
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(cPlayers.size());
        for (int i = 0; i < cPlayers.size(); i++) {
            aiStrategyModules.add(new AiStrategyModuleOI(core, N, D, ovl, ivl) {
                public ClientPlayer clientPlayer() {
                    return cPlayers.get(player.getIndex());
                }
                
                @Override
                public void makeBid() {
                    if (clientPlayer().getKickedAtRound() == core.getRoundNumber()) {
                        player.setKicked(true);
                        core.updateRounds();
                        core.restartRound();
                    } else {
                        double[] ps = getOvlPs();
                        double[] qs = AiStrategyModuleOI.subsetProb(ps, ps.length);
                        clientPlayer().addBidQs(qs);
                        clientPlayer().addAiBid(AiStrategyModuleOI.optimalBid(ps)[0]);
                        clientPlayer().addDiff(difficulty(qs));
                        
                        core.incomingBid(player, clientPlayer().getBids().get(core.getRoundNumber()));
                    }
                }
                
                @Override
                public void makePlay() {
                    ClientPlayer.Play play = clientPlayer().getPlays().get(core.getRoundNumber()).get(core.getPlayNumber());
                    if (play.isClaimed()) {
                        for (ClientPlayer p : cPlayers) {
                            if (p.getPlays().get(core.getRoundNumber()).get(core.getPlayNumber()).isClaiming()) {
                                core.makeAcceptedClaim(players.get(p.getIndex()));
                                break;
                            }
                        }
                    } else {
                        getMyPlay();
                        clientPlayer().addMakingProbs(getMakingProbs(), core.getRoundNumber());
                        core.incomingPlay(player, play.getCard());
                    }
                }
            });
        }
        
        return aiStrategyModules;
    }
    
    public void whenFinished() {}
}
