package core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Card {
    private int num = 0;
    // -1=empty, 0=clubs, 1=diamonds, 2=hearts, 3=spades
    private int suit = -1;
    
    private static int[] rowCode = {2, 1, 3, 0};
    private static int[] rowCodeInv = {3, 1, 0, 2};
    
    public Card(int num, int suit) {
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
            suit = 0;
        } else if (code.charAt(1) == 'D') {
            suit = 1;
        } else if (code.charAt(1) == 'H') {
            suit = 2;
        } else if (code.charAt(1) == 'S') {
            suit = 3;
        }
    }
    
    public Card(int number) {
        num = (number % 13) + 1;
        if (num == 1) {
            num = 14;
        }
        suit = rowCode[number / 13];
    }
    
    public Card() {}
    
    public static List<List<Card>> split(List<List<Card>> hands, boolean descending) {
        List<List<Card>> ans = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ans.add(new LinkedList<>());
        }
        for (List<Card> hand : hands) {
            for (Card card : hand) {
                if (!card.isEmpty()) {
                    ans.get(card.getSuit()).add(card);
                }
            }
        }
        int sign = descending ? 1 : -1;
        for (int i = 0; i < 4; i++) {
            ans.get(i).sort((c1, c2) -> c2.isGreaterThanSort(c1) ? sign : -sign);
        }
        return ans;
    }
    
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
        if (suit == 0) {
            out += "C";
        } else if (suit == 1) {
            out += "D";
        } else if (suit == 2) {
            out += "H";
        } else if (suit == 3) {
            out += "S";
        }
        return out;
    }
    
    public int toNumber() {
        if (isEmpty()) {
            return 52;
        }
        return (num - 1) % 13 + 13 * rowCodeInv[suit];
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
    
    public int getAdjustedNum(Card trump) {
        if (trump.getSuit() == suit && trump.getNum() >= num) {
            return num + 1;
        } else {
            return num;
        }
    }
    
    public int getSuit() {
        return suit;
    }
    
    public boolean isGreaterThan(Card card, int trumpSuit) {
        return (card.getSuit() == suit && num > card.getNum()) 
                || (suit == trumpSuit && card.getSuit() != trumpSuit)
                || card.isEmpty();
    }
    
    public boolean isGreaterThan(Card card, int ledSuit, int trumpSuit) {
        return (suit == ledSuit || suit == trumpSuit) && !card.isGreaterThan(this, trumpSuit);
    }
    
    public boolean isGreaterThanSort(Card card) {
        int s1 = suit;
        int s2 = card.getSuit();
        return (s1 > s2) || (s1 == s2 && num > card.getNum());
    }
    
    @Deprecated
    public int getSuitNumber() {
        return suit + 1;
    }
    
    public boolean equals(Card c) {
        return c.getNum() == num && c.getSuit() == suit;
    }
}