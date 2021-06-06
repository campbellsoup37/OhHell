package ml;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class Trainer extends Thread {
    private BufferedWriter logFile;
    
    public void notifyDatumNumber(Learner l, int datumNumber, int datumTotal) {}
    
    public void addLog(Learner l, String text) {}
    
    public BufferedWriter logFile() {
        return logFile;
    }
    
    public void setLogFile(BufferedWriter bw) {
        logFile = bw;
    }
    
    public void log(String text) {
        if (logFile != null) {
            try {
                logFile.write(text);
            } catch (IOException e) {}
        }
    }
    
    public void log(String text, int tabs) {
        for (int i = 0; i < tabs; i++) {
            text = "    " + text;
        }
        log(text);
    }
}
