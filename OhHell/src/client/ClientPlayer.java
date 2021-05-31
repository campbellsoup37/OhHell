package client;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import core.Card;

public class ClientPlayer {
    private String name;
    private String id;
    private int index = -1;
    private boolean human = true;
    private boolean host = false;
    private boolean disconnected;
    private boolean kicked;
    private boolean kibitzer;
    private int team = 0;
    
    private CanvasPlayerPosition pos;
    
    private int bid;
    private List<Integer> bids = new ArrayList<>();
    private List<Integer> takens = new ArrayList<>();
    private List<Integer> scores = new ArrayList<>();
    private int taken;
    private int bidding;
    private boolean playing;
    private boolean hasBid;
    
    private List<Card> hand = new ArrayList<>();
    
    private double bidTimer = 1;
    private Card lastTrick = new Card();
    private Card trick = new Card();
    private int trickRad;
    private double trickTimer;
    private boolean timerStarted;
    
    public class Play {
        private Card card;
        private boolean led;
        private boolean won;
        private boolean claiming;
        
        public Play(Card card, boolean led, boolean won) {
            this.card = card;
            this.led = led;
            this.won = won;
            claiming = false;
        }
        
        public Play(boolean claiming) {
            card = new Card();
            this.claiming = claiming;
        }
        
        public Card getCard() {
            return card;
        }
        
        public boolean isLed() {
            return led;
        }
        
        public boolean isWon() {
            return won;
        }
        
        public boolean isClaimed() {
            return card.isEmpty();
        }
        
        public boolean isClaiming() {
            return claiming;
        }
    }
    
    private int place;
    private List<List<Card>> hands = new ArrayList<>();
    private List<List<Play>> plays = new ArrayList<>();
    private int kickedAtRound = -1;
    private List<double[]> bidQs = new ArrayList<>();
    private List<List<Hashtable<Card, Double>>> makingProbs = new ArrayList<>();
    private List<Integer> aiBids = new ArrayList<>();
    private List<Double> diffs = new ArrayList<>();
    
    public ClientPlayer() {}
    
    public boolean isTeam() {
        return false;
    }
    
    public void reset() {
        bid = 0;
        bids = new ArrayList<>();
        scores = new ArrayList<>();
        taken = 0;
        bidding = 0;
        playing = false;
        hasBid = false;
        hand = new ArrayList<>();
        bidTimer = 1;
        lastTrick = new Card();
        trick = new Card();
        trickRad = -1;
        trickTimer = 0;
        
        hands = new ArrayList<>();
        plays = new ArrayList<>();
        kickedAtRound = -1;
        bidQs = new ArrayList<>();
        makingProbs = new ArrayList<>();
        aiBids = new ArrayList<>();
        diffs = new ArrayList<>();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
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
    
    public int getTeam() {
        return team;
    }
    
    public void setTeam(int team) {
        this.team = team;
    }
    
    public boolean posNotSet() {
        return pos == null;
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
    
    public int getTrumpX() {
        return pos.trumpX();
    }
    
    public int getTrumpY() {
        return pos.trumpY();
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
        return hasBid ? bid : 0;
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
    
    public int getScore() {
        return scores.isEmpty() ? 0 : scores.get(scores.size() - 1);
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
    
    public void removeCard(int index) {
        hand.remove(index);
    }
    
    public void removeCard(Card card) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).matches(card) || i == hand.size() - 1) {
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
    
    public void setBidTimer(double bidTimer) {
        this.bidTimer = bidTimer;
    }
    
    public double getBidTimer() {
        return bidTimer;
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

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }
    
    public void setTakens(List<Integer> takens) {
        this.takens = takens;
    }
    
    public List<Integer> getTakens() {
        return takens;
    }
    
    public void addTaken(int taken) {
        takens.add(taken);
    }
    
    public void addPostGameHand(List<Card> hand) {
        hands.add(hand);
    }
    
    public List<List<Card>> getHands() {
        return hands;
    }
    
    public void addPostGamePlay(Card card, boolean led, boolean won) {
        if (plays.isEmpty() 
                || plays.get(plays.size() - 1).size() == hands.get(plays.size() - 1).size()
                || plays.get(plays.size() - 1).get(plays.get(plays.size() - 1).size() - 1).isClaimed()) {
            plays.add(new ArrayList<>(hands.get(plays.size()).size()));
        }
        plays.get(plays.size() - 1).add(new Play(card, led, won));
    }
    
    public void addPostGameClaim(boolean claimer) {
        if (plays.isEmpty() 
                || plays.get(plays.size() - 1).size() == hands.get(plays.size() - 1).size()
                || plays.get(plays.size() - 1).get(plays.get(plays.size() - 1).size() - 1).isClaimed()) {
            plays.add(new ArrayList<>(hands.get(plays.size()).size()));
        }
        plays.get(plays.size() - 1).add(new Play(claimer));
    }
    
    public List<List<Play>> getPlays() {
        return plays;
    }
    
    public void setKickedAtRound(int kickedAtRound) {
        this.kickedAtRound = kickedAtRound;
    }
    
    public int getKickedAtRound() {
        return kickedAtRound;
    }
    
    public void addBidQs(double[] qs) {
        bidQs.add(qs);
    }
    
    public List<double[]> getBidQs() {
        return bidQs;
    }
    
    public void addAiBid(int bid) {
        aiBids.add(bid);
    }
    
    public List<Integer> getAiBids() {
        return aiBids;
    }
    
    public void addDiff(double diff) {
        diffs.add(diff);
    }
    
    public List<Double> getDiffs() {
        return diffs;
    }
    
    public void addMakingProbs(Hashtable<Card, Double> probs, int index) {
        for (int i = makingProbs.size(); i <= index; i++) {
            makingProbs.add(new ArrayList<>(hands.get(i).size()));
        }
        makingProbs.get(makingProbs.size() - 1).add(probs);
    }
    
    public List<List<Hashtable<Card, Double>>> getMakingProbs() {
        return makingProbs;
    }
}