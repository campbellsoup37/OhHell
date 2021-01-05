import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.Learner;
import ohHellCore.AiStrategyModule;
import ohHellCore.AiTrainer;
import ohHellCore.OhHellCore;
import ohHellCore.Player;
import strategyMI.AiStrategyModuleMI;
import strategyMI.ImmediateValueLearner;
import strategyMI.MacroValueLearner;

public class AiTrainerMI extends AiTrainer {
    private Dashboard dash;
    
    private MacroValueLearner mvl;
    private ImmediateValueLearner ivl;
    private Color mvlColor = Color.MAGENTA;
    private Color ivlColor = Color.CYAN;
    
    public AiTrainerMI() {}
    
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 5;
        int reps = 1000000;
        boolean verbose = true;
        boolean forMathematica = false;
        boolean printError = true;
        boolean showDash = true;
        
        double mvlWEta = 2;
        double mvlBEta = 2;
        double ivlWEta = 1;
        double ivlBEta = 1;
        double scale = 0.1;
        int groupingSize = 10;

        int maxH = Math.min(10, 51 / N);
        int[] mvlLayers = {
                52                   // Hand
                + 52                 // Played
                + (maxH + 1) * N,    // Bids - takens
                40,                  // Hidden layer
                maxH + 1             // Hand's qs
        };
        int[] ivlLayers = {
                52                      // Hand
                + 52                    // Played
                + (maxH + 1) * (N - 1), // Bids - takens
                15,                     // Hidden layer
                1                       // Card's p
        };
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 10;
        
        String folder = "resources/OhHellAIModels/MI/";

        mvlWEta *= scale;
        mvlBEta *= scale;
        ivlWEta *= scale;
        ivlBEta *= scale;
        
        String mvlFileSuffix = "o" + mvlLayers[1] + "";
        for (int i = 2; i < mvlLayers.length - 1; i++) {
            mvlFileSuffix += "_" + mvlLayers[i];
        }
        String ivlFileSuffix = "i" + ivlLayers[1] + "";
        for (int i = 2; i < ivlLayers.length - 1; i++) {
            ivlFileSuffix += "_" + ivlLayers[i];
        }
        String subFolder = mvlFileSuffix + ivlFileSuffix;
        
        mvl = new MacroValueLearner(mvlLayers, MacroValueLearner.getActFuncs(mvlLayers.length - 1));
        mvl.setTrainer(this);
        ivl = new ImmediateValueLearner(ivlLayers, ImmediateValueLearner.getActFuncs(ivlLayers.length - 1));
        ivl.setTrainer(this);
        
        if (openFromFile) {
            mvl.openFromFile(folder + "/" + subFolder + "/" + "mvlN" + N + ".txt");
            ivl.openFromFile(folder + "/" + subFolder + "/" + "ivlN" + N + ".txt");
        }
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new AiStrategyModuleMI(core, N, mvl, ivl));
        }
        
        int M = 10000;
        int[] toAve = {10, 100, 1000, 10000};
        double[] scores = new double[M];
        double[] mades = new double[M];
        double[] aves = new double[toAve.length];
        double[] accs = new double[toAve.length];
        int bestScore = Integer.MIN_VALUE;
        int overallBest = Integer.MIN_VALUE;
        
        if (showDash) {
            dash = new Dashboard();
            dash.execute();
            dash.setGraphLabel(0, "Average score");
            dash.setGraphLabel(1, "MVL cross entropy error");
            dash.setGraphLabel(2, "IVL mean squared error");
            dash.setGraphColor(0, 0, Color.RED);
            dash.setGraphColor(0, 1, Color.DARK_GRAY);
            dash.setGraphColor(1, 0, mvlColor);
            dash.setGraphColor(2, 0, ivlColor);
        }
        
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
            
            StringBuilder log = new StringBuilder();
            
            if (g % toAve[0] == 0 && verbose) {
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
                String dataString = aves[0] + ",";
                List<double[]> mvlError = mvl.doEpoch(mvlWEta, mvlBEta, printError);
                List<double[]> ivlError = ivl.doEpoch(ivlWEta, ivlBEta, printError);
                if (printError) {
                    for (double[] nums : mvlError) {
                        if (nums.length == 2) {
                            dataString += nums[0] + "," + nums[1] + ",";
                        } else {
                            dataString += nums[0] + "," + nums[1] + "," + nums[2] + "," + nums[3] + ",";
                        }
                    }
                    for (double[] nums : ivlError) {
                        if (nums.length == 2) {
                            dataString += nums[0] + "," + nums[1] + ",";
                        } else {
                            dataString += nums[0] + "," + nums[1] + "," + nums[2] + "," + nums[3] + ",";
                        }
                    }
                    
                    if (dash != null) {
                        dash.addGraphData(0, 0, aves[0]);
                        if (g >= toAve[1]) {
                            dash.addGraphData(0, 1, aves[1]);
                        }
                        dash.addGraphData(1, 0, mvlError.get(0)[0]);
                        dash.addGraphData(2, 0, ivlError.get(0)[0]);
                        dash.updateLog();
                    }
                    
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C:/Users/campbell/Desktop/data/outMI.txt"), true));
                        bw.write(dataString + "\n");
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    mvl.saveToFile(new File(folder + "/" + subFolder + "/" + "mvlN" + N + ".txt"));
                    ivl.saveToFile(new File(folder + "/" + subFolder + "/" + "ivlN" + N + ".txt"));
                }
            }
        }
    }
    
    @Override
    public void notifyDatumNumber(Learner l, int datumNumber, int datumTotal) {
        if (dash != null) {
            dash.updateDatum(datumNumber, datumTotal, l == mvl ? mvlColor : ivlColor);
        }
    }
    
    @Override
    public void addLog(Learner l, String text) {
        if (dash != null) {
            dash.addLog(text);
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainerMI();
        ait.start();
    }
}
