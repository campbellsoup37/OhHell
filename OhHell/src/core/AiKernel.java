package core;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import strategyOI.AiStrategyModuleOI;
import strategyOI.ImmediateValueLearner;
import strategyOI.OverallValueLearner;

/**
 * This class contains the AI. Functions makeBid and makePlay are used to bid and play cards on 
 * behalf of a given AiPlayer and are called by the appropriate AiPlayer objects. For the game to
 * work properly, these functions need to be called in a thread not running the corresponding
 * OhHellCore. An object of this class can act as this thread if it is currently running when the 
 * functions are called. If not, a Timer object is created to take care of it (see the code for
 * makeBid and makePlay). When running many games with only AI players, it is significantly more 
 * efficient to run this class as a Thread.
 */
public class AiKernel {
    private AiThread aiThread;

    private OhHellCore core;
    private List<String> firstNames;
    private Random random = new Random();
    private List<AiPlayer> aiPlayers = new ArrayList<>();
    private GameOptions options;
    
    public AiKernel(OhHellCore core) {
        this.core = core;
        
        try {
            String file = "resources/core/firstnames.txt";
            InputStream in = getClass().getResourceAsStream("/" + file);
            BufferedReader reader;
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
            firstNames = new ArrayList<>(18239);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                firstNames.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean hasAiPlayers() {
        return !aiPlayers.isEmpty();
    }
    
    public List<AiPlayer> createAiPlayers(int N, GameOptions options, List<AiStrategyModule> aiStrategyModules, 
            List<Player> dummies, int delay) {
        aiPlayers.clear();
        this.options = options;
        
        if (aiStrategyModules == null) {
            aiStrategyModules = createDefaultAiStrategyModules(N);
        }
        for (int i = 0; i < options.getNumRobots(); i++) {
            AiPlayer player = new AiPlayer(
                        firstNames.get(random.nextInt(firstNames.size())) + " Bot", 
                        this,
                        aiStrategyModules.get(i),
                        delay
                    );
            if (i < dummies.size()) {
                player.setTeam(dummies.get(i).getTeam());
            }
            aiPlayers.add(player);
        }
        
        return aiPlayers;
    }
    
    public List<AiStrategyModule> createDefaultAiStrategyModules(int N) {
        int D = core.getDeck().getD();
        OverallValueLearner ovl = new OverallValueLearner("resources/models/" + "ovlN" + N + "D" + D + ".txt");
        ImmediateValueLearner ivl = new ImmediateValueLearner("resources/models/" + "ivlN" + N + "D" + D + ".txt");
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(options.getNumRobots());
        for (int i = 0; i < options.getNumRobots(); i++) {
            AiStrategyModule aiStrategyModule = new AiStrategyModuleOI(core, N, ovl, ivl);
            aiStrategyModule.setOptions(options);
            aiStrategyModules.add(aiStrategyModule);
        }
        return aiStrategyModules;
    }
    
    public void reloadAiStrategyModules(int N, List<AiStrategyModule> aiStrategyModules) {
        aiPlayers.removeIf(Player::isKicked);
        if (aiStrategyModules == null) {
            aiStrategyModules = createDefaultAiStrategyModules(N);
        }
        for (int i = 0; i < aiPlayers.size(); i++) {
            aiPlayers.get(i).setAiStrategyModule(aiStrategyModules.get(i));
        }
    }
    
    public void start() {
        aiThread = new AiThread(this);
        aiThread.start();
    }
    
    public void stop() {
        if (aiThread != null && aiThread.isRunning()) {
            aiThread.end();
        }
        aiPlayers.clear();
    }
    
    /**
     * TODO
     * There is currently a flaw related to this function. It is possible that an AI player may 
     * ask for its bid before the AiThread object has fully started running. In that case, this 
     * function relies on the Timer thread in the else statement. This works and should not be 
     * noticeable, but it is unintended.
     */
    public void makeBid(AiPlayer player, int delay) {
        if (aiThread.isRunning()) {
            aiThread.makeBid(player, delay);
        } else {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    makeBid(player);
                    cancel();
                }
            }, delay);
        }
    }
    
    public void makePlay(AiPlayer player, int delay) {
        if (aiThread.isRunning()) {
            aiThread.makePlay(player, delay);
        } else {
            System.out.println(player.getName() + " play");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    makePlay(player);
                    cancel();
                }
            }, delay);
        }
    }
    
    public void makeBid(AiPlayer player) {
        player.getAiStrategyModule().makeBid();
    }
    
    public void makePlay(AiPlayer player) {
        player.getAiStrategyModule().makePlay();
    }
    
    public void processClaimRequest(AiPlayer player, int index) {
        core.processClaimResponse(player, true);
    }
    
    public void sendChat(AiPlayer sender, String recipient, String text) {
        core.sendChat(sender, recipient, text);
    }
}
