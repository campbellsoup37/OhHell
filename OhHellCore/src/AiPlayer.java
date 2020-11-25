import java.util.List;

public class AiPlayer extends Player {
    OhHellCore core;
    AiKernel aiKernel;
    
    public AiPlayer(String name, AiKernel aiKernel) {
        setName(name);
        this.aiKernel = aiKernel;
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
            aiKernel.makeBid(this);
        }
    }
    
    @Override
    public void commandPlay(int index) {
        if (index == getIndex()) {
            aiKernel.makePlay(this);
        }
    }
    
    @Override
    public void commandTrickWinner(int index, List<Card> trick) {
        aiKernel.addOuts(trick.get(index), trick);
    }
}
