package aiworkshop;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.GameOptions;
import core.OhHellCore;
import core.Player;
import ml.Learner;
import strategyOI.AiStrategyModuleOI;
import strategyOI.ImmediateValueLearner;
import strategyOI.OverallValueLearner;

public class AiTrainerOI extends AiTrainer {
    private Dashboard dash;
    
    private OverallValueLearner ovl;
    private ImmediateValueLearner ivl;
    private Color ovlColor = Color.PINK;
    private Color ivlColor = Color.CYAN;
    
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 10;
        boolean doubleDeck = true;
        int reps = 1000000;
        boolean verbose = true;
        boolean forMathematica = false;
        boolean printError = true;
        boolean showDash = true;
        
        double ovlEta = 1;
        double ivlEta = 1;
        double scale = 0.1;
        int groupingSize = 3;

        int D = doubleDeck ? 2 : 1;
        int maxH = Math.min(10, (52 * D - 1) / N);
        int maxCancels = (N - 1) / 2;
        int[] ovlLayers = {
                maxH              // Cards left in hand
                + (maxH + 1) * N  // Bids - takens
                + 4               // Number of voids
                + 13 * D          // Trump left
                
                + 2               // Card is trump
                + 13 * D          // That suit left
                + 13 * D          // Card's adjusted number
                + (D - 1),        // Matching cards left
                40,               // Hidden layer
                1                 // Card's predicted value
        };
        int[] ivlLayers = {
                (maxH + 1) * (N - 1) // Bids - takens
                + 13 * D             // Trump left
                
                + 2                  // Trump was led
                + 13 * D             // Led suit left
                
                + 2                  // Card is trump
                + 13 * D             // Card's adjusted number
                + (D - 1)            // Matching cards left
                
                + maxCancels,        // Required cancels
                30,                  // Hidden layer
                1                    // Card's predicted value
        };
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 100;
        
        String folder = "resources/ai workshop/OhHellAIModels/OI/";

        ovlEta *= scale;
        ivlEta *= scale;
        
        String ovlFileSuffix = "o" + ovlLayers[1] + "";
        for (int i = 2; i < ovlLayers.length - 1; i++) {
            ovlFileSuffix += "_" + ovlLayers[i];
        }
        String ivlFileSuffix = "i" + ivlLayers[1] + "";
        for (int i = 2; i < ivlLayers.length - 1; i++) {
            ivlFileSuffix += "_" + ivlLayers[i];
        }
        String fileSuffix = ovlFileSuffix + ivlFileSuffix;
        
        ovl = new OverallValueLearner(ovlLayers, OverallValueLearner.getActFuncs(ovlLayers.length - 1));
        ovl.setTrainer(this);
        ivl = new ImmediateValueLearner(ivlLayers, ImmediateValueLearner.getActFuncs(ivlLayers.length - 1));
        ivl.setTrainer(this);
        
        if (openFromFile) {
            ovl.openFromFile(folder + "ovlN" + N + "D" + D + fileSuffix + ".txt");
            ivl.openFromFile(folder + "ivlN" + N + "D" + D + fileSuffix + ".txt");
        }
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new AiStrategyModuleOI(core, N, D, ovl, ivl));
        }
        
        int M = 10000;
        int[] toAve = {1, 100};
        double[] scores = new double[M];
        double[] mades = new double[M];
        double[] aves = new double[toAve.length];
        double[] accs = new double[toAve.length];
        int bestScore = Integer.MIN_VALUE;
        int overallBest = Integer.MIN_VALUE;
        
        if (showDash) {
            dash = new Dashboard();
            dash.execute();
            dash.setGraphCount(3);
            dash.setGraphLabel(0, "Average score");
            dash.setGraphLabel(1, "OVL mean squared error");
            dash.setGraphLabel(2, "IVL mean squared error");
            dash.setGraphColor(0, 0, Color.RED);
            dash.setGraphColor(0, 1, Color.DARK_GRAY);
            dash.setGraphColor(1, 0, ovlColor);
            dash.setGraphColor(2, 0, ivlColor);
        }
        
        GameOptions options = new GameOptions(N);
        options.setD(D);
        options.setRobotDelay(0);
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(options, aiStrategyModules);
            
            try {
                while (true) {
                    sleep(Integer.MAX_VALUE);
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
            
            StringBuilder log = new StringBuilder();
            
            if (g % toAve[0] == 0 && verbose) {
                log.append("OI N=" + N + ", D=" + (doubleDeck ? 2 : 1) + "\n");
                log.append(g + "/" + reps + ": \n");
                log.append("     Best score: " + bestScore + " (" + overallBest + ")\n");
                bestScore = Integer.MIN_VALUE;
                for (int k = 0; k < toAve.length; k++) {
                    if (g >= toAve[k]) {
                        log.append("     Average of " + toAve[k] + ": " + String.format("%.5f", accs[k]) + "   (" + String.format("%.5f", aves[k]) + ")\n");
                    }
                }
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    log.append("     Time left: " + hours + ":" + minutes + ":" + seconds + "\n");
                }
            }
            if (dash == null) {
                System.out.println(log);
            } else {
                dash.addLog(log + "");
            }
            
            if (g % groupingSize == 0) {
                if (dash != null) {
                    dash.updateEpoch(g / groupingSize);
                }
                List<double[]> ovlError = ovl.doEpoch(ovlEta, ovlEta, printError);
                List<double[]> ivlError = ivl.doEpoch(ivlEta, ivlEta, printError);
                if (printError) {
                    if (dash != null) {
                        dash.addGraphData(0, 0, aves[0]);
                        if (g >= toAve[1]) {
                            dash.addGraphData(0, 1, aves[1]);
                        }
                        dash.addGraphData(1, 0, ovlError.get(0)[0]);
                        dash.addGraphData(2, 0, ivlError.get(0)[0]);
                        dash.updateLog();
                    }
                }
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    ovl.saveToFile(new File(folder + "ovlN" + N + "D" + D + fileSuffix + ".txt"));
                    ivl.saveToFile(new File(folder + "ivlN" + N + "D" + D + fileSuffix + ".txt"));
                }
            }
        }
    }
    
    @Override
    public void notifyDatumNumber(Learner l, int datumNumber, int datumTotal) {
        if (dash != null) {
            dash.updateDatum(datumNumber, datumTotal, l == ovl ? ovlColor : ivlColor);
        }
    }
    
    @Override
    public void addLog(Learner l, String text) {
        if (dash != null) {
            dash.addLog(text);
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainerOI();
        ait.start();
    }
}
