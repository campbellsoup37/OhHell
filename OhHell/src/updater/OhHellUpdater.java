package updater;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import common.FileTools;
import common.GraphicsTools;

public class OhHellUpdater extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private String newVersion;
    private String fileOnGithub;
    private String path;
    
    private JPanel loadingBar = new JPanel() {
        private static final long serialVersionUID = 1L;
        
        @Override
        public void paintComponent(Graphics graphics) {
            graphics.setColor(new Color(175, 255, 175));
            graphics.fillRect(0, 0, (int) (progress * getWidth()), getHeight());
            graphics.setColor(Color.BLACK);
            GraphicsTools.drawStringJustified(graphics, "Downloading v" + newVersion, getWidth() / 2, getHeight() / 2, 1, 1);
        }
    };

    private double progress = 0;
    
    public OhHellUpdater(String newVersion, String fileOnGithub, String jarPath) {
        this.newVersion = newVersion;
        if (!fileOnGithub.isEmpty()) {
            this.fileOnGithub = fileOnGithub;
        } else {
            this.fileOnGithub = "OhHellClient.jar";
        }
        if (!jarPath.isEmpty()) {
            path = jarPath;
        } else {
            try {
                path = new File(OhHellUpdater.class.getProtectionDomain().getCodeSource()
                                .getLocation().toURI()).getParent() + "/OhHellClient.jar";
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void execute() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            setTitle("");
            setLayout(null);
            
            setIconImage(FileTools.loadImage("resources/icon/cw.png", this));
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
            URL url = new URL("https://raw.githubusercontent.com/campbellsoup37/OhHell/master/OhHell/" + fileOnGithub);
            
            URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
            InputStream in = connection.getInputStream();
            int size = connection.getContentLength();
            in.close();
            
            BufferedInputStream newClientJarInput = new BufferedInputStream(
                    url.openStream());
            FileOutputStream newClientJarOutput = new FileOutputStream(path);
            
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
        Runtime.getRuntime().exec("java -jar \"" + path + "\"" + " -deleteupdater");
        dispose();
        System.exit(0);
    }
    
    public static void main(String[] args) {
        String version = "";
        String fileOnGithub = "";
        String jarPath = "";
        if (args.length > 0) {
            version = args[0];
            fileOnGithub = args[1];
            jarPath = args[2];
        }
        OhHellUpdater updater = new OhHellUpdater(version, fileOnGithub, jarPath);
        updater.execute();
    }
}
