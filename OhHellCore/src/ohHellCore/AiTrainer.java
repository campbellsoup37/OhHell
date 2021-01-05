package ohHellCore;
import java.util.List;

import ml.Trainer;

public class AiTrainer extends Trainer {
    private List<Player> players;
    private List<RoundDetails> rounds;
    
    public boolean backprop() {
        return false;
    }
    
    public List<Player> getPlayers() {
        return players;
    }

    public List<RoundDetails> getRounds() {
        return rounds;
    }
    
    public int[] getScores() {
        int[] scores = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            scores[i] = players.get(i).getScore();
        }
        return scores;
    }
    
    public int[] getRoundHandSizes() {
        int[] roundSizes = new int[rounds.size()];
        for (int j = 0; j < rounds.size(); j++) {
            roundSizes[j] = rounds.get(j).getHandSize();
        }
        return roundSizes;
    }
    
    public int[][] getBids() {
        int[][] bids = new int[players.size()][rounds.size()];
        for (int i = 0; i < players.size(); i++) {
            for (int j = 0; j < rounds.size(); j++) {
                bids[i][j] = players.get(i).getBids().get(j);
            }
        }
        return bids;
    }
    
    public int[][] getTakens() {
        int[][] takens = new int[players.size()][rounds.size()];
        for (int i = 0; i < players.size(); i++) {
            for (int j = 0; j < rounds.size(); j++) {
                takens[i][j] = players.get(i).getTakens().get(j);
            }
        }
        return takens;
    }
    
    public void notifyGameDone(List<Player> players, List<RoundDetails> rounds) {
        this.players = players;
        this.rounds = rounds;
        interrupt();
    }
}