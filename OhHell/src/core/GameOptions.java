package core;

public class GameOptions {
    private int numRobots = 0;
    private int D = 1;
    private boolean oregon = false;
    private int robotDelay = 0;
    private int startingH;
    private boolean teams = false;
    
    public GameOptions() {}
    
    public GameOptions(int N) {
        numRobots = N;
    }
    
    public GameOptions(String code) {
        String[] values = code.split(";");
        numRobots = Integer.parseInt(values[0]);
        D = Integer.parseInt(values[1]);
        oregon = values[2].equals("true");
        robotDelay = Integer.parseInt(values[3]);
        startingH = Integer.parseInt(values[4]);
        teams = values[5].equals("true");
    }

    public int getNumRobots() {
        return numRobots;
    }

    public void setNumRobots(int numRobots) {
        this.numRobots = numRobots;
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
    
    public boolean isTeams() {
        return teams;
    }

    public void setTeams(boolean teams) {
        this.teams = teams;
    }

    public void setTo(GameOptions options) {
        numRobots = options.getNumRobots();
        D = options.getD();
        oregon = options.isOregon();
        robotDelay = options.getRobotDelay();
        startingH = options.getStartingH();
        teams = options.isTeams();
    }
    
    public static int defaultStartingH(int N, int D) {
        return Math.min(10, (52 * D - 1) / N);
    }
    
    @Override
    public String toString() {
        return numRobots + ";"
                + D + ";"
                + oregon + ";"
                + robotDelay + ";"
                + startingH + ";"
                + teams;
    }
    
    public String toVerboseString() {
        return "robot count = " + numRobots + "\n"
                + "decks = " + D + "\n"
                + "oregon = " + oregon + "\n"
                + "robot delay = " + robotDelay + "\n"
                + "starting hand size = " + startingH + "\n"
                + "teams = " + teams;
    }
}
