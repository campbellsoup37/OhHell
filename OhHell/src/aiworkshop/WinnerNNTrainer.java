package aiworkshop;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.GameOptions;
import core.OhHellCore;
import core.Player;
import core.WinnerLearner;
import ml.BasicVector;
import ml.Vector;

public class WinnerNNTrainer extends AiTrainer {
    private Dashboard dash;
    
    private WinnerLearner wl;
    private Color wlColor = Color.PINK;
    
    public void run() {
        int N = 5;
        int reps = 1000000;
        boolean verbose = true;
        boolean printError = true;
        boolean showDash = true;
        
        double eta = 0.1;
        int groupingSize = 10;

        int[] wlLayers = {
                N                 // Players' scores
                + 1,              // Number of rounds left
                60,               
                N                 // Players' win probabilities
        };
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 100;
        
        String folder = "resources/ai workshop/OtherModels/Win/";
        
        String fileSuffix = "w" + wlLayers[1] + "";
        for (int i = 2; i < wlLayers.length - 1; i++) {
            fileSuffix += "_" + wlLayers[i];
        }
        
        wl = new WinnerLearner(wlLayers, WinnerLearner.getActFuncs(wlLayers.length - 1));
        wl.setTrainer(this);
        
        if (openFromFile) {
            wl.openFromFile(folder + "wlN" + N + fileSuffix + ".txt");
        }
        
        strategyOI.OverallValueLearner ovl = new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt");
        strategyOI.ImmediateValueLearner ivl = new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt");
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new strategyOI.AiStrategyModuleOI(core, N, ovl, ivl));
        }
        
        if (showDash) {
            dash = new Dashboard();
            dash.execute();
            dash.setGraphCount(2);
            dash.setGraphLabel(0, "WL cross entropy error");
            dash.setGraphColor(0, 0, wlColor);
        }
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(new GameOptions(N), aiStrategyModules);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                int[][] scores = getAllScores();
                for (int i = 0; i < scores[0].length; i++) {
                    double[] scoresVector = new double[N + 1];
                    for (int j = 0; j < N; j++) {
                        scoresVector[j] = (double) scores[j][i] / 100;
                    }
                    scoresVector[N] = (double) (scores[0].length - 1 - i) / scores[0].length;
                    Vector in = new BasicVector(scoresVector);
                    wl.putIn(in);
                    /*double[] v = wl.testValue(in).get(1).toArray();
                    for (int j = 0; j < N; j++) {
                        System.out.print(scores[j][i] + " (" + String.format("%.2f", v[j]) + ") ");
                    }
                    System.out.println();*/
                }
                wl.putOut(getWinner());
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % saveEvery == 0 && verbose) {
                StringBuilder log = new StringBuilder();
                log.append(g + "/" + reps + ": \n");
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    log.append("     Time left: " + hours + ":" + minutes + ":" + seconds + "\n");
                }
                if (dash == null) {
                    System.out.println(log);
                } else {
                    dash.addLog(log + "");
                }
            }
            
            if (g % groupingSize == 0) {
                if (dash != null) {
                    dash.updateEpoch(g / groupingSize);
                }
                List<double[]> wlError = wl.doEpoch(eta, eta, printError);
                if (printError) {
                    if (dash != null) {
                        dash.addGraphData(0, 0, wlError.get(0)[0]);
                        dash.updateLog();
                    }
                }
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    wl.saveToFile(new File(folder + "wlN" + N + fileSuffix + ".txt"));
                }
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new WinnerNNTrainer();
        ait.start();
    }
}
