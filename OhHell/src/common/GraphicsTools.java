package common;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class GraphicsTools {
    public static final Font font = new Font("Arial", Font.PLAIN, 13);
    public static final Font fontBold = new Font("Arial", Font.BOLD, 13);
    public static final Font fontSmall = new Font("Arial", Font.BOLD, 9);
    public static final Font fontLargeBold = new Font("Arial", Font.BOLD, 40);
    public static final Font fontTitle = new Font("Arial", Font.BOLD, 52);
    
    public static Graphics2D makeGraphics2D(Graphics graphics, boolean antialiasing, boolean smoothen) {
        Graphics2D graphics2 = (Graphics2D) graphics;
        RenderingHints qualityHints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING, antialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        //qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        //qualityHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        //qualityHints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        //qualityHints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        if (smoothen) {
            //qualityHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        //qualityHints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        qualityHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //qualityHints.put(RenderingHints.KEY_TEXT_LCD_CONTRAST, 250);
        graphics2.setRenderingHints(qualityHints);
        return graphics2;
    }
    
    public static String fitString(Graphics graphics, String text, double wid) {
        FontMetrics m = graphics.getFontMetrics();
        for (int i = 0; i < text.length(); i++) {
            String s0 = text.substring(0, text.length() - i).trim();
            if (m.stringWidth(s0) < wid + 1) {
                return s0;
            }
        }
        return text.charAt(0) + "";
    }
    
    public static void drawStringJustified(Graphics graphics, String text, double x, double y, int posX, 
            int posY) {
        FontMetrics m = graphics.getFontMetrics();
        graphics.drawString(text, 
                (int) (x - m.stringWidth(text) * posX / 2), 
                (int) (y + m.getHeight() * posY / 3));
    }
    
    public static void drawStringJustifiedBacked(Graphics graphics, String text, double x, double y) {
        FontMetrics m = graphics.getFontMetrics();
        graphics.setColor(new Color(255, 255, 255, 180));
        drawBox(graphics,
                (x - m.stringWidth(text) / 2 - 20), 
                (y - m.getHeight() / 3 - 12), 
                m.stringWidth(text) + 40, 
                m.getHeight() + 20, 
                20);
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, 
                (int) (x - m.stringWidth(text) / 2), 
                (int) (y + m.getHeight() / 3));
    }
    
    public static void drawBox(Graphics graphics, double x, double y, double width, double height, double roundness) {
        Color color = graphics.getColor();
        graphics.fillRoundRect(
                (int) x, (int) y, (int) width, (int) height, (int) roundness, (int) roundness);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(
                (int) x, (int) y, (int) width, (int) height, (int) roundness, (int) roundness);
        graphics.setColor(color);
    }
}
