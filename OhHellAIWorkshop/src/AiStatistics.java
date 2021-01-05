import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohHellCore.AiStrategyModule;
import ohHellCore.AiTrainer;
import ohHellCore.OhHellCore;
import ohHellCore.Player;

public class AiStatistics extends AiTrainer {
    public void run() {
        int N = 5;
        int reps = 1000000;
        
        String outputFolder = "C:/Users/Campbell/Desktop/OhHellAiStats/";
        int toPrint = 100;

        int maxH = Math.min(10, 51 / N);
        int[][][] bidsTakens = new int[maxH + 1][maxH + 1][maxH + 1];
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        /*strategyOI.OverallValueLearner ovl = new strategyOI.OverallValueLearner("resources/OhHellAIModels/OI/ovlN5o40i30.txt");
        strategyOI.ImmediateValueLearner ivl = new strategyOI.ImmediateValueLearner("resources/OhHellAIModels/OI/ivlN5o40i30.txt");
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new strategyOI.AiStrategyModuleOI(core, N, ovl, ivl));
        }*/
        strategyRBP.BiddingLearner bl = new strategyRBP.BiddingLearner("resources/OhHellAIModels/RBP/b30_30o10i40/5/bl.txt");
        strategyRBP.OverallValueLearner ovl = new strategyRBP.OverallValueLearner("resources/OhHellAIModels/RBP/b30_30o10i40/5/ovl.txt");
        strategyRBP.ImmediateValueLearner ivl = new strategyRBP.ImmediateValueLearner("resources/OhHellAIModels/RBP/b30_30o10i40/5/ivl.txt");
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            strategyRBP.AiStrategyModuleRBP asm = new strategyRBP.AiStrategyModuleRBP(core, N, bl, ovl, ivl);
            asm.setExploration(0, 0);
            aiStrategyModules.add(asm);
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
                    sleep(1);
                }
            } catch (InterruptedException e) {
                
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] -= scores[(g - 1 + M - toAve[k]) % M] / toAve[k];
                accs[k] -= mades[(g - 1 + M - toAve[k]) % M] / toAve[k];
            }
            scores[(g - 1) % M] = 0;
            mades[(g - 1) % M] = 0;
            
            int[] newRounds = getRoundHandSizes();
            int[][] newBids = getBids();
            int[][] newTakens = getTakens();
            int[] newScores = getScores();
            Arrays.sort(newScores);
            
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < newRounds.length; j++) {
                    bidsTakens[newRounds[j]][newBids[i][j]][newTakens[i][j]]++;
                }
            }
            
            for (int score : newScores) {
                scores[(g - 1) % M] += (double) score / N;
            }
            
            if (newScores[N - 1] > bestScore) {
                bestScore = newScores[N - 1];
                if (newScores[N - 1] > overallBest) {
                    overallBest = newScores[N - 1];
                }
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] += scores[(g - 1) % M] / toAve[k];
                accs[k] += mades[(g - 1) % M] / toAve[k];
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toAve[0] == 0) {
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
            
            if (g % toPrint == 0) {
                for (int j = 1; j <= maxH; j++) {
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFolder + j + " round.txt"));
                        bw.write("{");
                        for (int bid = 0; bid <= j; bid++) {
                            bw.write("{");
                            for (int taken = 0; taken <= j; taken++) {
                                bw.write("" + bidsTakens[j][bid][taken]);
                                if (taken < j) {
                                    bw.write(",");
                                } else {
                                    bw.write("}");
                                }
                            }
                            if (bid < j) {
                                bw.write(",\n");
                            } else {
                                bw.write("}\n");
                            }
                        }
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiStatistics();
        ait.start();
    }
}
