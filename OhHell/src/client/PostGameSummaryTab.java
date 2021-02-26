package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import core.Card;
import common.GraphicsTools;
import strategyOI.AiStrategyModuleOI;

public class PostGameSummaryTab extends CanvasInteractable {
    private final int trumpRowHeight = 35;
    private final int margin = 4;
    private final int buttonSize = 18;

    private final double scoreColumn = 7.0 / 32;
    private final double deltaColumn = 13.0 / 32;
    private final double bSkillColumn = 19.0 / 32;
    private final double pSkillColumn = 22.0 / 32;
    private final double luckColumn = 25.0 / 32;
    private final double diffColumn = 28.0 / 32;
    
    private List<ClientPlayer> players;
    
    private List<CanvasPlot> deltas;
    private List<Double> bidSkills;
    private List<Double> playSkills;
    private List<Double> lucks;
    private List<Double> totalDiffs;
    
    public PostGameSummaryTab(List<ClientPlayer> players, List<int[]> rounds) {
        this.players = players;
        
        PostGameSummaryTab tab = this;
        deltas = new ArrayList<>(players.size());
        bidSkills = new ArrayList<>(players.size());
        playSkills = new ArrayList<>(players.size());
        lucks = new ArrayList<>(players.size());
        totalDiffs = new ArrayList<>(players.size());
        for (int j = 0; j < players.size(); j++) {
            ClientPlayer player = players.get(j);
            List<Double> playerDeltas = new ArrayList<>(9);
            for (int k = -4; k <= 4; k++) {
                playerDeltas.add(0D);
            }

            double pointsLost = 0;
            double mistakes = 0;
            
//            double skill = 0;
            double luck = 0;
            double totalDiff = 0;
            for (int i = 0; i < rounds.size() && i != player.getKickedAtRound(); i++) {
                double delta = player.getBids().get(i) - player.getTakens().get(i);
                double diff = player.getDiffs().get(i);
                int cannotBid = rounds.get(i)[1];
                for (ClientPlayer p : players) {
                    if (p != player) {
                        cannotBid -= p.getBids().get(i);
                    }
                }
                
                double[] E = AiStrategyModuleOI.pointsMean(player.getBidQs().get(i));
                double bestE = Integer.MIN_VALUE;
                for (int k = 0; k < E.length; k++) {
                    if (k != cannotBid || rounds.get(i)[0] != player.getIndex()) {
                        bestE = Math.max(E[k], bestE);
                    }
                }
                pointsLost += (bestE - E[player.getBids().get(i)]);
                
                double V = AiStrategyModuleOI.pointsVariance(player.getBidQs().get(i), player.getBids().get(i));
                
//                System.out.println(player.getName());
//                System.out.println("   bid =  " + player.getBids().get(i));
//                System.out.println("   qs =   " + Arrays.toString(player.getBidQs().get(i)));
//                System.out.println("   mean = " + E[player.getBids().get(i)]);
//                System.out.println("   stdd = " + Math.sqrt(V));

                // The luck for a hand is the z-score of the number of points received. I cap this
                // at +-5 because sometimes the AI way underestimates probabilities in the one 
                // card round that are already low (e.g., the chance of winning with an off-suit
                // deuce when in first seat, resulting in a z-score of like -30).
                double handLuck = Math.max(-5, Math.min(5, 
                                    (player.getScores().get(i) 
                                            - (i > 0 ? player.getScores().get(i - 1) : 0) 
                                            - E[player.getBids().get(i)]) / Math.sqrt(V)
                                    ));
                if (handLuck != handLuck) {
                    handLuck = 0;
                }
                luck += handLuck;
                
//                double acc = player.getBids().get(i) - player.getAiBids().get(i);
//                boolean prevented = rounds.get(i)[0] == player.getIndex() 
//                        && cannotBid == player.getAiBids().get(i);
//                if ((acc >= 0 || prevented) && delta == 0) {
//                    skill += diff * (Math.abs(acc) + 1.0);
//                } else if ((acc == 0 || prevented) && delta != 0) {
//                    luck -= 5.0 * Math.abs(delta);
//                } else if (delta != 0) {
//                    skill -= (11.0 - diff) * Math.abs(delta);
//                } else if (acc < 0 && delta == 0) {
//                    luck += (11.0 - diff) * (-acc);
//                }
                double regDelta = Math.min(4, Math.max(-4, delta)) + 4;
                playerDeltas.set((int) regDelta, playerDeltas.get((int) regDelta) + 1);
                totalDiff += diff;// / rounds.size();
                
                for (int k = 0; k < rounds.get(i)[2] - 1; k++) {
                    Hashtable<Card, Double> probs = player.getMakingProbs().get(i).get(k);
                    double myProb = 0;
                    double maxProb = 0;
                    for (Card card : probs.keySet()) {
                        maxProb = Math.max(probs.get(card), maxProb);
                        if (card.equals(player.getPlays().get(i).get(k).getCard())) {
                            myProb = probs.get(card);
                        }
                    }
                    mistakes += maxProb == 0 ? 0 : Math.min(maxProb / myProb - 1, 1);
                }
            }
            
            final int jF = j;
            CanvasPlot dPlot = new CanvasPlot() {
                @Override
                public int x() {
                    return (int) (tab.x() + deltaColumn * tab.width() - width() / 2);
                }
                
                @Override
                public int y() {
                    int h = (tab.height() - trumpRowHeight - 3 * margin - 2 * buttonSize) / players.size();
                    return tab.y() + trumpRowHeight + jF * h;
                }
                
                @Override
                public int width() {
                    return tab.width() / 4;
                }
                
                @Override
                public int height() {
                    int h = (tab.height() - trumpRowHeight - 3 * margin - 2 * buttonSize) / players.size();
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
            bidSkills.add(10.0 * Math.exp(-pointsLost / 57.0));
            playSkills.add(10.0 * Math.exp(-mistakes / 5.0));
            lucks.add(luck);
            totalDiffs.add(totalDiff);
        }
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        
        graphics.setFont(GraphicsTools.fontSmall);
        GraphicsTools.drawStringJustified(graphics, 
                "Score", 
                x() + scoreColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Bids - tooks", 
                x() + deltaColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Bidding", 
                x() + bSkillColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Performance", 
                x() + bSkillColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Playing", 
                x() + pSkillColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Performance", 
                x() + pSkillColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Total", 
                x() + luckColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Luck", 
                x() + luckColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Total", 
                x() + diffColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Difficulty", 
                x() + diffColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        graphics.setFont(GraphicsTools.font);
        double h = ((double) height() - trumpRowHeight - 3 * margin - 2 * buttonSize) / players.size();
        for (int i = 0; i <= players.size(); i++) {
            graphics.setColor(new Color(192, 192, 192));
            graphics.drawLine(
                    x() + margin, 
                    (int) (y() + trumpRowHeight + i * h), 
                    x() + width() - margin, 
                    (int) (y() + trumpRowHeight + i * h));
            if (i < players.size()) {
                ClientPlayer player = players.get(i);
                graphics.setColor(Color.BLACK);
                GraphicsTools.drawStringJustified(graphics, 
                        GraphicsTools.fitString(graphics, player.getName(), 
                                width() * 3.0 / 16 - 2 * margin + 25), 
                        x() + 2 * margin + 25, 
                        y() + trumpRowHeight + i * h + h / 2, 
                        0, 1);
                
                GraphicsTools.drawStringJustified(graphics, 
                        player.getScore() + "", 
                        x() + scoreColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                GraphicsTools.drawStringJustified(graphics, 
                        players.get(i).isHuman() ? String.format("%.1f", bidSkills.get(i)) : "-", 
                        x() + bSkillColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                GraphicsTools.drawStringJustified(graphics, 
                        players.get(i).isHuman() ? String.format("%.1f", playSkills.get(i)) : "-", 
                        x() + pSkillColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                GraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", lucks.get(i)), 
                        x() + luckColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        1, 1);
                
                GraphicsTools.drawStringJustified(graphics, 
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
            for (CanvasPlot plot : deltas) {
                CanvasInteractable inter = plot.updateMoused(x, y);
                if (inter != null) {
                    ans = inter;
                }
            }
        }
        return ans;
    }
}
