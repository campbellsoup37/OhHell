package core;

public class GameOptions {
    private int D = 1;
    private boolean oregon = false;
    private int robotDelay = 0;
    private int startingH;
    
    public GameOptions() {}
    
    public GameOptions(String code) {
        String[] values = code.split(";");
        D = Integer.parseInt(values[0]);
        oregon = values[1].equals("true");
        robotDelay = Integer.parseInt(values[2]);
        startingH = Integer.parseInt(values[3]);
    }

    public int getD() {
        return D;
    }

    public void setD(int d) {
        D = d;
    }

    public boolean isOregon() {
        return oregon;
    }

    public void setOregon(boolean oregon) {
        this.oregon = oregon;
    }

    public int getRobotDelay() {
        return robotDelay;
    }

    public void setRobotDelay(int robotDelay) {
        this.robotDelay = robotDelay;
    }

    public int getStartingH() {
        return startingH;
    }

    public void setStartingH(int startingH) {
        this.startingH = startingH;
    }
    
    public static int defaultStartingH(int N, int D) {
        return Math.min(10, (52 * D - 1) / N);
    }
    
    @Override
    public String toString() {
        return D + ";" + oregon + ";" + robotDelay + ";" + startingH;
    }
    
    public String toVerboseString() {
        return "decks = " + D + "\n"
                + "oregon = " + oregon + "\n"
                + "robot delay = " + robotDelay + "\n"
                + "starting hand size = " + startingH;
    }
}
