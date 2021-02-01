package graphics;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

public class OhcButton extends JButton {
    private static final long serialVersionUID = 1L;
    
    private boolean mouseOver = false;
    private boolean alert = false;
    
    public OhcButton(String text) {
        super(text);
        
        setOpaque(false);
        setBorder(new EmptyBorder(2, 6, 2, 6));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseOver = true;
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                mouseOver = false;
            }
        });
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }
    
    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D graphics2 = OhcGraphicsTools.makeGraphics2D(graphics, true, false);
        if (mouseOver && !alert) {
            graphics2.setColor(new Color(192, 192, 192));
        } else if (!mouseOver && !alert) {
            graphics2.setColor(Color.WHITE);
        } else if (mouseOver) {
            graphics2.setColor(new Color(112, 225, 112));
        } else {
            graphics2.setColor(new Color(175, 255, 175));
        }
        graphics2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
        graphics2.setColor(Color.BLACK);
        graphics2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
        
        graphics2.setFont(OhcGraphicsTools.fontBold);
        OhcGraphicsTools.drawStringJustified(graphics2, getText(), getWidth() / 2, getHeight() / 2, 1, 1);
        
        //super.paintComponent(graphics2);
    }
}
