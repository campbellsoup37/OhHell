package core;
import java.util.List;

public class Recorder {
    private volatile StringBuilder file;
    private volatile StringBuilder round;
    
    public Recorder() {}
    
    public void start() {
        file = new StringBuilder();
    }
    
    public void recordInfo(int numDecks, List<Player> players) {
        file.append("decks;" + numDecks + "|");
        file.append("players");
        for (Player player : players) {
            file.append(";" + player.getId() + ":" + player.getName() + ":" + (player.isHuman() ? "human" : "ai"));
        }
        file.append("|");
    }
    
    public void recordRoundInfo(int handSize, int dealer, List<Player> players, Card trump) {
        round = new StringBuilder();
        round.append("round;" + dealer + ";" + handSize + "|");
        round.append("hands");
        for (Player player : players) {
            round.append(";" + player.getHand().get(0));
            for (int i = 1; i < handSize; i++) {
                round.append(":" + player.getHand().get(i));
            }
        }
        round.append("|");
        round.append("trump;" + trump + "|");
    }
    
    public void recordBids(List<Player> players, int leader) {
        round.append("bids");
        for (Player player : players) {
            round.append(";" + (player.getIndex() == leader ? 1 : 0) + ":" + player.getBid());
        }
        round.append("|");
    }
    
    public void recordTrick(List<Player> players, int leader, int winner) {
        round.append("trick");
        for (Player player : players) {
            round.append(";" 
                + (player.getIndex() == leader ? 1 : 0) + ":" 
                + (player.getIndex() == winner ? 1 : 0) + ":"            
                + player.getTrick());
        }
        round.append("|");
    }
    
    public void recordRoundEnd(List<Player> players) {
        round.append("takens");
        for (Player player : players) {
            round.append(";" + player.getTaken());
        }
        round.append("|");
        round.append("scores");
        for (Player player : players) {
            round.append(";" + player.getScore());
        }
        round.append("|");
        file.append(round);
    }
    
    public void recordFinalScores(List<Player> players) {
        file.append("final scores");
        for (Player player : players) {
            file.append(";" + player.getScore() + ":" + player.getPlace());
        }
    }
    
    public void recordKick(int index) {
        file.append("kick;" + index);
    }
    
    public void sendFile(List<Player> players) {
        String finishedFile = file.toString();
        for (Player player : players) {
            player.commandPostGameFile(finishedFile);
        }
    }
}
