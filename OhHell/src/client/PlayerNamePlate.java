package client;

import java.awt.Color;
import java.awt.Graphics;

import common.GraphicsTools;

public class PlayerNamePlate extends CanvasInteractable {
    private GameCanvas canvas;
    private ClientPlayer player;
    
    public PlayerNamePlate(GameCanvas canvas, ClientPlayer player) {
        this.canvas = canvas;
        this.player = player;
    }
    
    @Override
    public int x() {
        return (int) (player.getX() - player.getJust() * width() / 2);
    }
    
    @Override
    public int y() {
        return (int) (player.getY() - 10);
    }
    
    @Override
    public int width() {
        return (int) canvas.getMaxWid();
    }
    
    @Override
    public int height() {
        return 20;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (canvas.getState() == GameState.POSTGAME) {
            return;
        }
        
        if (isEnabled()) {
            graphics.setColor(new Color(175, 175, 0, 20));
            for (int i = -10; i < 0; i++) {
                graphics.fillRoundRect(x() + i, y() + i, width() - 2 * i, height() - 2 * i, 50, 50);
            }
        }
        
        if (canvas.getState() != GameState.PREGAME 
                && (player.getBidding() == 1 || player.isPlaying()) 
                || canvas.getState() == GameState.PREGAME && player.isHost()) {
            if (isMoused() && isEnabled()) {
                graphics.setColor(new Color(192, 192, 0));
            } else {
                graphics.setColor(new Color(255, 255, 0));
            }
        } else if (player.isHuman()) {
            if (isMoused() && isEnabled()) {
                graphics.setColor(new Color(192, 192, 192));
            } else {
                graphics.setColor(Color.WHITE);
            }
        } else {
            graphics.setColor(new Color(210, 255, 255));
        }
        GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 20);
        
        // Name
        if (player.isDisconnected()) {
            graphics.setColor(Color.GRAY);
        }
        if (player.isKicked()) {
            graphics.setColor(Color.RED);
        }
        GraphicsTools.drawStringJustified(graphics, 
                GraphicsTools.fitString(graphics, player.getName(), width() - 20 * 2), 
                x() + width() / 2, 
                y() + height() / 2, 
                1, 1);
        
        if (canvas.getState() != GameState.PREGAME) {
            // Bid chip
            if (player.hasBid()) {
                int iRelToMe = player.getIndex() - canvas.getMyPlayer().getIndex();
                double startX = (canvas.getWidth() - 450) / 2
                        - 100 * Math.sin(2 * Math.PI * iRelToMe / canvas.getPlayers().size());
                double startY = canvas.getHeight() / 2 - 50
                        + 100 * Math.cos(2 * Math.PI * iRelToMe / canvas.getPlayers().size());
                
                double endX = x() + 10;
                double endY = y() + height() / 2;
                
                double bidX = startX * (1 - player.getBidTimer()) + endX * player.getBidTimer();
                double bidY = startY * (1 - player.getBidTimer()) + endY * player.getBidTimer();
                double radius = 50 * (1 - player.getBidTimer()) + 16 * player.getBidTimer();
                
                if (player.getBidTimer() < 1) {
                    graphics.setColor(new Color(255, 255, 255, 180));
                } else if (canvas.getState() == GameState.BIDDING || canvas.getState() == GameState.PLAYING && player.getBid() > player.getTaken()) {
                    graphics.setColor(new Color(175, 175, 175, 180));
                } else if (player.getBid() == player.getTaken()) {
                    graphics.setColor(new Color(175, 255, 175));
                } else {
                    graphics.setColor(new Color(255, 175, 175));
                }
                graphics.fillOval((int) (bidX - radius / 2), (int) (bidY - radius / 2), (int) radius, (int) radius);
                graphics.setColor(Color.BLACK);
                if (player.getBidTimer() == 0) {
                    graphics.drawOval((int) (bidX - radius / 2), (int) (bidY - radius / 2), (int) radius, (int) radius);
                    graphics.setFont(GraphicsTools.fontLargeBold);
                }
                GraphicsTools.drawStringJustified(graphics, player.getBid() + "", 
                        bidX, 
                        bidY, 
                        1, 1);
                graphics.setFont(GraphicsTools.font);
            }
            
            // Dealer chip
            if (player.getIndex() == canvas.getDealer()) {
                graphics.setColor(Color.CYAN);
                graphics.fillOval((int) (x() + width() - 19), y() + height() / 2 - 8, 16, 16);
                graphics.setColor(Color.BLACK);
                GraphicsTools.drawStringJustified(graphics, "D", 
                        (int) (x() + width() - 11), 
                        y() + height() / 2, 
                        1, 1);
            }
        }
    }
}
