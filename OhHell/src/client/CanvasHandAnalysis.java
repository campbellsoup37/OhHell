package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import core.Card;
import graphics.OhcGraphicsTools;

public class CanvasHandAnalysis extends CanvasInteractable {
    private final int trumpRowHeight = 35;
    private final int margin = 4;
    private final int buttonSize = 18;
    
    private GameCanvas canvas;
    private List<ClientPlayer> players;
    private List<int[]> rounds;
    private List<CanvasButton> roundButtons;
    private int roundSelected = 0;
    
    private List<List<CanvasScorePlot>> qPlots;
    
    private List<List<Integer>> aiBids;
    private List<List<Double>> diffs;
    
    public CanvasHandAnalysis(GameCanvas canvas, List<ClientPlayer> players, List<int[]> rounds) {
        this.canvas = canvas;
        this.players = players;
        this.rounds = rounds;
        
        CanvasHandAnalysis tab = this;
        roundButtons = new ArrayList<>(rounds.size());
        for (int i = 0; i < rounds.size(); i++) {
            final int iF = i;
            roundButtons.add(new CanvasButton(rounds.get(i)[1] + "") {
                @Override
                public int x() {
                    return tab.x()
                            + tab.width() / 2
                            - (rounds.size() * buttonSize + (rounds.size() - 1) * margin) / 2
                            + (buttonSize + margin) * iF;
                }
                
                @Override
                public int y() {
                    return tab.y()
                            + tab.height()
                            - margin
                            - buttonSize;
                }
                
                @Override
                public int width() {
                    return buttonSize;
                }
                
                @Override
                public int height() {
                    return buttonSize;
                }
                
                @Override
                public boolean isSelected() {
                    return getSelectedRound() == iF;
                }
                
                @Override
                public void click() {
                    selectRound(iF);
                }
            });
        }
        
        qPlots = new ArrayList<>(rounds.size());
        for (int i = 0; i < rounds.size(); i++) {
            final int iF = i;
            qPlots.add(new ArrayList<>(players.size()));
            for (int j = 0; j < players.size(); j++) {
                final int jF = j;
                qPlots.get(i).add(new CanvasScorePlot() {
                    @Override
                    public int x() {
                        return tab.x() + tab.width() / 2;
                    }
                    
                    @Override
                    public int y() {
                        int h = (tab.height() - trumpRowHeight - 2 * margin - buttonSize) / players.size();
                        return tab.y() + trumpRowHeight + ((jF + rounds.get(iF)[0] + 1) % players.size()) * h;
                    }
                    
                    @Override
                    public int width() {
                        return tab.width() / 4;
                    }
                    
                    @Override
                    public int height() {
                        int h = (tab.height() - trumpRowHeight - 2 * margin - buttonSize) / players.size();
                        return h;
                    }
                    
                    @Override
                    public boolean isShown() {
                        return roundSelected == iF;
                    }
                });
            }
        }
    }
    
    public int getSelectedRound() {
        return roundSelected;
    }
    
    public void selectRound(int round) {
        roundSelected = Math.min(rounds.size() - 1, Math.max(0, round));
    }
    
    public void addData(List<List<double[]>> qs, List<List<Integer>> aiBids) {
        for (int i = 0; i < rounds.size(); i++) {
            List<String> ticks = new ArrayList<>(rounds.get(i)[1] + 1);
            for (int k = 0; k <= rounds.get(i)[1] + 1; k++) {
                ticks.add(k + "");
            }
            for (int j = 0; j < players.size(); j++) {
                List<Double> data = new ArrayList<>(qs.get(i).get(j).length);
                for (double q : qs.get(i).get(j)) {
                    data.add(q * 100);
                }
                qPlots.get(i).get(j).addData("Probability (%)", data);
                qPlots.get(i).get(j).setTicks(ticks);
                qPlots.get(i).get(j).setRange(-40, 100);
                qPlots.get(i).get(j).setBoxed(false);
                qPlots.get(i).get(j).setAxes(false);
            }
        }

        this.aiBids = aiBids;
        diffs = new ArrayList<>(rounds.size());
        for (int i = 0; i < rounds.size(); i++) {
            diffs.add(new ArrayList<>(players.size()));
            for (int j = 0; j < players.size(); j++) {
                diffs.get(i).add(difficulty(qs.get(i).get(j)));
            }
        }
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
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(x(), y(), width(), height(), 10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(x(), y(), width(), height(), 10, 10);
        
        int h = (height() - trumpRowHeight - 2 * margin - buttonSize) / players.size();
        canvas.drawCard(graphics, canvas.getTrumps().get(roundSelected), 
                x() + width() / 4, 
                y() + trumpRowHeight / 2 + 30, 
                GameCanvas.smallCardScale, true, false,
                y() + trumpRowHeight);
        for (int i = 0; i <= players.size(); i++) {
            graphics.setColor(new Color(192, 192, 192));
            graphics.drawLine(
                    x() + margin, 
                    y() + trumpRowHeight + i * h, 
                    x() + width() - margin, 
                    y() + trumpRowHeight + i * h);
            if (i < players.size()) {
                ClientPlayer player = players.get(i);
                if (player.getIndex() == rounds.get(roundSelected)[0]) {
                    graphics.setColor(Color.CYAN);
                    graphics.fillOval(
                            x() + 2 * margin, 
                            y() + trumpRowHeight + i * h + h / 2 - 8, 
                            16, 16);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, "D", 
                            x() + 2 * margin + 8, 
                            y() + trumpRowHeight + i * h + h / 2, 
                            1, 1);
                }
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, player.getName(), 
                                width() / 4 - 2 * margin + 25), 
                        x() + 2 * margin + 25, 
                        y() + trumpRowHeight + i * h + h / 2, 
                        0, 1);
                
                int j = 0;
                for (Card card : player.getHands().get(roundSelected)) {
                    canvas.drawCard(graphics, card, 
                            x() + 3 * width() / 8 + 10 * j - 10 * rounds.get(roundSelected)[1] / 2, 
                            y() + trumpRowHeight + i * h + h / 2 + 30, 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);
                    j++;
                }
                
                int iRelToLeader = (i + 1 + rounds.get(roundSelected)[0]) % players.size();
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, 
                                "Player bid: " + players.get(iRelToLeader).getBids().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + h / 5, 
                        0, 1);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, 
                                "Tricks taken: " + players.get(iRelToLeader).getTakens().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + 2 * h / 5, 
                        0, 1);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, 
                                "AI preferred bid: " + aiBids.get(roundSelected).get(i), 
                                width() / 4 - 2 * margin), 
                        x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + 3 * h / 5, 
                        0, 1);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        "Difficulty score: ", 
                        x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + 4 * h / 5, 
                        0, 1);
                float dScale = (float) ((diffs.get(roundSelected).get(i) - 1) / 9);
                graphics.setColor(new Color(dScale, 0.75F * (1 - dScale), 0F));
                OhcGraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", diffs.get(roundSelected).get(i)), 
                        x() + 3 * width() / 4 + margin + graphics.getFontMetrics().stringWidth("Difficulty score: "), 
                        y() + trumpRowHeight + iRelToLeader * h + 4 * h / 5, 
                        0, 1);
                
                qPlots.get(roundSelected).get(i).paint(graphics);
            }
        }
        
        for (CanvasButton button : roundButtons) {
            button.paint(graphics);
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            for (CanvasScorePlot plot : qPlots.get(roundSelected)) {
                CanvasInteractable inter = plot.updateMoused(x, y);
                if (inter != null) {
                    ans = inter;
                }
            }
            for (CanvasButton button : roundButtons) {
                CanvasInteractable inter = button.updateMoused(x, y);
                if (inter != null) {
                    ans = inter;
                }
            }
        }
        return ans;
    }
}
