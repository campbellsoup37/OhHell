package strategyRBP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.AiStrategyModule;
import core.AiTrainer;
import core.Card;
import core.Deck;
import core.OhHellCore;
import core.Player;
import ml.SparseVector;
import ml.Vector;

public class AiStrategyModuleRBP extends AiStrategyModule {
    private static boolean debugBid = false;
    private static boolean debugPlay = false;
    
    private OhHellCore core;
    private List<Player> players;
    private Deck deck;
    private int maxH;
    private BiddingLearner bl;
    private OverallValueLearner ovl;
    private ImmediateValueLearner ivl;
    
    private Random r = new Random();
    private double bidExploration = 0;
    private double playExploration = 0;
    
    private AiTrainer aiTrainer;
    
    private Card myPlayMemo;
    private HashMap<Card, List<Vector>> ovlInsLoseMemo;
    private HashMap<Card, List<Vector>> ovlInsWinMemo;
    
    public AiStrategyModuleRBP(OhHellCore core, int N,
            BiddingLearner bl, OverallValueLearner ovl, ImmediateValueLearner ivl) {
        this.core = core;
        players = core.getPlayers();
        deck = core.getDeck();
        maxH = Math.min(10, 51 / N);
        this.bl = bl;
        this.ovl = ovl;
        this.ivl = ivl;
        aiTrainer = core.getAiTrainer();
    }
    
    public void setExploration(double bidExploration, double playExploration) {
        this.bidExploration = bidExploration;
        this.playExploration = playExploration;
    }
    
    public void reload(int N, BiddingLearner bl, OverallValueLearner ovl, ImmediateValueLearner ivl) {
        maxH = Math.min(10, 51 / N);
        this.bl = bl;
        this.ovl = ovl;
        this.ivl = ivl;
    }
    
    @Override
    public void makeBid() {
        int myBid = getMyBid();
        core.incomingBid(player, myBid);
    }
    
    @Override
    public void makePlay() {
        Card cardToPlay = getMyPlay();
        core.incomingPlay(player, cardToPlay);
    }
    
    private static int[][] perms = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
    
    public List<Vector> getBlInputs(int bid) {
        int dealer = core.getDealer();
        int M = players.size();
        Card trump = core.getTrump();
        
        List<List<Integer>> handSplit = split(player.getHand(), trump);
        
        List<Vector> ins = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            SparseVector in = new SparseVector();
            
            in.addOneHot(bid + 1, maxH + 1);
            
            in.addBinaryVector(handSplit.get(perms[i][0]), 13);
            in.addBinaryVector(handSplit.get(perms[i][1]), 13);
            in.addBinaryVector(handSplit.get(perms[i][2]), 13);
            in.addBinaryVector(handSplit.get(3), 12);
            
            for (int j = 1; j <= M; j++) {
                Player iterPlayer = players.get((dealer + j) % M);
                if (!iterPlayer.isKicked() && iterPlayer != player) {
                    if (iterPlayer.hasBid()) {
                        in.addOneHot(iterPlayer.getBid() + 2, maxH + 2);
                    } else {
                        in.addOneHot(1, maxH + 2);
                    }
                }
            }
            
            ins.add(in);
        }
        
        return ins;
    }
    
    public List<Vector> getOvlInputs(Card card, int[] lengths, boolean decrementWant) {
        int turn = player.getIndex();
        int M = players.size();
        Card trump = core.getTrump();
        
        List<List<Card>> seen = Arrays.asList(
                deck.getPlayedCards(),
                player.getHand(),
                core.getTrick());
        
        List<List<Integer>> suit = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            suit.add(new LinkedList<>());
            if (i == card.getSuitNumber() - 1) {
                suit.get(i).add(1);
            }
        }
        suit.add(suit.remove(trump.getSuitNumber() - 1));
        
        List<Integer> myLengths = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            myLengths.add(lengths[i]);
        }
        myLengths.add(myLengths.remove(trump.getSuitNumber() - 1));
        
        List<Integer> unseenLengths = unseenLengths(seen, trump);
        unseenLengths.add(unseenLengths.remove(trump.getSuitNumber() - 1));

        List<Vector> ins = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            SparseVector in = new SparseVector();
            
            in.addOneHot(adjustedCardValue(card, seen), 13);
            
            in.addBinaryVector(suit.get(perms[i][0]), 1);
            in.addBinaryVector(suit.get(perms[i][1]), 1);
            in.addBinaryVector(suit.get(perms[i][2]), 1);
            in.addBinaryVector(suit.get(3), 1);
            
            in.addOneHot(myLengths.get(perms[i][0]) + 1, maxH + 1);
            in.addOneHot(myLengths.get(perms[i][1]) + 1, maxH + 1);
            in.addOneHot(myLengths.get(perms[i][2]) + 1, maxH + 1);
            in.addOneHot(myLengths.get(3) + 1, maxH + 1);
            
            in.addOneHot(unseenLengths.get(perms[i][0]) + 1, 14);
            in.addOneHot(unseenLengths.get(perms[i][1]) + 1, 14);
            in.addOneHot(unseenLengths.get(perms[i][2]) + 1, 14);
            in.addOneHot(unseenLengths.get(3) + 1, 13);
            
            for (int j = 0; j < M; j++) {
                Player iterPlayer = players.get((turn + j) % M);
                if (!iterPlayer.isKicked()) {
                    if (j == 0 && decrementWant && wants(iterPlayer) > 0) {
                        in.addOneHot(wants(iterPlayer), maxH + 1);
                    } else {
                        in.addOneHot(wants(iterPlayer) + 1, maxH + 1);
                    }
                }
            }

            ins.add(in);
        }
        
        return ins;
    }
    
    public List<Vector> getIvlInputs(Card card) {
        int turn = player.getIndex();
        int M = players.size();
        Card trump = core.getTrump();
        
        List<List<Card>> seen = Arrays.asList(
                deck.getPlayedCards(),
                player.getHand(),
                core.getTrick());
        
        int state = 1;
        Card led = players.get(core.getLeader()).getTrick();
        if (!led.isEmpty()) {
            if (!card.getSuit().equals(trump.getSuit())) {
                state = 2;
            } else if (!led.getSuit().equals(trump.getSuit())) {
                state = 3;
            } else {
                state = 4;
            }
        } else {
            led = card;
        }
        
        List<Integer> unseenLengths = unseenLengths(seen, trump);

        List<Vector> ins = new ArrayList<>(1);
        for (int i = 0; i < 1; i++) {
            SparseVector in = new SparseVector();
            
            in.addOneHot(adjustedCardValue(card, seen), 13);
            
            in.addOneHot(state, 4);
            
            in.addOneHot(unseenLengths.get(led.getSuitNumber() - 1) + 1, 13);
            
            in.addOneHot(unseenLengths.get(trump.getSuitNumber() - 1) + 1, 13);
            
            for (int k = 1; k < players.size(); k++) {
                Player iterPlayer = players.get((turn + k) % M);
                if (!iterPlayer.isKicked()) {
                    if ((turn - core.getLeader() + M) % M + k < M) {
                        in.addOneHot(wants(iterPlayer) + 2, maxH + 2);
                    } else {
                        in.addOneHot(1, maxH + 2);
                    }
                }
            }

            ins.add(in);
        }
        
        return ins;
    }
    
    public List<List<Integer>> split(List<Card> cards, Card trump) {
        List<List<Integer>> ans = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ans.add(new LinkedList<>());
        }
        for (Card card : cards) {
            ans.get(card.getSuitNumber() - 1).add(card.getAdjustedNum(trump) - 1 - (trump.getSuit().equals(card.getSuit()) ? 1 : 0));
        }
        ans.add(ans.remove(trump.getSuitNumber() - 1));
        return ans;
    }
    
    public int adjustedCardValue(Card card, List<List<Card>> playeds) {
        int ans = card.getNum() - 1;
        for (List<Card> played : playeds) {
            for (Card c : played) {
                if (!c.isEmpty() && c.isGreaterThan(card, "")) {
                    ans++;
                }
            }
        }
        return ans;
    }
    
    public List<Integer> unseenLengths(List<List<Card>> cardses, Card trump) {
        Integer[] lengths = {13, 13, 13, 13};
        for (List<Card> cards : cardses) {
            for (Card card : cards) {
                if (!card.isEmpty()) {
                    lengths[card.getSuitNumber() - 1]--;
                }
            }
        }
        return new ArrayList<>(Arrays.asList(lengths));
    }
    
    public int wants(Player player) {
        return Math.max(
                Math.min(player.getBid() - player.getTaken(), player.getHand().size()), 
                0);
    }
    
    public int getMyBid() {
        @SuppressWarnings("unchecked")
        List<Vector>[] allIns = new List[player.getHand().size() + 1];
        double[] expects = new double[player.getHand().size() + 1];
        for (int i = 0; i <= player.getHand().size(); i++) {
            allIns[i] = getBlInputs(i);
            expects[i] = (bl.testValue(allIns[i].get(0)).get(1).get(0) - 5.0 / 7.0) * 385.0;
        }
        
        int[] bestBids = chooseBestBid(expects);
        int myBid = bestBids[0] != core.whatCanINotBid() ? bestBids[0] : bestBids[1];
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            bl.putIn(player.getIndex(), allIns[myBid]);
        }
        
        if (debugBid) {
            System.out.println("BIDDING: " + player.getName() + " ===========================================");
            System.out.println("Bid: " + bestBids[0] + " (" + bestBids[1] + ")");
            System.out.println("Hand: ");
            for (Card card : player.getHand()) {
                System.out.print(card + " ");
            }
            System.out.println();
            System.out.println("Trump: " + core.getTrump());
            System.out.println("BL:");
            allIns[myBid].get(0).print();
            System.out.println("Input Es: ");
            for (int j = 0; j <= player.getHand().size(); j++) {
                System.out.println(j + ": " + expects[j]);
            }
            System.out.println();
        }
        
        return myBid;
    }
    
    public Card getMyPlay() {
        int[] lengths = new int[4];
        for (Card card : player.getHand()) {
            lengths[card.getSuitNumber() - 1]++;
        }

        List<Card> canPlay = core.whatCanIPlay(player);
        Hashtable<Card, Double> adjustedProbs = new Hashtable<>();
        HashMap<Card, HashMap<Card, List<Vector>>> ovlInsLose = new HashMap<>();
        HashMap<Card, HashMap<Card, List<Vector>>> ovlInsWin = new HashMap<>();
        HashMap<Card, List<Vector>> ivlIns = new HashMap<>();
        for (Card card : canPlay) {
            List<Vector> iIns = null;
            double probOfWinning = 0;
            if (core.cardCanWin(card)) {
                iIns = getIvlInputs(card);
                probOfWinning = ivl.testValue(iIns.get(0)).get(1).get(0);
            }
            ivlIns.put(card, iIns);

            ovlInsLose.put(card, new HashMap<>());
            ovlInsWin.put(card, new HashMap<>());
            lengths[card.getSuitNumber() - 1]--;
            double totalProb = 0;
            if (player.getHand().size() > 1) {
                double[] psLose = new double[player.getHand().size() - 1];
                double[] psWin = new double[player.getHand().size() - 1];
                int i = 0;
                for (Card card2 : player.getHand()) {
                    if (card2 != card) {
                        List<Vector> oInsLose = getOvlInputs(card2, lengths, false);
                        psLose[i] = ovl.testValue(oInsLose.get(0)).get(1).get(0);
                        ovlInsLose.get(card).put(card2, oInsLose);
                        List<Vector> oInsWin = getOvlInputs(card2, lengths, true);
                        psWin[i] = ovl.testValue(oInsWin.get(0)).get(1).get(0);
                        ovlInsWin.get(card).put(card2, oInsWin);
                        i++;
                    }
                }
                
                int iWant = wants(player);
                double probIfLose = iWant == player.getHand().size() ? 0 : subsetProb(psLose, iWant)[iWant];
                double probIfWin = iWant == 0 ? 0 : subsetProb(psWin, iWant - 1)[iWant - 1];
                totalProb = probOfWinning * probIfWin + (1 - probOfWinning) * probIfLose;
            }
            lengths[card.getSuitNumber() - 1]++;
            
            adjustedProbs.put(card, totalProb);
        }
        
        Card cardToPlay = chooseBestCard(adjustedProbs);
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            myPlayMemo = cardToPlay;
            ovlInsLoseMemo = ovlInsLose.get(cardToPlay);
            ovlInsWinMemo = ovlInsWin.get(cardToPlay);
            ivl.putIn(cardToPlay, ivlIns.get(cardToPlay));
        }
        
        if (debugPlay) {
            System.out.println("PLAYING: " + player.getName() + " -------------------------------------------");
            System.out.println("Play: " + cardToPlay);
            System.out.println("Prob. of making bid: " + adjustedProbs.get(cardToPlay));
            System.out.println("Hand: ");
            for (Card card : player.getHand()) {
                System.out.print(card + " ");
            }
            System.out.println();
            System.out.println("Trump: " + core.getTrump());
            System.out.println("OVL:");
            for (Card card2 : ovlInsLose.get(cardToPlay).keySet()) {
                System.out.println("Card " + card2);
                System.out.println("(Losing)");
                ovlInsLose.get(cardToPlay).get(card2).get(0).print();
                System.out.println("(Winning)");
                ovlInsWin.get(cardToPlay).get(card2).get(0).print();
            }
            System.out.println("IVL:");
            if (ivlIns.get(cardToPlay) == null) {
                System.out.println("null");
            } else {
                ivlIns.get(cardToPlay).get(0).print();
            }
            System.out.println();
        }
        
        return cardToPlay;
    }
    
    @Override
    public void addTrickData(Card winner, List<Card> trick) {
        if (aiTrainer != null && aiTrainer.backprop()) {
            if (winner == myPlayMemo) {
                for (Card card : player.getHand()) {
                    ovl.putIn(card, ovlInsWinMemo.get(card));
                }
                ovl.putOut(myPlayMemo, 1);
                ivl.putOut(myPlayMemo, 1);
            } else {
                for (Card card : player.getHand()) {
                    ovl.putIn(card, ovlInsLoseMemo.get(card));
                }
                ovl.putOut(myPlayMemo, 0);
                ivl.putOut(myPlayMemo, 0);
            }
        }
    }
    
    @Override
    public void endOfRound(int points) {
        if (aiTrainer != null && aiTrainer.backprop()) {
            bl.putOut(player.getIndex(), (double) points * 1.0 / 385.0 + 5.0 / 7.0);
        }
    }
    
    /**
     * 
     */
    public int[] chooseBestBid(double[] Es) {
        int[] ans = {-1, -1};
        
        double choice = r.nextDouble();
        if (choice < bidExploration) {
            ans[0] = r.nextInt(Es.length);
            do {
                ans[1] = r.nextInt(Es.length);
            } while (ans[1] == ans[0]);
        } else {
            double[] bestEs = {Integer.MIN_VALUE, Integer.MIN_VALUE};
            
            for (int k = 0; k < Es.length; k++) {
                if (Es[k] >= bestEs[0]) {
                    ans[1] = ans[0];
                    ans[0] = k;
                    bestEs[1] = bestEs[0];
                    bestEs[0] = Es[k];
                } else if (Es[k] >= bestEs[1]) {
                    ans[1] = k;
                    bestEs[1] = Es[k];
                }
            }
        }
        
        return ans;
    }
    
    /**
     * adjustedProbs maps each card in the hand to the estimated probability that it will make the
     * bid given it plays that card. Normally, we play the card with the highest said probability.
     * The reason I wrote this function is to experiment with other choice functions, e.g., one
     * that chooses randomly, weighting the cards by their probabilities.
     */
    public Card chooseBestCard(Hashtable<Card, Double> adjustedProbs) {
        Card maxCard = null;
        
        double choice = r.nextDouble();
        if (choice < playExploration) {
            Card[] hand = new Card[adjustedProbs.size()];
            adjustedProbs.keySet().toArray(hand);
            maxCard = hand[r.nextInt(hand.length)];
        } else {
            double max = Integer.MIN_VALUE;
            for (Card card : adjustedProbs.keySet()) {
                double value = adjustedProbs.get(card);
                if (value > max) {
                    max = value;
                    maxCard = card;
                }
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
    
    /**
     * Given the probabilities of a winning a trick with each card, p_1, p_2, ..., p_n, and an
     * integer l, this function computes the numbers q_0, q_1, ..., q_l, where q_k is the
     * probability of making exactly k tricks. This can be done in quadratic time with an 
     * algorithm similar to computing binomial coefficients.
     */
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
}