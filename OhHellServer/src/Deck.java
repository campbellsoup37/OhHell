import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Deck {
    private List<Card> deck = new ArrayList<Card>();
    Random random = new Random();
    
    public Deck() {}
    
    public void initialize() {
        deck = new ArrayList<Card>();
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
}