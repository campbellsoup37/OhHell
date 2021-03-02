package common;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

public class OhcScrollPane extends JScrollPane {
    private static final long serialVersionUID = 1L;
    
    private boolean boxed = true;
    
    public OhcScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
        
        setOpaque(false);
        setBorder(new EmptyBorder(2, 6, 2, 6));
        getVerticalScrollBar().setPreferredSize(new Dimension(5, getHeight()));
        getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        view.setFont(GraphicsTools.font);
    }

    public boolean isBoxed() {
        return boxed;
    }

    public void setBoxed(boolean boxed) {
        this.boxed = boxed;
    }
    
    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D graphics2 = GraphicsTools.makeGraphics2D(graphics, true, false);
        if (isBoxed()) {
            graphics2.setColor(Color.WHITE);
            GraphicsTools.drawBox(graphics2, 0, 0, getWidth() - 1, getHeight() - 1, 15);
        }
        super.paintComponent(graphics2);
    }
}
