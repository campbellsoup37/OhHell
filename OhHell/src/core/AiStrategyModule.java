package core;
import java.util.List;

public class AiStrategyModule {
    protected Player player;
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void newHand() {}
    
    public void makeBid() {}
    
    public void makePlay() {}
    
    public void addTrickData(Card winner, List<Card> trick) {}
    
    public void endOfRound(int points) {}
}
