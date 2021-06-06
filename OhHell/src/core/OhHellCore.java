package core;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class OhHellCore {
    private final boolean verbose = false;
    
    private List<Player> players = new ArrayList<>();
    private List<Player> kibitzers = new ArrayList<>();
    private Map<Integer, Team> teams = new HashMap<>();
    
    private AiKernel aiKernel;
    private AiTrainer aiTrainer;
    
    private GameOptions options;
    
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
    
    private CoreData data = new CoreData();
    
    private List<Player> trickOrder = new LinkedList<>();
    
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
    
    public void setTeams(Map<Integer, Team> teams) {
        this.teams = teams;
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
    
    public void setOptions(GameOptions options) {
        this.options = options;
    }
    
    public void overrideAiKernel(AiKernel aiKernel) {
        this.aiKernel = aiKernel;
    }
    
    public void startGame(GameOptions options) {
        this.options = options;
        int robotCount = options.getNumRobots();

        if (verbose) {
            System.out.println("Game started with options:");
            System.out.println(options.toVerboseString());
        }
        
        List<Player> dummies = players.stream().filter(p -> !p.isHuman()).collect(Collectors.toList());
        players.removeAll(dummies);
        for (Player dummy : dummies) {
            for (Player player : players) {
                player.commandRemovePlayer(dummy);
            }
            for (Player kibitzer : kibitzers) {
                kibitzer.commandRemovePlayer(dummy);
            }
        }
        
        deck.setD(options.getD());
        int defaultStartingH = GameOptions.defaultStartingH(players.size() + robotCount, deck.getD());
        if (options.getStartingH() <= 0 || options.getStartingH() > defaultStartingH) {
            options.setStartingH(defaultStartingH);
        }
        
        if (robotCount > 0) {
            List<AiPlayer> aiPlayers = aiKernel.createAiPlayers(
                    players.size() + robotCount, options, dummies, 0);
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
        
        // Sort team members
        if (teams.isEmpty()) {
            for (Player player : players) {
                if (!teams.containsKey(player.getTeam())) {
                    Team team = new Team();
                    team.setIndex(player.getTeam());
                    team.setName("Team " + team.getIndex());
                    teams.put(player.getTeam(), team);
                }
            }
        }
        int realIndex = 0;
        for (Team team : teams.values()) {
            team.setMembers(new ArrayList<>());
            team.setRealIndex(realIndex);
            realIndex++;
        }
        for (Player player : players) {
            teams.get(player.getTeam()).addMember(player);
        }
        
        gameStarted = true;
        randomizePlayerOrder();

        if (record) {
            recorder.start();
            recorder.recordInfo(options, players, teams);
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
        rounds.removeIf(r -> r.getDealer() != -1 && r.getHandSize() == 1 && players.get(r.getDealer()).isKicked() && !r.isRoundOver());
        
        List<RoundDetails> remainingRounds = rounds.stream()
                .filter(r -> !r.isRoundOver())
                .collect(Collectors.toList());
        int dIndex = -1;
        if (remainingRounds.get(0).getDealer() != -1) {
            dIndex = players.get(remainingRounds.get(0).getDealer()).getIndex() - 1;
        }
        
        for (RoundDetails r : remainingRounds) {
            dIndex = nextUnkicked(dIndex);
            r.setDealer(dIndex);
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
        
        Map<Player, Player> dummyMap = new HashMap<>();
        for (Player player : players) {
            if (player.isKicked() || player.isDisconnected()) {
                for (Player p : players) {
                    p.commandRemovePlayer(player);
                }
                for (Player p : kibitzers) {
                    p.commandRemovePlayer(player);
                }
            }
            if (player.isHuman()) {
                dummyMap.put(player, player);
            } else {
                DummyPlayer dummy = new DummyPlayer(random.nextLong());
                dummy.setTeam(player.getTeam());
                for (Player p : players) {
                    p.commandRemovePlayer(player);
                    p.commandAddPlayers(Arrays.asList(dummy), null);
                }
                for (Player p : kibitzers) {
                    p.commandRemovePlayer(player);
                    p.commandAddPlayers(Arrays.asList(dummy), null);
                }
                dummyMap.put(player, dummy);
            }
        }
        players.replaceAll(p -> dummyMap.get(p));
        players.removeIf(p -> p.isKicked() || p.isDisconnected());
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setIndex(i);
        }
        for (Player p : players) {
            p.commandUpdatePlayers(players);
        }
        for (Player p : kibitzers) {
            p.commandUpdatePlayers(players);
        }
        stopKernel();
    }
    
    public void stopKernel() {
        aiKernel.stop();
    }
    
    public void requestEndGame(Player player) {
        for (Player p : players) {
            p.commandEndGame(player);
        }
        for (Player p : kibitzers) {
            p.commandEndGame(player);
        }
        stopGame();
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
            player.clearPlayed();
        }
        trickOrder.clear();
        
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
        // Validity check
        if (player.getIndex() != turn) {
            System.out.println(
                    "ERROR: Player \"" + player.getName() + "\" attempted to bid out of turn.");
            return;
        } else if (bid < 0 || bid > player.getHand().size()) {
            System.out.println(
                    "ERROR: Player \"" + player.getName() + "\" attempted to bid " + bid
                    + " with a hand size of " + player.getHand().size() + ".");
            return;
        } else if (turn == getDealer() && bid == data.whatCanINotBid(players.get(turn))) {
            System.out.println(
                    "ERROR: Player \"" + player.getName() + "\" attempted to bid what they cannot bid as dealer.");
            return;
        }
        
        if (verbose) {
            System.out.println(player.getId() + " bid " + bid);
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
            pendingAction = new PendingAction(options.getRobotDelay()) {
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
    
    public int getDealer() {
        return rounds.get(roundNumber).getDealer();
    }
    
    public List<Card> getTrick() {
        return players.stream().map(Player::getTrick).collect(Collectors.toList());
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
        // Validity check
        if (player.getIndex() != turn) {
            System.out.println(
                    "ERROR: Player \"" + player.getName() + "\" attempted to play out of turn.");
            return;
        } else if (player.getHand().stream().noneMatch(c -> c.matches(card))) {
            System.out.println(
                    "ERROR: Player \"" + player.getName() + "\" attempted to play " + card
                    + ", but they do not have that card.");
            return;
        } else if (data.whatCanIPlay(player).stream().noneMatch(c -> c.matches(card))) {
            System.out.println(
                    "ERROR: Player \"" + player.getName() + "\" attempted to play " + card
                    + ", failing to follow suit.");
            return;
        }
        
        if (verbose) {
            System.out.println(player.getId() + " played " + card);
        }
        
        int ledSuit = players.get(leader).getTrick().getSuit();
        if (player.getIndex() == leader) {
            ledSuit = card.getSuit();
        }
        
        player.recordCardPlay(card);
        if (card.getSuit() != ledSuit) {
            player.recordShownOut(ledSuit);
        } else {
            player.recordHadSuit(ledSuit);
        }
        
        // Insert in trick order
        if (turn == leader) {
            trickOrder.add(player);
        } else if (card.getSuit() == ledSuit || card.getSuit() == trump.getSuit()) {
            ListIterator<Player> iter = trickOrder.listIterator();
            boolean spotFound = false;
            boolean canceled = false;
            while (iter.hasNext() && !spotFound && !canceled) {
                Card next = iter.next().getTrick();
                if (next.matches(card)) {
                    canceled = true;
                    iter.remove();
                } else {
                    spotFound = !next.isGreaterThan(card, ledSuit, trump.getSuit());
                }
            }
            if (!canceled) {
                if (spotFound) {
                    iter.previous();
                }
                iter.add(player);
            }
        }
        
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
            turn = trickOrder.isEmpty() ? leader : trickOrder.get(0).getIndex();
            
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
            trickOrder.clear();
            
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
        Map<Integer, Integer> teamBids = new HashMap<>();
        Map<Integer, Integer> teamTakens = new HashMap<>();
        Map<Integer, Boolean> teamKickeds = new HashMap<>();
        
        if (options.isTeams()) {
            for (Player p : players) {
                if (teamBids.containsKey(p.getTeam())) {
                    teamBids.put(p.getTeam(), teamBids.get(p.getTeam()) + p.getBid());
                    teamTakens.put(p.getTeam(), teamTakens.get(p.getTeam()) + p.getTaken());
                    teamKickeds.put(p.getTeam(), teamKickeds.get(p.getTeam()) && p.isKicked());
                } else {
                    teamBids.put(p.getTeam(), p.getBid());
                    teamTakens.put(p.getTeam(), p.getTaken());
                    teamKickeds.put(p.getTeam(), p.isKicked());
                }
            }
        }
        
        List<Integer> newScores = new LinkedList<>();
        for (Player p : players) {
            if (!options.isTeams() && p.isKicked() || options.isTeams() && teamKickeds.get(p.getTeam())) {
                newScores.add(null);
            } else {
                p.addTaken();
                int b = options.isTeams() ? teamBids.get(p.getTeam()) : p.getBid();
                int t = options.isTeams() ? teamTakens.get(p.getTeam()) : p.getTaken();
                int d = Math.abs(t - b);
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
            reloadAiStrategyModules(
                    (int) players.stream().filter(p -> !p.isKicked()).count(),
                    options.getD(),
                    data.getT()
                    );
        }
        for (Player player : players) {
            player.commandRedeal();
        }
        for (Player player : kibitzers) {
            player.commandRedeal();
        }
        deal();
    }
    
    public void reloadAiStrategyModules(int N, int D, int T) {
        aiKernel.reloadAiStrategyModules(N, D, T, null);
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
                            && trick.get(out).getSuit() == trick.get(i).getSuit())) {
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
    
    public void processUndoBid(Player player) {
        Player nextPlayer = players.get(nextUnkicked(player.getIndex()));
        if (nextPlayer.getIndex() == turn 
                && (!nextPlayer.hasBid() 
                || player.getIndex() == getDealer() && nextPlayer.getTrick().isEmpty())) {
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
        } else {
            System.out.println("ERROR: Player \"" + player.getName() + "\" attempted to undo bid too late.");
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
                if (mySplit.get(trump.getSuit()).size()
                        < yourSplit.get(trump.getSuit()).size()) {
                    return false;
                }
                for (int i = 0; i < 4; i++) {
                    ListIterator<Card> myli = mySplit.get(i).listIterator();
                    ListIterator<Card> yourli = yourSplit.get(i).listIterator();
                    while (myli.hasNext() && yourli.hasNext()) {
                        if (yourli.next().isGreaterThan(myli.next(), -2)) {
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
        if (recips == null) {
            for (Player player : players) {
                player.commandChat(sender.getName() + ": " + text);
            }
            for (Player player : kibitzers) {
                player.commandChat(sender.getName() + ": " + text);
            }
        } else if (recips.size() > 0) {
            sender.commandChat("*" + sender.getName() + " (to " + recips.get(0).getName() + "): " + text);
            for (Player player : recips) {
                player.commandChat("*" + sender.getName() + " (to you): " + text);
            }
        }
    }
    
    public void sendChat(Player sender, String recipient, String text) {
        List<Player> recips = null;
        if (!recipient.isEmpty()) {
            recips = new LinkedList<>();
            for (Player player : players) {
                if (player.getName().equals(recipient)) {
                    recips.add(player);
                }
            }
            for (Player player : kibitzers) {
                if (player.getName().equals(recipient)) {
                    recips.add(player);
                }
            }
        }
        sendChat(sender, recips, text);
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
    
    public CoreData getCoreData() {
        return data;
    }
    
    public class CoreData {
        public int getN(boolean includeKicks) {
            return includeKicks ? 
                    players.size() : 
                    (int) players.stream().filter(p -> !p.isKicked()).count();
        }
        
        public int getD() {
            return options.getD();
        }
        
        public boolean isTeams() {
            return options.isTeams();
        }
        
        public int getT() {
            return teams.size();
        }
        
        public Random getRandom() {
            return random;
        }
        
        public Player.PlayerData getPlayerData(int index) {
            return players.get(index).getPlayerData();
        }
        
        public Team.TeamData getTeamData(int index) {
            return teams.get(index).getTeamData();
        }
        
        public List<Integer> getTeam(int teamNumber) {
            return new ArrayList<>(teams.get(teamNumber).getMembers().stream()
                    .map(Player::getIndex)
                    .collect(Collectors.toList()));
        }
        
        public int getLeader() {
            return leader;
        }
        
        public int getDealer() {
            return OhHellCore.this.getDealer();
        }
        
        public Card getTrump() {
            return trump;
        }
        
        public RoundDetails getRoundDetails() {
            return rounds.get(roundNumber);
        }
        
        public int whatCanINotBid(Player player) {
            if (player.getIndex() == getDealer()) {
                return rounds.get(roundNumber).getHandSize()
                        - players.stream().map(p -> p.hasBid() ? p.getBid() : 0).reduce(0, (a, b) -> a + b);
            } else {
                return -1;
            }
        }
        
        public List<Card> getTrick() {
            return players.stream()
                    .map(Player::getTrick)
                    .filter(c -> !c.isEmpty())
                    .collect(Collectors.toList());
        }
        
        public int adjustedCardValue(Card card, List<List<Card>> additionalPlayeds) {
            return deck.adjustedCardValueSmall(card, additionalPlayeds);
        }
        public int adjustedCardValue(Card card, List<List<Card>> additionalPlayeds, boolean ignorePlayed) {
            return deck.adjustedCardValueSmall(card, additionalPlayeds, ignorePlayed);
        }
        
        public int cardsLeftOfSuit(int suit, List<List<Card>> additionalPlayeds) {
            return deck.cardsLeftOfSuit(suit, additionalPlayeds);
        }
        
        public int cardsLeftOfSuit(int suit, List<List<Card>> additionalPlayeds, boolean ignorePlayed) {
            return deck.cardsLeftOfSuit(suit, additionalPlayeds, ignorePlayed);
        }
        
        public int matchingCardsLeft(Card card, List<List<Card>> additionalPlayeds) {
            return deck.matchingCardsLeft(card, additionalPlayeds);
        }
        
        public int matchingCardsLeft(Card card, List<List<Card>> additionalPlayeds, boolean ignorePlayed) {
            return deck.matchingCardsLeft(card, additionalPlayeds, ignorePlayed);
        }
        
        // Used in bidding. Returns the highest makeable bid (max 0) for the given player.
        public int highestMakeableBid(Player player, boolean considerDealer) {
            if (options.isTeams()) {
                int totalBid = 0;
                int teamBid = 0;
                for (Player p : players) {
                    totalBid += p.getBid();
                    if (p.getTeam() == player.getTeam()) {
                        teamBid += p.getBid();
                    }
                }
                
                return Math.max(
                        rounds.get(roundNumber).getHandSize()
                        - teamBid
                        - (considerDealer && totalBid == teamBid && isDealerOnMyTeam(player) ? 1 : 0),
                        0);
            } else {
                return rounds.get(roundNumber).getHandSize();
            }
        }
        
        public boolean isDealerOnMyTeam(Player player) {
            return players.get(getDealer()).getTeam() == player.getTeam();
        }
        
        // Returns how many tricks the given player wants
        public int wants(int index) {
            Player player = players.get(index);
            int h = rounds.get(roundNumber).getHandSize();
                    
            int myWants = player.getBid() - player.getTaken();
            myWants = Math.max(Math.min(myWants, h), 0);
            
            if (options.isTeams()) {
                int teamWants = teamWants(player.getTeam());
                myWants = Math.max(Math.min(myWants, teamWants), 0);
                if (teamWants == h) {
                    myWants = teamWants;
                }
            }
            
            return myWants;
        }
        
        public int teamWants(int index) {
            Team team = teams.get(index);
            int h = rounds.get(roundNumber).getHandSize();
            
            int teamWants = team.getMembers().stream()
                    .map(p -> p.getBid() - p.getTaken())
                    .reduce(0, Integer::sum);
            return Math.max(Math.min(teamWants, h), 0);
        }
        
        public List<Card> whatCanIPlay(Player player) {
            List<Card> canPlay = player.getHand().stream()
                    .filter(c -> players.get(leader).getTrick() == null 
                    || players.get(leader).getTrick().isEmpty()
                    || c.getSuit() == players.get(leader).getTrick().getSuit())
                    .collect(Collectors.toList());
            if (canPlay.isEmpty()) {
                canPlay = player.getHand();
            }
            return canPlay;
        }
        
        /**
         * This function is used by the IVL as a first pass to determine if a card can win the current 
         * trick. Given a card, a return value of -1 guarantees that the card will not win the trick if
         * played by the current player. Otherwise, a nonnegative integer will be returned that will 
         * convey information about the likelihood of winning.
         * 
         * - In standard single-deck, it returns 0 if the card will currently be winning the trick.
         * 
         * - In double-deck, it returns the number of cards already played that would need to be 
         * canceled in order for the card to win. Note that this number will not exceed the number of
         * cards currently in the trick or the number of cards to be played afterward. In particular, 
         * this number will not exceed (N - 1) / 2, where N is the number of players.
         * 
         * Note that this function only uses information currently available to the current player. 
         * Also note that it is imperfect; for example (TODO), it does not take into account suit 
         * voids known to all players. Therefore, currently, the return value may not be -1 even if it 
         * can be deduced with certainty that the card will not win the trick.
         */
        public int cardCanWin(Card card, int index) {
            final boolean debug = true;
            
            // If the player is leading, always return 0.
            if (turn == leader) {
                if (debug) {
                    System.out.println(card + " winnable = 0");
                    System.out.println("   card is led");
                }
                return 0;
            }
            
            // If the card does not match the led suit or trump suit, return -1.
            if (card.getSuit() != players.get(leader).getTrick().getSuit()
                    && card.getSuit() != trump.getSuit()) {
                if (debug) {
                    System.out.println(card + " winnable = -1:");
                    System.out.println("   card is not led suit or trump");
                }
                return -1;
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
            
            // If the matching copy of this card is in the trick already, return -1.
            if (counts.get(card.toString()) != null && counts.get(card.toString()) == 1) {
                if (debug) {
                    System.out.println(card + " winnable = -1:");
                    System.out.println("   card is canceled");
                }
                return -1;
            }
            
            int requiredCancels = 0;

            for (int i = 0; i < players.size(); i++) {
                Card c = players.get(i).getTrick();
                
                // If c is not canceled...
                if (counts.get(c.toString()) == 1) {
                    // If c beats card...
                    if (c.isGreaterThan(card, trump.getSuit())) {
                        // If c cannot be canceled, we can return -1 immediately. Otherwise, increment
                        // requiredCancels.
                        boolean uncancelableBecauseSeenAlready = 
                                deck.matchingCardsLeft(c, new LinkedList<>()) == deck.getD() - 1;
                        boolean uncancelableBecauseIHaveIt = 
                                players.get(turn).getHand().stream().anyMatch(c1 -> c1.matches(c));
                        if (uncancelableBecauseSeenAlready) {
                            if (debug) {
                                System.out.println(card + " winnable = -1:");
                                System.out.println("   card is beat by " + c + ", which was seen already");
                            }
                            return -1;
                        } else if (uncancelableBecauseIHaveIt) {
                            if (debug) {
                                System.out.println(card + " winnable = -1:");
                                System.out.println("   card is beat by " + c + ", which is in my hand");
                            }
                            return -1;
                        }
                        
                        requiredCancels++;
                    }
                }
            }
            
            int N = (int) players.stream().filter(p -> !p.isKicked()).count();
            int playersLeft = (leader - turn - 1 + N) % N;
            // If there are more cards to be canceled than there are players left to play in the trick,
            // then return -1.
            if (requiredCancels > playersLeft) {
                if (debug) {
                    System.out.println(card + " winnable = -1:");
                    System.out.println("   " + playersLeft + " remaining players cannot cancel " + requiredCancels + " cards");
                }
                return -1;
            } else {
                if (debug) {
                    System.out.println(card + " winnable = " + requiredCancels);
                }
                return requiredCancels;
            }
        }
        
        public Integer[] cancelsRequired(Player whoIsAsking, Card card) {
            Integer[] ans = new Integer[players.size()];
            
            List<CancelsRequiredHelperTrick> trick = new LinkedList<>();
            for (Player player : trickOrder) {
                trick.add(new CancelsRequiredHelperTrick(player, player.getTrick()));
            }
            
            CancelsRequiredHelperTrick hypo = new CancelsRequiredHelperTrick(players.get(turn), card);
            
            int ledSuit = players.get(leader).getTrick().getSuit();
            if (turn == leader) {
                trick.add(hypo);
            } else if (card.getSuit() == ledSuit || card.getSuit() == trump.getSuit()) {
                ListIterator<CancelsRequiredHelperTrick> iter = trick.listIterator();
                boolean spotFound = false;
                boolean canceled = false;
                while (iter.hasNext() && !spotFound && !canceled) {
                    Card next = iter.next().card;
                    if (next.matches(card)) {
                        canceled = true;
                        iter.remove();
                    } else {
                        spotFound = !next.isGreaterThan(card, ledSuit, trump.getSuit());
                    }
                }
                if (!canceled) {
                    if (spotFound) {
                        iter.previous();
                    }
                    iter.add(hypo);
                }
            }
            
            if (trick.isEmpty()) {
                ans[leader] = 0;
            }
            
            Set<Integer> myHand = whoIsAsking.getHand().stream()
                    .filter(c -> c != card)
                    .map(Card::toNumber)
                    .collect(Collectors.toSet());
            
            
            int i = 0;
            int maxCancels = (leader - turn + players.size() - 1) % players.size();
            for (CancelsRequiredHelperTrick t : trick) {
                ans[t.player.getIndex()] = i;
                
                if (options.getD() == 1) {
                    break;
                }
                
                boolean uncancelableBecauseSeenAlready = 
                        deck.matchingCardsLeft(t.card, new LinkedList<>()) == deck.getD() - 1;
                boolean uncancelableBecauseInMyHand = 
                        myHand.contains(t.card.toNumber());
                if (uncancelableBecauseSeenAlready || uncancelableBecauseInMyHand || i == maxCancels) {
                    break;
                }
                
                i++;
            }
            
            for (i = 0; i < players.size(); i++) {
                int j = (i + leader) % players.size();
                if (ans[j] == null) {
                    if (i <= (turn - leader + players.size()) % players.size()) {
                        ans[j] = -2;
                    } else {
                        ans[j] = -1;
                    }
                }
            }
            
            return ans;
        }
        
        public IndicesRelativeTo getIndicesRelativeTo(int index) {
            return new IndicesRelativeTo(index);
        }
        
        public int nextUnkicked(int index) {
            return OhHellCore.this.nextUnkicked(index);
        }
        
        private class CancelsRequiredHelperTrick {
            public Player player;
            public Card card;
            
            public CancelsRequiredHelperTrick(Player player, Card card) {
                this.player = player;
                this.card = card;
            }
        }
        
        public class IndicesRelativeTo implements Iterable<Integer> {
            public class IndicesRelativeToIterator implements Iterator<Integer> {
                @Override
                public boolean hasNext() {
                    return next != start || current == -1;
                }

                @Override
                public Integer next() {
                    current = next;
                    next = OhHellCore.this.nextUnkicked(current);
                    return current;
                }
            }
            
            private int start;
            private int current;
            private int next;
            
            public IndicesRelativeTo(int index) {
                index = OhHellCore.this.nextUnkicked(index - 1);
                
                start = index;
                current = -1;
                next = index;
            }

            @Override
            public Iterator<Integer> iterator() {
                return new IndicesRelativeToIterator();
            }
        }
    }
}