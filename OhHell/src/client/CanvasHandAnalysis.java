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
    
    private final double handColumn = 3.0 / 8;
    private final double plotColumn = 5.0 / 8;
    private final double bidColumn = 25.0 / 32;
    private final double tookColumn = 26.75 / 32;
    private final double aiBidColumn = 28.5 / 32;
    private final double diffColumn = 30.5 / 32;
    
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
    
    public void addData(List<List<double[]>> qs, List<List<Integer>> aiBids, List<List<Double>> diffs) {
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
                qPlots.get(i).get(j).setMinY(-40);
                qPlots.get(i).get(j).setMaxY(100);
                qPlots.get(i).get(j).setBoxed(false);
                qPlots.get(i).get(j).setAxes(false);
            }
        }

        this.aiBids = aiBids;
        this.diffs = diffs;
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        OhcGraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        
        graphics.setColor(Color.BLACK);
        canvas.drawCard(graphics, canvas.getTrumps().get(roundSelected), 
                x() + handColumn * width(), 
                y() + trumpRowHeight / 2 + 30, 
                GameCanvas.smallCardScale, true, false,
                y() + trumpRowHeight);
        graphics.setFont(OhcGraphicsTools.fontSmall);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Predicted distribution", 
                x() + plotColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Bid", 
                x() + bidColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Took", 
                x() + tookColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "AI", 
                x() + aiBidColumn * width(), 
                y() + 1 * trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Bid", 
                x() + aiBidColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Difficulty", 
                x() + diffColumn * width(), 
                y() + 1 * trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Score", 
                x() + diffColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        graphics.setFont(OhcGraphicsTools.font);
        int h = (height() - trumpRowHeight - 2 * margin - buttonSize) / players.size();
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
                            x() + handColumn * width() + 10 * j - 10 * rounds.get(roundSelected)[1] / 2, 
                            y() + trumpRowHeight + i * h + h / 2 + 30, 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);
                    j++;
                }
                
                int iRelToLeader = (i + 1 + rounds.get(roundSelected)[0]) % players.size();
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, 
                                "" + players.get(iRelToLeader).getBids().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + bidColumn * width(),//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + h / 5, 
                        1, 1);
                int delta = Math.min(
                        Math.abs(players.get(iRelToLeader).getBids().get(roundSelected) 
                                - players.get(iRelToLeader).getTakens().get(roundSelected)),
                        3);
                if (players.get(iRelToLeader).getBids().get(roundSelected)
                        == players.get(iRelToLeader).getTakens().get(roundSelected)) {
                    graphics.setColor(new Color(0, 175, 0));
                } else {
                    graphics.setColor(new Color(255 * (delta - 1) / 3 + 195 * (4 - delta) / 3, 0, 0));
                }
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, 
                                "" + players.get(iRelToLeader).getTakens().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + tookColumn * width(),//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 2 * h / 5, 
                        1, 1);
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, 
                                "" + aiBids.get(roundSelected).get(i), 
                                width() / 4 - 2 * margin), 
                        x() + aiBidColumn * width(),//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 3 * h / 5, 
                        1, 1);
                /*OhcGraphicsTools.drawStringJustified(graphics, 
                        "Difficulty score: ", 
                        x() + 31 * width() / 32,//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 4 * h / 5, 
                        0, 1);*/
                float dScale = (float) ((diffs.get(roundSelected).get(i) - 1) / 9);
                graphics.setColor(new Color(dScale, 0.75F * (1 - dScale), 0F));
                OhcGraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", diffs.get(roundSelected).get(i)), 
                        x() + diffColumn * width(),//x() + 3 * width() / 4 + margin + graphics.getFontMetrics().stringWidth("Difficulty score: "), 
                        y() + trumpRowHeight + iRelToLeader * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 4 * h / 5, 
                        1, 1);
                
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
