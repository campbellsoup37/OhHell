package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import core.Card;
import common.GraphicsTools;

public class PostGameBiddingTab extends CanvasInteractable {
    private final int trumpRowHeight = 35;
    private final int margin = 4;
    private final int buttonSize = 18;
    
    private final double trumpColumn = 3.0 / 16;
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
    
    private List<List<CanvasPlot>> qPlots;
    
    public PostGameBiddingTab(GameCanvas canvas, List<ClientPlayer> players, List<int[]> rounds) {
        this.canvas = canvas;
        this.players = players;
        this.rounds = rounds;
        
        PostGameBiddingTab tab = this;
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
            List<String> ticks = new ArrayList<>(rounds.get(i)[1] + 1);
            for (int k = 0; k <= rounds.get(i)[1] + 1; k++) {
                ticks.add(k + "");
            }
            for (int j = 0; j < players.size(); j++) {
                if (players.get(j).getKickedAtRound() != -1 && i >= players.get(j).getKickedAtRound()) {
                    qPlots.get(i).add(null);
                    continue;
                }
                
                final int jF = j;
                qPlots.get(i).add(new CanvasPlot() {
                    @Override
                    public int x() {
                        return tab.x() + tab.width() / 2;
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
                    
                    @Override
                    public boolean isShown() {
                        return roundSelected == iF;
                    }
                });
                
                List<Double> data = new ArrayList<>(players.get(j).getBidQs().get(i).length);
                for (double q : players.get(j).getBidQs().get(i)) {
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
    }
    
    public int getSelectedRound() {
        return roundSelected;
    }
    
    public void selectRound(int round) {
        roundSelected = Math.min(rounds.size() - 1, Math.max(0, round));
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        
        graphics.setColor(Color.BLACK);
        /*canvas.drawCard(graphics, canvas.getTrumps().get(roundSelected), 
                x() + handColumn * width(), 
                y() + trumpRowHeight / 2 + 30, 
                GameCanvas.smallCardScale, true, false,
                y() + trumpRowHeight);*/
        graphics.setFont(GraphicsTools.fontSmall);
        GraphicsTools.drawStringJustified(graphics, 
                "Trump", 
                x() + trumpColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Hand", 
                x() + handColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Predicted distribution", 
                x() + plotColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Bid", 
                x() + bidColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Took", 
                x() + tookColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "AI", 
                x() + aiBidColumn * width(), 
                y() + 1 * trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Bid", 
                x() + aiBidColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Difficulty", 
                x() + diffColumn * width(), 
                y() + 1 * trumpRowHeight / 3, 
                1, 1);
        GraphicsTools.drawStringJustified(graphics, 
                "Score", 
                x() + diffColumn * width(), 
                y() + 2 * trumpRowHeight / 3, 
                1, 1);
        graphics.setFont(GraphicsTools.font);
        double h = ((double) (height() - trumpRowHeight - 3 * margin - 2 * buttonSize) / players.size());
        for (int i = 0; i <= players.size(); i++) {
            graphics.setColor(new Color(192, 192, 192));
            graphics.drawLine(
                    x() + margin, 
                    (int) (y() + trumpRowHeight + i * h), 
                    x() + width() - margin, 
                    (int) (y() + trumpRowHeight + i * h));
            if (i < players.size()) {
                ClientPlayer player = players.get(i);
                /*if (player.getIndex() == rounds.get(roundSelected)[0]) {
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
                }*/
                if (player.getKickedAtRound() != -1 && roundSelected >= player.getKickedAtRound()) {
                    graphics.setColor(Color.RED);
                } else {
                    graphics.setColor(Color.BLACK);
                }
                GraphicsTools.drawStringJustified(graphics, 
                        GraphicsTools.fitString(graphics, player.getName(), 
                                width() / 8 - 3 * margin), 
                        x() + 2 * margin, 
                        y() + trumpRowHeight + i * h + h / 2, 
                        0, 1);
                graphics.setColor(Color.BLACK);
                
                if (player.getKickedAtRound() != -1 && roundSelected >= player.getKickedAtRound()) {
                    continue;
                }
                
                
                if (player.getIndex() == rounds.get(roundSelected)[0]) {
                    canvas.drawCard(graphics, 
                            new Card(), 
                            x() + trumpColumn * width() - 4, 
                            y() + trumpRowHeight + i * h + h / 2 + 30 - 4, 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);
                    canvas.drawCard(graphics, 
                            new Card(), 
                            x() + trumpColumn * width() - 2, 
                            y() + trumpRowHeight + i * h + h / 2 + 30 - 2, 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);
                    canvas.drawCard(graphics, 
                            canvas.getTrumps().get(roundSelected), 
                            x() + trumpColumn * width(), 
                            y() + trumpRowHeight + i * h + h / 2 + 30, 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);
                }
                
                int j = 0;
                for (Card card : player.getHands().get(roundSelected)) {
                    canvas.drawCard(graphics, card, 
                            x() + handColumn * width() + 10 * j - 10 * (rounds.get(roundSelected)[1] - 1) / 2, 
                            y() + trumpRowHeight + i * h + h / 2 + 30, 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);
                    j++;
                }
                
                graphics.setColor(Color.BLACK);
                GraphicsTools.drawStringJustified(graphics, 
                        GraphicsTools.fitString(graphics, 
                                "" + players.get(i).getBids().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + bidColumn * width(),//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + i * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + h / 5, 
                        1, 1);
                int delta = Math.min(
                        Math.abs(players.get(i).getBids().get(roundSelected) 
                                - players.get(i).getTakens().get(roundSelected)),
                        3);
                if (players.get(i).getBids().get(roundSelected)
                        == players.get(i).getTakens().get(roundSelected)) {
                    graphics.setColor(new Color(0, 175, 0));
                } else {
                    graphics.setColor(new Color(255 * (delta - 1) / 3 + 195 * (4 - delta) / 3, 0, 0));
                }
                GraphicsTools.drawStringJustified(graphics, 
                        GraphicsTools.fitString(graphics, 
                                "" + players.get(i).getTakens().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + tookColumn * width(),//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + i * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 2 * h / 5, 
                        1, 1);
                graphics.setColor(Color.BLACK);
                GraphicsTools.drawStringJustified(graphics, 
                        GraphicsTools.fitString(graphics, 
                                "" + players.get(i).getAiBids().get(roundSelected), 
                                width() / 4 - 2 * margin), 
                        x() + aiBidColumn * width(),//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + i * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 3 * h / 5, 
                        1, 1);
                /*OhcGraphicsTools.drawStringJustified(graphics, 
                        "Difficulty score: ", 
                        x() + 31 * width() / 32,//x() + 3 * width() / 4 + margin, 
                        y() + trumpRowHeight + iRelToLeader * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 4 * h / 5, 
                        0, 1);*/
                float dScale = (float) ((players.get(i).getDiffs().get(roundSelected) - 1) / 9);
                graphics.setColor(new Color(dScale, 0.75F * (1 - dScale), 0F));
                GraphicsTools.drawStringJustified(graphics, 
                        String.format("%.1f", players.get(i).getDiffs().get(roundSelected)), 
                        x() + diffColumn * width(),//x() + 3 * width() / 4 + margin + graphics.getFontMetrics().stringWidth("Difficulty score: "), 
                        y() + trumpRowHeight + i * h + h / 2,//y() + trumpRowHeight + iRelToLeader * h + 4 * h / 5, 
                        1, 1);
            }
        }
        
        for (int i = 0; i < players.size(); i++) {
            if (qPlots.get(roundSelected).get(i) != null) {
                qPlots.get(roundSelected).get(i).paint(graphics);
            }
        }
        
        graphics.setColor(Color.BLACK);
        graphics.setFont(GraphicsTools.fontSmall);
        GraphicsTools.drawStringJustified(graphics, 
                "Round:", 
                x() + width() / 2
                    - (rounds.size() * buttonSize + (rounds.size() - 1) * margin) / 2
                    - 2 * margin, 
                y() + height()
                    - margin
                    - 0.5 * buttonSize, 
                2, 1);
        graphics.setFont(GraphicsTools.font);
        
        for (CanvasButton button : roundButtons) {
            button.paint(graphics);
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            for (CanvasPlot plot : qPlots.get(roundSelected)) {
                CanvasInteractable inter = plot == null ? null : plot.updateMoused(x, y);
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
