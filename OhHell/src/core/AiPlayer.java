package core;
import java.util.List;

public class AiPlayer extends Player {
    private AiKernel aiKernel;
    
    private int delay = 10;
    
    public AiPlayer(String name, AiKernel aiKernel, AiStrategyModule aiStrategyModule, int delay) {
        setName(name);
        this.aiKernel = aiKernel;
        setAiStrategyModule(aiStrategyModule);
        this.delay = delay;
    }
    
    public boolean isHuman() {
        return false;
    }
    
    public String realName() {
        return "Robot";
    }
    
    @Override
    public void commandDeal(List<Player> players, Card trump) {
        getAiStrategyModule().newHand();
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
        getAiStrategyModule().addTrickData(trick.get(index), trick);
    }
    
    @Override
    public void commandClaimRequest(int index) {
        aiKernel.processClaimRequest(this, index);
    }
    
    @Override
    public void commandNewScores(List<Integer> scores) {
        if (scores.get(getIndex()) == null) {
            return;
        }
        
        int round = getScores().size() - 1;
        int points = getScores().get(round) - (round == 0 ? 0 : getScores().get(round - 1));
        getAiStrategyModule().endOfRound(points);
    }
}
