public class Card {
    private int num = 0;
    private String suit = "";
    
    public Card(int num, String suit) {
        this.num = num;
        this.suit = suit;
    }
    
    public Card(String code) {
        if (code.equals("0")) {
            return;
        }
        if (code.charAt(0) == 'T') {
            num = 10;
        } else if (code.charAt(0) == 'J') {
            num = 11;
        } else if (code.charAt(0) == 'Q') {
            num = 12;
        } else if (code.charAt(0) == 'K') {
            num = 13;
        } else if (code.charAt(0) == 'A') {
            num = 14;
        } else {
            num = 2 + (int)(code.charAt(0) - '2');
        }
        if (code.charAt(1) == 'C') {
            suit = "clubs";
        } else if (code.charAt(1) == 'D') {
            suit = "diamonds";
        } else if (code.charAt(1) == 'H') {
            suit = "hearts";
        } else if (code.charAt(1) == 'S') {
            suit = "spades";
        }
    }
    
    public Card() {}
    
    @Override
    public String toString() {
        String out = "";
        if (num <= 9) {
            out += num;
        } else if (num == 10) {
            out += "T";
        } else if (num == 11) {
            out += "J";
        } else if (num == 12) {
            out += "Q";
        } else if (num == 13) {
            out += "K";
        } else if (num == 14) {
            out += "A";
        }
        if (suit.equals("clubs")) {
            out += "C";
        } else if (suit.equals("diamonds")) {
            out += "D";
        } else if (suit.equals("hearts")) {
            out += "H";
        } else if (suit.equals("spades")) {
            out += "S";
        }
        return out;
    }
    
    public int toNumber() {
        if (suit.equals("hearts")) {
            return (num - 1) % 13;
        } else if (suit.equals("diamonds")) {
            return (num - 1) % 13 + 13;
        } else if (suit.equals("spades")) {
            return (num - 1) % 13 + 26;
        } else if (suit.equals("clubs")) {
            return (num - 1) % 13 + 39;
        } else {
            return 52;
        }
    }
    
    public boolean isEmpty() {
        return num == 0;
    }
    
    public Card copy() {
        return new Card(num, suit);
    }
    
    public int getNum() {
        return num;
    }
    
    public String getSuit() {
        return suit;
    }
    
    public boolean isGreaterThan(Card card, String trumpSuit) {
        return (card.getSuit().equals(suit) && num>card.getNum()) 
                || (suit.equals(trumpSuit) && !card.getSuit().equals(trumpSuit));
    }
    
    public boolean isGreaterThanSort(Card card) {
        int s1 = getSuitNumber();
        int s2 = card.getSuitNumber();
        return (s1 > s2) || (s1 == s2 && num > card.getNum());
    }
    
    public int getSuitNumber() {
        if (suit.equals("clubs")) {
            return 1;
        } else if (suit.equals("diamonds")) {
            return 2;
        } else if (suit.equals("spades")) {
            return 3;
        } else if (suit.equals("hearts")) {
            return 4;
        } else {
            return 0;
        }
    }
    
    public boolean equals(Card c) {
        return (c.getNum() == num) && c.getSuit().equals(suit);
    }
}