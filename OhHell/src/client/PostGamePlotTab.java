package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import common.GraphicsTools;

public class PostGamePlotTab extends CanvasInteractable {
    private List<ClientPlayer> sortedPlayers;
    private CanvasPlot plot;
    
    private double maxValueWidth = Integer.MIN_VALUE;
    private boolean widthMemo = false;
    
    public PostGamePlotTab(List<ClientPlayer> sortedPlayers) {
        this.sortedPlayers = sortedPlayers;
        PostGamePlotTab tab = this;
        plot = new CanvasPlot() {
            @Override
            public int x() {
                return tab.x()
                        + GameCanvas.finalScoreInnerMargin
                        + GameCanvas.finalScoreListWidth;
            }
            
            @Override
            public int y() {
                return tab.y();
            }
            
            @Override
            public int width() {
                return tab.width()
                        - GameCanvas.finalScoreInnerMargin
                        - GameCanvas.finalScoreListWidth;
            }
            
            @Override
            public int height() {
                return tab.height();
            }
        };
    }
    
    public CanvasPlot getPlot() {
        return plot;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (!widthMemo) {
            for (ClientPlayer player : sortedPlayers) {
                maxValueWidth = Math.max(
                        graphics.getFontMetrics().stringWidth(player.getScore() + ""), 
                        maxValueWidth);
            }
            widthMemo = true;
        }
        
        graphics.setColor(Color.WHITE);
        GraphicsTools.drawBox(graphics, 
                x(),
                y(),
                GameCanvas.finalScoreListWidth,
                height(),
                10);
        graphics.setFont(GraphicsTools.fontBold);
        graphics.setColor(Color.BLACK);
        GraphicsTools.drawStringJustified(graphics, 
                "Final scores", 
                x() + GameCanvas.finalScoreListWidth / 2, 
                y() + 20,
                1, 1);
        graphics.setFont(GraphicsTools.font);
        for (int i = 0; i < sortedPlayers.size(); i++) {
            ClientPlayer player = sortedPlayers.get(i);
            graphics.setColor(GameCanvas.colors[player.getIndex()]);
            graphics.fillOval(
                    (int) (x() + GameCanvas.finalScoreInnerMargin - GameCanvas.pointSize / 2), 
                    (int) (y() + 30 + 15 * (i + 1) - GameCanvas.pointSize / 2), 
                    (int) (GameCanvas.pointSize),
                    (int) (GameCanvas.pointSize));
            graphics.setColor(Color.BLACK);
            GraphicsTools.drawStringJustified(graphics, 
                    GraphicsTools.fitString(
                            graphics, 
                            player.getPlace() + ". " 
                            + player.getName(), 
                            GameCanvas.finalScoreListWidth - 4 * GameCanvas.finalScoreInnerMargin - maxValueWidth),
                    x() + 2 * GameCanvas.finalScoreInnerMargin, 
                    y() + 30 + 15 * (i + 1), 
                    0, 1);
            GraphicsTools.drawStringJustified(graphics, 
                    player.getScore() + "",
                    x() + (int) (GameCanvas.finalScoreListWidth - GameCanvas.finalScoreInnerMargin - maxValueWidth / 2), 
                    y() + 30 + 15 * (i + 1), 
                    1, 1);
        }
        
        plot.paint(graphics);
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            CanvasInteractable inter = plot.updateMoused(x, y);
            if (inter != null) {
                ans = inter;
            }
        }
        return ans;
    }
}
