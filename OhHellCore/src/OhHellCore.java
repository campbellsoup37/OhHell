import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class OhHellCore {
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    
    private List<String> firstNames;
    private AiThread aiThread;
    
    private boolean training;
    private AiTrainer aiTrainer;
    
    private Random random = new Random();
    private Deck deck = new Deck();
    
    private boolean gameStarted = false;
    private String state = "";

    private Card trump;
    
    private List<RoundDetails> rounds;
    private int roundNumber;
    private int leader;
    private int turn;
    
    private Recorder recorder;
    private boolean record;
    
    public OhHellCore() {}
    
    public void setPlayers(List<Player> players) {
        this.players = players;
    }
    
    public void setKibitzers(List<Player> kibitzers) {
        this.kibitzers = kibitzers;
    }
    
    public void execute(boolean record) {
        try {
            String file = "resources/firstnames.txt";
            InputStream in = getClass().getResourceAsStream("/" + file);
            BufferedReader reader;
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
            firstNames = new ArrayList<>(18239);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                firstNames.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        this.record = record;
        if (record) {
            recorder = new Recorder();
        }
    }
    
    public void randomizePlayerOrder() {
        for (int i = players.size(); i > 0; i--) {
            int j = random.nextInt(i);
            Player player = players.remove(j);
            players.add(player);
        }
        updatePlayersList();
    }
    
    public int nextUnkicked(int index) {
        for (int i = 0; i < players.size(); i++) {
            int relativeI = (index + 1 + i) % players.size();
            if (!players.get(relativeI).isKicked()) {
                return relativeI;
            }
        }
        return index;
    }
    
    public void updatePlayersList() {
        if (gameStarted && (players.stream().allMatch(p -> p.isKicked() || p.isDisconnected()))) {
            stopGame();
        }
        
        for (Player player : players) {
            player.commandPlayersInfo(players, kibitzers, player);
        }
        for (Player player : kibitzers) {
            player.commandPlayersInfo(players, kibitzers, player);
        }
    }
    
    public void sendFullGameState(Player player) {
        //updatePlayersList();
        updateRounds();
        giveHands(player);
        sendDealerLeader(player);
        for (Player p : players) {
            player.commandStatePlayer(p);
        }
        communicateTurn();
    }
    
    public void sendDealerLeader(Player player) {
        player.commandDealerLeader(rounds.get(roundNumber).getDealer().getIndex(), leader);
    }
    
    public boolean getGameStarted() {
        return gameStarted;
    }
    
    public void startGame(int robotCount, boolean training, OverallValueLearner ovl, ImmediateValueLearner ivl) {
        if (robotCount > 0) {
            int N = (int) players.stream().filter(p -> !p.isKicked()).count() + robotCount;
            aiThread = new AiThread(this, N, training, ovl, ivl);
            for (int i = 0; i < robotCount; i++) {
                players.add(new AiPlayer(firstNames.get(random.nextInt(firstNames.size())) + " Bot", aiThread));
            }
            aiThread.start();
        }
        //deck = new Deck(-4258269598777096215L);
        this.training = training;
        
        gameStarted = true;
        randomizePlayerOrder();

        if (record) {
            recorder.start();
            recorder.recordPlayers(
                    players.stream()
                    .map(Player::realName)
                    .collect(Collectors.toList()));
        }
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setIndex(i);
            player.reset();
            player.commandStart();
        }
        for (Player player : kibitzers) {
            player.commandStart();
        }
        
        rounds = new ArrayList<RoundDetails>();
        roundNumber = 0;
        
        int maxHand = Math.min(10, 52 / players.size());
        for (int i = maxHand; i >= 2; i--) {
            rounds.add(new RoundDetails(i));
        }
        for (int i = 0; i < players.size(); i++) {
            rounds.add(new RoundDetails(1));
        }
        for (int i = 2; i <= maxHand; i++) {
            rounds.add(new RoundDetails(i));
        }
        updateRounds();
        
        deal();
    }
    
    public void updateRounds() {
        rounds.removeIf(r -> r.getHandSize() == 1 && r.getDealer().isKicked() && !r.isRoundOver());
        
        List<RoundDetails> remainingRounds = rounds.stream()
                .filter(r -> !r.isRoundOver())
                .collect(Collectors.toList());
        int dIndex = remainingRounds.get(0).getDealer().getIndex() - 1;
        
        for (RoundDetails r : remainingRounds) {
            dIndex = nextUnkicked(dIndex);
            r.setDealer(players.get(dIndex));
        }
        
        for (Player player : players) {
            player.commandUpdateRounds(rounds, roundNumber);
        }
        for (Player player : kibitzers) {
            player.commandUpdateRounds(rounds, roundNumber);
        }
    }
    
    public void stopGame() {
        gameStarted = false;
        if (!gameStarted) {
            players.removeIf(p -> p.isKicked() || p.isDisconnected() || !p.isHuman());
        }
        if (record) {
            recorder.stop();
        }
        if (aiThread != null && aiThread.isRunning()) {
            aiThread.end();
        }
    }
    
    public void deal() {
        int handSize = rounds.get(roundNumber).getHandSize();
        List<List<Card>> hands = deck.deal(players.size(), handSize);
        
        for (int i = 0; i < hands.size() - 1; i++) {
            players.get(i).setHand(hands.get(i));
        }
        trump = hands.get(players.size()).get(0);
        deck.playCard(trump);
        
        int dealer = rounds.get(roundNumber).getDealer().getIndex();
        
        turn = nextUnkicked(dealer);
        leader = turn;
        
        if (record) {
            recorder.recordTrump(trump);
            recorder.recordDealer(dealer);
        }
        
        for (Player player : players) {
            player.resetBid();
            if (player.getBids().size() >= roundNumber + 1) {
                player.getBids().remove(roundNumber);
            }
            player.clearTrick();
            player.setTaken(0);
            giveHands(player);
            sendDealerLeader(player);
        }
        
        for (Player player : kibitzers) {
            giveHands(player);
            sendDealerLeader(player);
        }
        
        state = "BIDDING";
        communicateTurn();
    }
    
    public void giveHands(Player player) {
        player.commandDeal(players, trump);
    }
    
    public void incomingBid(Player player, int bid) {
        player.addBid(bid);
        
        for (Player p : players) {
            p.commandBidReport(player.getIndex(), bid);
        }
        for (Player p : kibitzers) {
            p.commandBidReport(player.getIndex(), bid);
        }
        
        turn = nextUnkicked(turn);
        
        if (players.stream().filter(p -> !p.isKicked()).noneMatch(p -> !p.hasBid())) {
            if (record) {
                recorder.recordBids(
                        players.stream()
                        .filter(p -> !p.isKicked())
                        .map(p -> (Integer) p.getBid())
                        .collect(Collectors.toList()));
            }
            state = "PLAYING";
        }
        communicateTurn();
    }
    
    // AI ------------------------------------------------------------------------------------------
    
    public void setAiTrainer(AiTrainer aiTrainer) {
        this.aiTrainer = aiTrainer;
    }
    
    public void addOvlInput(BinaryLayerVector in, Card card, int hOffSet, int voidsOffset) {
        int maxH = Math.min(10, 51 / players.size());
        int numOfVoids = voids(players.get(turn).getHand()) + voidsOffset;
        List<List<Card>> split = splitBySuit(players.get(turn).getHand());
        List<Card> trick = players.stream().map(Player::getTrick).filter(c -> !c.isEmpty()).collect(Collectors.toList());
        
        in.addOneHot(players.get(turn).getHand().size() - hOffSet, maxH);
        if (state.equals("BIDDING")) {
            for (int j = 0; j < players.size(); j++) {
                if ((turn - leader + players.size()) % players.size() + j >= players.size()) {
                    in.addOneHot(players.get((turn + j) % players.size()).getBid() + 1, maxH + 1);
                } else {
                    in.addZeros(maxH + 1);
                }
            }
        } else {
            for (int j = 0; j < players.size(); j++) {
                in.addOneHot(Math.max(players.get((turn + j) % players.size()).getBid() - players.get((turn + j) % players.size()).getTaken(), 0) + 1, maxH + 1);
            }
        }
        in.addOneHot(numOfVoids + 1, 4);
        in.addOneHot(deck.cardsLeftOfSuit(trump, Arrays.asList(split.get(trump.getSuitNumber() - 1), trick)) + 1, 13);
        
        in.addOneHot(card.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
        in.addOneHot(deck.cardsLeftOfSuit(card, Arrays.asList(split.get(card.getSuitNumber() - 1), trick)) + 1, 13);
        
        in.addOneHot(deck.adjustedCardValueSmall(card, Arrays.asList(split.get(card.getSuitNumber() - 1), trick)) + 1, 13);
    }
    
    public void addIvlInput(BinaryLayerVector in, Card card) {
        int maxH = Math.min(10, 51 / players.size());
        List<List<Card>> split = splitBySuit(players.get(turn).getHand());
        List<Card> trick = players.stream().map(Player::getTrick).filter(c -> !c.isEmpty()).collect(Collectors.toList());
        
        for (int k = 1; k < players.size(); k++) {
            if ((turn - leader + players.size()) % players.size() + k < players.size()) {
                in.addOneHot(Math.max(players.get((turn + k) % players.size()).getBid() - players.get((turn + k) % players.size()).getTaken(), 0) + 1, maxH + 1);
            } else {
                in.addZeros(maxH + 1);
            }
        }
        in.addOneHot(deck.cardsLeftOfSuit(trump, Arrays.asList(split.get(trump.getSuitNumber() - 1), trick)) + 1, 13);

        Card led = players.get(leader).getTrick().isEmpty() ? card : players.get(leader).getTrick();
        in.addOneHot(led.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
        in.addOneHot(deck.cardsLeftOfSuit(led, Arrays.asList(split.get(led.getSuitNumber() - 1), trick)) + 1, 13);
        
        in.addOneHot(card.getSuit().equals(trump.getSuit()) ? 2 : 1, 2);
        in.addOneHot(deck.adjustedCardValueSmall(card, Arrays.asList(split.get(card.getSuitNumber() - 1), trick)) + 1, 13);
    }
    
    public static int voids(List<Card> hand) {
        boolean[] notVoid = new boolean[4];
        for (Card c : hand) {
            notVoid[c.getSuitNumber() - 1] = true;
        }
        int count = 0;
        for (boolean nv : notVoid) {
            if (!nv) {
                count++;
            }
        }
        return count;
    }
    
    public static int[] suitLengths(List<Card> hand) {
        int[] counts = new int[4];
        for (Card c : hand) {
            counts[c.getSuitNumber() - 1]++;
        }
        return counts;
    }
    
    public static List<List<Card>> splitBySuit(List<Card> hand) {
        List<List<Card>> out = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            out.add(new LinkedList<>());
        }
        for (Card c : hand) {
            out.get(c.getSuitNumber() - 1).add(c);
        }
        return out;
    }
    
    public int whatCanINotBid() {
        if (turn == rounds.get(roundNumber).getDealer().getIndex()) {
            return rounds.get(roundNumber).getHandSize()
                    - players.stream().map(Player::getBid).reduce(0, (a, b) -> a + b);
        } else {
            return -1;
        }
    }
    
    public List<Card> whatCanIPlay(Player player) {
        List<Card> canPlay = player.getHand().stream()
                .filter(c -> players.get(leader).getTrick() == null 
                || players.get(leader).getTrick().isEmpty()
                || c.getSuit().equals(players.get(leader).getTrick().getSuit()))
                .collect(Collectors.toList());
        if (canPlay.isEmpty()) {
            canPlay = player.getHand();
        }
        return canPlay;
    }
    
    public boolean cardIsWinning(Card card) {
        for (Player p : players) {
            if (!card.isGreaterThan(p.getTrick(), trump.getSuit())) {
                return false;
            }
        }
        return true;
    }
    
    // ---------------------------------------------------------------------------------------------
    
    public void communicateTurn() {
        if (state.equals("BIDDING")) {
            for (Player player : players) {
                player.commandBid(turn);
            }
            for (Player player : kibitzers) {
                player.commandBid(turn);
            }
        }
        if (state.equals("PLAYING")) {
            for (Player player : players) {
                player.commandPlay(turn);
            }
            for (Player player : kibitzers) {
                player.commandPlay(turn);
            }
        }
    }
    
    public void incomingPlay(Player player, Card card) {
        player.setTrick(card);
        player.removeCard(card);
        
        for (Player p : players) {
            p.commandPlayReport(player.getIndex(), card);
        }
        for (Player p : kibitzers) {
            p.commandPlayReport(player.getIndex(), card);
        }
        
        turn = nextUnkicked(turn);
        
        if (players.stream().filter(p -> !p.isKicked()).anyMatch(p -> p.getTrick().isEmpty())) {
            communicateTurn();
        } else {
            turn = trickWinner();
            
            for (Player p : players) {
                deck.playCard(p.getTrick());
            }
            
            if (record) {
                recorder.recordTrick(
                        players.stream()
                            .map(Player::getTrick)
                            .collect(Collectors.toList()),
                        turn);
            }
            
            players.get(turn).incTaken();
            for (Player p : players) {
                p.commandTrickWinner(
                        turn, 
                        players.stream().map(Player::getTrick).collect(Collectors.toList()));
                p.resetTrick();
            }
            for (Player p : kibitzers) {
                p.commandTrickWinner(
                        turn,
                        players.stream().map(Player::getTrick).collect(Collectors.toList()));
            }
            
            if (players.stream()
                    .map(Player::getTaken)
                    .reduce(0, (sofar, t) -> sofar + t)
                    < rounds.get(roundNumber).getHandSize()) {
                communicateTurn();
                leader = turn;
            } else {
                List<Integer> newScores = new LinkedList<>();
                for (Player p : players) {
                    if (p.isKicked()) {
                        newScores.add(null);
                    } else {
                        int b = p.getBid();
                        int d = Math.abs(p.getTaken() - b);
                        if (d == 0) {
                            p.addScore(10 + b * b);
                        } else {
                            p.addScore(-5 * d * (d + 1) / 2);
                        }
                        newScores.add(p.getScore());
                    }
                }
                for (Player p : players) {
                    p.commandNewScores(newScores);
                }
                for (Player p : kibitzers) {
                    p.commandNewScores(newScores);
                }
                
                if (record) {
                    recorder.recordResults(
                            players.stream()
                            .map(p -> (Integer) (p.getTaken() - p.getBid()))
                            .collect(Collectors.toList()));
                }
                
                rounds.get(roundNumber).setRoundOver();
                roundNumber++;
                if (roundNumber < rounds.size()) {
                    deal();
                } else {
                    List<Player> playersSorted = new ArrayList<Player>();
                    for (Player p : players) {
                        int i = 0;
                        while (i < playersSorted.size() 
                                && p.getScore() < playersSorted.get(i).getScore()) {
                            i++;
                        }
                        playersSorted.add(i, p);
                    }
                    for (Player p : players) {
                        p.commandFinalScores(playersSorted);
                    }
                    for (Player p : kibitzers) {
                        p.commandFinalScores(playersSorted);
                    }
                    if (training) {
                        int[] scores = new int[players.size()];
                        int i = 0;
                        for (Player p : players) {
                            scores[i] = p.getScore();
                            i++;
                        }
                        aiTrainer.notifyGameDone(scores);
                    }
                    stopGame();
                }
            }
        }
    }
    
    public AiThread getAiThread() {
        return aiThread;
    }
    
    public void restartRound() {
        if (aiThread != null && aiThread.isRunning()) {
            aiThread.loadOvlIvl((int) players.stream().filter(p -> !p.isKicked()).count());
        }
        for (Player player : players) {
            player.commandRedeal();
        }
        for (Player player : kibitzers) {
            player.commandRedeal();
        }
        deal();
    }
    
    public int trickWinner() {
        int out = turn;
        for (Player player : players) { 
            if (player.getTrick().isGreaterThan(players.get(out).getTrick(), trump.getSuit())) {
                out = player.getIndex();
            }
        }
        return out;
    }
    
    public void sendChat(String text) {
        for (Player player : players) {
            player.commandChat(text);
        }
        for (Player player : kibitzers) {
            player.commandChat(text);
        }
    }
}