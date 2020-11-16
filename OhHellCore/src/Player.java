import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int index = 0;
    private int score;
    private int bid;
    private int taken;
    
    private List<Card> hand;
    private List<Integer> bids = new ArrayList<Integer>();
    private List<Integer> scores = new ArrayList<Integer>();
    
    private boolean bidded = false;
    private Card lastTrick;
    private Card trick;

    private boolean host = false;

    private boolean joined = false;
    private boolean disconnected = false;
    private boolean kicked = false;
    private boolean kibitzer = false;

    private List<Player> kickVotes = new ArrayList<Player>();
    
    public Player() {
        name = "";
    }
    
    public Player(String name) {
        this.name = name;
    }
    
    public String realName() {
        return "";
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
    
    public void addScore(int score) {
        this.score += score;
        scores.add(this.score);
    }
    
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    
    public void resetBid() {
        bid = 0;
        bidded = false;
    }
    
    public boolean hasBid() {
        return bidded;
    }
    
    public int getBid() {
        return bid;
    }
    
    public void addBid(int bid) {
        this.bid = bid;
        bidded = true;
        bids.add(bid);
    }
    
    public void setTaken(int taken) {
        this.taken = taken;
    }
    
    public void incTaken() {
        taken++;
    }
    
    public int getTaken() {
        return taken;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }
    
    public void removeCard(Card card) {
        hand.removeIf(c -> c.equals(card));
    }
    
    public List<Integer> getBids() {
        return bids;
    }
    
    public List<Integer> getScores() {
        return scores;
    }
    
    public void clearTrick() {
        lastTrick = new Card();
        trick = new Card();
    }
    
    public void resetTrick() {
        lastTrick = trick;
        trick = new Card();
    }

    public Card getLastTrick() {
        return lastTrick;
    }

    public void setLastTrick(Card lastTrick) {
        this.lastTrick = lastTrick;
    }

    public Card getTrick() {
        return trick;
    }

    public void setTrick(Card trick) {
        this.trick = trick;
    }

    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }
    
    public boolean isHuman() {
        return true;
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

    public int getNumberOfKickVotes() {
        return kickVotes.size();
    }

    public void addKickVote(Player player) {
        if (!kickVotes.contains(player)) {
            kickVotes.add(player);
        }
    }
    
    public void resetKickVotes() {
        kickVotes = new ArrayList<Player>();
    }
    
    public void reset() {
        score = 0;
        clearTrick();
        bids = new ArrayList<Integer>();
        scores = new ArrayList<Integer>();
    }
    
    public void commandStart() {}
    
    public void commandPlayersInfo(List<Player> players, List<Player> kibitzers, Player player) {}
    
    public void commandStatePlayer(Player player) {}
    
    public void commandDealerLeader(int dealer, int leader) {}
    
    public void commandUpdateRounds(List<RoundDetails> rounds, int roundNumber) {}
    
    public void commandDeal(List<Player> players, Card trump) {}
    
    public void commandRedeal() {}
    
    public void commandBid(int index) {}
    
    public void commandPlay(int index) {}
    
    public void commandBidReport(int index, int bid) {}
    
    public void commandPlayReport(int index, Card card) {}
    
    public void commandTrickWinner(int index, List<Card> trick) {}
    
    public void commandNewScores(List<Integer> scores) {}
    
    public void commandFinalScores(List<Player> playersSorted) {}
    
    public void commandChat(String text) {}
    
    public void commandKick() {}
}