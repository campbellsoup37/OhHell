package core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    
    public Card(int number) {
        num = (number % 13) + 1;
        if (num == 1) {
            num = 14;
        }
        switch (number / 13) {
        case 0:
            suit = "hearts";
            return;
        case 1:
            suit = "diamonds";
            return;
        case 2:
            suit = "spades";
            return;
        case 3:
            suit = "clubs";
            return;
        }
    }
    
    public Card() {}
    
    public static List<List<Card>> split(List<List<Card>> hands) {
        List<List<Card>> ans = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ans.add(new LinkedList<>());
        }
        for (List<Card> hand : hands) {
            for (Card card : hand) {
                if (!card.isEmpty()) {
                    ans.get(card.getSuitNumber() - 1).add(card);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            ans.get(i).sort((c1, c2) -> c2.isGreaterThanSort(c1) ? 1 : -1);
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
    
    public int getAdjustedNum(Card trump) {
        if (trump.getSuit().equals(suit) && trump.getNum() >= num) {
            return num + 1;
        } else {
            return num;
        }
    }
    
    public String getSuit() {
        return suit;
    }
    
    public boolean isGreaterThan(Card card, String trumpSuit) {
        return (card.getSuit().equals(suit) && num > card.getNum()) 
                || (suit.equals(trumpSuit) && !card.getSuit().equals(trumpSuit))
                || card.isEmpty();
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