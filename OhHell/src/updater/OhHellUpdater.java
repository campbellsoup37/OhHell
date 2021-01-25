package updater;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import graphics.OhcGraphicsTools;

public class OhHellUpdater extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private String newVersion;
    
    private JPanel loadingBar = new JPanel() {
        private static final long serialVersionUID = 1L;
        
        @Override
        public void paintComponent(Graphics graphics) {
            graphics.setColor(new Color(175, 255, 175));
            graphics.fillRect(0, 0, (int) (progress * getWidth()), getHeight());
            graphics.setColor(Color.BLACK);
            OhcGraphicsTools.drawStringJustified(graphics, "Downloading v" + newVersion, getWidth() / 2, getHeight() / 2, 1, 1);
        }
    };

    private double progress = 0;
    
    public OhHellUpdater(String newVersion) {
        this.newVersion = newVersion;
    }

    public void execute() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            setTitle("");
            setLayout(null);
            
            setIconImage(OhcGraphicsTools.loadImage("resources/icon/cw.png", this));
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.NONE);
            
            loadingBar.setBounds(0, 0, 200, 50);
            add(loadingBar);
            
            setSize(new Dimension(200, 50));
            setResizable(false);
            setLocationRelativeTo(null);
            setVisible(true);
            
            download();
            closeAndRunClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void download() {
        try {
            URL url = new URL("https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/OhHellClient.jar");
            
            URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
            InputStream in = connection.getInputStream();
            int size = connection.getContentLength();
            in.close();
            
            BufferedInputStream newClientJarInput = new BufferedInputStream(
                    url.openStream());
            FileOutputStream newClientJarOutput = new FileOutputStream("OhHellClient.jar");
            
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = newClientJarInput.read(dataBuffer, 0, 1024)) != -1) {
                newClientJarOutput.write(dataBuffer, 0, bytesRead);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progress += (double) 1024 / size;
                        loadingBar.repaint();
                    }
                });
            }
            
            newClientJarInput.close();
            newClientJarOutput.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    public void closeAndRunClient() throws IOException {
        dispose();
        Runtime.getRuntime().exec("java -jar OhHellClient.jar");
    }
    
    public static void main(String[] args) {
        String version = "";
        if (args.length > 0) {
            version = args[0];
        }
        OhHellUpdater updater = new OhHellUpdater(version);
        updater.execute();
    }
}
