package ohHellCore;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Deck {
    private List<Card> deck = new ArrayList<Card>();
    private boolean doubleDeck = false;
    
    private List<List<Card>> played = new ArrayList<>();
    Random random = new Random();
    
    public Deck() {}
    
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
    
    public void setDoubleDeck(boolean doubleDeck) {
        this.doubleDeck = doubleDeck;
    }
    
    public void initialize() {
        deck = new ArrayList<Card>();
        for (int d = 1; d <= (doubleDeck ? 2 : 1); d++) {
            for (int i = 2; i <= 14; i++) {
                deck.add(new Card(i, "clubs"));
            }
            for (int i = 2; i <= 14; i++) {
                deck.add(new Card(i, "diamonds"));
            }
            for (int i = 2; i <= 14; i++) {
                deck.add(new Card(i, "hearts"));
            }
            for (int i = 2; i <= 14; i++) {
                deck.add(new Card(i, "spades"));
            }
        }
        
        played = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            played.add(new LinkedList<>());
        }
    }
    
    public List<List<Card>> deal(int numHands, int handSize) {
        initialize();
        List<List<Card>> out = new ArrayList<List<Card>>();
        for (int i = 0; i < numHands + 1; i++) {
            out.add(new ArrayList<Card>());
        }
        for (int j = 0; j < handSize; j++) {
            for (int i = 0; i < numHands; i++) {
                insert(out.get(i), deck.remove(random.nextInt(deck.size())));
            }
        }
        out.get(numHands).add(deck.remove(random.nextInt(deck.size())));
        return out;
    }
    
    private void insert(List<Card> hand, Card c) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).isGreaterThanSort(c)) {
                hand.add(i, c);
                return;
            }
        }
        hand.add(c);
    }
    
    public void playCard(Card card) {
        if (!card.isEmpty()) {
            played.get(card.getSuitNumber() - 1).add(card);
        }
    }
    
    public int adjustedCardValue(Card card, Card lead, Card trump) {
        int numInSuit = card.getNum() - 2
                + (int) played.get(card.getSuitNumber() - 1).stream()
                .filter(c -> c.getNum() > card.getNum())
                .count();
        if (card.getSuit().equals(trump.getSuit())) {
            return numInSuit + 26;
        } else if (lead == null 
                || lead.isEmpty()
                || card.getSuit().equals(lead.getSuit())) {
            return numInSuit + 13 + played.get(trump.getSuitNumber() - 1).size();
        } else {
            return numInSuit;
        }
    }
    
    public int cardsLeftOfSuit(Card card, List<List<Card>> additionalPlayeds) {
        int count = 0;
        for (List<Card> additionalPlayed : additionalPlayeds) {
            for (Card c : additionalPlayed) {
                if (c != null && c.getSuit().equals(card.getSuit())) {
                    count++;
                }
            }
        }
        return 13 - played.get(card.getSuitNumber() - 1).size() - count;
    }
    
    public int adjustedCardValueSmall(Card card, List<List<Card>> additionalPlayeds) {
        int val = card.getNum() - 2;
        for (Card c : played.get(card.getSuitNumber() - 1)) {
            if (c.getNum() > card.getNum()) {
                val++;
            }
        }
        for (List<Card> additionalPlayed : additionalPlayeds) {
            for (Card c : additionalPlayed) {
                if (c != null && c.getNum() > card.getNum() && c.getSuit().equals(card.getSuit())) {
                    val++;
                }
            }
        }
        return val;
    }
    
    public List<Card> getPlayedCards() {
        List<Card> ans = new LinkedList<>();
        for (List<Card> playedSuit : played) {
            ans.addAll(playedSuit);
        }
        return ans;
    }
}