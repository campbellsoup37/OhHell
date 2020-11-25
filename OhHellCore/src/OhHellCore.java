import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class OhHellCore {
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    
    private List<String> firstNames;
    private AiKernel aiKernel;
    
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
    
    public List<Player> getPlayers() {
        return players;
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
    
    public void startGame(int robotCount, boolean doubleDeck,  
            OverallValueLearner ovl, ImmediateValueLearner ivl) {
        if (robotCount > 0) {
            aiKernel = new AiKernel(this, deck, aiTrainer, ovl, ivl);
            for (int i = 0; i < robotCount; i++) {
                players.add(new AiPlayer(firstNames.get(random.nextInt(firstNames.size())) + " Bot", aiKernel));
            }
            aiKernel.start();
        }
        deck.setDoubleDeck(doubleDeck);
        //deck.setSeed(-4258269598777096215L);
        
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
        
        //rounds.add(new RoundDetails(2));
        
        int numDecks = doubleDeck ? 2 : 1;
        int maxHand = Math.min(10, (numDecks * 52 - 1) / players.size());
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
        players.removeIf(p -> p.isKicked() || p.isDisconnected() || !p.isHuman());
        if (record) {
            recorder.stop();
        }
        if (aiKernel != null && aiKernel.isRunning()) {
            aiKernel.end();
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
    
    public int getLeader() {
        return leader;
    }
    
    public Card getTrump() {
        return trump;
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
                        p.addTaken();
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
                    if (aiTrainer != null) {
                        int[] roundSizes = new int[rounds.size()];
                        int[][] bids = new int[players.size()][rounds.size()];
                        int[][] takens = new int[players.size()][rounds.size()];
                        int[] scores = new int[players.size()];
                        for (int j = 0; j < rounds.size(); j++) {
                            roundSizes[j] = rounds.get(j).getHandSize();
                        }
                        for (int i = 0; i < players.size(); i++) {
                            for (int j = 0; j < rounds.size(); j++) {
                                bids[i][j] = players.get(i).getBids().get(j);
                                takens[i][j] = players.get(i).getTakens().get(j);
                            }
                            scores[i] = players.get(i).getScore();
                        }
                        stopGame();
                        aiTrainer.notifyGameDone(roundSizes, bids, takens, scores);
                    } else {
                        stopGame();
                    }
                }
            }
        }
    }
    
    public AiKernel getAiKernel() {
        return aiKernel;
    }
    
    public void restartRound() {
        if (aiKernel != null && aiKernel.isRunning()) {
            aiKernel.loadPlayers();
            aiKernel.loadOvlIvl();
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
        Hashtable<String, Integer> counts = new Hashtable<>();
        for (Player player : players) {
            String cardName = player.getTrick().toString();
            if (counts.get(cardName) == null) {
                counts.put(cardName, 1);
            } else {
                counts.put(cardName, counts.get(cardName) + 1);
            }
        }

        int out = turn;
        for (Player player : players) {
            if (counts.get(player.getTrick().toString()) == 1 && 
                    (player.getTrick().isGreaterThan(players.get(out).getTrick(), trump.getSuit())
                            || counts.get(players.get(out).getTrick().toString()) == 2)) {
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