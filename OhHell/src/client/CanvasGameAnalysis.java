package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import graphics.OhcGraphicsTools;

public class CanvasGameAnalysis extends CanvasInteractable {
    private final int trumpRowHeight = 35;
    private final int margin = 4;
    private final int buttonSize = 18;

    private final double scoreColumn = 10.0 / 32;
    private final double deltaColumn = 4.0 / 8;
    private final double skillColumn = 22.0 / 32;
    private final double luckColumn = 25.0 / 32;
    private final double diffColumn = 28.0 / 32;
    
    private List<ClientPlayer> players;
    private List<int[]> rounds;
    
    private List<CanvasScorePlot> deltas;
    private List<Double> skills;
    private List<Double> lucks;
    private List<Double> totalDiffs;
    
    public CanvasGameAnalysis(List<ClientPlayer> players, List<int[]> rounds) {
        this.players = players;
        this.rounds = rounds;
    }
    
    public void addData(List<List<double[]>> qs, List<List<Integer>> aiBids, List<List<Double>> diffs) {
        CanvasGameAnalysis tab = this;
        deltas = new ArrayList<>(players.size());
        skills = new ArrayList<>(players.size());
        lucks = new ArrayList<>(players.size());
        totalDiffs = new ArrayList<>(players.size());
        for (int j = 0; j < players.size(); j++) {
            ClientPlayer player = players.get(j);
            List<Double> playerDeltas = new ArrayList<>(9);
            for (int k = -4; k <= 4; k++) {
                playerDeltas.add(0D);
            }
            double skill = 0;
            double luck = 0;
            double totalDiff = 0;
            for (int i = 0; i < rounds.size(); i++) {
                int jRelToLeader = (j - 1 - rounds.get(i)[0] + players.size()) % players.size();
                double delta = player.getBids().get(i) - player.getTakens().get(i);
                double acc = player.getBids().get(i) - aiBids.get(i).get(jRelToLeader);
                double diff = diffs.get(i).get(jRelToLeader);
                int cannotBid = rounds.get(i)[1];
                for (ClientPlayer p : players) {
                    if (p != player) {
                        cannotBid -= p.getBids().get(i);
                    }
                }
                boolean prevented = rounds.get(i)[0] == player.getIndex() 
                        && cannotBid == aiBids.get(i).get(jRelToLeader);
                if ((acc >= 0 || prevented) && delta == 0) {
                    skill += diff * (Math.abs(acc) + 1);
                } else if ((acc == 0 || prevented) && delta != 0) {
                    luck -= 5.0 * Math.abs(delta);
                } else if (delta != 0) {
                    skill -= (11 - diff) * Math.abs(delta);
                } else if (acc < 0 && delta == 0) {
                    luck += (11 - diff) * (-acc);
                }
                double regDelta = Math.min(4, Math.max(-4, delta)) + 4;
                playerDeltas.set((int) regDelta, playerDeltas.get((int) regDelta) + 1);
                totalDiff += diff;// / rounds.size();
            }
            
            final int jF = j;
            CanvasScorePlot dPlot = new CanvasScorePlot() {
                @Override
                public int x() {
                    return tab.x() + 3 * tab.width() / 8;
                }
                
                @Override
                public int y() {
                    int h = (tab.height() - trumpRowHeight - 2 * margin - buttonSize) / players.size();
                    return tab.y() + trumpRowHeight + jF * h;
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
            };
            dPlot.setBoxed(false);
            dPlot.setAxes(false);
            dPlot.addData("bid - took", playerDeltas);
            dPlot.setTicks(Arrays.asList("<-3", "-3", "-2", "-1", "0", "1", "2", "3", ">3"));
            dPlot.setMinY(-0.4 * rounds.size());
            dPlot.setMaxY(rounds.size());
            deltas.add(dPlot);
            skills.add(skill);
            lucks.add(luck);
            totalDiffs.add(totalDiff);
        }
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(x(), y(), width(), height(), 10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(x(), y(), width(), height(), 10, 10);
        
        graphics.setFont(OhcGraphicsTools.fontSmall);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Score", 
                x() + scoreColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Bids - tooks", 
                x() + deltaColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Skill", 
                x() + skillColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Score", 
                x() + skillColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Luck", 
                x() + luckColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Score", 
                x() + luckColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Total", 
                x() + diffColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Difficulty", 
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
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, player.getName(), 
                                width() / 4 - 2 * margin + 25), 
                        x() + 2 * margin + 25, 
                        y() + trumpRowHeight + i * h + h / 2, 
                        0, 1);
                
                OhcGraphicsTools.drawStringJustified(graphics, 
                        player.getScore() + "", 
                        x() + scoreColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                OhcGraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", skills.get(i)), 
                        x() + skillColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                OhcGraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", lucks.get(i)), 
                        x() + luckColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                OhcGraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", totalDiffs.get(i)), 
                        x() + diffColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                deltas.get(i).paint(graphics);
            }
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            for (CanvasScorePlot plot : deltas) {
                CanvasInteractable inter = plot.updateMoused(x, y);
                if (inter != null) {
                    ans = inter;
                }
            }
        }
        return ans;
    }
}
