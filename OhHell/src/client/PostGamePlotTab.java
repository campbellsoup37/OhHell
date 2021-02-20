package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import graphics.OhcGraphicsTools;

public class PostGamePlotTab extends CanvasInteractable {
    private List<ClientPlayer> sortedPlayers;
    private CanvasScorePlot plot;
    
    public PostGamePlotTab(List<ClientPlayer> sortedPlayers) {
        this.sortedPlayers = sortedPlayers;
        PostGamePlotTab tab = this;
        plot = new CanvasScorePlot() {
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
    
    public CanvasScorePlot getPlot() {
        return plot;
    }
    
    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        OhcGraphicsTools.drawBox(graphics, 
                x(),
                y(),
                GameCanvas.finalScoreListWidth,
                height(),
                10);
        graphics.setFont(OhcGraphicsTools.fontBold);
        OhcGraphicsTools.drawStringJustified(graphics, 
                "Final scores", 
                x() + GameCanvas.finalScoreListWidth / 2, 
                y() + 20,
                1, 1);
        graphics.setFont(OhcGraphicsTools.font);
        for (int i = 0; i < sortedPlayers.size(); i++) {
            ClientPlayer player = sortedPlayers.get(i);
            graphics.setColor(GameCanvas.colors[player.getIndex()]);
            graphics.fillOval(
                    (int) (x() + GameCanvas.finalScoreInnerMargin - GameCanvas.pointSize / 2), 
                    (int) (y() + 30 + 15 * (i + 1) - GameCanvas.pointSize / 2), 
                    (int) (GameCanvas.pointSize),
                    (int) (GameCanvas.pointSize));
            graphics.setColor(Color.BLACK);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    OhcGraphicsTools.fitString(
                            graphics, 
                            player.getPlace() + ". " 
                            + player.getName(), 
                            0.8 * GameCanvas.finalScoreListWidth - 2 * GameCanvas.finalScoreInnerMargin),
                    x() + GameCanvas.finalScoreInnerMargin + GameCanvas.finalScoreInnerMargin / 2, 
                    y() + 30 + 15 * (i + 1), 
                    0, 1);
            OhcGraphicsTools.drawStringJustified(graphics, 
                    player.getScore() + "",
                    x() + (int) (0.8 * GameCanvas.finalScoreListWidth), 
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
