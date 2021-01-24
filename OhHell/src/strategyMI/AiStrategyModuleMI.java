package strategyMI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import core.AiStrategyModule;
import core.AiTrainer;
import core.Card;
import core.Deck;
import core.OhHellCore;
import core.Player;
import ml.SparseVector;
import ml.Vector;

public class AiStrategyModuleMI extends AiStrategyModule {
    private static boolean debug = false;
    
    private OhHellCore core;
    private List<Player> players;
    private Deck deck;
    private int maxH;
    private MacroValueLearner mvl;
    private ImmediateValueLearner ivl;
    
    private AiTrainer aiTrainer;
    
    public AiStrategyModuleMI(OhHellCore core, int N,
            MacroValueLearner mvl, ImmediateValueLearner ivl) {
        this.core = core;
        players = core.getPlayers();
        deck = core.getDeck();
        maxH = Math.min(10, 51 / N);
        this.mvl = mvl;
        this.ivl = ivl;
        aiTrainer = core.getAiTrainer();
    }
    
    public void reload(int N, MacroValueLearner mvl, ImmediateValueLearner ivl) {
        maxH = Math.min(10, 51 / N);
        this.mvl = mvl;
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
    
    public List<Vector> getMvlInputs(Card card) {
        int turn = player.getIndex();
        int M = players.size();
        Card trump = core.getTrump();
        
        List<List<Integer>> handSplit = split(
                player.getHand(), 
                new LinkedList<>(), 
                card, trump);
        List<List<Integer>> playedSplit = split(
                deck.getPlayedCards(), 
                Arrays.asList(players.stream().map(Player::getTrick).collect(Collectors.toList()), 
                        player.getHand()),
                null, trump);

        List<Vector> ins = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            SparseVector in = new SparseVector();
            
            in.addBinaryVector(handSplit.get(perms[i][0]), 13);
            in.addBinaryVector(handSplit.get(perms[i][1]), 13);
            in.addBinaryVector(handSplit.get(perms[i][2]), 13);
            in.addBinaryVector(handSplit.get(3), 13);
            
            in.addBinaryVector(playedSplit.get(perms[i][0]), 13);
            in.addBinaryVector(playedSplit.get(perms[i][1]), 13);
            in.addBinaryVector(playedSplit.get(perms[i][2]), 13);
            in.addBinaryVector(playedSplit.get(3), 13);
            
            for (int j = 0; j < M; j++) {
                Player iterPlayer = players.get((turn + j) % M);
                if (!iterPlayer.isKicked()) {
                    if (iterPlayer.hasBid()) {
                        in.addOneHot(wants(iterPlayer) + 1, maxH + 1);
                    } else {
                        in.addZeros(maxH + 1);
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
        
        List<List<Integer>> handSplit = split(
                Arrays.asList(card), 
                new LinkedList<>(), 
                null, trump);
        List<List<Integer>> playedSplit = split(
                deck.getPlayedCards(), 
                Arrays.asList(players.stream().map(Player::getTrick).collect(Collectors.toList()), 
                        player.getHand()),
                null, trump);

        List<Vector> ins = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            SparseVector in = new SparseVector();
            
            in.addBinaryVector(handSplit.get(perms[i][0]), 13);
            in.addBinaryVector(handSplit.get(perms[i][1]), 13);
            in.addBinaryVector(handSplit.get(perms[i][2]), 13);
            in.addBinaryVector(handSplit.get(3), 13);
            
            in.addBinaryVector(playedSplit.get(perms[i][0]), 13);
            in.addBinaryVector(playedSplit.get(perms[i][1]), 13);
            in.addBinaryVector(playedSplit.get(perms[i][2]), 13);
            in.addBinaryVector(playedSplit.get(3), 13);
            
            for (int k = 1; k < players.size(); k++) {
                Player iterPlayer = players.get((turn + k) % M);
                if (!iterPlayer.isKicked()) {
                    if ((turn - core.getLeader() + M) % M + k < M) {
                        in.addOneHot(wants(iterPlayer) + 1, maxH + 1);
                    } else {
                        in.addZeros(maxH + 1);
                    }
                }
            }

            ins.add(in);
        }
        
        return ins;
    }
    
    public List<Integer> cardsToNumbers(List<Card> cards, List<Card> additionalCards, Card toIgnore) {
        List<Integer> numbers = new LinkedList<>();
        for (Card card : cards) {
            if (card != toIgnore && !card.isEmpty()) {
                numbers.add(card.toNumber() + 1);
            }
        }
        for (Card card : additionalCards) {
            if (!card.isEmpty()) {
                numbers.add(card.toNumber() + 1);
            }
        }
        return numbers;
    }
    
    public List<List<Integer>> split(List<Card> cards, List<List<Card>> additionalCards, Card toIgnore, Card trump) {
        List<List<Integer>> ans = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ans.add(new LinkedList<>());
        }
        for (Card card : cards) {
            if (card != toIgnore && !card.isEmpty()) {
                ans.get(card.getSuitNumber() - 1).add(card.getNum() - 1);
            }
        }
        for (List<Card> moreCards : additionalCards) {
            for (Card card : moreCards) {
                if (!card.isEmpty()) {
                    ans.get(card.getSuitNumber() - 1).add(card.getNum() - 1);
                }
            }
        }
        ans.add(ans.remove(trump.getSuitNumber() - 1));
        return ans;
    }
    
    public int wants(Player player) {
        return Math.max(
                Math.min(player.getBid() - player.getTaken(), player.getHand().size()), 
                0);
    }
    
    public int getMyBid() {
        List<Vector> ins = getMvlInputs(null);
        double[] qs = mvl.testValue(ins.get(0)).get(1).toArray();
        /*double total = 0;
        for (int i = 0; i <= player.getHand().size(); i++) {
            total += qs[i];
        }
        for (int i = 0; i <= player.getHand().size(); i++) {
            qs[i] /= total;
        }*/
        //core.sendChat(player.getName() + ": " + Arrays.toString(qs));
        
        int[] bestBids = optimalBid(qs, player.getHand().size());

        if (debug) {
            System.out.println("BIDDING: " + player.getName() + " ===========================================");
            System.out.println("Hand: ");
            for (Card card : player.getHand()) {
                System.out.print(card + " ");
            }
            System.out.println();
            System.out.println("Trump: " + core.getTrump());
            for (int i = 0; i < 52; i++) {
                System.out.print(new Card(i) + "");
            }
            System.out.println();
            System.out.println("MVL:");
            for (Vector in : ins) {
                in.print();
            }
            System.out.println("Input qs: ");
            for (int i = 0; i <= player.getHand().size(); i++) {
                System.out.println(i + ": " + qs[i]);
            }
            System.out.println("Bid: " + bestBids[0] + " (" + bestBids[1] + ")");
            System.out.println();
        }
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            mvl.putIn(player.getIndex(), ins, 0);
        }
        
        return bestBids[0] != core.whatCanINotBid() ? bestBids[0] : bestBids[1];
    }
    
    public Card getMyPlay() {
        List<Card> canPlay = core.whatCanIPlay(player);
        
        if (debug) {
            System.out.println("PLAYING: " + player.getName() + " -------------------------------------------");
            System.out.println("Hand: ");
            for (Card card : player.getHand()) {
                System.out.print(card + " ");
            }
            System.out.println();
            System.out.println("Trump: " + core.getTrump());
        }
        
        Hashtable<Card, Double> adjustedProbs = new Hashtable<>();
        HashMap<Card, List<Vector>> mvlIns = new HashMap<>();
        HashMap<Card, List<Vector>> ivlIns = new HashMap<>();
        for (Card card : canPlay) {
            List<Vector> iIns = null;
            double probOfWinning = 0;
            
            if (core.cardWinning(card)) {
                iIns = getIvlInputs(card);
                
                probOfWinning = ivl.testValue(iIns.get(0)).get(1).get(0);
            }
            ivlIns.put(card, iIns);
            
            List<Vector> mIns = null;
            double totalProb = 0;
            if (player.getHand().size() > 1) {
                mIns = getMvlInputs(card);
                double[] qs = mvl.testValue(mIns.get(0)).get(1).toArray();
                /*double total = 0;
                for (int i = 0; i <= player.getHand().size(); i++) {
                    total += qs[i];
                }
                for (int i = 0; i <= player.getHand().size(); i++) {
                    qs[i] /= total;
                }*/
                
                int iWant = wants(player);
                double probIfWin = iWant == 0 ? 0 : qs[iWant - 1];
                double probIfLose = iWant == player.getHand().size() ? 0 : qs[iWant];
                totalProb = probOfWinning * probIfWin + (1 - probOfWinning) * probIfLose;
            }
            mvlIns.put(card, mIns);
            
            adjustedProbs.put(card, totalProb);
            
            if (debug) {
                System.out.println("Card: " + card);
                if (mIns != null && mIns.get(0) != null) {
                    for (int i = 0; i < 52; i++) {
                        System.out.print(new Card(i) + "");
                    }
                    System.out.println();
                    System.out.println("MVL:");
                    for (Vector in : mIns) {
                        in.print();
                    }
                }
                if (iIns != null && iIns.get(0) != null) {
                    for (int i = 0; i < 52; i++) {
                        System.out.print(new Card(i) + "");
                    }
                    System.out.println();
                    System.out.println("IVL:");
                    for (Vector in : iIns) {
                        in.print();
                    }
                }
                /*System.out.println("Input qs: ");
                for (int i = 0; i <= player.getHand().size(); i++) {
                    System.out.println(i + ": " + qs[i]);
                }*/
                System.out.println("Value: " + totalProb);
                System.out.println();
            }
        }
        
        Card cardToPlay = chooseBestCard(adjustedProbs);
        
        if (debug) {
            System.out.println("Play: " + cardToPlay);
            System.out.println();
        }
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            mvl.putIn(player.getIndex(), mvlIns.get(cardToPlay), player.getTaken());
            ivl.putIn(cardToPlay, ivlIns.get(cardToPlay));
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
        double max = Integer.MIN_VALUE;
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
            for (Card card : trick) {
                ivl.putOut(card, card == winner ? 1 : 0);
            }
        }
    }
    
    @Override
    public void endOfRound(int points) {
        if (aiTrainer != null && aiTrainer.backprop()) {
            mvl.putOut(player.getIndex(), player.getTaken(), maxH);
        }
    }
    
    /**
     * Given the probabilities of winning a trick with each card, p_1, p_2, ..., p_n, this
     * function computes the bid that maximizes expected points. First compute q_0, q_1, ..., q_n
     * using subsetProb, and then compute the maximum dot product q.s_k, where s_k is the vector of 
     * scores: s_kl = points gotten from bidding k and making l. The entries of s_k are almost
     * quadratic; for k >= 2, they satisfy the equation
     *      s_k = 2s_(k-1) - s_(k-2) - [5] + t_k,
     * where [5] is the vector of all 5s and t_k is a vector with exactly three nonzero entries, 
     * namely at k-2, k-1, and k. This explains the strange recursive algorithm here.
     */
    public static int[] optimalBid(double[] qs, int h) {
        int n = h;
        
        double[] E = {0, 0};
        for (int k = 0; k <= 1; k++) {
            for (int l = 0; l <= n; l++) {
                double points = 0;
                int diff = Math.abs(k - l);
                if (diff == 0) {
                    points = 10 + k * k;
                } else {
                    points = -5 * diff * (diff + 1) / 2;
                }
                E[k] += qs[l] * points;
            }
        }
        
        /*if (debug) {
            System.out.println("E0: " + E[0]);
            System.out.println("E1: " + E[1]);
        }*/

        int[] ans;
        if (E[0] < E[1]) {
            ans = new int[] {1, 0};
        } else {
            ans = new int[] {0, 1};
        }
        double[] bestEs = {E[ans[0]], E[ans[1]]};
        
        for (int k = 2; k <= n; k++) {
            double newE = 
                    E[1] * 2
                    - E[0]
                    - 5
                    + qs[k - 2] * (14 - 4 * k + k * k)
                    + qs[k - 1] * (-27 + 4 * k - 2 * k * k)
                    + qs[k] * (10 + k * k);
            if (newE > bestEs[0]) {
                ans[1] = ans[0];
                ans[0] = k;
                bestEs[1] = bestEs[0];
                bestEs[0] = newE;
            } else if (newE > bestEs[1]) {
                ans[1] = k;
                bestEs[1] = newE;
            }
            E = new double[] {E[1], newE};
            /*if (debug) {
                System.out.println("E" + k + ": " + newE);
            }*/
        }
        return ans;
    }
}