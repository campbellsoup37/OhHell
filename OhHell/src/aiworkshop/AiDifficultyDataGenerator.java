package aiworkshop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.Card;
import core.GameOptions;
import core.OhHellCore;
import core.Player;

public class AiDifficultyDataGenerator extends AiTrainer {
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 5;
        int reps = 500;
        
        String outputFolder = "resources/ai workshop/ai data/";
        String subfolder = "OI/";
        
        try {
            File file = new File(outputFolder + subfolder + N + ".txt");
            file.getParentFile().mkdirs();
            final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            
            OhHellCore core = new OhHellCore(false);
            List<Player> players = new ArrayList<>();
            core.setPlayers(players);
            core.setAiTrainer(this);
            
            strategyOI.OverallValueLearner ovl = new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN5o40i30.txt");
            strategyOI.ImmediateValueLearner ivl = new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN5o40i30.txt");
            List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                aiStrategyModules.add(new strategyOI.AiStrategyModuleOI(core, N, ovl, ivl) {
                    @Override
                    public int getMyBid(double[] ps) {
                        double[] qs = subsetProb(ps, ps.length);
                        /*try {
                            writer.append(ps.length + "," + max(qs) + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        if (difficulty(qs) > 9.9) {
                            System.out.println("Trump:       " + core.getTrump());
                            System.out.println("Hand:        " + this.player.getHand().stream().map(Card::toString).reduce("", (c1, c2) -> c1 + c2 + ", "));
                            String bids =      "Bids so far: ";
                            for (int j = 1; j < N; j++) {
                                Player p = players.get((this.player.getIndex() + j) % N);
                                if (p.hasBid()) {
                                    bids += p.getBid() + ", ";
                                }
                            }
                            System.out.println(bids);
                            System.out.println("Difficulty:  " + difficulty(qs));
                            System.out.println();
                        }
                        return super.getMyBid(ps);
                    }
                });
            }
            
            int toPrint = -1;
            
            int R = 20;
            long[] times = new long[R];
            for (int g = 1; g <= reps; g++) {
                core.startGame(new GameOptions(N), aiStrategyModules);
                
                try {
                    while (true) {
                        sleep(1);
                    }
                } catch (InterruptedException e) {
                    
                }
                
                long newTime = System.currentTimeMillis();
                long timeDiff = newTime - times[(g - 1) % R];
                times[(g - 1) % R] = newTime;
                
                if (toPrint > 0 && g % toPrint == 0) {
                    System.out.println(g + "/" + reps + ": ");
                    if (g > R) {
                        double timeLeft = (double) timeDiff / R * (reps - g);
                        long hours = (long) (timeLeft / 1000 / 60 / 60);
                        long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                        long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                        System.out.println("     Time left: " + hours + ":" + minutes + ":" + seconds);
                    }
                }
            }
            
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiDifficultyDataGenerator();
        ait.start();
    }
}
