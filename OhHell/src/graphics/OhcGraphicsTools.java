package graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class OhcGraphicsTools {
    public static final Font font = new Font("Arial", Font.PLAIN, 13);
    public static final Font fontBold = new Font("Arial", Font.BOLD, 13);
    public static final Font fontSmall = new Font("Arial", Font.BOLD, 9);
    public static final Font fontTitle = new Font("Arial", Font.BOLD, 52);
    
    public static BufferedImage loadImage(String file, Object obj) {
        BufferedImage img;
        URL imgurl = obj.getClass().getResource("/" + file);
        try {
            if (imgurl != null) {
                img = ImageIO.read(imgurl);
            } else {
                img = ImageIO.read(new File(file));
            }
            return img;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Clip loadSound(String file, Object obj) {
        try {
            Clip clip = AudioSystem.getClip();
            URL soundUrl = obj.getClass().getResource("/" + file);
            if (soundUrl != null) {
                clip.open(AudioSystem.getAudioInputStream(soundUrl));
            } else {
                clip.open(AudioSystem.getAudioInputStream(new File(file)));
            }
            //playSound(clip);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Graphics2D makeGraphics2D(Graphics graphics, boolean smoothen) {
        Graphics2D graphics2 = (Graphics2D) graphics;
        RenderingHints qualityHints = new RenderingHints(
                  RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
            if (m.stringWidth(s0) < wid) {
                return s0;
            }
        }
        return text.charAt(0) + "";
    }
    
    public static void drawStringJustified(Graphics graphics, String text, int x, int y, int posX, 
            int posY) {
        FontMetrics m = graphics.getFontMetrics();
        graphics.drawString(text, x - m.stringWidth(text) * posX / 2, y + m.getHeight() * posY / 3);
    }
    
    public static void drawStringJustifiedBacked(Graphics graphics, String text, int x, int y) {
        FontMetrics m = graphics.getFontMetrics();
        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.fillRoundRect(x - m.stringWidth(text) / 2 - 20, 
                y - m.getHeight() / 3 - 12, 
                m.stringWidth(text) + 40, 
                m.getHeight() + 20, 
                20, 20);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(x - m.stringWidth(text) / 2 - 20, 
                y - m.getHeight() / 3 - 12, 
                m.stringWidth(text) + 40, 
                m.getHeight() + 20, 
                20, 20);
        graphics.drawString(text, x - m.stringWidth(text) / 2, y + m.getHeight() / 3);
    }
}