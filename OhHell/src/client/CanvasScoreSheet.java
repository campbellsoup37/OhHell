package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import common.GraphicsTools;

public class CanvasScoreSheet extends CanvasInteractable {
    public static final int margin = 5;
    public static final int scoreVSpacing = GameCanvas.scoreVSpacing;
    public static final int lineV = 4;
    
    private double dealerHWidth = 10;
    
    private GameCanvas canvas;
    
    public CanvasScoreSheet(GameCanvas canvas) {
        this.canvas = canvas;
    }
    
    @Override
    public void paint(Graphics graphics) {
        List<ClientPlayer> players = canvas.getPlayersForScoreSheet();
        List<int[]> rounds = canvas.getRoundsForScoreSheet();
        ClientPlayer myPlayer = canvas.getMyPlayer();
        
        int numRounds = rounds.size();
        
        if (players.isEmpty() || rounds.isEmpty()) {
            return;
        }
        
        // box
        graphics.setColor(Color.WHITE);
        GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        
        double wid = (double) (width() - 4 * margin - 2 * dealerHWidth) / players.size();

        // horizontal line
        graphics.setColor(Color.BLACK);
        graphics.drawLine(
                (int) (x() + 3 * margin + 2 * dealerHWidth), 
                y() + margin + scoreVSpacing + lineV / 2, 
                x() + width() - margin, 
                y() + margin + scoreVSpacing + lineV / 2);
        for (ClientPlayer player : players) {
            int index = player.getIndex();
            graphics.setColor(Color.BLACK);
            // vertical line
            if (index > 0) {
                graphics.drawLine(
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid), 
                        y() + margin, 
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid), 
                        y() + height() - margin);
            }
            
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
                    (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid + wid / 2), 
                    y() + margin + scoreVSpacing / 2, 
                    1, 1);
            graphics.setFont(GraphicsTools.font);
        }
        
        // dealers and hand sizes
        graphics.setColor(Color.BLACK);
        for (int i = 0; i < numRounds; i++) {
            int[] round = rounds.get(i);
            GraphicsTools.drawStringJustified(graphics, 
                    "" + round[1], 
                    x() + margin + dealerHWidth / 2, 
                    y() + margin + lineV + scoreVSpacing * (i + 1.5), 
                    1, 1);
            GraphicsTools.drawStringJustified(graphics, 
                    players.get(round[0]).getName().substring(0, 1), 
                    x() + 2 * margin + 1.5 * dealerHWidth, 
                    y() + margin + lineV + scoreVSpacing * (i + 1.5), 
                    1, 1);
        }
    
        for (ClientPlayer player : players) {
            int index = player.getIndex();
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
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid + 1 + wid - margin - b), 
                        (int) (y() + margin + lineV + scoreVSpacing * (j + 1.5) - b / 2), 
                        b, b);
                graphics.setColor(Color.BLACK);
                GraphicsTools.drawStringJustified(graphics, 
                        bid, 
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid + 1 + wid - margin - b / 2), 
                        y() + margin + lineV + scoreVSpacing * (j + 1.5), 
                        1, 1);
                
                // scores
                GraphicsTools.drawStringJustified(graphics, 
                        score, 
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid + 1 + wid / 2 - margin / 2 - b / 2), 
                        y() + margin + lineV + scoreVSpacing * (j + 1.5), 
                        1, 1);

                graphics.setFont(GraphicsTools.font);
            }
        }
    }
}
