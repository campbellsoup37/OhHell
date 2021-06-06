package aiworkshop;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.AiKernel;
import core.AiPlayer;
import core.AiStrategyModule;
import core.AiTrainer;
import core.GameCoordinator;
import core.GameOptions;
import core.OhHellCore;
import core.Player;

public class AiComparer extends AiTrainer {
    public void run() {
        int N = 10;
        int D = 2;
        int T = 5;
        int reps = 1000000;
        
        int toPrint = 1;
        
        // Strategy 1
        strategyOI.OverallValueLearner ovlBase = new strategyOI.OverallValueLearner("resources/models/ovlN" + N + "D" + D + ".txt");
        strategyOI.ImmediateValueLearner ivlBase = new strategyOI.ImmediateValueLearner("resources/models/ivlN" + N + "D" + D + ".txt");
        
        // Strategy 2
        strategyOITeam.OverallValueLearner ovlTeam = new strategyOITeam.OverallValueLearner(N, T, D, 50);
        strategyOITeam.ImmediateValueLearner ivlTeam = new strategyOITeam.ImmediateValueLearner(N, T, D, 100);
        strategyOITeam.TeammateTakesLearner ttlTeam = new strategyOITeam.TeammateTakesLearner(N, T, D, 100);
        ovlTeam.openFromFile("resources/ai workshop/OhHellAiModels/OIT/ovlN" + N + "D" + D + "T" + T + "o50i50t50.txt");
        ivlTeam.openFromFile("resources/ai workshop/OhHellAiModels/OIT/ivlN" + N + "D" + D + "T" + T + "o50i50t50.txt");
        ttlTeam.openFromFile("resources/ai workshop/OhHellAiModels/OIT/ttlN" + N + "D" + D + "T" + T + "o50i50t50.txt");
        
        // --------------------------------------------------------
        
        int[][] teams = new int[T][N / T];
        int ind = 0;
        for (int i = 0; i < T; i++) {
            for (int j = 0; j < N / T; j++) {
                teams[i][j] = ind;
                ind++;
            }
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
            public List<AiPlayer> createAiPlayers(int N, GameOptions options, 
                    List<Player> dummies, int delay) {
                List<AiPlayer> ans = super.createAiPlayers(N, options, dummies, delay);
                for (int i = 0; i < N; i++) {
                    ans.get(i).setName(i + "");
                    ans.get(i).setTeam(i / 2);
                }
                return ans;
            }
            
            @Override
            public List<AiStrategyModule> createDefaultAiStrategyModules(int N) {
                List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
                for (int i = 0; i < N; i++) {
                    if (i < 0) {
                        aiStrategyModules.add(new strategyOITeam.AiStrategyModuleOITeam(N, T, core.getCoreData(), ovlTeam, ivlTeam, ttlTeam, AiComparer.this));
                    } else {
                        AiStrategyModule module = new strategyOI.AiStrategyModuleOI(core, N, D, ovlBase, ivlBase);
                        module.setCoreData(core.getCoreData());
                        aiStrategyModules.add(module);
                    }
                }
                return aiStrategyModules;
            }
        });
        coordinator.startNewCore(core);
        
        double[] means = new double[N];
        double[] vars = new double[N];
        double[] wins = new double[N];
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(options);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                
            }
            
            List<Player> players = getPlayers();
            
            double winningScore = Integer.MIN_VALUE;
            int[] scoreMap = new int[N];
            for (Player player : players) {
                scoreMap[Integer.parseInt(player.getName())] = player.getScore();
                winningScore = Math.max(winningScore, player.getScore());
            }
            
            for (int i = 0; i < N; i++) {
                double x = scoreMap[i];
                double prevMean = means[i];
                means[i] = (means[i] * (g - 1) + x) / g;
                if (g >= 2) {
                    vars[i] = (vars[i] * (g - 2) + Math.pow(x - means[i], 2)) / (g - 1) + Math.pow(prevMean - means[i], 2);
                }
                double won = scoreMap[i] == winningScore ? 1 : 0;
                wins[i] = (wins[i] * (g - 1) + won) / g;
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
                for (int i = 0; i < N; i++) {
                    System.out.println("     AI #" + (i + 1)
                            + ":\tμ = " + String.format("%.5f", means[i])
                            + ",\tσ² = " + String.format("%.5f", vars[i])
                            + ",\tw = " + String.format("%.5f", wins[i]));
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
    
    @FunctionalInterface
    private interface StrategyGenerator {
        AiStrategyModule getModule(int index);
    }
}
