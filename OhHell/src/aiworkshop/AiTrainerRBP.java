package aiworkshop;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.OhHellCore;
import core.Player;
import ml.Learner;
import strategyRBP.AiStrategyModuleRBP;
import strategyRBP.BiddingLearner;
import strategyRBP.ImmediateValueLearner;
import strategyRBP.OverallValueLearner;

public class AiTrainerRBP extends AiTrainer {
    private Dashboard dash;
    
    private BiddingLearner bl;
    private OverallValueLearner ovl;
    private ImmediateValueLearner ivl;
    private Color blColor = Color.MAGENTA;
    private Color ovlColor = Color.CYAN;
    private Color ivlColor = Color.GREEN;
    
    public AiTrainerRBP() {}
    
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
        
        double blEta = 5;
        double ovlEta = 1;
        double ivlEta = 1;
        double scale = 1;
        int groupingSize = 3;
        
        double bidExploration = 0;
        double playExploration = 0;

        int maxH = Math.min(10, 51 / N);
        int[] blLayers = {
                maxH + 1                   // Bid
                + 51                       // Hand
                + (maxH + 2) * (N - 1),    // Hasn't bids/bids
                100, 
                1
        };
        int[] ovlLayers = {
                13                         // Adjusted card number
                + 4                        // Card suit
                + (maxH + 1) * 4           // My suit lengths
                + 14 * 3 + 13              // Unseen suit lengths
                + (maxH + 1) * N,          // Wants
                10,                  
                1
        };
        int[] ivlLayers = {
                13                         // Adjusted card number
                + 4                        // State: 0=leading, 1=following nontrump, 2=trumping nontrump, 3=following trump
                + 13                       // Unseen length in led suit
                + 13                       // Unseen length in trump suit
                + (maxH + 2) * (N - 1),    // Already played/wants
                40,
                1
        };
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 10;
        
        String folder = "resources/ai workshop/OhHellAIModels/RBP/";

        blEta *= scale;
        ovlEta *= scale;
        ivlEta *= scale;
        
        String blFileSuffix = "b" + blLayers[1] + "";
        for (int i = 2; i < blLayers.length - 1; i++) {
            blFileSuffix += "_" + blLayers[i];
        }
        String ovlFileSuffix = "o" + ovlLayers[1] + "";
        for (int i = 2; i < ovlLayers.length - 1; i++) {
            ovlFileSuffix += "_" + ovlLayers[i];
        }
        String ivlFileSuffix = "i" + ivlLayers[1] + "";
        for (int i = 2; i < ivlLayers.length - 1; i++) {
            ivlFileSuffix += "_" + ivlLayers[i];
        }
        String subFolder = blFileSuffix + ovlFileSuffix + ivlFileSuffix;
        
        bl = new BiddingLearner(blLayers, BiddingLearner.getActFuncs(blLayers.length - 1));
        bl.setTrainer(this);
        ovl = new OverallValueLearner(ovlLayers, OverallValueLearner.getActFuncs(ovlLayers.length - 1));
        ovl.setTrainer(this);
        ivl = new ImmediateValueLearner(ivlLayers, ImmediateValueLearner.getActFuncs(ivlLayers.length - 1));
        ivl.setTrainer(this);
        
        if (openFromFile) {
            bl.openFromFile(folder + "/" + subFolder + "/" + N + "/" + "bl" + ".txt");
            ovl.openFromFile(folder + "/" + subFolder + "/" + N + "/" + "ovl" + ".txt");
            ivl.openFromFile(folder + "/" + subFolder + "/" + N + "/" + "ivl" + ".txt");
        }
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            AiStrategyModuleRBP asm = new AiStrategyModuleRBP(core, N, bl, ovl, ivl);
            asm.setExploration(bidExploration, playExploration);
            aiStrategyModules.add(asm);
        }
        /*AiStrategyModuleRBP asm = new AiStrategyModuleRBP(core, N, bl, ovl, ivl);
        asm.setExploration(bidExploration, playExploration);
        aiStrategyModules.add(asm);
        strategyOI.OverallValueLearner ovlo = new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAiModels/OI/ovlN5o40i30.txt");
        strategyOI.ImmediateValueLearner ivlo = new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAiModels/OI/ivlN5o40i30.txt");
        for (int i = 1; i < N; i++) {
            strategyOI.AiStrategyModuleOI asmo = new strategyOI.AiStrategyModuleOI(core, N, ovlo, ivlo);
            aiStrategyModules.add(asmo);
        }*/
        
        int M = 10000;
        int[] toAve = {1, 10, 100, 1000, 10000};
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
            dash.setGraphLabel(1, "BL mean squared error");
            dash.setGraphLabel(2, "OVL mean squared error");
            dash.setGraphLabel(3, "IVL mean squared error");
            dash.setGraphColor(0, 0, Color.RED);
            dash.setGraphColor(0, 1, Color.DARK_GRAY);
            dash.setGraphColor(1, 0, blColor);
            dash.setGraphColor(2, 0, ovlColor);
            dash.setGraphColor(3, 0, ivlColor);
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
                List<double[]> blError = bl.doEpoch(blEta, blEta, printError);
                List<double[]> ovlError = ovl.doEpoch(ovlEta, ovlEta, printError);
                List<double[]> ivlError = ivl.doEpoch(ivlEta, ivlEta, printError);
                
                if (dash != null) {
                    dash.addGraphData(0, 0, aves[0]);
                    if (g >= toAve[1]) {
                        dash.addGraphData(0, 1, aves[1]);
                    }
                    dash.addGraphData(1, 0, blError.get(0)[0]);
                    dash.addGraphData(2, 0, ovlError.get(0)[0]);
                    dash.addGraphData(3, 0, ivlError.get(0)[0]);
                    dash.updateLog();
                }
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    bl.saveToFile(new File(folder + "/" + subFolder + "/" + N + "/" + "bl" + ".txt"));
                    ovl.saveToFile(new File(folder + "/" + subFolder + "/" + N + "/" + "ovl" + ".txt"));
                    ivl.saveToFile(new File(folder + "/" + subFolder + "/" + N + "/" + "ivl" + ".txt"));
                }
            }
        }
    }
    
    @Override
    public void notifyDatumNumber(Learner l, int datumNumber, int datumTotal) {
        if (dash != null) {
            if (l == bl) {
                dash.updateDatum(datumNumber, datumTotal, blColor);
            } else if (l == ovl) {
                dash.updateDatum(datumNumber, datumTotal, ovlColor);
            } else {
                dash.updateDatum(datumNumber, datumTotal, ivlColor);
            }
        }
    }
    
    @Override
    public void addLog(Learner l, String text) {
        if (dash != null) {
            dash.addLog(text);
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainerRBP();
        ait.start();
    }
}
