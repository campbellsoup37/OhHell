package core;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class Player {
    private String name;
    private String id;
    private int index = 0;
    private int place;
    private int team = 0;
    private int score;
    private int bid;
    private int taken;
    
    private boolean bidded = false;
    private Card lastTrick;
    private Card trick;
    
    private boolean kicked = false;
    
    private List<Card> played = new LinkedList<>();
    private boolean[] shownOut = new boolean[4];
    private boolean[] hadSuit = new boolean[4];
    
    private List<Card> hand;
    private List<List<Card>> hands = new ArrayList<>();
    private List<Integer> bids = new ArrayList<>();
    private List<Integer> takens = new ArrayList<>();
    private List<Integer> scores = new ArrayList<>();
    
    private boolean handRevealed = false;
    private boolean isClaiming = false;
    private boolean acceptedClaim = false;
    
    private AiStrategyModule aiStrategyModule;

    private boolean host = false;

    private boolean joined = false;
    private boolean disconnected = false;
    private boolean kibitzer = false;

    private List<Player> kickVotes = new ArrayList<>();
    
    public class PlayerData {
        public String getName() {
            return Player.this.getName();
        }
        
        public String getId() {
            return Player.this.getId();
        }
        
        public int getIndex() {
            return Player.this.getIndex();
        }
        
        public int getTeam() {
            return Player.this.getTeam();
        }
        
        public int getScore() {
            return Player.this.getScore();
        }
        
        public int getBid() {
            return Player.this.getBid();
        }
        
        public int getTaken() {
            return Player.this.getTaken();
        }
        
        public boolean hasBid() {
            return Player.this.hasBid();
        }
        
        public Card getLastTrick() {
            return Player.this.getLastTrick();
        }
        
        public Card getTrick() {
            return Player.this.getTrick();
        }
        
        public boolean hasPlayed() {
            return !getTrick().isEmpty();
        }
        
        public boolean isKicked() {
            return Player.this.isKicked();
        }
        
        public List<Card> getCardsPlayed() {
            return played;
        }
        
        public boolean shownOut(int suit) {
            return shownOut[suit];
        }
        
        public boolean voidDealt(int suit) {
            return shownOut[suit] && !hadSuit[suit];
        }
    }
    private PlayerData data;
    
    public Player() {
        this("");
    }
    
    public Player(String name) {
        data = new PlayerData();
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
    
    public boolean isTeam() {
        return false;
    }
    
    public int getTeam() {
        return team;
    }
    
    public void setTeam(int team) {
        this.team = team;
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
    
    public int getPlace() {
        return place;
    }
    
    public void setPlace(int place) {
        this.place = place;
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
    
    public void removeBid() {
        bidded = false;
        bids.remove(bids.size() - 1);
    }
    
    public void setTaken(int taken) {
        this.taken = taken;
    }
    
    public void incTaken() {
        taken++;
    }
    
    public int getTaken() {
        return this.taken;
    }
    
    public void addTaken() {
        takens.add(taken);
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
        List<Card> handCopy = new ArrayList<>(hand.size());
        for (Card card : hand) {
            handCopy.add(card);
        }
        hands.add(handCopy);
    }
    
    public List<List<Card>> getHands() {
        return hands;
    }
    
    public void removeCard(Card card) {
        // First check for equality as pointers
        for (Card c : hand) {
            if (c == card) {
                hand.remove(c);
                return;
            }
        }
        // Then check for equality as cards
        for (Card c : hand) {
            if (c.matches(card)) {
                hand.remove(c);
                return;
            }
        }
    }
    
    public List<Integer> getBids() {
        return bids;
    }
    
    public List<Integer> getTakens() {
        return takens;
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
    
    public void recordCardPlay(Card card) {
        setTrick(card);
        removeCard(card);
        played.add(card);
    }
    
    public void recordShownOut(int suit) {
        shownOut[suit] = true;
    }
    
    public void recordHadSuit(int suit) {
        hadSuit[suit] = true;
    }
    
    public void clearPlayed() {
        played = new LinkedList<>();
        shownOut = new boolean[4];
        hadSuit = new boolean[4];
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
        bids = new ArrayList<>();
        takens = new ArrayList<>();
        scores = new ArrayList<>();
        hands = new ArrayList<>();
    }

    public AiStrategyModule getAiStrategyModule() {
        return aiStrategyModule;
    }

    public void setAiStrategyModule(AiStrategyModule aiStrategyModule) {
        this.aiStrategyModule = aiStrategyModule;
        aiStrategyModule.setPlayer(this);
    }

    public boolean isHandRevealed() {
        return handRevealed;
    }

    public void setHandRevealed(boolean handRevealed) {
        this.handRevealed = handRevealed;
    }
    
    public boolean isClaiming() {
        return isClaiming;
    }

    public void setClaiming(boolean isClaiming) {
        this.isClaiming = isClaiming;
    }
    
    public boolean hasAcceptedClaim() {
        return acceptedClaim;
    }
    
    public void setAcceptedClaim(boolean acceptedClaim) {
        this.acceptedClaim = acceptedClaim;
    }
    
    public PlayerData getPlayerData() {
        return data;
    }
    
    public void commandStart(GameOptions options) {}
    
    public void commandEndGame(Player player) {}
    
    public void commandAddPlayers(List<? extends Player> player, List<? extends Player> kibitzers) {}
    
    public void commandRemovePlayer(Player player) {}
    
    public void commandUpdatePlayers(List<? extends Player> player) {}
    
    public void commandUpdateOptions(GameOptions options) {}
    
    public void commandUpdateTeams(List<Team> teams) {}
    
    //public void commandPlayersInfo(List<Player> players, List<Player> kibitzers, Player player) {}
    
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
    
    public void commandUndoBidReport(int index) {}
    
    public void commandClaimRequest(int index) {}
    
    public void commandClaimResult(boolean accept) {}
    
    public void commandNewScores(List<Integer> scores) {}
    
    public void commandPostGameTrumps(List<Card> trumps) {}
    
    public void commandPostGameTakens(List<Player> players) {}
    
    public void commandPostGameHands(List<Player> players) {}
    
    public void commandPostGame() {}
    
    public void commandPostGameFile(String file) {}
    
    public void commandChat(String text) {}
    
    public void commandKick() {}
    
    public void commandPoke() {}
}