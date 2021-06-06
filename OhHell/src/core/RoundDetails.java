package core;
public class RoundDetails {
    private int dealer;
    private int handSize;
    private boolean roundOver;
    
    public RoundDetails(int handSize) {
        dealer = -1;
        this.handSize = handSize;
        roundOver = false;
    }

    public int getDealer() {
        return dealer;
    }

    public void setDealer(int dealer) {
        this.dealer = dealer;
    }

    public int getHandSize() {
        return handSize;
    }

    public void setHandSize(int handSize) {
        this.handSize = handSize;
    }

    public boolean isRoundOver() {
        return roundOver;
    }

    public void setRoundOver() {
        roundOver = true;
    }
}