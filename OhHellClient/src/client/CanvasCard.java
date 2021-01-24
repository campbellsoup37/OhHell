package client;
import java.awt.Graphics;

import core.Card;

public class CanvasCard extends CanvasInteractable {
    private Card card;
    private double scale;
    private boolean small;
    private GameCanvas canvas;
    
    public CanvasCard(Card card, double scale, boolean small, GameCanvas canvas) {
        this.card = card;
        this.scale = scale;
        this.small = small;
        this.canvas = canvas;
    }
    
    public Card getCard() {
        return card;
    }
    
    public int xCenter() {
        return 0;
    }
    
    public int yCenter() {
        return 0;
    }
    
    public int xPaintOffset() {
        return 0;
    }
    
    public int yPaintOffset() {
        return 0;
    }
    
    @Override
    public int x() {
        return (int) ((double) xCenter() - scale * canvas.getCardWidth(small) / 2);
    }
    
    @Override
    public int y() {
        return (int) ((double) yCenter() - scale * canvas.getCardHeight(small) / 2);
    }
    
    @Override
    public int width() {
        return (int) (scale * canvas.getCardWidth(small));
    }

    @Override
    public int height() {
        return (int) (scale * canvas.getCardHeight(small));
    }
    
    public boolean hidden() {
        return false;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            /*graphics.setColor(Color.WHITE);
            graphics.fillRoundRect(x() + xPaintOffset(), y() + yPaintOffset(), width(), height(), 10, 10);
            graphics.setColor(Color.BLACK);
            graphics.drawRoundRect(x() + xPaintOffset(), y() + yPaintOffset(), width(), height(), 10, 10);
            OhcGraphicsTools.drawStringJustified(graphics, card + "", x() + xPaintOffset() + 10, y() + yPaintOffset() + 10, 1, 1);*/
            canvas.drawCard(graphics, hidden() ? new Card() : card, xCenter() + xPaintOffset(), yCenter() + yPaintOffset(), scale, small, isMoused());
        }
    }
}
