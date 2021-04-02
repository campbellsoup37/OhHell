package aiworkshop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.GameOptions;
import core.OhHellCore;
import core.Player;

public class AiComparer extends AiTrainer {
    public void run() {
        int N = 5;
        int reps = 1000000;
        
        int toPrint = 1;
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = Arrays.asList(
//                new strategyRBP.AiStrategyModuleRBP(core, N,
//                        new strategyRBP.BiddingLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/bl.txt"),
//                        new strategyRBP.OverallValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ovl.txt"),
//                        new strategyRBP.ImmediateValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ivl.txt")),
//                new strategyRBP.AiStrategyModuleRBP(core, N,
//                        new strategyRBP.BiddingLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/bl.txt"),
//                        new strategyRBP.OverallValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ovl.txt"),
//                        new strategyRBP.ImmediateValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ivl.txt")),
//                new strategyRBP.AiStrategyModuleRBP(core, N,
//                        new strategyRBP.BiddingLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/bl.txt"),
//                        new strategyRBP.OverallValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ovl.txt"),
//                        new strategyRBP.ImmediateValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ivl.txt")),
//                new strategyRBP.AiStrategyModuleRBP(core, N,
//                        new strategyRBP.BiddingLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/bl.txt"),
//                        new strategyRBP.OverallValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ovl.txt"),
//                        new strategyRBP.ImmediateValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ivl.txt")),
//                new strategyRBP.AiStrategyModuleRBP(core, N,
//                        new strategyRBP.BiddingLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/bl.txt"),
//                        new strategyRBP.OverallValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ovl.txt"),
//                        new strategyRBP.ImmediateValueLearner("resources/ai workshop/OhHellAiModels/RBP/b100o10i40/5/ivl.txt")),
                
                new strategyOI.AiStrategyModuleOI(core, N, 
                        new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt"), 
                        new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt")),
                new strategyOI.AiStrategyModuleOI(core, N, 
                        new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt"), 
                        new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt")),
                new strategyOI.AiStrategyModuleOI(core, N, 
                        new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt"), 
                        new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt")),
                new strategyOI.AiStrategyModuleOI(core, N, 
                        new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt"), 
                        new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt")),
                new strategyOI.AiStrategyModuleOI(core, N, 
                        new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt"), 
                        new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt"))
        );
        
        double[] means = new double[N];
        double[] vars = new double[N];
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(new GameOptions(N), aiStrategyModules);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                
            }
            
            players = getPlayers();
            
            double winningScore = Integer.MIN_VALUE;
            Hashtable<AiStrategyModule, Integer> scoreMap = new Hashtable<>();
            for (Player player : players) {
                scoreMap.put(player.getAiStrategyModule(), player.getScore());
                winningScore = Math.max(winningScore, player.getScore());
            }
            
            int i = 0;
            for (AiStrategyModule aiStrategyModule : aiStrategyModules) {
                double x = scoreMap.get(aiStrategyModule);
                double prevMean = means[i];
                means[i] = (means[i] * (g - 1) + x) / g;
                if (g >= 2) {
                    vars[i] = (vars[i] * (g - 2) + Math.pow(x - means[i], 2)) / (g - 1) + Math.pow(prevMean - means[i], 2);
                }
                i++;
            }
            
//            int i;
//            for (i = 0; i < players.size(); i++) {
//                double x = (players.get(i).getScore() == winningScore ? 1.0 : 0.0);
//                double prevMean = means[i];
//                means[i] = (means[i] * (g - 1) + x) / g;
//                if (g >= 2) {
//                    vars[i] = (vars[i] * (g - 2) + Math.pow(x - means[i], 2)) / (g - 1) + Math.pow(prevMean - means[i], 2);
//                }
//            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toPrint == 0) {
                System.out.println(g + "/" + reps + ": ");
                for (i = 0; i < N; i++) {
                    System.out.println("     AI #" + (i + 1)
                            + ": μ = " + String.format("%.5f", means[i])
                            + ", σ² = " + String.format("%.5f", vars[i]));
                }
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    System.out.println("     Time left: " + hours + ":" + minutes + ":" + seconds);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiComparer();
        ait.start();
    }
}
