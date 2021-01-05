import java.util.List;

import ohHellCore.Card;
import ohHellCore.Player;
import ohHellCore.RoundDetails;

public class HumanPlayer extends Player {
    private PlayerThread thread;
    
    public HumanPlayer(String name, PlayerThread thread) {
        setName(name);
        this.thread = thread;
    }
    
    public String realName() {
        return thread.getSocket().getInetAddress().toString();
    }
    
    public PlayerThread getThread() {
        return thread;
    }
    
    public void setThread(PlayerThread thread) {
        this.thread = thread;
    }
    
    public void commandStart() {
        thread.sendCommand("START");
    }
    
    public void commandPlayersInfo(List<Player> players, List<Player> kibitzers, Player player) {
        thread.sendCommand(playerInfoCommand(players, kibitzers, player));
    }
    
    public String playerInfoCommand(List<Player> players, List<Player> kibitzers, Player player) {
        return players.stream()
                .map(p -> 
                    "STRING " + p.getName().length() + ":"
                        + p.getName() + ":"
                        + p.isHost() + ":"
                        + p.isDisconnected() + ":"
                        + p.isKicked() + ":"
                        + p.isKibitzer() + ":"
                        + p.equals(player) + ":")
                .reduce("UPDATEPLAYERS:", (sofar, pString) -> sofar + pString)
            + kibitzers.stream()
                .map(p -> 
                    "STRING " + p.getName().length() + ":"
                        + p.getName() + ":"
                        + p.isHost() + ":"
                        + p.isDisconnected() + ":"
                        + p.isKicked() + ":"
                        + p.isKibitzer() + ":"
                        + p.equals(player) + ":")
                .reduce("", (sofar, pString) -> sofar + pString);
    }
    
    public void commandStatePlayer(Player player) {
        thread.sendCommand("STATEPLAYER:" +
                player.getIndex() + ":" + 
                player.getBid() + ":" + 
                player.getTaken() + ":" + 
                player.getLastTrick() + ":" + 
                player.getTrick() + ":");
        thread.sendCommand(player.getBids().stream()
                .map(bid -> bid + ":")
                .reduce("STATEPLAYERBIDS:" + player.getIndex() + ":", 
                        (sofar, bid) -> sofar + bid));
        thread.sendCommand(player.getScores().stream()
                .map(score -> score + ":")
                .reduce("STATEPLAYERSCORES:" + player.getIndex() + ":", 
                        (sofar, score) -> sofar + score));
    }
    
    public void commandDealerLeader(int dealer, int leader) {
        thread.sendCommand("STATEDEALERLEADER:" + dealer + ":" + leader);
    }
    
    public void commandUpdateRounds(List<RoundDetails> rounds, int roundNumber) {
        thread.sendCommand(rounds.stream()
                .map(r -> r.getDealer().getIndex() + ":" + r.getHandSize() + ":")
                .reduce("UPDATEROUNDS:" + roundNumber + ":", 
                        (sofar, cString) -> sofar + cString));
    }
    
    public void commandDeal(List<Player> players, Card trump) {
        for (Player p : players) {
            if (p.isKicked()) {
                thread.sendCommand("DEAL:" + p.getIndex());
            } else {
                thread.sendCommand(
                        p.getHand().stream()
                        .map(card -> 
                            (this == p || isKibitzer() ? card.toString() : "0") + ":")
                        .reduce("DEAL:" + p.getIndex() + ":", 
                                (sofar, cString) -> sofar + cString));
            }
        }
        thread.sendCommand("TRUMP:" + trump.toString());
    }
    
    public void commandRedeal() {
        thread.sendCommand("REDEAL");
    }
    
    public void commandBid(int index) {
        thread.sendCommand("BID:" + index);
    }
    
    public void commandPlay(int index) {
        thread.sendCommand("PLAY:" + index);
    }
    
    public void commandBidReport(int index, int bid) {
        thread.sendCommand("BIDREPORT:" + index + ":" + bid);
    }
    
    public void commandPlayReport(int index, Card card) {
        thread.sendCommand("PLAYREPORT:" + index + ":" + card);
    }
    
    public void commandTrickWinner(int index, List<Card> trick) {
        thread.sendCommand("TRICKWINNER:" + index);
    }
    
    public void commandNewScores(List<Integer> scores) {
        thread.sendCommand(scores.stream()
                .map(score -> score == null ? "-:" : score + ":")
                .reduce("REPORTSCORES:", (a, b) -> a + b));
    }
    
    public void commandFinalScores(List<Player> playersSorted) {
        thread.sendCommand(playersSorted.stream()
                .map(p -> "STRING " + p.getName().length() 
                        + ":" + p.getName() + ":" + p.getScore()+":")
                .reduce("FINALSCORES:", (a, b) -> a + b));
    }
    
    public void commandChat(String text) {
        thread.sendCommand("CHAT:STRING " + text.length() + ":" + text);
    }
    
    public void commandKick() {
        thread.sendCommand("KICK");
    }
}
