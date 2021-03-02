package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
    public static final int px = 1;
    public static final int py = 5;
    
    private double dealerHWidth = 10;
    
    private GameCanvas canvas;
    private List<ClientPlayer> players;
    private List<int[]> rounds;
    private ClientPlayer myPlayer;
    
    private JPanel scorePanel;
    private CanvasEmbeddedSwing scoreScrollPane;
    
    public CanvasScoreSheet(GameCanvas canvas) {
        this.canvas = canvas;
        
        CanvasScoreSheet sheet = this;
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
                            scoreVSpacing * numRounds + CanvasScoreSheet.lineV / 2 + CanvasScoreSheet.margin - 2));
                }
                
                double wid = (double) (width() - 4 * margin - 2 * dealerHWidth) / players.size();
                
                // dealers and hand sizes
                graphics.setColor(Color.BLACK);
                for (int i = 0; i < numRounds; i++) {
                    int[] round = rounds.get(i);
                    GraphicsTools.drawStringJustified(graphics, 
                            "" + round[1], 
                            margin + dealerHWidth / 2 - px, 
                            scoreVSpacing * (i + 1) - py, 
                            1, 1);
                    GraphicsTools.drawStringJustified(graphics, 
                            players.get(round[0]).getName().substring(0, 1), 
                            2 * margin + 1.5 * dealerHWidth - px, 
                            scoreVSpacing * (i + 1) - py, 
                            1, 1);
                }
            
                for (ClientPlayer player : players) {
                    int index = player.getIndex();
                    // vertical line
                    if (index > 0) {
                        graphics.drawLine(
                                (int) (3 * margin + 2 * dealerHWidth + index * wid - px), 
                                0, 
                                (int) (3 * margin + 2 * dealerHWidth + index * wid - px), 
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
                                (int) (3 * margin + 2 * dealerHWidth + index * wid + 1 + wid - margin - b) - px, 
                                (int) (scoreVSpacing * (j + 1) - b / 2) - py, 
                                b, b);
                        graphics.setColor(Color.BLACK);
                        GraphicsTools.drawStringJustified(graphics, 
                                bid, 
                                (int) (3 * margin + 2 * dealerHWidth + index * wid + 1 + wid - margin - b / 2) - px, 
                                scoreVSpacing * (j + 1) - py, 
                                1, 1);
                        
                        // scores
                        GraphicsTools.drawStringJustified(graphics, 
                                score, 
                                (int) (3 * margin + 2 * dealerHWidth + index * wid + 1 + wid / 2 - margin / 2 - b / 2) - px, 
                                scoreVSpacing * (j + 1) - py, 
                                1, 1);

                        graphics.setFont(GraphicsTools.font);
                    }
                }
            }
        };
        OhcScrollPane swingScrollPane = new OhcScrollPane(scorePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        swingScrollPane.setBoxed(false);
        swingScrollPane.setBorder(BorderFactory.createEmptyBorder());
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
                return sheet.height() - margin - scoreVSpacing - lineV / 2 - 2 * 1;
            }
            
            @Override
            public boolean isShown() {
                return sheet.isShown();
            }
            
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() {
//                        scorePanel.paint(graphics);
//                    }
//                });
            }
        };
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (canvas.getState() == GameState.PREGAME) {
            return;
        }
        
        players = canvas.getPlayersForScoreSheet();
        rounds = canvas.getRoundsForScoreSheet();
        myPlayer = canvas.getMyPlayer();
        
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
            
            // vertical line
            if (index > 0) {
                graphics.drawLine(
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid), 
                        y() + margin, 
                        (int) (x() + 3 * margin + 2 * dealerHWidth + index * wid), 
                        y() + height() - margin);
            }
        }
        
        scoreScrollPane.paint(graphics);
    }
}
