package common;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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
    
    public static void downloadFile(String urlPath, String outputPath) {
        try {
            URL url = new URL(urlPath);
            
            URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
            InputStream in = connection.getInputStream();
            in.close();
            
            BufferedInputStream newUpdaterJarInput = new BufferedInputStream(
                    url.openStream());
            FileOutputStream newUpdaterJarOutput = new FileOutputStream(outputPath);
            
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = newUpdaterJarInput.read(dataBuffer, 0, 1024)) != -1) {
                newUpdaterJarOutput.write(dataBuffer, 0, bytesRead);
            }
            
            newUpdaterJarInput.close();
            newUpdaterJarOutput.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    public static void deleteFile(String path) {
        new File(path).delete();
    }
    
    public static String cmdJava() {
        return System.getProperty("java.home") + "/bin/java";
    }
    
    public static String getCurrentVersion() {
        String version = "";
        
        try {
            BufferedReader versionReader = new BufferedReader(
                    new InputStreamReader(
                            new URL("https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/version")
                            .openStream()));
            version = versionReader.readLine();
            versionReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return version;
    }
}
