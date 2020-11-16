import java.util.List;

public class AiPlayer extends Player {
    OhHellCore core;
    AiThread aiThread;
    
    public AiPlayer(String name, AiThread aiThread) {
        setName(name);
        this.aiThread = aiThread;
    }
    
    public boolean isHuman() {
        return false;
    }
    
    public String realName() {
        return "Robot";
    }

    @Override
    public void commandBid(int index) {
        if (index == getIndex()) {
            aiThread.makeBid(this);
        }
    }
    
    @Override
    public void commandPlay(int index) {
        if (index == getIndex()) {
            aiThread.makePlay(this);
        }
    }
    
    @Override
    public void commandTrickWinner(int index, List<Card> trick) {
        aiThread.addOuts(trick.get(index), trick);
    }
}
