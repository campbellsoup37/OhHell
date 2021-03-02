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
    private final boolean verbose = false;
    
    private List<Player> players = new ArrayList<Player>();
    private List<Player> kibitzers = new ArrayList<Player>();
    
    private AiKernel aiKernel;
    private AiTrainer aiTrainer;
    
    private int robotDelay = 0;
    
    private Random random = new Random();
    private Deck deck = new Deck();
    
    private boolean gameStarted = false;
    private String state = "";

    private Card trump;
    private List<Card> trumps;
    
    private List<RoundDetails> rounds;
    private int roundNumber;
    private int playNumber;
    private int leader;
    private int turn;
    
    private Recorder recorder;
    private boolean record;
    
    private PendingAction pendingAction;
    
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
        for (Player player : players) {
            player.commandUpdatePlayers(players);
        }
        for (Player player : kibitzers) {
            player.commandUpdatePlayers(players);
        }
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
    
    public void startGame(int robotCount, GameOptions options, List<AiStrategyModule> aiStrategyModules) {
        deck.setD(options.getD());
        robotDelay = options.getRobotDelay();
        int defaultStartingH = GameOptions.defaultStartingH(players.size() + robotCount, deck.getD());
        if (options.getStartingH() <= 0 || options.getStartingH() > defaultStartingH) {
            options.setStartingH(defaultStartingH);
        }

        if (verbose) {
            System.out.println("Game started with options:");
            System.out.println(options.toVerboseString());
        }
        
        if (robotCount > 0) {
            List<AiPlayer> aiPlayers = aiKernel.createAiPlayers(
                    players.size() + robotCount, robotCount, aiStrategyModules, 0);
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
        //deck.setSeed(-4258269598777096215L);
        
        gameStarted = true;
        randomizePlayerOrder();

        if (record) {
            recorder.start();
            recorder.recordInfo(options, players);
//            recorder.recordPlayers(
//                    players.stream()
//                    .map(Player::realName)
//                    .collect(Collectors.toList()));
        }
        
        rounds = new ArrayList<>();
        roundNumber = 0;
        playNumber = 0;
        
        buildRounds(options);
        updateRounds();
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setIndex(i);
            player.reset();
            player.commandStart(options);
        }
        for (Player player : kibitzers) {
            player.commandStart(options);
        }
        
        trumps = new ArrayList<>(rounds.size());
        
        deal();
    }
    
    public List<RoundDetails> getRounds() {
        return rounds;
    }
    
    public void buildRounds(GameOptions options) {
//        rounds.add(new RoundDetails(2));
//        rounds.add(new RoundDetails(2));
        
        int maxHand = options.getStartingH();
        for (int i = maxHand; i >= 2; i--) {
            rounds.add(new RoundDetails(i));
        }
        for (int i = 0; i < players.size(); i++) {
            rounds.add(new RoundDetails(1));
        }
        for (int i = 2; i <= maxHand; i++) {
            rounds.add(new RoundDetails(i));
        }
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
        stopKernel();
    }
    
    public void stopKernel() {
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
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public int getPlayNumber() {
        return playNumber;
    }
    
    public List<List<Card>> getNextHands() {
        int handSize = rounds.get(roundNumber).getHandSize();
        return deck.deal(players.size(), handSize);
    }
    
    public void deal() {
        deck.initialize();
        List<List<Card>> hands = getNextHands();
        
        for (int i = 0; i < hands.size() - 1; i++) {
            if (!players.get(i).isKicked()) {
                players.get(i).setHand(hands.get(i));
            } else {
                players.get(i).setHand(
                        hands.get(i).stream()
                        .map(c -> new Card())
                        .collect(Collectors.toList()));
            }
        }
        trump = hands.get(players.size()).get(0);
        trumps.add(trump);
        deck.playCard(trump);
        
        int dealer = getDealer();
        
        turn = nextUnkicked(dealer);
        leader = turn;
        
        if (record) {
            recorder.recordRoundInfo(rounds.get(roundNumber).getHandSize(), dealer, players, trump);
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
        if (verbose) {
            System.out.println(player.getId() + " bid " + bid);
        }
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
        
        if (player.isHuman() && !players.get(turn).isHuman()) {
            pendingAction = new PendingAction(robotDelay) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    communicateTurn();
                }
            };
        } else {
            communicateTurn();
        }
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
        if (verbose) {
            System.out.println(player.getId() + " played " + card);
        }
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
            
            playNumber++;
            
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
        playNumber = 0;
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
            reloadAiStrategyModules((int) players.stream().filter(p -> !p.isKicked()).count());
        }
        for (Player player : players) {
            player.commandRedeal();
        }
        for (Player player : kibitzers) {
            player.commandRedeal();
        }
        deal();
    }
    
    public void reloadAiStrategyModules(int N) {
        aiKernel.reloadAiStrategyModules(N, null);
    }
    
    public static int trickWinner(List<Card> trick, Card trump) {
        Hashtable<String, Integer> counts = new Hashtable<>();
        for (Card card : trick) {
            String cardName = card.toString();
            if (counts.get(cardName) == null) {
                counts.put(cardName, 1);
            } else {
                counts.put(cardName, counts.get(cardName) + 1);
            }
        }

        int out = 0;
        for (int i = 0; i < trick.size(); i++) {
            if (counts.get(trick.get(i).toString()) % 2 == 1 && 
                    (trick.get(i).isGreaterThan(trick.get(out), trump.getSuit())
                            || counts.get(trick.get(out).toString()) % 2 == 0
                            && trick.get(out).getSuit().equals(trick.get(i).getSuit()))) {
                out = i;
            }
        }
        return out;
    }
    
    public int trickWinner() {
        List<Card> trick = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            trick.add(players.get((leader + i) % players.size()).getTrick());
        }
        return (leader + trickWinner(trick, trump)) % players.size();
    }
    
    /**
     * TODO This function feels too complicated. This is used by the IVL as a first pass to 
     * determine if a card can win the current trick. I should also look into having it take into
     * account the number of players left (e.g., if it is the last player to play a trick, it 
     * knows for sure if it will win; if it is the second to last player, the top two cannot both 
     * be canceled, etc.).
     */
    public boolean cardCanWin(Card card) {
        if (turn == leader) {
//            System.out.println(card + " winnable = true");
            return true;
        }
        
        if (!card.getSuit().equals(players.get(leader).getTrick().getSuit())
                && !card.getSuit().equals(trump.getSuit())) {
//            System.out.println(card + " winnable = false:");
//            System.out.println("   card is not led suit or trump");
            return false;
        }
        
        Hashtable<String, Integer> counts = new Hashtable<>();
        for (Player player : players) {
            String cardName = player.getTrick().toString();
            if (counts.get(cardName) == null) {
                counts.put(cardName, 1);
            } else {
                counts.put(cardName, counts.get(cardName) + 1);
            }
        }
        
        if (counts.get(card.toString()) != null && counts.get(card.toString()) == 1) {
//            System.out.println(card + " winnable = false:");
//            System.out.println("   card is canceled");
            return false;
        }

        Card smallestWinnable = players.get(leader).getTrick();
        for (int i = 0; i < players.size(); i++) {
            Card c = players.get(i).getTrick();
            boolean cNotCanceled = counts.get(c.toString()) == 1;
            boolean cNotCancelable = deck.matchingCardsLeft(c, new LinkedList<>()) == deck.getD() - 1;
            boolean cBeatsSmallestWinnable = c.isGreaterThan(smallestWinnable, trump.getSuit());
            boolean cMatchesSuit = c.getSuit().equals(players.get(leader).getTrick().getSuit());
            boolean smallestWinnableIsLosable = deck.matchingCardsLeft(smallestWinnable, new LinkedList<>()) == 2;
            if (cNotCanceled 
                    && cNotCancelable 
                    && (cBeatsSmallestWinnable 
                            || cMatchesSuit && smallestWinnableIsLosable)) {
                smallestWinnable = c;
            }
        }

        boolean cardBeatsSmallestWinnable = card.isGreaterThan(smallestWinnable, trump.getSuit());
        boolean cardMatchesSuit = card.getSuit().equals(players.get(leader).getTrick().getSuit());
        boolean smallestWinnableIsLosable = deck.matchingCardsLeft(smallestWinnable, new LinkedList<>()) == 2;
        boolean ans = cardBeatsSmallestWinnable || cardMatchesSuit && smallestWinnableIsLosable;
        
//        if (ans) {
//            System.out.println(card + " winnable = true");
//        } else {
//            System.out.println(card + " winnable = false:");
//            if (!cardBeatsSmallestWinnable) {
//                System.out.println("   card does not beat smallest winnable (" + smallestWinnable + ")");
//            }
//            if (!(cardMatchesSuit && smallestWinnableIsLosable)) {
//                System.out.println("   smallest winnable (" + smallestWinnable + ") is not cancelable");
//            }
//        }
        
        return ans;
    }
    
    public void processUndoBid(Player player) {
        Player nextPlayer = players.get(nextUnkicked(player.getIndex()));
        if (!nextPlayer.hasBid() || player.getIndex() == getDealer() && nextPlayer.getTrick().isEmpty()) {
            if (pendingAction != null) {
                pendingAction.turnOff();
            }
            
            if (state.equals("PLAYING")) {
                state = "BIDDING";
                if (record) {
                    recorder.unrecordBids();
                }
            }
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
                ), true);
        for (Player p : players) {
            if (p != player && !p.isKicked()) {
                List<List<Card>> yourSplit = Card.split(Arrays.asList(
                        p.getHand(), 
                        Arrays.asList(p.getTrick())
                        ), false);
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
        if (!winningCold(player) && players.stream().anyMatch(p -> !p.isHuman())) {
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
                makeAcceptedClaim(player);
            }
        }
    }
    
    public void makeAcceptedClaim(Player player) {
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
        if (record) {
            recorder.recordClaim(player.getIndex());
        }
        doNextRound();
    }
    
    public void sendChat(Player sender, List<Player> recips, String text) {
        if (recips == null || recips.isEmpty()) {
            for (Player player : players) {
                player.commandChat(sender.getName() + ": " + text);
            }
            for (Player player : kibitzers) {
                player.commandChat(sender.getName() + ": " + text);
            }
        } else {
            sender.commandChat("*" + sender.getName() + " (to " + recips.get(0).getName() + "): " + text);
            for (Player player : recips) {
                player.commandChat("*" + sender.getName() + " (to you): " + text);
            }
        }
    }
    
    public void pokePlayer() {
        players.get(turn).commandPoke();
    }
    
    public void reportKick(int index) {
        if (gameStarted && (players.stream().allMatch(p -> 
                p.isKicked() || !p.isHuman()))) {
            stopGame();
        } else {
            if (record) {
                recorder.recordKick(index);
            }
            updateRounds();
            restartRound();
        }
    }
}