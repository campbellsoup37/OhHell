import java.util.Hashtable;
import java.util.HashMap;
import java.util.List;
//import java.util.Random;

public class AiThread extends Thread {
    OhHellCore core;
    OverallValueLearner ovl;
    ImmediateValueLearner ivl;
    boolean training;
    
    boolean running = true;
    
    private enum Task {
        BID,
        PLAY,
        END
    }
    int waitTime = 0;
    Task task;
    AiPlayer player;
    
    public AiThread(OhHellCore core, int N, boolean training, OverallValueLearner ovl, ImmediateValueLearner ivl) {
        this.core = core;
        this.training = training;
        if (training) {
            this.ovl = ovl;
            this.ivl = ivl;
        } else {
            loadOvlIvl(N);
        }
    }
    
    public void loadOvlIvl(int N) {
        int maxH = Math.min(10, 51 / N);
        
        ovl = new OverallValueLearner(new int[] {
                maxH              // Cards left in hand
                + (maxH + 1) * N  // Bids - takens
                + 4               // Number of voids
                + 13              // Trump left
                
                + 2               // Card is trump
                + 13              // That suit left
                + 13,             // Card's adjusted number
                40,               // Hidden layer
                1                 // Card's predicted value
        });
        ovl.openFromFile("resources/OhHellAIModels/" + "ovlN" + N + ".txt");
        ivl = new ImmediateValueLearner(new int[] {
                (maxH + 1) * (N - 1) // Bids - takens
                + 13                 // Trump left
                
                + 2                  // Trump was led
                + 13                 // Led suit left
                
                + 2                  // Card is trump
                + 13,                // Card's adjusted number
                30,                  // Hidden layer
                1                    // Card's predicted value
        });
        ivl.openFromFile("resources/OhHellAIModels/" + "ivlN" + N + ".txt");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void end() {
        task = Task.END;
        interrupt();
    }
    
    public void run() {
        while (running) {
            try {
                while (true) {
                    sleep(100);
                }
            } catch (InterruptedException e) {
                try {
                    sleep(waitTime);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                
                switch (task) {
                case BID:
                    // OIBot
                    ////System.out.println(player.getName() + " bidding: -------------------------------------------");
                    double[] ps = getOvlPs(player);
                    int myBid = getMyBid(ps);

                    //core.sendChat(player.getName() + ": I have a " + String.format("%.2f", 100 * subsetProb(ps, myBid)) + "% chance of making this.");
                    core.incomingBid(player, myBid);
                    break;
                case PLAY:
                    // OIBot
                    //System.out.println(player.getName() + " playing: -------------------------------------------");
                    Card cardToPlay = getMyPlay(player);
                    
                    core.incomingPlay(player, cardToPlay);
                    break;
                case END:
                    running = false;
                    break;
                }
            }
        }
    }
    
    public double[] getOvlPs(Player player) {
        double[] ps = new double[player.getHand().size()];
        int l = 0;
        for (Card c : player.getHand()) {
            BinaryLayerVector in = new BinaryLayerVector();
            core.addOvlInput(in, c, 0, 0);
            ps[l] = ovl.testValue(in).get(1).get(0);
            
            if (training) {
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
    
    public Card getMyPlay(Player player) {
        List<Card> canPlay = core.whatCanIPlay(player);
        List<List<Card>> split = OhHellCore.splitBySuit(player.getHand());
        
        boolean singleton = false;
        for (List<Card> suits : split) {
            if (suits.size() == 1) {
                singleton = true;
            }
        }
        boolean canPlaySingleton = canPlay.size() == player.getHand().size() && singleton || canPlay.size() == 1;

        Hashtable<Card, Double> ovlValsNonsingleton = new Hashtable<>();
        Hashtable<Card, Double> ovlValsSingleton = new Hashtable<>();

        Hashtable<Card, LayerVector> ovlInsNonsingleton = new Hashtable<>();
        Hashtable<Card, LayerVector> ovlInsSingleton = new Hashtable<>();
        
        if (player.getHand().size() > 1) {
            for (Card card : player.getHand()) {
                BinaryLayerVector in = new BinaryLayerVector();
                core.addOvlInput(in, card, 1, 0);
                ovlValsNonsingleton.put(card, ovl.testValue(in).get(1).get(0));
                ovlInsNonsingleton.put(card, in);
                
                //System.out.println(card + " (pre)");
                //in.print();
                //System.out.println();
                
                if (canPlaySingleton) {
                    BinaryLayerVector inS = new BinaryLayerVector();
                    core.addOvlInput(inS, card, 1, 1);
                    ovlValsSingleton.put(card, ovl.testValue(inS).get(1).get(0));
                    ovlInsSingleton.put(card, inS);
                }
            }
        } else {
            ovlValsSingleton.put(player.getHand().get(0), 0.0);
        }
        
        Hashtable<Card, Double> adjustedProbs = new Hashtable<>();
        HashMap<Card, LayerVector> ivlIns = new HashMap<>();
        for (Card card : canPlay) {
            BinaryLayerVector in = null;
            double probOfWinning = 0;
            
            //System.out.println(card);
            if (core.cardIsWinning(card)) {
                in = new BinaryLayerVector();
                core.addIvlInput(in, card);
                
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
        LayerVector inToPlay = ivlIns.get(cardToPlay);
        
        if (training) {
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
    
    public void addOuts(Card winner, List<Card> trick) {
        if (training) {
            for (Card card : trick) {
                ovl.putOut(card, card == winner ? 1 : 0);
                ivl.putOut(card, card == winner ? 1 : 0);
            }
        }
    }
    
    public void makeBid(AiPlayer player) {
        task = Task.BID;
        this.player = player;
        interrupt();
    }
    
    public void makePlay(AiPlayer player) {
        task = Task.PLAY;
        this.player = player;
        interrupt();
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