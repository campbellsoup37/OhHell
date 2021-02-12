package core;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.stream.Collectors;

public class OhHellCore {
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    
    private AiKernel aiKernel;
    private AiTrainer aiTrainer;
    
    private Random random = new Random();
    private Deck deck = new Deck();
    
    private boolean gameStarted = false;
    private String state = "";

    private Card trump;
    private List<Card> trumps;
    
    private List<RoundDetails> rounds;
    private int roundNumber;
    private int leader;
    private int turn;
    
    private Recorder recorder;
    private boolean record;
    
    public OhHellCore(boolean record) {
        aiKernel = new AiKernel(this);
        
        this.record = record;
        if (record) {
            recorder = new Recorder();
        }
    }
    
    public void setPlayers(List<Player> players) {
        this.players = players;
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public void setKibitzers(List<Player> kibitzers) {
        this.kibitzers = kibitzers;
    }
    
    public Deck getDeck() {
        return deck;
    }
    
    public void randomizePlayerOrder() {
        for (int i = players.size(); i > 0; i--) {
            int j = random.nextInt(i);
            Player player = players.remove(j);
            players.add(player);
            player.setIndex(players.size() - i);
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
            player.commandUpdatePlayers(players);
        }
        for (Player player : kibitzers) {
            player.commandUpdatePlayers(players);
        }
    }
    
    public void sendFullGameState(Player player) {
        updateRounds();
        giveHands(player);
        sendDealerLeader(player);
        for (Player p : players) {
            player.commandStatePlayer(p);
        }
        communicateTurn();
    }
    
    public void sendDealerLeader(Player player) {
        player.commandDealerLeader(getDealer(), leader);
    }
    
    public boolean getGameStarted() {
        return gameStarted;
    }
    
    public void startGame(int robotCount, boolean doubleDeck, 
            List<AiStrategyModule> aiStrategyModules, int robotDelay) {
        if (robotCount > 0) {
            List<AiPlayer> aiPlayers = aiKernel.createAiPlayers(
                    players.size() + robotCount, robotCount, aiStrategyModules, robotDelay);
            for (int i = 0; i < aiPlayers.size(); i++) {
                aiPlayers.get(i).setId("@bot" + i);
            }
            for (Player player : players) {
                player.commandAddPlayers(aiPlayers, null);
            }
            for (Player kibitzer : kibitzers) {
                kibitzer.commandAddPlayers(aiPlayers, null);
            }
            players.addAll(aiPlayers);
            aiKernel.start();
        }
        deck.setDoubleDeck(doubleDeck);
        //deck.setSeed(-4258269598777096215L);
        
        gameStarted = true;
        randomizePlayerOrder();

        if (record) {
            recorder.start();
            recorder.recordInfo(doubleDeck ? 2 : 1, players);
//            recorder.recordPlayers(
//                    players.stream()
//                    .map(Player::realName)
//                    .collect(Collectors.toList()));
        }
        
        rounds = new ArrayList<>();
        roundNumber = 0;

//        rounds.add(new RoundDetails(10));
//        rounds.add(new RoundDetails(2));
//        rounds.add(new RoundDetails(6));
//        rounds.add(new RoundDetails(5));
//        rounds.add(new RoundDetails(4));
        
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
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setIndex(i);
            player.reset();
            player.commandStart();
        }
        for (Player player : kibitzers) {
            player.commandStart();
        }
        
        trumps = new ArrayList<>(rounds.size());
        
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
        for (Player player : players) {
            if (player.isKicked() || player.isDisconnected() || !player.isHuman()) {
                for (Player p : players) {
                    p.commandRemovePlayer(player);
                }
                for (Player p : kibitzers) {
                    p.commandRemovePlayer(player);
                }
            }
        }
        players.removeIf(p -> p.isKicked() || p.isDisconnected() || !p.isHuman());
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setIndex(i);
        }
        aiKernel.stop();
    }
    
    public void requestEndGame(Player player) {
//        if (player.isHost()) {
            for (Player p : players) {
                p.commandEndGame(player);
            }
            for (Player p : kibitzers) {
                p.commandEndGame(player);
            }
            stopGame();
//        }
    }
    
    public void deal() {
        int handSize = rounds.get(roundNumber).getHandSize();
        List<List<Card>> hands = deck.deal(players.size(), handSize);
        
        for (int i = 0; i < hands.size() - 1; i++) {
            players.get(i).setHand(hands.get(i));
        }
        trump = hands.get(players.size()).get(0);
        trumps.add(trump);
        deck.playCard(trump);
        
        int dealer = getDealer();
        
        turn = nextUnkicked(dealer);
        leader = turn;
        
        if (record) {
            recorder.recordRoundInfo(handSize, dealer, players, trump);
//            recorder.recordTrump(trump);
//            recorder.recordDealer(dealer);
        }
        
        for (Player player : players) {
            player.resetBid();
            if (player.getBids().size() >= roundNumber + 1) {
                player.getBids().remove(roundNumber);
            }
            player.clearTrick();
            player.setTaken(0);
            player.setHandRevealed(false);
            player.setClaiming(false);
            player.setAcceptedClaim(false);
        }
        
        for (Player player : players) {
            sendDealerLeader(player);
            giveHands(player);
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
        if (bid < 0 || bid > player.getHand().size()) {
            System.out.println("ERROR: Player \"" + player.getName() + "\" bid " + bid + " with a hand size of " + player.getHand().size() + ".");
        }
        
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
                recorder.recordBids(players, turn);
//                recorder.recordBids(
//                        players.stream()
//                        .filter(p -> !p.isKicked())
//                        .map(p -> (Integer) p.getBid())
//                        .collect(Collectors.toList()));
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
    
    public int getDealer() {
        return rounds.get(roundNumber).getDealer().getIndex();
    }
    
    public List<Card> getTrick() {
        return players.stream().map(Player::getTrick).collect(Collectors.toList());
    }
    
    public int whatCanINotBid() {
        if (turn == getDealer()) {
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
                recorder.recordTrick(players, leader, turn);
//                recorder.recordTrick(
//                        players.stream()
//                            .map(Player::getTrick)
//                            .collect(Collectors.toList()),
//                        turn);
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
            
            if (!players.get(nextUnkicked(turn)).getHand().isEmpty()) {
                communicateTurn();
                leader = turn;
            } else {
                doNextRound();
            }
        }
    }
    
    public void doNextRound() {
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
            recorder.recordRoundEnd(players);
//            recorder.recordResults(
//                    players.stream()
//                    .map(p -> (Integer) (p.getTaken() - p.getBid()))
//                    .collect(Collectors.toList()));
        }
        
        rounds.get(roundNumber).setRoundOver();
        roundNumber++;
        if (roundNumber < rounds.size()) {
            deal();
        } else {
            List<Player> sortedPlayers = new ArrayList<>(players.size());
            sortedPlayers.addAll(players);
            sortedPlayers.sort((p1, p2) -> (int) Math.signum(p2.getScore() - p1.getScore()));
            for (int i = 0, place = 1; i < players.size(); place = i + 1) {
                for (int score = sortedPlayers.get(i).getScore(); i < players.size() && sortedPlayers.get(i).getScore() == score; i++) {
                    sortedPlayers.get(i).setPlace(place);
                }
            }
            if (record) {
                recorder.recordFinalScores(players);
            }
            if (record) {
                recorder.sendFile(players);
                recorder.sendFile(kibitzers);
            }
//            for (Player p : players) {
//                p.commandPostGameTrumps(trumps);
//                p.commandPostGameHands(players);
//                p.commandPostGameTakens(players);
//                p.commandPostGame();
//            }
//            for (Player p : kibitzers) {
//                p.commandPostGameTrumps(trumps);
//                p.commandPostGameHands(players);
//                p.commandPostGameTakens(players);
//                p.commandPostGame();
//            }
            if (aiTrainer != null) {
                List<Player> playersCopy = new ArrayList<>(players.size());
                playersCopy.addAll(players);
                List<RoundDetails> roundsCopy = new ArrayList<>(rounds.size());
                roundsCopy.addAll(rounds);

                stopGame();
                aiTrainer.notifyGameDone(playersCopy, roundsCopy);
            } else {
                stopGame();
            }
        }
    }
    
    public AiKernel getAiKernel() {
        return aiKernel;
    }
    
    public AiTrainer getAiTrainer() {
        return aiTrainer;
    }
    
    public void restartRound() {
        if (aiKernel.hasAiPlayers()) {
            aiKernel.reloadAiStrategyModules((int) players.stream().filter(p -> !p.isKicked()).count());
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

        int out = leader;
        for (Player player : players) {
            if (counts.get(player.getTrick().toString()) == 1 && 
                    (player.getTrick().isGreaterThan(players.get(out).getTrick(), trump.getSuit())
                            || counts.get(players.get(out).getTrick().toString()) == 2)) {
                out = player.getIndex();
            }
        }
        return out;
    }
    
    public boolean cardWinning(Card card) {
        int winning = leader;
        for (Player player : players) {
            if (player.getTrick().isGreaterThan(players.get(winning).getTrick(), trump.getSuit())) {
                winning = player.getIndex();
            }
        }
        return card.isGreaterThan(players.get(winning).getTrick(), trump.getSuit());
    }
    
    public void processUndoBid(Player player) {
        Player nextPlayer = players.get(nextUnkicked(player.getIndex()));
        if (!nextPlayer.hasBid() || player.getIndex() == getDealer() && nextPlayer.getTrick().isEmpty()) {
            turn = player.getIndex();
            player.removeBid();
            for (Player p : players) {
                p.commandUndoBidReport(player.getIndex());
            }
        }
    }
    
    /**
     * This function returns true if the given player can win the remaining tricks by running down
     * trump and then running down all other suits. This information is used to accept or reject
     * claims in games where there is at least one AI player.
     */
    public boolean winningCold(Player player) {
        List<List<Card>> mySplit = Card.split(Arrays.asList(
                player.getHand(), 
                Arrays.asList(player.getTrick())
                ));
        for (Player p : players) {
            if (p != player && !p.isKicked()) {
                List<List<Card>> yourSplit = Card.split(Arrays.asList(
                        p.getHand(), 
                        Arrays.asList(p.getTrick())
                        ));
                if (mySplit.get(trump.getSuitNumber() - 1).size()
                        < yourSplit.get(trump.getSuitNumber() - 1).size()) {
                    return false;
                }
                for (int i = 0; i < 4; i++) {
                    ListIterator<Card> myli = mySplit.get(i).listIterator();
                    ListIterator<Card> yourli = yourSplit.get(i).listIterator();
                    while (myli.hasNext() && yourli.hasNext()) {
                        if (yourli.next().isGreaterThan(myli.next(), "")) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    public void processClaim(Player player) {
        if (!winningCold(player) && players.stream().anyMatch(Player::isHuman)) {
            player.commandClaimResult(false);
        } else {
            player.setHandRevealed(true);
            player.setClaiming(true);
            for (Player p : players) {
                p.commandDeal(players, trump);
                p.commandClaimRequest(player.getIndex());
            }
        }
    }
    
    public void processClaimResponse(Player player, boolean accept) {
        if (!accept) {
            for (Player p : players) {
                p.setClaiming(false);
                p.setAcceptedClaim(false);
                p.commandClaimResult(false);
            }
        } else {
            player.setAcceptedClaim(true);
            boolean fullAccept = players.stream().filter(p -> !p.hasAcceptedClaim() && !p.isKicked()).count() == 0;
            if (fullAccept) {
                int remainingTricks = players.get(leader).getHand().size();
                if (!players.get(leader).getTrick().isEmpty()) {
                    remainingTricks++;
                }
                for (Player p : players) {
                    if (p.isClaiming()) {
                        for (int i = 0; i < remainingTricks; i++) {
                            p.incTaken();
                        }
                    }
                    p.clearTrick();
                    p.commandClaimResult(true);
                }
                doNextRound();
            }
        }
    }
    
    public void sendChat(String text) {
        for (Player player : players) {
            player.commandChat(text);
        }
        for (Player player : kibitzers) {
            player.commandChat(text);
        }
    }
    
    public void pokePlayer() {
        players.get(turn).commandPoke();
    }
}