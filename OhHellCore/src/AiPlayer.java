import java.util.List;

public class AiPlayer extends Player {
    OhHellCore core;
    AiKernel aiKernel;
    
    int delay = 10;
    
    public AiPlayer(String name, AiKernel aiKernel, int delay) {
        setName(name);
        this.aiKernel = aiKernel;
        this.delay = delay;
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
            aiKernel.makeBid(this, delay);
        }
    }
    
    @Override
    public void commandPlay(int index) {
        if (index == getIndex()) {
            aiKernel.makePlay(this, delay);
        }
    }
    
    @Override
    public void commandTrickWinner(int index, List<Card> trick) {
        aiKernel.addOuts(trick.get(index), trick);
    }
}
