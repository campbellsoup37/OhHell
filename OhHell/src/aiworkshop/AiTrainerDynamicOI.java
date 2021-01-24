package aiworkshop;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.OhHellCore;
import core.Player;
import strategyOI.AiStrategyModuleDynamicOI;
import strategyOI.ImmediateValueLearner;
import strategyOI.OverallValueLearner;

public class AiTrainerDynamicOI extends AiTrainer {
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 5;
        int reps = 1000000;
        boolean verbose = true;
        boolean forMathematica = true;
        boolean printError = true;
        
        double ovlWEta = 1;
        double ovlBEta = 0.0001;
        double ivlWEta = 1;
        double ivlBEta = 0.0001;
        double scale = 0.01;
        int groupingSize = 1;
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 10;
        
        String folder = "ai resources/OhHellAIModels/dOI/";
        
        ovlWEta *= scale;
        ovlBEta *= scale;
        ivlWEta *= scale;
        ivlBEta *= scale;

        int maxH = Math.min(10, 51 / N);
        int[] ovlLayers = {
                maxH              // Cards left in hand
                + (maxH + 1) * N  // Bids - takens
                + 4               // Number of voids
                + 13              // Trump left
                
                + 2               // Card is trump
                + 13              // That suit left
                + 13,             // Card's adjusted number
                40,               // Hidden layer
                //20,
                1                 // Card's predicted value
        };
        int[] ivlLayers = {
                (maxH + 1) * (N - 1) // Bids - takens
                + 13                 // Trump left
                
                + 2                  // Trump was led
                + 13                 // Led suit left
                
                + 2                  // Card is trump
                + 13,                // Card's adjusted number
                30,                  // Hidden layer
                //20,
                1                    // Card's predicted value
        };
        OverallValueLearner[] ovls = new OverallValueLearner[maxH + 1];
        for (int i = 0; i <= maxH; i++) {
            //ovls[i] = new OverallValueLearner(ovlLayers, OverallValueLearner.getActFuncs(ovlLayers.length - 1));
            ovls[i] = new OverallValueLearner(folder + "ovlN" + N + ".txt");
        }
        //ImmediateValueLearner ivl = new ImmediateValueLearner(ivlLayers, ImmediateValueLearner.getActFuncs(ivlLayers.length - 1));
        ImmediateValueLearner ivl = new ImmediateValueLearner(folder + "ivlN" + N + ".txt");
        
        if (openFromFile) {
            for (int i = 0; i < ovls.length; i++) {
                ovls[i].openFromFile(folder + "dovlN" + N + "bid" + i + ".txt");
            }
            ivl.openFromFile(folder + "divlN" + N + ".txt");
        }
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new AiStrategyModuleDynamicOI(core, N, ovls, ivl));
        }
        
        int M = 10000;
        int[] toAve = {1, 10, 100, 1000, 10000};
        double[] scores = new double[M];
        double[] mades = new double[M];
        double[] aves = new double[toAve.length];
        double[] accs = new double[toAve.length];
        int bestScore = Integer.MIN_VALUE;
        int overallBest = Integer.MIN_VALUE;
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(N, false, aiStrategyModules, 0);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] -= scores[(g - 1 + M - toAve[k]) % M] / toAve[k];
                accs[k] -= mades[(g - 1 + M - toAve[k]) % M] / toAve[k];
            }
            scores[(g - 1) % M] = 0;
            mades[(g - 1) % M] = 0;
            
            int[] sortedScores = getScores();
            Arrays.sort(sortedScores);
            if (forMathematica) {
                System.out.print("{");
                for (int i = 0; i < N - 1; i++) {
                    System.out.print(sortedScores[i] + ",");
                }
                System.out.println(sortedScores[N - 1] + "},");
            }
            
            for (int score : sortedScores) {
                scores[(g - 1) % M] += (double) score / N;
            }
            
            if (sortedScores[N - 1] > bestScore) {
                bestScore = sortedScores[N - 1];
                if (sortedScores[N - 1] > overallBest) {
                    overallBest = sortedScores[N - 1];
                }
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] += scores[(g - 1) % M] / toAve[k];
                accs[k] += mades[(g - 1) % M] / toAve[k];
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toAve[0] == 0 && verbose) {
                System.out.println(g + "/" + reps + ": ");
                System.out.println("     Best score: " + bestScore + " (" + overallBest + ")");
                bestScore = Integer.MIN_VALUE;
                for (int k = 0; k < toAve.length; k++) {
                    if (g >= toAve[k]) {
                        System.out.println("     Average of " + toAve[k] + ": " + String.format("%.5f", accs[k]) + "   (" + String.format("%.5f", aves[k]) + ")");
                    }
                }
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    System.out.println("     Time left: " + hours + ":" + minutes + ":" + seconds);
                }
            }
            if (g % groupingSize == 0) {
                for (OverallValueLearner ovl : ovls) {
                    ovl.doEpoch(ovlWEta, ovlBEta, printError);
                }
                ivl.doEpoch(ivlWEta, ivlBEta, printError);
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    for (int i = 0; i < ovls.length; i++) {
                        ovls[i].saveToFile(new File(folder + "dovlN" + N + "bid" + i + ".txt"));
                    }
                    ivl.saveToFile(new File(folder + "divlN" + N + ".txt"));
                }
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainerDynamicOI();
        ait.start();
    }
}
