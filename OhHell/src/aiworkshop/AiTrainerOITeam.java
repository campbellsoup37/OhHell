package aiworkshop;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.GameClient;
import core.AiKernel;
import core.AiStrategyModule;
import core.AiTrainer;
import core.GameCoordinator;
import core.GameOptions;
import core.OhHellCore;
import ml.Learner;
import strategyOITeam.AiStrategyModuleOITeam;
import strategyOITeam.ImmediateValueLearner;
import strategyOITeam.OverallValueLearner;
import strategyOITeam.TeammateTakesLearner;

public class AiTrainerOITeam extends AiTrainer {
    private Dashboard dash;
    
    private OverallValueLearner ovl;
    private ImmediateValueLearner ivl;
    private TeammateTakesLearner ttl;
    private Color ovlColor = Color.PINK;
    private Color ivlColor = Color.CYAN;
    private Color ttlColor = Color.ORANGE;
    
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 6;
        int D = 2;
        int[][] teams = {
                {0, 1},
                {2, 3},
                {4, 5}
        };
        int reps = 1000000;
        boolean verbose = true;
        boolean forMathematica = false;
        boolean printError = true;
        boolean showDash = true;
        boolean showClient = false;
        boolean stoppers = false;
        
        double ovlEta = 1;
        double ivlEta = 1;
        double ttlEta = 1;
        double scale = 1;
        int groupingSize = 1;
        
        boolean openFromFile = false;
        boolean saveToFile = false;
        int saveEvery = 100;
        
        String folder = "resources/ai workshop/OhHellAIModels/OIT/";

        ovlEta *= scale;
        ivlEta *= scale;
        
        int[] ovlLayers = {50};
        int[] ivlLayers = {50};
        int[] ttlLayers = {50};
        
        String ovlFileSuffix = "o" + ovlLayers[0] + "";
        for (int i = 1; i < ovlLayers.length; i++) {
            ovlFileSuffix += "_" + ovlLayers[i];
        }
        String ivlFileSuffix = "i" + ivlLayers[0] + "";
        for (int i = 1; i < ivlLayers.length; i++) {
            ivlFileSuffix += "_" + ivlLayers[i];
        }
        String ttlFileSuffix = "t" + ttlLayers[0] + "";
        for (int i = 1; i < ttlLayers.length; i++) {
            ttlFileSuffix += "_" + ttlLayers[i];
        }
        String fileSuffix = ovlFileSuffix + ivlFileSuffix + ttlFileSuffix;
        
        int T = teams.length;
        
        ovl = new OverallValueLearner(N, T, D, ivlLayers[0]);
        ovl.setTrainer(this);
        ivl = new ImmediateValueLearner(N, T, D, ivlLayers[0]);
        ivl.setTrainer(this);
        ttl = new TeammateTakesLearner(N, T, D, ivlLayers[0]);
        ttl.setTrainer(this);
        
        if (openFromFile) {
            ovl.openFromFile(folder + "ovlN" + N + "D" + D + fileSuffix + ".txt");
            ivl.openFromFile(folder + "ivlN" + N + "D" + D + fileSuffix + ".txt");
            int x = 1 / 0;
        }
        
        GameCoordinator coordinator = new GameCoordinator() {};
        
        GameOptions options = new GameOptions(N);
        options.setD(D);
        options.setRobotDelay(0);
        options.setTeams(true);
        coordinator.updateOptions(options);
        
        Map<Integer, Integer> teamMap = new HashMap<>();
        for (int i = 0; i < teams.length; i++) {
            for (int j : teams[i]) {
                teamMap.put(j, i);
            }
        }
        coordinator.reteamPlayers(teamMap);
        
        OhHellCore core = new OhHellCore(false);
        core.setAiTrainer(this);
        core.overrideAiKernel(new AiKernel(core) {
            @Override
            public List<AiStrategyModule> createDefaultAiStrategyModules(int N) {
                List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
                for (int i = 0; i < N; i++) {
                    aiStrategyModules.add(new AiStrategyModuleOITeam(N, T, core.getCoreData(), ovl, ivl, ttl, AiTrainerOITeam.this));
                }
                return aiStrategyModules;
            }
        });
        coordinator.startNewCore(core);
        
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
            dash.setGraphCount(4);
            dash.setGraphLabel(0, "Average score");
            dash.setGraphLabel(1, "OVL mean squared error");
            dash.setGraphLabel(2, "IVL mean squared error");
            dash.setGraphLabel(3, "TTL mean squared error");
            dash.setGraphColor(0, 0, Color.RED);
            dash.setGraphColor(0, 1, Color.DARK_GRAY);
            dash.setGraphColor(1, 0, ovlColor);
            dash.setGraphColor(2, 0, ivlColor);
            dash.setGraphColor(3, 0, ttlColor);
        }
        
        GameClient client = null;
        if (showClient) {
            client = new GameClient();
            client.execute(false);
        }
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            if (showClient) {
                client.openGame(coordinator, options, stoppers);
            } else {
                core.startGame(options);
            }
            
            try {
                while (true) {
                    sleep(Integer.MAX_VALUE);
                }
            } catch (InterruptedException e) {}
            
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
                log.append("OI N=" + N + ", D=" + D + "\n");
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
                List<double[]> ttlError = ttl.doEpoch(ttlEta, ttlEta, printError);
                if (printError) {
                    if (dash != null) {
                        dash.addGraphData(0, 0, aves[0]);
                        if (g >= toAve[1]) {
                            dash.addGraphData(0, 1, aves[1]);
                        }
                        dash.addGraphData(1, 0, ovlError.get(0)[0]);
                        dash.addGraphData(2, 0, ivlError.get(0)[0]);
                        dash.addGraphData(3, 0, ttlError.get(0)[0]);
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
            Color color = null;
            if (l == ovl) {
                color = ovlColor;
            } else if (l == ivl) {
                color = ivlColor;
            } else if (l == ttl) {
                color = ttlColor;
            }
            dash.updateDatum(datumNumber, datumTotal, color);
        }
    }
    
    @Override
    public void addLog(Learner l, String text) {
        if (dash != null) {
            dash.addLog(text);
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainerOITeam();
        ait.start();
    }
}
