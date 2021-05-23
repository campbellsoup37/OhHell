package core;
import java.util.List;

public abstract class AiStrategyModule {
    protected Player player;
    protected OhHellCore.CoreData coreData;
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void setCoreData(OhHellCore.CoreData coreData) {
        this.coreData = coreData;
    }
    
    public void newHand() {}
    
    public int makeBid() {
        throw new IllegalCoreStateException("makeBid() not implemented.");
    }
    
    public Card makePlay() {
        throw new IllegalCoreStateException("makePlay() not implemented.");
    }
    
    public void addTrickData(Card winner, List<Card> trick) {}
    
    public void endOfRound(int points) {}
}
