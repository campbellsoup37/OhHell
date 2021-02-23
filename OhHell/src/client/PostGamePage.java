package client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import common.GraphicsTools;
import ml.BasicVector;
import ml.BootstrapAggregator;

public class PostGamePage extends CanvasInteractable {
    private List<ClientPlayer> players;
    private List<ClientPlayer> sortedPlayers;
    private List<CanvasInteractable> tabs = new ArrayList<>();
    private int tabSelected;
    private List<CanvasButton> buttons = new ArrayList<>();
    
    private boolean dataLoaded = false;
    
    public PostGamePage() {
        makeButtons();
    }
    
    public void makeButtons() {
        String[] names = {
                "Scores", 
                "Win %",
                "Summary",
                "Bids",
                "Plays"
        };
        PostGamePage page = this;
        for (int i = 0; i < names.length; i++) {
            final int iF = i;
            buttons.add(new CanvasButton(names[i]) {
                @Override
                public int x() {
                    return (2 * page.x()
                            + page.width()
                            + GameCanvas.finalScoreInnerMargin) / 2
                            - (width() * names.length + GameCanvas.finalScoreInnerMargin * (names.length - 1)) / 2
                            + (width() + GameCanvas.finalScoreInnerMargin) * iF;
                }
                
                @Override
                public int y() {
                    return page.y()
                            + page.height()
                            - GameCanvas.finalScoreInnerMargin
                            - height();
                }
                
                @Override
                public int width() {
                    return 100;
                }
                
                @Override
                public int height() {
                    return 30;
                }
                
                @Override
                public boolean isSelected() {
                    return getSelectedTab() == iF;
                }
                
                @Override
                public void click() {
                    selectTab(iF);
                }
            });
        }
    }
    
    public List<CanvasButton> getButtons() {
        return buttons;
    }
    
    public void buildTabs(List<ClientPlayer> finalPlayers, List<int[]> rounds, GameCanvas canvas) {
        players = finalPlayers;
        sortedPlayers = new ArrayList<>(players.size());
        sortedPlayers.addAll(players);
        sortedPlayers.sort((p1, p2) -> (int) Math.signum(p1.getPlace() - p2.getPlace()));
        
        PostGamePage page = this;
        
        List<String> ticks = new ArrayList<>(rounds.size());
        ticks.add("");
        for (int[] round : rounds) {
            ticks.add(round[1] + "");
        }
        
        // Score plot
        PostGamePlotTab scorePlot = new PostGamePlotTab(sortedPlayers) {
            @Override
            public int x() {
                return page.x()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.width()
                        - 2 * GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int height() {
                return page.height()
                        - 3 * GameCanvas.finalScoreInnerMargin
                        - 30;
            }
        };
        for (ClientPlayer player : players) {
            List<Integer> scoresAug = new LinkedList<>();
            scoresAug.add(0);
            scoresAug.addAll(player.getScores());
            scorePlot.getPlot().addIntData(player.getName(), scoresAug);
        }
        scorePlot.getPlot().setTicks(ticks);
        
        // Win % plot
        PostGamePlotTab winProbPlot = new PostGamePlotTab(sortedPlayers) {
            @Override
            public int x() {
                return page.x()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.width()
                        - 2 * GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int height() {
                return page.height()
                        - 3 * GameCanvas.finalScoreInnerMargin
                        - 30;
            }
        };
        ///////////////
        List<List<Double>> probs = new ArrayList<>(players.size());
        for (int k = 0; k < players.size(); k++) {
            probs.add(new ArrayList<>(players.get(0).getScores().size()));
            probs.get(k).add(100D / players.size());
        }
        BootstrapAggregator winModel = new BootstrapAggregator("resources/models/wb" + players.size() + ".txt");
        for (int j = 0; j < players.get(0).getScores().size(); j++) {
            double[] in = new double[players.size() + 1];
            for (int k = 0; k < players.size(); k++) {
                in[k] = (double) players.get(k).getScores().get(j);
            }
            in[players.size()] = (double) (players.get(0).getScores().size() - 1 - j);
            double[] out = winModel.testValue(new BasicVector(in)).get(1).toArray();
            for (int k = 0; k < players.size(); k++) {
                probs.get(k).add(out[k] * 100);
            }
        }
        for (int k = 0; k < players.size(); k++) {
            if (players.get(k).getPlace() == 1) {
                probs.get(k).set(players.get(0).getScores().size(), 100D);
            } else {
                probs.get(k).set(players.get(0).getScores().size(), 0D);
            }
            winProbPlot.getPlot().addData(k, CanvasScorePlot.circlePoint, players.get(k).getName(), probs.get(k));
        }
        ///////////////
//        probs = new ArrayList<>(players.size());
//        for (int k = 0; k < players.size(); k++) {
//            probs.add(new ArrayList<>(players.get(0).getScores().size()));
//            probs.get(k).add(100D / players.size());
//        }
//        GradientBooster winModel2 = new GradientBooster("resources/ai workshop/OtherModels/WinBoost/boost9000000.txt");
//        for (int j = 0; j < players.get(0).getScores().size(); j++) {
//            double[] in = new double[players.size() + 1];
//            for (int k = 0; k < players.size(); k++) {
//                in[k] = (double) players.get(k).getScores().get(j);
//            }
//            in[players.size()] = (double) (players.get(0).getScores().size() - 1 - j);
//            double[] out = winModel2.testValue(new BasicVector(in)).get(1).toArray();
//            for (int k = 0; k < players.size(); k++) {
//                probs.get(k).add(out[k] * 100);
//            }
//        }
//        for (int k = 0; k < players.size(); k++) {
//            if (players.get(k).getPlace() == 1) {
//                probs.get(k).set(players.get(0).getScores().size(), 100D);
//            } else {
//                probs.get(k).set(players.get(0).getScores().size(), 0D);
//            }
//            winProbPlot.addData(k, CanvasScorePlot.starPoint, "boost: " + players.get(k).getName(), probs.get(k));
//        }
        ///////////////
        winProbPlot.getPlot().setTicks(ticks);
        
        // Game analysis
        PostGameSummaryTab gameAnalysis = new PostGameSummaryTab(players, rounds) {
            @Override
            public int x() {
                return page.x()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.width()
                        - 2 * GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int height() {
                return page.height()
                        - 3 * GameCanvas.finalScoreInnerMargin
                        - 30;
            }
        };
        
        // Bid analysis
        PostGameBiddingTab handAnalysis = new PostGameBiddingTab(canvas, players, rounds) {
            @Override
            public int x() {
                return page.x()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.width()
                        - 2 * GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int height() {
                return page.height()
                        - 3 * GameCanvas.finalScoreInnerMargin
                        - 30;
            }
            
            @Override
            public void wheel(int clicks) {
                selectRound(getSelectedRound() + clicks);
            }
            
            @Override
            public void pressKey(int keyCode) {
                if (keyCode == KeyEvent.VK_LEFT) {
                    selectRound(getSelectedRound() - 1);
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    selectRound(getSelectedRound() + 1);
                }
            }
        };
        
        // Play analysis
        PostGamePlayingTab playAnalysis = new PostGamePlayingTab(canvas, players, rounds) {
            @Override
            public int x() {
                return page.x()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.width()
                        - 2 * GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int height() {
                return page.height()
                        - 3 * GameCanvas.finalScoreInnerMargin
                        - 30;
            }
            
            @Override
            public void wheel(int clicks) {
                selectTrick(getSelectedTrick() + clicks);
            }
            
            @Override
            public void pressKey(int keyCode) {
                if (keyCode == KeyEvent.VK_LEFT) {
                    selectTrick(getSelectedTrick() - 1);
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    selectTrick(getSelectedTrick() + 1);
                }
            }
        };
        
        tabs.add(scorePlot);
        tabs.add(winProbPlot);
        tabs.add(gameAnalysis);
        tabs.add(handAnalysis);
        tabs.add(playAnalysis);
        
        dataLoaded = true;
    }
    
    /**
     * Given a list qs, where qs[k] is the probability of making bid k, then determine a difficulty
     * rating between 1 and 10. A list like {1, 0, 0, 0} should give a rating of 1, and a list like
     * {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1} should give a rating of 10.
     * 
     * On a 0 to 1 scale, the difficulty is currently defined as
     * (p ^ u - 1) / ((A - B * h) ^ u - 1),
     * where p is max(qs), h is the number of cards in the hand, and u, A, and B are tuning 
     * parameters.
     */
    public static double difficulty(double[] qs) {
        double A = 0.45;
        double B = 0.025;
        double u = 0;
        
        double max = 0;
        for (double q : qs) {
            max = Math.max(q, max);
        }
        double r = A - B * (qs.length - 1);
        double s = 0;
        if (u == 0) {
            s = Math.log(max) / Math.log(r);
        } else {
            s = (Math.pow(max, u) - 1) / (Math.pow(r, u) - 1);
        }
        return Math.max(1, Math.min(10, 1 + 9D * s));
    }
    
    public void selectTab(int tab) {
        tabSelected = tab;
    }
    
    public int getSelectedTab() {
        return tabSelected;
    }
    
    @Override
    public boolean isShown() {
        return dataLoaded;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (!isShown()) {
            return;
        }
        
        graphics.setColor(new Color(255, 255, 255, 180));
        GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        
        tabs.get(tabSelected).paint(graphics);
        
        for (CanvasButton button : buttons) {
            button.paint(graphics);
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            CanvasInteractable inter0 = tabs.get(tabSelected).updateMoused(x, y);
            if (inter0 != null) {
                ans = inter0;
            }
            for (CanvasButton button : buttons) {
                CanvasInteractable inter1 = button.updateMoused(x, y);
                if (inter1 != null) {
                    ans = inter1;
                }
            }
        }
        return ans;
    }
    
    public void wheel(int clicks) {
        tabs.get(tabSelected).wheel(clicks);
    }
    
    public void pressKey(int keyCode) {
        tabs.get(tabSelected).pressKey(keyCode);
    }
}
