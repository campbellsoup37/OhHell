package common;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class FileTools {
    public static BufferedReader getInternalFile(String path, Object obj) {
        BufferedReader reader;
        InputStream in = obj.getClass().getResourceAsStream("/" + path);
        try {
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(path));
            }
            return reader;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
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
}
