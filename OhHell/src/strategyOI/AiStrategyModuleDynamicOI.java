package strategyOI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import core.AiStrategyModule;
import core.AiTrainer;
import core.Card;
import core.Deck;
import core.OhHellCore;
import core.Player;
import ml.SparseVector;
import ml.Vector;

public class AiStrategyModuleDynamicOI extends AiStrategyModule {
    private OhHellCore core;
    private List<Player> players;
    private Deck deck;
    private int maxH;
    private OverallValueLearner[] ovls;
    private ImmediateValueLearner ivl;
    
    private Card cardToPlay;
    private Hashtable<Card, Vector> ovlIns;
    private Hashtable<Card, Set<Integer>> ovlsAddedTo;
    
    private AiTrainer aiTrainer;
    
    public AiStrategyModuleDynamicOI(OhHellCore core, int N,
            OverallValueLearner[] ovls, ImmediateValueLearner ivl) {
        this.core = core;
        players = core.getPlayers();
        deck = core.getDeck();
        maxH = Math.min(10, 51 / N);
        this.ovls = ovls;
        this.ivl = ivl;
        aiTrainer = core.getAiTrainer();
    }
    
    @Override
    public void newHand() {
        ovlsAddedTo = new Hashtable<>();
    }
    
    @Override
    public void makeBid() {
        int myBid = getMyBid();

        //System.out.println("Player " + player.getIndex() + " bids " + myBid);
        core.incomingBid(player, myBid);
    }
    
    @Override
    public void makePlay() {
        Card cardToPlay = getMyPlay();

        //System.out.println("Player " + player.getIndex() + " plays " + cardToPlay);
        core.incomingPlay(player, cardToPlay);
    }
    
    public void addOvlInput(SparseVector in, Card card, int hOffSet, int voidsOffset) {
        int turn = player.getIndex();
        int M = players.size();
        Card trump = core.getTrump();
        
        int numOfVoids = voids(player.getHand()) + voidsOffset;
        List<List<Card>> split = splitBySuit(player.getHand());
        List<Card> trick = players.stream()
                .map(Player::getTrick)
                .filter(c -> !c.isEmpty())
                .collect(Collectors.toList());
        
        in.addOneHot(player.getHand().size() - hOffSet, maxH);
        if (!player.hasBid()) {
            for (int j = 0; j < M; j++) {
                Player iterPlayer = players.get((turn + j) % M);
                if (!iterPlayer.isKicked()) {
                    if ((turn - core.getLeader() + M) % M + j >= M) {
                        in.addOneHot(iterPlayer.getBid() + 1, maxH + 1);
                    } else {
                        in.addZeros(maxH + 1);
                    }
                }
            }
        } else {
            for (int j = 0; j < M; j++) {
                Player iterPlayer = players.get((turn + j) % M);
                if (!iterPlayer.isKicked()) {
                    in.addOneHot(Math.max(iterPlayer.getBid() - iterPlayer.getTaken(), 0) + 1, maxH + 1);
                }
            }
        }
        in.addOneHot(numOfVoids + 1, 4);
        in.addOneHot(deck.cardsLeftOfSuit(trump, Arrays.asList(split.get(trump.getSuitNumber() - 1), trick)) + 1, 13);
        
        in.addOneHot(card.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
        in.addOneHot(deck.cardsLeftOfSuit(card, Arrays.asList(split.get(card.getSuitNumber() - 1), trick)) + 1, 13);
        
        in.addOneHot(deck.adjustedCardValueSmall(card, Arrays.asList(split.get(card.getSuitNumber() - 1), trick)) + 1, 13);
    }
    
    public void addIvlInput(SparseVector in, Card card) {
        int turn = player.getIndex();
        int M = players.size();
        Card trump = core.getTrump();

        List<List<Card>> split = splitBySuit(players.get(turn).getHand());
        List<Card> trick = players.stream().map(Player::getTrick).filter(c -> !c.isEmpty()).collect(Collectors.toList());
        
        for (int k = 1; k < players.size(); k++) {
            Player iterPlayer = players.get((turn + k) % M);
            if (!iterPlayer.isKicked()) {
                if ((turn - core.getLeader() + M) % M + k < M) {
                    in.addOneHot(Math.max(iterPlayer.getBid() - iterPlayer.getTaken(), 0) + 1, maxH + 1);
                } else {
                    in.addZeros(maxH + 1);
                }
            }
        }
        in.addOneHot(deck.cardsLeftOfSuit(trump, Arrays.asList(split.get(trump.getSuitNumber() - 1), trick)) + 1, 13);

        Card led = players.get(core.getLeader()).getTrick().isEmpty() ? card : players.get(core.getLeader()).getTrick();
        in.addOneHot(led.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
        in.addOneHot(deck.cardsLeftOfSuit(led, Arrays.asList(split.get(led.getSuitNumber() - 1), trick)) + 1, 13);
        
        in.addOneHot(card.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
        in.addOneHot(deck.adjustedCardValueSmall(card, Arrays.asList(split.get(card.getSuitNumber() - 1), trick)) + 1, 13);
    }
    
    public static int voids(List<Card> hand) {
        boolean[] notVoid = new boolean[4];
        for (Card c : hand) {
            notVoid[c.getSuitNumber() - 1] = true;
        }
        int count = 0;
        for (boolean nv : notVoid) {
            if (!nv) {
                count++;
            }
        }
        return count;
    }
    
    public static List<List<Card>> splitBySuit(List<Card> hand) {
        List<List<Card>> out = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            out.add(new LinkedList<>());
        }
        for (Card c : hand) {
            out.get(c.getSuitNumber() - 1).add(c);
        }
        return out;
    }
    
    public int getMyBid() {
        double[][] pses = new double[player.getHand().size() + 1][player.getHand().size()];
        
        Hashtable<Card, Vector> ovlIns = new Hashtable<>();
        int l = 0;
        for (Card card : player.getHand()) {
            SparseVector in = new SparseVector();
            addOvlInput(in, card, 0, 0);
            ovlIns.put(card, in);
            
            for (int b = 0; b <= player.getHand().size(); b++) {
                pses[b][l] = ovls[b].testValue(in).get(1).get(0);
            }
            
            l++;
        }

        int[] bestBids = optimalBid(pses);
        int myBid = bestBids[0] != core.whatCanINotBid() ? bestBids[0] : bestBids[1];
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            for (Card card : player.getHand()) {
                ovls[myBid].putIn(card, ovlIns.get(card));
                Set<Integer> newSet = new HashSet<>();
                newSet.add(myBid);
                ovlsAddedTo.put(card, newSet);
            }
        }
        
        return myBid;
    }
    
    public Card getMyPlay() {
        int iWant = Math.max(player.getBid() - player.getTaken(), 0);
        
        List<Card> canPlay = core.whatCanIPlay(player);
        List<List<Card>> split = splitBySuit(player.getHand());
        
        boolean singleton = false;
        for (List<Card> suits : split) {
            if (suits.size() == 1) {
                singleton = true;
            }
        }
        boolean canPlaySingleton = canPlay.size() == player.getHand().size() && singleton || canPlay.size() == 1;

        Hashtable<Card, Double> ovlValsNonsingleton0 = new Hashtable<>();
        Hashtable<Card, Double> ovlValsSingleton0 = new Hashtable<>();
        Hashtable<Card, Double> ovlValsNonsingleton1 = new Hashtable<>();
        Hashtable<Card, Double> ovlValsSingleton1 = new Hashtable<>();

        Hashtable<Card, Vector> ovlInsNonsingleton = new Hashtable<>();
        Hashtable<Card, Vector> ovlInsSingleton = new Hashtable<>();
        
        if (player.getHand().size() > 1) {
            for (Card card : player.getHand()) {
                SparseVector in = new SparseVector();
                addOvlInput(in, card, 1, 0);
                double val0 = ovls[iWant].testValue(in).get(1).get(0);
                double val1 = iWant == 0 ? val0 : ovls[iWant - 1].testValue(in).get(1).get(0);
                ovlValsNonsingleton0.put(card, val0);
                ovlValsNonsingleton1.put(card, val1);
                ovlInsNonsingleton.put(card, in);
                
                if (canPlaySingleton) {
                    SparseVector inS = new SparseVector();
                    addOvlInput(inS, card, 1, 1);
                    val0 = ovls[iWant].testValue(inS).get(1).get(0);
                    val1 = iWant == 0 ? val0 : ovls[iWant - 1].testValue(inS).get(1).get(0);
                    ovlValsSingleton0.put(card, val0);
                    ovlValsSingleton1.put(card, val1);
                    ovlInsSingleton.put(card, inS);
                }
            }
        } else {
            ovlValsSingleton0.put(player.getHand().get(0), 0.0);
            ovlValsSingleton1.put(player.getHand().get(0), 0.0);
        }
        
        Hashtable<Card, Double> adjustedProbs = new Hashtable<>();
        HashMap<Card, Vector> ivlIns = new HashMap<>();
        for (Card card : canPlay) {
            SparseVector in = null;
            double probOfWinning = 0;
            
            if (core.cardCanWin(card)) {
                in = new SparseVector();
                addIvlInput(in, card);
                
                probOfWinning = ivl.testValue(in).get(1).get(0);
            }
            
            Hashtable<Card, Double> ovlVals0 = split.get(card.getSuitNumber() - 1).size() == 1 ? ovlValsSingleton0 : ovlValsNonsingleton0;
            Hashtable<Card, Double> ovlVals1 = split.get(card.getSuitNumber() - 1).size() == 1 ? ovlValsSingleton1 : ovlValsNonsingleton1;
            
            double[] ps0 = new double[player.getHand().size() - 1];
            double[] ps1 = new double[player.getHand().size() - 1];
            int ll = 0;
            for (Card c : player.getHand()) {
                if (c != card) {
                    ps0[ll] = ovlVals0.get(c);
                    ps1[ll] = ovlVals1.get(c);
                    ll++;
                }
            }
            
            double conditional0 = subsetProb(ps0, iWant)[iWant];
            double conditional1 = iWant == 0 ? 0 : subsetProb(ps1, iWant - 1)[iWant - 1];
            
            double value = (1 - probOfWinning) * conditional0 + probOfWinning * conditional1;
            
            adjustedProbs.put(card, value);
            ivlIns.put(card, in);
        }
        
        cardToPlay = chooseBestCard(adjustedProbs);
        Vector inToPlay = ivlIns.get(cardToPlay);
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            if (inToPlay != null) {
                ivl.putIn(cardToPlay, inToPlay);
            }
            ovlIns = split.get(cardToPlay.getSuitNumber() - 1).size() == 1 ? ovlInsSingleton : ovlInsNonsingleton;
        }
        return cardToPlay;
    }
    
    /**
     * adjustedProbs maps each card in the hand to the estimated probability that it will make the
     * bid given it plays that card. Normally, we play the card with the highest said probability.
     * The reason I wrote this function is to experiment with other choice functions, e.g., one
     * that chooses randomly, weighting the cards by their probabilities.
     */
    public Card chooseBestCard(Hashtable<Card, Double> adjustedProbs) {
        double max = -1;
        Card maxCard = null;
        for (Card card : adjustedProbs.keySet()) {
            double value = adjustedProbs.get(card);
            if (value > max) {
                max = value;
                maxCard = card;
            }
        }
        return maxCard;
        
        /*double sum = 0;
        for (Card card : adjustedProbs.keySet()) {
            double value = adjustedProbs.get(card);
            sum += value * value;
        }
        
        double x = new Random().nextDouble() * sum;
        for (Card card : adjustedProbs.keySet()) {
            double value = adjustedProbs.get(card);
            x -= value * value;
            if (x <= 0) {
                return card;
            }
        }
        return null;*/
    }
    
    @Override
    public void addTrickData(Card winner, List<Card> trick) {
        if (aiTrainer != null && aiTrainer.backprop()) {
            int iWant = Math.max(player.getBid() - player.getTaken(), 0);
            for (Card card : player.getHand()) {
                ovls[iWant].putIn(card, ovlIns.get(card));
                ovlsAddedTo.get(card).add(iWant);
            }
            
            int truth = cardToPlay == winner ? 1 : 0;
            for (Integer i : ovlsAddedTo.get(cardToPlay)) {
                ovls[i].putOut(cardToPlay, truth);
            }
            ivl.putOut(cardToPlay, truth);
        }
    }
    
    public static double[] subsetProb(double[] ps, int l) {
        double[] q = new double[l + 1];
        q[0] = 1;
        for (int i = 0; i < ps.length; i++) {
            double prev = 0;
            for (int j = 0; j <= i + 1 && j <= l; j++) {
                double next = q[j];
                q[j] = prev * ps[i] + next * (1 - ps[i]);
                prev = next;
            }
        }
        return q;
    }
    
    public static int[] optimalBid(double[][] pses) {
        int n = pses[0].length;
        double[][] qses = new double[pses.length][];
        for (int i = 0; i < pses.length; i++) {
            qses[i] = subsetProb(pses[i], n);
        }

        int[] ans = {0, 0};
        double[] bestEs = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int k = 0; k <= n; k++) {
            double E = 0;
            for (int l = 0; l <= n; l++) {
                double points = 0;
                int diff = Math.abs(k - l);
                if (diff == 0) {
                    points = 10 + k * k;
                } else {
                    points = -5 * diff * (diff + 1) / 2;
                }
                E += qses[k][l] * points;
            }
            if (E > bestEs[0]) {
                ans[1] = ans[0];
                ans[0] = k;
                bestEs[1] = bestEs[0];
                bestEs[0] = E;
            } else if (E > bestEs[1]) {
                ans[1] = k;
                bestEs[1] = E;
            }
        }
        
        return ans;
    }
}
