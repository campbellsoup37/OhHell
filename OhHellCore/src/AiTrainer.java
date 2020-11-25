
public class AiTrainer extends Thread {
    private int[] newRounds;
    private int[][] newBids;
    private int[][] newTakens;
    private int[] newScores;
    
    public boolean backprop() {
        return false;
    }
    
    public int[] getNewRounds() {
        return newRounds;
    }
    
    public int[][] getNewBids() {
        return newBids;
    }
    
    public int[][] getNewTakens() {
        return newTakens;
    }
    
    public int[] getNewScores() {
        return newScores;
    }
    
    public void notifyGameDone(int[] rounds, int[][] bids, int[][] takens, int[] scores) {
        newRounds = rounds;
        newBids = bids;
        newTakens = takens;
        newScores = scores;
        interrupt();
    }
}
