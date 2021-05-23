package strategyOITeam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import core.AiStrategyModule;
import core.AiTrainer;
import core.Card;
import core.IllegalAIStateException;
import core.OhHellCore;
import core.Player;
import ml.SparseVector;
import ml.Vector;

public class AiStrategyModuleOITeam extends AiStrategyModule {
    OhHellCore.CoreData coreData;
    private int maxH;
    private int maxCancels;
    private OverallValueLearner ovl;
    private ImmediateValueLearner ivl;
    private TeammateTakesLearner ttl;
    private AiTrainer aiTrainer;
    
    public AiStrategyModuleOITeam(int N, OhHellCore.CoreData coreData, OverallValueLearner ovl, ImmediateValueLearner ivl, TeammateTakesLearner ttl, AiTrainer aiTrainer) {
        this.coreData = coreData;
        maxH = Math.min(10, (52 * coreData.getD() - 1) / N);
        maxCancels = (N - 1) / 2;
        this.ovl = ovl;
        this.ivl = ivl;
        this.ttl = ttl;
        this.aiTrainer = aiTrainer;
    }
    
    // Base functions
    
    @Override
    public int makeBid() {
        //double[] qs = getQs(player.getHand().size(), null, (coreData.getDealer() + 1) % N);
        //int myBid = chooseBid(qs);
        
        int myBid = new java.util.Random().nextInt(player.getHand().size() + 1);
        
        return myBid;
    }
    
    @Override
    public Card makePlay() {
        Map<Card, Double> makingProbs = getMakingProbs();
        Card myPlay = choosePlay(makingProbs);
        
        if (myPlay == null) {
            throw new IllegalAIStateException("Failed to choose a card.");
        }
        
        return myPlay;
    }
    
    // Probability estimates
    
    public Map<Card, Double> getMakingProbs() {
        List<Card> canPlay = coreData.whatCanIPlay(player);
        
        Map<Card, Double> makingProbs = new HashMap<>();
        for (Card card : canPlay) {
            makingProbs.put(card, prMakingIfPlayCard(card));
        }
        
        return makingProbs;
    }
    
    public double prMakingIfPlayCard(Card card) {
        double sum = 0;
        
        Integer[] requiredCancels = coreData.cancelsRequired(player, card);
        
        double[] p1s = prPlayersWinTrick(card, requiredCancels);
        
        for (int i : coreData.getIndicesRelativeTo(0)) {
            double p2 = prMakingIfPlayerWinsTrick(card, i);
            sum += p1s[i] * p2;
        }
        
        return sum;
    }
    
    public double[] prPlayersWinTrick(Card card, Integer[] requiredCancels) {
        Vector in = getIvlIn(card, requiredCancels);
        return ivl.testValue(in).get(1).toArray();
    }

    public double prMakingIfPlayerWinsTrick(Card card, int index) {
        List<Integer> myTeam;
        int teamWants;
        
        if (coreData.isTeams()) {
            myTeam = coreData.getTeam(player.getTeam());
            teamWants = clip(
                    coreData.getTeamData(player.getTeam()).getBid()
                        - coreData.getTeamData(player.getTeam()).getTaken()
                        - (myTeam.contains(index) ? 1 : 0),
                    0, player.getHand().size());
        } else {
            myTeam = Arrays.asList(player.getIndex());
            teamWants = clip(
                    player.getBid() 
                        - player.getTaken()
                        - (player.getIndex() == index ? 1 : 0),
                    0, player.getHand().size());
        }
        
        clearQsMemo();
        
        // Iterate through all combinations of takens that add to wants
        double p = 0;
        Double[][] probMemo = new Double[myTeam.size()][teamWants];
        for (Partition part = new Partition(myTeam.size(), teamWants); !part.isEnd(); part.increment()) {
            double term = 1;
            for (int i = 0; i < myTeam.size(); i++) {
                int toTake = part.getValue(i);
                if (probMemo[i][toTake] == null) {
                    probMemo[i][toTake] = prPlayerTakesExactly(myTeam.get(i), toTake, teamWants, card, index);
                }
                term *= probMemo[i][toTake];
            }
            p += term;
        }
        
        return p;
    }
    
    private double[] qsMemo = null;
    public void clearQsMemo() {
        qsMemo = null;
    }
    
    public double prPlayerTakesExactly(int index, int toTake, int maxForMemo, Card card, int prevIndex) {
        if (index == player.getIndex()) {
            if (qsMemo == null) {
                qsMemo = getQs(maxForMemo, card, prevIndex);
            }
            return qsMemo[toTake];
        } else {
            Vector in = getTtlIn(index, toTake, card, prevIndex);
            
            if (aiTrainer != null && aiTrainer.backprop()) {
                //ttl.putIn(index, coreData.getPlayerData(index).getTaken(), in); TODO
            }
            
            return 0;//ttl.testValue(in).get(1).get(0); TODO
        }
    }
    
    public double[] getQs(int max, Card card, int index) {
        double[] ps = getPs(card, index);
        return subsetProb(ps, max);
    }
    
    public double[] getPs(Card card, int index) {
        double[] ps = new double[player.getHand().size()];
        
        int l = 0;
        for (Card c : player.getHand()) {
            Vector in = getOvlIn(c, card, index);
            ps[l] = ovl.testValue(in).get(1).get(0);
            
            if (aiTrainer != null && aiTrainer.backprop()) {
                ovl.putIn(c, in);
            }
            
            l++;
        }

        return ps;
    }
    
    // Neural network functions
    
    public Vector getIvlIn(Card myCard, Integer[] requiredCancels) {
        SparseVector vec = new SparseVector();
        
        // Some basic data
        boolean iAmLeading = player.getIndex() == coreData.getLeader();
        Card trump = coreData.getTrump();
        Card lead = iAmLeading ? myCard : coreData.getPlayerData(coreData.getLeader()).getTrick();
        
        List<Card> myHand = player.getHand();
        List<Card> trick = coreData.getTrick();
        List<List<Card>> additionalPlayeds = Arrays.asList(myHand, trick);
        
        // Features
        vec.addOneHot("Initial hand size", coreData.getRoundDetails().getHandSize(), 1, maxH);
        vec.addOneHot("Current hand size", player.getHand().size() - 1, 0, maxH - 1);
        vec.addOneHot(
                "Trump unseen", 
                coreData.cardsLeftOfSuit(trump, additionalPlayeds), 
                0, 13 * coreData.getD() - 1);
        vec.addOneHot(
                "Led suit unseen", 
                coreData.cardsLeftOfSuit(lead, additionalPlayeds), 
                0, 13 * coreData.getD() - 1);
        vec.addOneHot("Lead is trump", lead.getSuit() == coreData.getTrump().getSuit() ? 1 : 0, 0, 1);
        int j = 0;
        for (int i : coreData.getIndicesRelativeTo(player.getIndex())) {
            Player.PlayerData playerData = coreData.getPlayerData(i);
            
            int bidFeat = 0;
            int takenFeat = 0;
            int teamWantsFeat = 0;
            int trumpVoidFeat = 0;
            int leadVoidFeat = 0;
            int isTrumpFeat = 0;
            int adjustedNumberFeat = 0;
            int matchingCardsLeftFeat = 0;
            int requiredCancelsFeat = requiredCancels[i];
            int ledFeat = 0;
            
            if (!(playerData.hasPlayed() || i == player.getIndex())) {
                bidFeat = playerData.getBid();
                takenFeat = playerData.getTaken();
                teamWantsFeat = coreData.teamWants(playerData.getTeam());
                trumpVoidFeat = playerData.shownOut(trump.getSuit()) ? 1 : 0;
                leadVoidFeat = playerData.shownOut(lead.getSuit()) ? 1 : 0;
            } else {
                Card card = i == player.getIndex() ? myCard : playerData.getTrick();
                isTrumpFeat = card.getSuit() == trump.getSuit() ? 1 : 0;
                adjustedNumberFeat = coreData.adjustedCardValue(card, additionalPlayeds);
                matchingCardsLeftFeat = coreData.matchingCardsLeft(card, additionalPlayeds);
                ledFeat = card == lead ? 1 : 0;
            }

            vec.addOneHot(j + " Bid", bidFeat, 0, maxH);
            vec.addOneHot(j + " Taken", takenFeat, 0, maxH - 1);
            vec.addOneHot(j + " Team wants", teamWantsFeat, 0, maxH);
            vec.addOneHot(j + " Trump void", trumpVoidFeat, 0, 1);
            vec.addOneHot(j + " Lead void", leadVoidFeat, 0, 1);
            vec.addOneHot(j + " Is trump", isTrumpFeat, 0, 1);
            vec.addOneHot(j + " Adjusted number", adjustedNumberFeat, 0, 13 * coreData.getD());
            vec.addOneHot(j + " Matches unseen", matchingCardsLeftFeat, 0, coreData.getD());
            vec.addOneHot(j + " Required cancels", requiredCancelsFeat, -2, maxCancels);
            vec.addOneHot(j + " Led", ledFeat, 0, 1);
            j++;
        }
        
        vec.print();
        
        return vec;
    }
    
    public Vector getOvlIn(Card card, Card myCard, int leaderIndex) {
        return null; //TODO
    }
    
    public Vector getTtlIn(int index, int toTake, Card myCard, int leaderIndex) {
        return null; //TODO
    }
    
    // Final decision makers
    
    public int chooseBid(double[] qs) {
        int[] bids = orderBids(qs);
        
        // Find the top choice bid that will not make the team overbid or force the dealer to 
        // overbid if the dealer is on the team (note to self (TODO) -- this dealer thing may not
        // be necessary in all cases. Maybe we could want to bid to the max even if the dealer is
        // on our team because we know the other team will bid in between?).
        int maxBid = coreData.highestMakeableBid(player, true);
        
        int choice = 0;
        while (bids[choice] > maxBid) {
            choice++;
        }
        
        // If we can't bid that, then move one further
        if (bids[choice] == coreData.whatCanINotBid(player)) {
            choice++;
        }
        
        if (choice < bids.length) {
            return bids[choice];
        } else {
            // I think this can happen only when (1) bidding 0 was our last choice, (2) we 
            // couldn't bid it because we're the dealer, and (3) all other teams bid 0, and (4)
            // someone on our team fucked us. Extremely unlikely scenario. Bid 1.
            return 1;
        }
    }
    
    /**
     * This function computes the bid that maximizes expected points given q_0, q_1, ..., q_n.
     * Compute the maximum dot product q.s_k, where s_k is the vector of scores: s_kl = points 
     * gotten from bidding k and making l. The entries of s_k are almost quadratic; for k >= 2, 
     * they satisfy the equation
     *      s_k = 2s_(k-1) - s_(k-2) - [5] + t_k,
     * where [5] is the vector of all 5s and t_k is a vector with exactly three nonzero entries, 
     * namely at k-2, k-1, and k. This explains the strange recursive algorithm here.
     */
    public static int[] orderBids(double[] qs) {
        int n = qs.length - 1;

        double[][] bidEPairs = new double[n + 1][2];
        
        for (int k = 0; k <= 1; k++) {
            for (int l = 0; l <= n; l++) {
                bidEPairs[k][0] = k;
                bidEPairs[k][1] += qs[l] * points(k, l);
            }
        }
        
        for (int k = 2; k <= n; k++) {
            bidEPairs[k][0] = k;
            bidEPairs[k][1] = 
                    bidEPairs[k - 1][1] * 2
                    - bidEPairs[k - 2][1]
                    - 5
                    + qs[k - 2] * (14 - 4 * k + k * k)
                    + qs[k - 1] * (-27 + 4 * k - 2 * k * k)
                    + qs[k] * (10 + k * k);
        }
        
        Arrays.sort(bidEPairs, (pair1, pair2) -> (int) Math.signum(pair2[1] - pair1[1]));
        
        int[] ans = new int[n + 1];
        for (int k = 0; k <= n; k++) {
            ans[k] = (int) bidEPairs[k][0];
        }
        
        return ans;
    }
    
    public Card choosePlay(Map<Card, Double> makingProbs) {
        double bestProb = -1;
        List<Card> bestPlays = new LinkedList<>();
        
        for (Entry<Card, Double> pair : makingProbs.entrySet()) {
            if (pair.getValue() > bestProb) {
                bestProb = pair.getValue();
                bestPlays.clear();
            }
            if (pair.getValue() == bestProb) {
                bestPlays.add(pair.getKey());
            }
        }
        
        return bestPlays.get(coreData.getRandom().nextInt(bestPlays.size()));
    }
    
    // Utility
    
    public int clip(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
    
    private class Partition {
        private int[] values;
        private int partial;
        private int sum;
        private boolean end;
        
        public Partition(int length, int sum) {
            values = new int[length - 1];
            this.sum = sum;
            end = length == 1 || sum == 0;
        }
        
        public int getValue(int index) {
            if (index < values.length) {
                return values[index];
            } else {
                return sum - partial;
            }
        }
        
        public void increment() {
            if (partial < sum) {
                partial++;
                values[0]++;
            } else {
                int i;
                for (i = 0; i < values.length && values[i] == 0; i++);
                partial -= values[i];
                values[i] = 0;
                if (i < values.length - 1) {
                    partial++;
                    values[i + 1]++;
                } else {
                    end = true;
                }
            }
        }
        
        public boolean isEnd() {
            return end;
        }
        
        @Override
        public String toString() {
            String ans = "[";
            for (int i : values) {
                ans += i + ", ";
            }
            ans += (sum - partial) + "]";
            return ans;
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
    
    public static double points(int bid, int took) {
        if (bid == took) {
            return 10 + bid * bid;
        } else {
            int diff = Math.abs(bid - took);
            return -5.0 * diff * (diff + 1) / 2.0;
        }
    }
}
