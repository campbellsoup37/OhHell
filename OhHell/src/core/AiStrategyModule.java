package core;
import java.util.List;

public abstract class AiStrategyModule {
    protected Player player;
    protected GameOptions options;
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void setOptions(GameOptions options) {
        this.options = options;
    }
    
    public void newHand() {}
    
    public void makeBid() {}
    
    public void makePlay() {}
    
    public void addTrickData(Card winner, List<Card> trick) {}
    
    public void endOfRound(int points) {}
}
