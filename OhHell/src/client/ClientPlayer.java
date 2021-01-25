package client;
import java.util.ArrayList;
import java.util.List;

import core.Card;

public class ClientPlayer {
    private String name;
    private int index = -1;
    private boolean human = true;
    private boolean host = false;
    private boolean disconnected;
    private boolean kicked;
    private boolean kibitzer;
    
    private CanvasPlayerPosition pos;
    
    private int bid;
    private List<Integer> bids = new ArrayList<Integer>();
    private List<Integer> scores = new ArrayList<Integer>();
    private int taken;
    private int bidding;
    private boolean playing;
    private boolean hasBid;
    
    private List<Card> hand = new ArrayList<Card>();
    
    private Card lastTrick = new Card();
    private Card trick = new Card();
    private int trickRad;
    private double trickTimer;
    private boolean timerStarted;
    
    public ClientPlayer() {}
    
    public void reset() {
        bid = 0;
        bids = new ArrayList<Integer>();
        scores = new ArrayList<Integer>();
        taken = 0;
        bidding = 0;
        playing = false;
        hasBid = false;
        hand = new ArrayList<Card>();
        lastTrick = new Card();
        trick = new Card();
        trickRad = -1;
        trickTimer = 0;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setHost(boolean host) {
        this.host = host;
    }
    
    public boolean isHost() {
        return host;
    }

    public boolean isDisconnected() {
        return disconnected;
    }
    
    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public boolean isKicked() {
        return kicked;
    }

    public void setKicked(boolean kicked) {
        this.kicked = kicked;
    }

    public boolean isKibitzer() {
        return kibitzer;
    }

    public void setKibitzer(boolean kibitzer) {
        this.kibitzer = kibitzer;
    }
    
    public void setPos(CanvasPlayerPosition pos) {
        this.pos = pos;
    }
    
    public int getX() {
        return pos.x();
    }
    
    public int getY() {
        return pos.y();
    }
    
    public int getJust() {
        return pos.justification();
    }
    
    public int getTakenX() {
        return pos.takenX();
    }
    
    public int getTakenY() {
        return pos.takenY();
    }
    
    public int getBid() {
        return bid;
    }
    
    public void setBid(int bid) {
        this.bid = bid;
    }
    
    public void addBid(int bid) {
        setBid(bid);
        bids.add(bid);
        setBidding(0);
        setHasBid(true);
    }
    
    public void removeBid() {
        bids.remove(bids.size() - 1);
        setBidding(1);
        setHasBid(false);
    }
    
    public List<Integer> getBids() {
        return bids;
    }
    
    public void setBids(List<Integer> bids) {
        this.bids = bids;
    }
    
    public void setScores(List<Integer> scores) {
        this.scores = scores;
    }
    
    public void addScore(int score) {
        scores.add(score);
    }
    
    public List<Integer> getScores() {
        return scores;
    }
    
    public int getTaken() {
        return taken;
    }
    
    public void setTaken(int taken) {
        this.taken = taken;
    }
    
    public void incrementTaken() {
        taken++;
    }
    
    public void incTaken() {
        taken++;
    }

    public int getBidding() {
        return bidding;
    }

    public void setBidding(int bidding) {
        this.bidding = bidding;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
    
    public boolean hasBid() {
        return hasBid;
    }
    
    public void setHasBid(boolean hasBid) {
        this.hasBid = hasBid;
    }

    public List<Card> getHand() {
        return hand;
    }
    
    public void setHand(List<Card> hand) {
        this.hand = hand;
    }
    
    public void removeCard(Card card) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).equals(card) || i == hand.size() - 1) {
                hand.remove(i);
                return;
            }
        }
    }

    public Card getLastTrick() {
        return lastTrick;
    }

    public void setLastTrick(Card lastTrick) {
        this.lastTrick = lastTrick;
    }
    
    public void resetTrick() {
        lastTrick = trick.copy();
        trick = new Card();
        trickRad = -1;
        trickTimer = 0;
        timerStarted = false;
    }

    public Card getTrick() {
        return trick;
    }

    public void setTrick(Card trick) {
        this.trick = trick;
        trickTimer = 1;
    }

    public int getTrickRad() {
        return trickRad;
    }

    public void setTrickRad(int trickRad) {
        this.trickRad = trickRad;
    }

    public double getTrickTimer() {
        return trickTimer;
    }

    public void setTrickTimer(double trickTimer) {
        this.trickTimer = trickTimer;
    }
    
    public void setTimerStarted(boolean timerStarted) {
        this.timerStarted = timerStarted;
    }
    
    public boolean timerStarted() {
        return timerStarted;
    }

    public boolean isHuman() {
        return human;
    }

    public void setHuman(boolean human) {
        this.human = human;
    }
}