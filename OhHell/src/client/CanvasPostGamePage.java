package client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import core.Card;
import core.Deck;
import graphics.OhcGraphicsTools;
import ml.BasicVector;
import ml.BootstrapAggregator;
import ml.SparseVector;
import strategyOI.AiStrategyModuleOI;
import strategyOI.OverallValueLearner;

public class CanvasPostGamePage extends CanvasInteractable {
    private List<ClientPlayer> players;
    private List<ClientPlayer> sortedPlayers;
    private List<int[]> rounds;
    private List<CanvasInteractable> tabs = new ArrayList<>();
    private int tabSelected;
    private List<CanvasButton> buttons = new ArrayList<>();
    
    public CanvasPostGamePage(List<ClientPlayer> players, List<ClientPlayer> sortedPlayers, List<int[]> rounds) {
        this.players = players;
        this.sortedPlayers = sortedPlayers;
        this.rounds = rounds;
        makeButtons();
    }
    
    public void makeButtons() {
        String[] names = {
                "Scores", 
                "Win %",
                "Hand analysis"
        };
        CanvasPostGamePage page = this;
        for (int i = 0; i < names.length; i++) {
            final int iF = i;
            buttons.add(new CanvasButton(names[i]) {
                @Override
                public int x() {
                    return (2 * page.x()
                            + page.width()
                            + GameCanvas.finalScoreInnerMargin
                            + GameCanvas.finalScoreListWidth) / 2
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
    
    public void buildTabs(GameCanvas canvas) {
        CanvasPostGamePage page = this;
        
        List<String> ticks = new ArrayList<>(rounds.size());
        ticks.add("");
        for (int[] round : rounds) {
            ticks.add(round[1] + "");
        }
        
        // Score plot
        CanvasScorePlot scorePlot = new CanvasScorePlot() {
            @Override
            public int x() {
                return page.x()
                        + 2 * GameCanvas.finalScoreInnerMargin
                        + GameCanvas.finalScoreListWidth;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.x() 
                        + page.width()
                        - GameCanvas.finalScoreInnerMargin
                        - x();
            }
            
            @Override
            public int height() {
                return page.y()
                        + page.height()
                        - 2 * GameCanvas.finalScoreInnerMargin
                        - 30
                        - y();
            }
        };
        for (ClientPlayer player : players) {
            List<Integer> scoresAug = new LinkedList<>();
            scoresAug.add(0);
            for (Integer score : player.getScores()) {
                scoresAug.add(score);
            }
            scorePlot.addIntData(player.getName(), scoresAug);
        }
        scorePlot.setTicks(ticks);
        tabs.add(scorePlot);
        
        // Win % plot
        CanvasScorePlot winProbPlot = new CanvasScorePlot() {
            @Override
            public int x() {
                return page.x()
                        + 2 * GameCanvas.finalScoreInnerMargin
                        + GameCanvas.finalScoreListWidth;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.x() 
                        + page.width()
                        - GameCanvas.finalScoreInnerMargin
                        - x();
            }
            
            @Override
            public int height() {
                return page.y()
                        + page.height()
                        - 2 * GameCanvas.finalScoreInnerMargin
                        - 30
                        - y();
            }
        };
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
            winProbPlot.addData(players.get(k).getName(), probs.get(k));
        }
        winProbPlot.setTicks(ticks);
        tabs.add(winProbPlot);
        
        // Hand analysis
        CanvasHandAnalysis handAnalysis = new CanvasHandAnalysis(canvas, players, rounds) {
            @Override
            public int x() {
                return page.x()
                        + 2 * GameCanvas.finalScoreInnerMargin
                        + GameCanvas.finalScoreListWidth;
            }
            
            @Override
            public int y() {
                return page.y()
                        + GameCanvas.finalScoreInnerMargin;
            }
            
            @Override
            public int width() {
                return page.x() 
                        + page.width()
                        - GameCanvas.finalScoreInnerMargin
                        - x();
            }
            
            @Override
            public int height() {
                return page.y()
                        + page.height()
                        - 2 * GameCanvas.finalScoreInnerMargin
                        - 30
                        - y();
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
        List<List<double[]>> allQs = new ArrayList<>(rounds.size());
        List<List<Integer>> allAiBids = new ArrayList<>(rounds.size());
        int M = players.size();
        int maxH = Math.min(10, 51 / M);
        OverallValueLearner ovl = new OverallValueLearner("resources/models/" + "ovlN" + players.size() + ".txt");
        for (int i = 0; i < rounds.size(); i++) {
            List<double[]> roundQs = new ArrayList<>(M);
            List<Integer> roundAiBids = new ArrayList<>(M);
            Card trump = canvas.getTrumps().get(i);
            for (int j = 1; j <= players.size(); j++) {
                ClientPlayer player = players.get((j + rounds.get(i)[0]) % M);
                
                double[] ps = new double[rounds.get(i)[1]];
                
                int turn = player.getIndex();
                int numOfVoids = AiStrategyModuleOI.voids(player.getHands().get(i));
                List<List<Card>> split = AiStrategyModuleOI.splitBySuit(player.getHands().get(i));
                
                for (int k = 0; k < rounds.get(i)[1]; k++) {
                    Card card = player.getHands().get(i).get(k);
                    
                    SparseVector in = new SparseVector();
                    in.addOneHot(player.getHands().get(i).size(), maxH);
                    for (int l = 0; l < M; l++) {
                        ClientPlayer iterPlayer = players.get((turn + l) % M);
                        if (j - 1 + l >= M) {
                            in.addOneHot(iterPlayer.getBids().get(i) + 1, maxH + 1);
                        } else {
                            in.addZeros(maxH + 1);
                        }
                    }
                    in.addOneHot(numOfVoids + 1, 4);
                    in.addOneHot(13 - split.get(trump.getSuitNumber() - 1).size(), 13);
                    
                    in.addOneHot(card.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
                    in.addOneHot(14 - (card.getSuit().equals(trump.getSuit()) ? 1 : 0) - split.get(card.getSuitNumber() - 1).size(), 13);
                    
                    in.addOneHot(Deck.adjustedCardValueSmallNoPlayed(card, Arrays.asList(split.get(card.getSuitNumber() - 1), Arrays.asList(trump))) + 1, 13);
                    
                    ps[k] = ovl.testValue(in).get(1).get(0);
                }
                
                roundQs.add(AiStrategyModuleOI.subsetProb(ps, ps.length));
                roundAiBids.add(AiStrategyModuleOI.optimalBid(ps)[0]);
            }
            allQs.add(roundQs);
            allAiBids.add(roundAiBids);
        }
        handAnalysis.addData(allQs, allAiBids);
        tabs.add(handAnalysis);
    }
    
    public void selectTab(int tab) {
        tabSelected = tab;
    }
    
    public int getSelectedTab() {
        return tabSelected;
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.fillRoundRect(x(), y(), width(), height(), 10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(x(), y(), width(), height(), 10, 10);
        
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(
                x() + GameCanvas.finalScoreInnerMargin,
                y() + GameCanvas.finalScoreInnerMargin,
                GameCanvas.finalScoreListWidth,
                height() - 2 * GameCanvas.finalScoreInnerMargin,
                10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(
                x() + GameCanvas.finalScoreInnerMargin,
                y() + GameCanvas.finalScoreInnerMargin,
                GameCanvas.finalScoreListWidth,
                height() - 2 * GameCanvas.finalScoreInnerMargin,
                10, 10);
        graphics.setFont(OhcGraphicsTools.fontBold);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Final scores", 
                x() + GameCanvas.finalScoreInnerMargin + GameCanvas.finalScoreListWidth / 2, 
                y() + GameCanvas.finalScoreInnerMargin + 20,
                1, 1);
        graphics.setFont(OhcGraphicsTools.font);
        for (int i = 0; i < players.size(); i++) {
            ClientPlayer player = sortedPlayers.get(i);
            graphics.setColor(GameCanvas.colors[player.getIndex()]);
            graphics.fillOval(
                    (int) (GameCanvas.finalScoreOuterMargin + 2 * GameCanvas.finalScoreInnerMargin - GameCanvas.pointSize / 2), 
                    (int) (GameCanvas.finalScoreOuterMargin + GameCanvas.finalScoreInnerMargin + 30 + 15 * (i + 1) - GameCanvas.pointSize / 2), 
                    (int) (GameCanvas.pointSize),
                    (int) (GameCanvas.pointSize));
            graphics.setColor(Color.BLACK);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    OhcGraphicsTools.fitString(
                            graphics, 
                            player.getPlace() + ". " 
                            + player.getName(), 
                            0.8 * GameCanvas.finalScoreListWidth - 2 * GameCanvas.finalScoreInnerMargin),
                    GameCanvas.finalScoreOuterMargin + 2 * GameCanvas.finalScoreInnerMargin + GameCanvas.finalScoreInnerMargin / 2, 
                    GameCanvas.finalScoreOuterMargin + GameCanvas.finalScoreInnerMargin + 30 + 15 * (i + 1), 
                    0, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    player.getScore() + "",
                    GameCanvas.finalScoreOuterMargin + GameCanvas.finalScoreInnerMargin + (int) (0.8 * GameCanvas.finalScoreListWidth), 
                    GameCanvas.finalScoreOuterMargin + GameCanvas.finalScoreInnerMargin + 30 + 15 * (i + 1), 
                    1, 1);
        }
        
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
