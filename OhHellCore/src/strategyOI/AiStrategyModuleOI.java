package strategyOI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import ml.SparseVector;
import ml.Vector;
import ohHellCore.AiStrategyModule;
import ohHellCore.AiTrainer;
import ohHellCore.Card;
import ohHellCore.Deck;
import ohHellCore.OhHellCore;
import ohHellCore.Player;

public class AiStrategyModuleOI extends AiStrategyModule {
    private OhHellCore core;
    private List<Player> players;
    private Deck deck;
    private int maxH;
    private OverallValueLearner ovl;
    private ImmediateValueLearner ivl;
    
    private AiTrainer aiTrainer;
    
    public AiStrategyModuleOI(OhHellCore core, int N,
            OverallValueLearner ovl, ImmediateValueLearner ivl) {
        this.core = core;
        players = core.getPlayers();
        deck = core.getDeck();
        maxH = Math.min(10, 51 / N);
        this.ovl = ovl;
        this.ivl = ivl;
        aiTrainer = core.getAiTrainer();
    }
    
    public void reload(int N, OverallValueLearner ovl, ImmediateValueLearner ivl) {
        maxH = Math.min(10, 51 / N);
        this.ovl = ovl;
        this.ivl = ivl;
    }
    
    @Override
    public void makeBid() {
        double[] ps = getOvlPs();
        int myBid = getMyBid(ps);

        core.incomingBid(player, myBid);
    }
    
    @Override
    public void makePlay() {
        Card cardToPlay = getMyPlay();
        
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
    
    /*public boolean cardIsWinning(Card card) {
        for (Player p : players) {
            System.out.print(card + " > " + p.getTrick() + "? ");
            if (!p.isKicked() && !card.isGreaterThan(p.getTrick(), core.getTrump().getSuit())) {
                System.out.println("false");
                return false;
            }
            System.out.println("true");
        }
        return true;
    }*/
    
    public double[] getOvlPs() {
        double[] ps = new double[player.getHand().size()];
        int l = 0;
        for (Card c : player.getHand()) {
            SparseVector in = new SparseVector();
            addOvlInput(in, c, 0, 0);
            ps[l] = ovl.testValue(in).get(1).get(0);
            
            if (aiTrainer != null && aiTrainer.backprop()) {
                ovl.putIn(c, in);
            }
            
            l++;
        }

        return ps;
    }
    
    public int getMyBid(double[] ps) {
        int[] bestBids = optimalBid(ps);
        return bestBids[0] != core.whatCanINotBid() ? bestBids[0] : bestBids[1];
    }
    
    public Card getMyPlay() {
        List<Card> canPlay = core.whatCanIPlay(player);
        List<List<Card>> split = splitBySuit(player.getHand());
        
        boolean singleton = false;
        for (List<Card> suits : split) {
            if (suits.size() == 1) {
                singleton = true;
            }
        }
        boolean canPlaySingleton = canPlay.size() == player.getHand().size() && singleton || canPlay.size() == 1;

        Hashtable<Card, Double> ovlValsNonsingleton = new Hashtable<>();
        Hashtable<Card, Double> ovlValsSingleton = new Hashtable<>();

        Hashtable<Card, Vector> ovlInsNonsingleton = new Hashtable<>();
        Hashtable<Card, Vector> ovlInsSingleton = new Hashtable<>();
        
        if (player.getHand().size() > 1) {
            for (Card card : player.getHand()) {
                SparseVector in = new SparseVector();
                addOvlInput(in, card, 1, 0);
                ovlValsNonsingleton.put(card, ovl.testValue(in).get(1).get(0));
                ovlInsNonsingleton.put(card, in);
                
                //System.out.println(card + " (pre)");
                //in.print();
                //System.out.println();
                
                if (canPlaySingleton) {
                    SparseVector inS = new SparseVector();
                    addOvlInput(inS, card, 1, 1);
                    ovlValsSingleton.put(card, ovl.testValue(inS).get(1).get(0));
                    ovlInsSingleton.put(card, inS);
                }
            }
        } else {
            ovlValsSingleton.put(player.getHand().get(0), 0.0);
        }
        
        Hashtable<Card, Double> adjustedProbs = new Hashtable<>();
        HashMap<Card, Vector> ivlIns = new HashMap<>();
        for (Card card : canPlay) {
            SparseVector in = null;
            double probOfWinning = 0;
            
            //System.out.println(card);
            if (core.cardWinning(card)) {
                in = new SparseVector();
                addIvlInput(in, card);
                
                probOfWinning = ivl.testValue(in).get(1).get(0);
                
                //in.print();
            }
            //System.out.println();
            
            Hashtable<Card, Double> ovlVals = split.get(card.getSuitNumber() - 1).size() == 1 ? ovlValsSingleton : ovlValsNonsingleton;
            
            double temp = ovlVals.get(card);
            ovlVals.put(card, probOfWinning);
            double[] pps = new double[player.getHand().size()];
            int ll = 0;
            for (Double v : ovlVals.values()) {
                pps[ll] = v;
                ll++;
            }
            int l = Math.max(player.getBid() - player.getTaken(), 0);
            double value = subsetProb(pps, l)[l];
            ovlVals.put(card, temp);
            
            adjustedProbs.put(card, value);
            ivlIns.put(card, in);
        }
        
        Card cardToPlay = chooseBestCard(adjustedProbs);
        Vector inToPlay = ivlIns.get(cardToPlay);
        
        if (aiTrainer != null && aiTrainer.backprop()) {
            for (Card card : player.getHand()) {
                if (card != cardToPlay) {
                    if (split.get(cardToPlay.getSuitNumber() - 1).size() == 1) {
                        ovl.putIn(card, ovlInsSingleton.get(card));
                    } else {
                        ovl.putIn(card, ovlInsNonsingleton.get(card));
                    }
                } else if (inToPlay != null) {
                    ivl.putIn(card, inToPlay);
                }
            }
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
            for (Card card : trick) {
                ovl.putOut(card, card == winner ? 1 : 0);
                ivl.putOut(card, card == winner ? 1 : 0);
            }
        }
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
    public static int[] optimalBid(double[] ps) {
        int n = ps.length;
        double[] qs = subsetProb(ps, n);
        
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
        }
        return ans;
    }
}
