package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import common.GraphicsTools;
import common.OhcScrollPane;

public class CanvasScoreSheet extends CanvasInteractable {
    public static final int margin = 5;
    public static final int scoreVSpacing = GameCanvas.scoreVSpacing;
    public static final int lineV = 4;
    public static final int sortByHeight = 30;
    public static final int bidInfoHeight = 20;
    public static final int buttonWidth = 60;
    public static final int px = 1;
    public static final int py = 0;
    
    private double dealerHWidth = 10;
    
    private GameCanvas canvas;
    private List<ClientPlayer> players;
    private List<int[]> rounds;
    private ClientPlayer myPlayer;
    private String sortBy = "Seat";
    
    private JPanel scorePanel;
    private OhcScrollPane swingScrollPane;
    private List<CanvasButton> buttons;
    private CanvasEmbeddedSwing scoreScrollPane;
    
    public CanvasScoreSheet(GameCanvas canvas) {
        this.canvas = canvas;

        CanvasScoreSheet sheet = this;
        
        buttons = new LinkedList<>();
        int numButtons = 2;
        // Sort by seat button
        buttons.add(new CanvasButton("Seat") {
            @Override
            public int x() {
                return sheet.x() + sheet.width() / 2 
                        - (numButtons * buttonWidth + (numButtons - 1) * margin) / 2
                        + (buttonWidth + margin) * 0;
            }
            
            @Override
            public int y() {
                return sheet.y() + sheet.height() - bidInfoHeight - margin - sortByHeight + margin;
            }
            
            @Override
            public int width() {
                return buttonWidth;
            }
            
            @Override
            public int height() {
                return sortByHeight - 2 * margin;
            }
            
            @Override
            public boolean isSelected() {
                return sortBy.equals("Seat");
            }
            
            @Override
            public void click() {
                sortBy = "Seat";
            }
        });
        
        // Sort by score button
        buttons.add(new CanvasButton("Score") {
            @Override
            public int x() {
                return sheet.x() + sheet.width() / 2 
                        - (numButtons * buttonWidth + (numButtons - 1) * margin) / 2
                        + (buttonWidth + margin) * 1;
            }
            
            @Override
            public int y() {
                return sheet.y() + sheet.height() - bidInfoHeight - margin - sortByHeight + margin;
            }
            
            @Override
            public int width() {
                return buttonWidth;
            }
            
            @Override
            public int height() {
                return sortByHeight - 2 * margin;
            }
            
            @Override
            public boolean isSelected() {
                return sortBy.equals("Score");
            }
            
            @Override
            public void click() {
                sortBy = "Score";
            }
        });
        
        // Scrollable score panel
        scorePanel = new JPanel() {
            private static final long serialVersionUID = 1L;
            
            private int numRoundsMemo = 0;

            @Override
            public void paintComponent(Graphics graphics1) {
                if (canvas.getState() == GameState.PREGAME) {
                    return;
                }
                
                Graphics2D graphics = GraphicsTools.makeGraphics2D(graphics1, true, false);
                graphics.setColor(new Color(255, 255, 255));
                graphics.fillRect(0, 0, getWidth(), getHeight());
                
                int numRounds = rounds.size();
                
                if (numRounds != numRoundsMemo) {
                    numRoundsMemo = numRounds;
                    setPreferredSize(new Dimension(
                            sheet.width(), 
                            scoreVSpacing * numRounds));
                }
                
                double wid = (double) (width() - 4 * margin - 2 * dealerHWidth) / players.size();
                
                // dealers and hand sizes
                List<ClientPlayer> playersSortedIndex = canvas.getPlayersForScoreSheet("Seat");
                graphics.setColor(Color.BLACK);
                for (int i = 0; i < numRounds; i++) {
                    int[] round = rounds.get(i);
                    GraphicsTools.drawStringJustified(graphics, 
                            "" + round[1], 
                            margin + dealerHWidth / 2 - px, 
                            scoreVSpacing * (i + 0.5) - py, 
                            1, 1);
                    GraphicsTools.drawStringJustified(graphics, 
                            playersSortedIndex.get(round[0]).getName().substring(0, 1), 
                            2 * margin + 1.5 * dealerHWidth - px, 
                            scoreVSpacing * (i + 0.5) - py, 
                            1, 1);
                }

                for (int i = 0; i < players.size(); i++) {
                    ClientPlayer player = players.get(i);
                    // vertical line
                    if (i > 0) {
                        graphics.drawLine(
                                (int) (3 * margin + 2 * dealerHWidth + i * wid - px), 
                                0, 
                                (int) (3 * margin + 2 * dealerHWidth + i * wid - px), 
                                getHeight());
                    }
                    
                    for (int j = 0; j < player.getBids().size()
                            && (player.getKickedAtRound() == -1 || j < player.getKickedAtRound()); j++) {
                        String bid = player.getBids().get(j) + "";
                        String score = j < player.getScores().size() ? player.getScores().get(j) + "" : "";
                        double currentWid = 3 * margin 
                                + graphics.getFontMetrics().stringWidth(score)
                                + GraphicsTools.font.getSize();
                        if (currentWid >= wid) {
                            graphics.setFont(GraphicsTools.fontSmall);
                        }
                        
                        int b = graphics.getFont().getSize() + 3;

                        // bid chips
                        graphics.setColor(new Color(200, 200, 200, 180));
                        graphics.fillOval(
                                (int) (3 * margin + 2 * dealerHWidth + i * wid + 1 + wid - margin - b) - px, 
                                (int) (scoreVSpacing * (j + 0.5) - b / 2) - py, 
                                b, b);
                        graphics.setColor(Color.BLACK);
                        GraphicsTools.drawStringJustified(graphics, 
                                bid, 
                                (int) (3 * margin + 2 * dealerHWidth + i * wid + 1 + wid - margin - b / 2) - px, 
                                scoreVSpacing * (j + 0.5) - py, 
                                1, 1);
                        
                        // scores
                        GraphicsTools.drawStringJustified(graphics, 
                                score, 
                                (int) (3 * margin + 2 * dealerHWidth + i * wid + 1 + wid / 2 - margin / 2 - b / 2) - px, 
                                scoreVSpacing * (j + 0.5) - py, 
                                1, 1);

                        graphics.setFont(GraphicsTools.font);
                    }
                }
            }
        };
        swingScrollPane = new OhcScrollPane(scorePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        swingScrollPane.setBoxed(false);
        swingScrollPane.setBorder(BorderFactory.createEmptyBorder());
        swingScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scoreScrollPane = new CanvasEmbeddedSwing(swingScrollPane, canvas) {
            @Override
            public int x() {
                return sheet.x() + px;
            }
            
            @Override
            public int y() {
                return sheet.y() + margin + scoreVSpacing + lineV / 2 + 1;
            }
            
            @Override
            public int width() {
                return sheet.width() - 2 * px;
            }
            
            @Override
            public int height() {
                return sheet.height() - margin - scoreVSpacing - lineV / 2 - 4 - sortByHeight - bidInfoHeight - margin;
            }
            
            @Override
            public boolean isShown() {
                return sheet.isShown();
            }
        };
    }
    
    public void autoScroll(int roundNumber) {
        int newY = scoreVSpacing * roundNumber - py;
        swingScrollPane.getVerticalScrollBar().setValue(
                Math.min(Math.max(
                            swingScrollPane.getVerticalScrollBar().getValue(), 
                            newY + scoreVSpacing - swingScrollPane.getHeight()),
                        newY));
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (canvas.getState() == GameState.PREGAME) {
            return;
        }
        
        players = canvas.getPlayersForScoreSheet(sortBy);
        rounds = canvas.getRoundsForScoreSheet();
        myPlayer = canvas.getMyPlayer();
        
        if (players.isEmpty() || rounds.isEmpty()) {
            return;
        }
        
        // box
        graphics.setColor(Color.WHITE);
        GraphicsTools.drawBox(graphics, x(), y(), width(), height() - bidInfoHeight - margin, 10);
        
        double wid = (double) (width() - 4 * margin - 2 * dealerHWidth) / players.size();

        // horizontal line
        graphics.setColor(Color.BLACK);
        graphics.drawLine(
                (int) (x() + 3 * margin + 2 * dealerHWidth), 
                y() + margin + scoreVSpacing + lineV / 2, 
                x() + width() - margin, 
                y() + margin + scoreVSpacing + lineV / 2);
        for (int i = 0; i < players.size(); i++) {
            ClientPlayer player = players.get(i);
            // name
            graphics.setColor(Color.BLACK);
            if (player.isDisconnected()) {
                graphics.setColor(Color.GRAY);
            }
            if (player.isKicked()) {
                graphics.setColor(Color.RED);
            }
            
            if (player.equals(myPlayer)) {
                graphics.setFont(GraphicsTools.fontBold);
            } else {
                graphics.setFont(GraphicsTools.font);
            }
            GraphicsTools.drawStringJustified(graphics, 
                    GraphicsTools.fitString(graphics, player.getName(), wid - 2), 
                    (int) (x() + 3 * margin + 2 * dealerHWidth + i * wid + wid / 2), 
                    y() + margin + scoreVSpacing / 2, 
                    1, 1);
            graphics.setFont(GraphicsTools.font);
            
            // vertical line
            if (i > 0) {
                graphics.drawLine(
                        (int) (x() + 3 * margin + 2 * dealerHWidth + i * wid), 
                        y() + margin, 
                        (int) (x() + 3 * margin + 2 * dealerHWidth + i * wid), 
                        y() + margin + scoreVSpacing + lineV / 2);
            }
        }
        
        graphics.setFont(GraphicsTools.fontSmall);
        GraphicsTools.drawStringJustified(graphics, 
                "Sort by:", 
                x() + width() / 2 
                    - (2 * buttonWidth + (2 - 1) * margin) / 2
                    - 2 * margin, 
                y() + height() - bidInfoHeight - margin - sortByHeight / 2, 
                2, 1);

        if (canvas.getState() != GameState.POSTGAME) {
            graphics.setColor(Color.WHITE);
            GraphicsTools.drawBox(graphics, x(), y() + height() - bidInfoHeight, width(), bidInfoHeight, 10);
        }
        graphics.setFont(GraphicsTools.fontBold);
        if (canvas.getState() == GameState.BIDDING
                || canvas.getState() == GameState.PLAYING
                || canvas.getState() == GameState.ENDOFROUND) {
            int handSize = canvas.thisRound()[1];
            if (handSize > 0) {
                int totalBid = players.stream()
                        .map(ClientPlayer::getBid)
                        .reduce(0, (a, b) -> a + b);
                int totalMaxBidTaken = players.stream()
                        .map(p -> Math.max(p.getBid(), p.getTaken()))
                        .reduce(0, (a, b) -> a + b);
                
                if (totalBid <= handSize) {
                    graphics.setColor(new Color(0, 0, 120));
                    GraphicsTools.drawStringJustified(graphics, 
                            "Underbid by " + (handSize - totalBid), 
                            x() + width() / 4, 
                            y() + height() - bidInfoHeight / 2, 
                            1, 1);
                } else {
                    graphics.setColor(new Color(120, 0, 0));
                    GraphicsTools.drawStringJustified(graphics, 
                            "Overbid by " + (totalBid - handSize), 
                            x() + width() / 4, 
                            y() + height() - bidInfoHeight / 2, 
                            1, 1);
                }

                if (totalMaxBidTaken < handSize) {
                    graphics.setColor(new Color(0, 0, 120));
                    GraphicsTools.drawStringJustified(graphics, 
                            "Unwanted tricks: " + (handSize - totalMaxBidTaken), 
                            x() + 3 * width() / 4, 
                            y() + height() - bidInfoHeight / 2, 
                            1, 1);
                } else if (totalMaxBidTaken > handSize) {
                    graphics.setColor(new Color(120, 0, 0));
                    GraphicsTools.drawStringJustified(graphics, 
                            "Excess wanted tricks: " + (totalMaxBidTaken - handSize), 
                            x() + 3 * width() / 4, 
                            y() + height() - bidInfoHeight / 2, 
                            1, 1);
                } else {
                    graphics.setColor(new Color(0, 120, 0));
                    GraphicsTools.drawStringJustified(graphics, 
                            "Unwanted tricks: 0", 
                            x() + 3 * width() / 4, 
                            y() + height() - bidInfoHeight / 2, 
                            1, 1);
                }
            }
        }

        graphics.setFont(GraphicsTools.font);
        scoreScrollPane.paint(graphics);
        for (CanvasButton button : buttons) {
            button.paint(graphics);
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            for (CanvasButton button : buttons) {
                CanvasInteractable inter = button.updateMoused(x, y);
                if (inter != null) {
                    ans = inter;
                }
            }
        }
        return ans;
    }
}
