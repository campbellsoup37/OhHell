package core;
public class RoundDetails {
    private Player dealer;
    private int handSize;
    private boolean roundOver;
    
    public RoundDetails(int handSize) {
        dealer = new Player("");
        this.handSize = handSize;
        roundOver = false;
    }

    public Player getDealer() {
        return dealer;
    }

    public void setDealer(Player dealer) {
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