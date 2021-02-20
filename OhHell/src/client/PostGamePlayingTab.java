package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import core.Card;
import graphics.OhcGraphicsTools;

public class PostGamePlayingTab extends CanvasInteractable {
    private final int trumpRowHeight = 35;
    private final int margin = 4;
    private final int buttonSize = 18;
    
    private final double trumpColumn = 3.0 / 16;
    private final double ledColumn = 5.5 / 16;
    private final double handColumn = 5.0 / 8;
    private final double wantColumn = 7.0 / 8;
    
    private GameCanvas canvas;
    private List<ClientPlayer> players;
    private List<int[]> rounds;
    private List<CanvasButton> roundButtons;
    private int roundSelected = 0;
    private int trickSelected = 0;
    
    private List<List<List<Integer>>> wants;
    private class CardInfo {
        public Card card;
        public Double prob;
        public boolean played;
        public boolean led;
        public boolean won;
    }
    
    private List<List<List<List<CardInfo>>>> allCards;
    
    public PostGamePlayingTab(GameCanvas canvas, List<ClientPlayer> players, List<int[]> rounds) {
        this.canvas = canvas;
        this.players = players;
        this.rounds = rounds;
        
        PostGamePlayingTab tab = this;
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
        
        wants = new ArrayList<>(rounds.size());
        allCards = new ArrayList<>(rounds.size());
        for (int i = 0; i < rounds.size(); i++) {
            wants.add(new ArrayList<>(rounds.get(i)[1]));
            allCards.add(new ArrayList<>(rounds.get(i)[1]));
            for (int j = 0; j < rounds.get(i)[1]; j++) {
                wants.get(i).add(new ArrayList<>(players.size()));
                allCards.get(i).add(new ArrayList<>(players.size()));
                for (int k = 0; k < players.size(); k++) {
                    if (j == 0) {
                        wants.get(i).get(j).add(players.get(k).getBids().get(i));
                    } else {
                        wants.get(i).get(j).add(Math.max(0, 
                                        wants.get(i).get(j - 1).get(k) - (players.get(k).getWons().get(i).get(j - 1) ? 1 : 0)));
                    }
                    
                    allCards.get(i).get(j).add(new ArrayList<>(rounds.get(i)[1] - j));
                    
                    List<Card> hand = new ArrayList<>(rounds.get(i)[1]);
                    hand.addAll(players.get(k).getHands().get(i));
                    for (int j1 = 0; j1 < j; j1++) {
                        final int iF = i;
                        final int kF = k;
                        final int j1F = j1;
                        hand.removeIf(c -> c.equals(players.get(kF).getPlays().get(iF).get(j1F)));
                    }
                    
                    for (Card card : hand) {
                        CardInfo ci = new CardInfo();
                        ci.card = card;
                        ci.prob = players.get(k).getMakingProbs().get(i).get(j).get(card);
                        ci.played = card.equals(players.get(k).getPlays().get(i).get(j));
                        ci.led = players.get(k).getLeds().get(i).get(j);
                        ci.won = players.get(k).getWons().get(i).get(j);
                        allCards.get(i).get(j).get(k).add(ci);
                    }
                }
            }
        }
    }
    
    public int getSelectedRound() {
        return roundSelected;
    }
    
    public void selectRound(int round) {
        trickSelected = 0;
        roundSelected = Math.min(rounds.size() - 1, Math.max(0, round));
    }
    
    public int getSelectedTrick() {
        return trickSelected;
    }
    
    public void selectTrick(int trick) {
        if (trick < 0) {
            if (roundSelected == 0) {
                trickSelected = 0;
            } else {
                selectRound(roundSelected - 1);
                selectTrick(trick + rounds.get(roundSelected)[1]);
            }
        } else if (trick > rounds.get(roundSelected)[1] - 1) {
            if (roundSelected == rounds.size() - 1) {
                trickSelected = rounds.get(roundSelected)[1] - 1;
            } else {
                int diff = trick - rounds.get(roundSelected)[1];
                selectRound(roundSelected + 1);
                selectTrick(diff);
            }
        } else {
            trickSelected = Math.min(rounds.get(roundSelected)[1] - 1, Math.max(0, trick));
        }
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        OhcGraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        
        graphics.setColor(Color.BLACK);
        /*canvas.drawCard(graphics, canvas.getTrumps().get(roundSelected), 
                x() + handColumn * width(), 
                y() + trumpRowHeight / 2 + 30, 
                GameCanvas.smallCardScale, true, false,
                y() + trumpRowHeight);*/
        graphics.setFont(OhcGraphicsTools.fontSmall);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Trump", 
                x() + trumpColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Led/Won", 
                x() + ledColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Hand", 
                x() + handColumn * width(), 
                y() + trumpRowHeight / 2, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Tricks", 
                x() + wantColumn * width(), 
                y() + trumpRowHeight / 3, 
                1, 1);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Needed", 
                x() + wantColumn * width(), 
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
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(graphics, player.getName(), 
                                width() / 4 - 2 * margin + 25), 
                        x() + 2 * margin + 25, 
                        y() + trumpRowHeight + i * h + h / 2, 
                        0, 1);
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
                
                boolean led = players.get(i).getLeds().get(roundSelected).get(trickSelected);
                boolean won = players.get(i).getWons().get(roundSelected).get(trickSelected);
                
                if (led) {
                    graphics.setColor(new Color(200, 200, 200));
                    graphics.fillOval(
                            (int) (x() + ledColumn * width() - 8 - (won ? 10 : 0)), 
                            (int) (y() + trumpRowHeight + i * h + h / 2 - 8), 
                            16, 16);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, 
                            ">", 
                            x() + ledColumn * width() - (won ? 10 : 0), 
                            y() + trumpRowHeight + i * h + h / 2, 
                            1, 1);
                }
                if (won) {
                    graphics.setColor(new Color(175, 175, 0));
                    graphics.fillOval(
                            (int) (x() + ledColumn * width() - 8 + (led ? 10 : 0)), 
                            (int) (y() + trumpRowHeight + i * h + h / 2 - 8), 
                            16, 16);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, 
                            "w", 
                            x() + ledColumn * width() + (led ? 10 : 0), 
                            y() + trumpRowHeight + i * h + h / 2, 
                            1, 1);
                }
                
                graphics.setFont(OhcGraphicsTools.fontSmall);
                int j = 0;
                for (CardInfo ci : allCards.get(roundSelected).get(trickSelected).get(i)) {
                    canvas.drawCard(graphics, ci.card, 
                            x() + handColumn * width() + 30 * j - 30 * (rounds.get(roundSelected)[1] - trickSelected - 1) / 2, 
                            y() + trumpRowHeight + i * h + h / 2 + 40 - (ci.played ? 15 : 0), 
                            GameCanvas.smallCardScale, true, false,
                            y() + trumpRowHeight + i * h + h);

                    if (ci.prob != null) {
                        float dScale = (float) (double) (1 - ci.prob);
                        graphics.setColor(new Color(dScale, 0.75F * (1 - dScale), 0F));
                        OhcGraphicsTools.drawStringJustified(graphics, 
                                (int) (ci.prob * 100) + "%", 
                                x() + handColumn * width() + 30 * j - 30 * (rounds.get(roundSelected)[1] - trickSelected - 1) / 2, 
                                y() + trumpRowHeight + i * h + h / 2 - 18,
                                1, 1);
                    }
                    
                    j++;
                }
                graphics.setFont(OhcGraphicsTools.font);

                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        wants.get(roundSelected).get(trickSelected).get(i) + "",
                        x() + wantColumn * width(), 
                        y() + trumpRowHeight + i * h + h / 2, 
                        0, 1);
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
