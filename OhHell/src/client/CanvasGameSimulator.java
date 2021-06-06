package client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.AiKernel;
import core.AiStrategyModule;
import core.Card;
import core.GameOptions;
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
    
    public void simulate(GameOptions options) {
        core = new OhHellCore(false) {
            @Override
            public void buildRounds(GameOptions options) {
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
            public void reloadAiStrategyModules(int N, int D, int T) {
                getAiKernel().reloadAiStrategyModules(N, D, T, createAiStrategyModules(N, options));
            }
        };
        players = new ArrayList<>(cPlayers.size());
        core.setPlayers(players);
        
        core.overrideAiKernel(new AiKernel(core) {
            @Override
            public List<AiStrategyModule> createDefaultAiStrategyModules(int N, int D, int T) {
                return createAiStrategyModules(N, options);
            }
        });
        
        core.startGame(options);
    }
    
    public List<AiStrategyModule> createAiStrategyModules(int N, GameOptions options) {
        int D = options.getD();
        OverallValueLearner ovl = new OverallValueLearner(String.format("resources/models/N%d/D%d/T0/ovl.txt", N, D));
        ImmediateValueLearner ivl = new ImmediateValueLearner(String.format("resources/models/N%d/D%d/T0/ivl.txt", N, D));
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(cPlayers.size());
        for (int i = 0; i < cPlayers.size(); i++) {
            AiStrategyModule aiStrategyModule = new AiStrategyModuleOI(core, N, D, ovl, ivl) {
                public ClientPlayer clientPlayer() {
                    return cPlayers.get(player.getIndex());
                }
                
                @Override
                public int makeBid() {
                    if (clientPlayer().getKickedAtRound() == core.getRoundNumber()) {
                        player.setKicked(true);
                        core.updateRounds();
                        core.restartRound();
                        return -1;
                    } else {
                        double[] ps = getOvlPs();
                        double[] qs = AiStrategyModuleOI.subsetProb(ps, ps.length);
                        clientPlayer().addBidQs(qs);
                        clientPlayer().addAiBid(AiStrategyModuleOI.orderBids(ps)[0]);
                        clientPlayer().addDiff(difficulty(qs));
                        
                        return clientPlayer().getBids().get(core.getRoundNumber());
                    }
                }
                
                @Override
                public Card makePlay() {
                    ClientPlayer.Play play = clientPlayer().getPlays().get(core.getRoundNumber()).get(core.getPlayNumber());
                    if (play.isClaimed()) {
                        for (ClientPlayer p : cPlayers) {
                            if (p.getPlays().get(core.getRoundNumber()).get(core.getPlayNumber()).isClaiming()) {
                                core.makeAcceptedClaim(players.get(p.getIndex()));
                                break;
                            }
                        }
                        return null;
                    } else {
                        getMyPlay();
                        clientPlayer().addMakingProbs(getMakingProbs(), core.getRoundNumber());
                        return play.getCard();
                    }
                }
            };
            aiStrategyModule.setCoreData(core.getCoreData());
            aiStrategyModules.add(aiStrategyModule);
        }
        
        return aiStrategyModules;
    }
    
    public void whenFinished() {}
}
