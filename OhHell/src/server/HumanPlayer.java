package server;
import java.util.List;

import core.Card;
import core.Player;
import core.RoundDetails;

public class HumanPlayer extends Player {
    private PlayerThread thread;
    
    public HumanPlayer(String name, PlayerThread thread) {
        setName(name);
        this.thread = thread;
    }
    
    @Override
    public String realName() {
        return thread.getSocket().getInetAddress().toString();
    }
    
    @Override
    public boolean isHuman() {
        return true;
    }
    
    public PlayerThread getThread() {
        return thread;
    }
    
    public void setThread(PlayerThread thread) {
        this.thread = thread;
    }

    @Override
    public void commandStart() {
        thread.sendCommand("START");
    }

    @Override
    public void commandPlayersInfo(List<Player> players, List<Player> kibitzers, Player player) {
        thread.sendCommand(playerInfoCommand(players, kibitzers, player));
    }

    public String playerInfoCommand(List<Player> players, List<Player> kibitzers, Player player) {
        return players.stream()
                .map(p -> 
                    "STRING " + p.getName().length() + ":" + p.getName() + ":"
                        + p.isHuman() + ":"
                        + p.isHost() + ":"
                        + p.isDisconnected() + ":"
                        + p.isKicked() + ":"
                        + p.isKibitzer() + ":"
                        + p.equals(player) + ":")
                .reduce("UPDATEPLAYERS:", (sofar, pString) -> sofar + pString)
            + kibitzers.stream()
                .map(p -> 
                    "STRING " + p.getName().length() + ":" + p.getName() + ":"
                        + p.isHuman() + ":"
                        + p.isHost() + ":"
                        + p.isDisconnected() + ":"
                        + p.isKicked() + ":"
                        + p.isKibitzer() + ":"
                        + p.equals(player) + ":")
                .reduce("", (sofar, pString) -> sofar + pString);
    }

    @Override
    public void commandStatePlayer(Player player) {
        thread.sendCommand("STATEPLAYER:" +
                player.getIndex() + ":" + 
                player.hasBid() + ":" + 
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

    @Override
    public void commandDealerLeader(int dealer, int leader) {
        thread.sendCommand("STATEDEALERLEADER:" + dealer + ":" + leader);
    }

    @Override
    public void commandUpdateRounds(List<RoundDetails> rounds, int roundNumber) {
        thread.sendCommand(rounds.stream()
                .map(r -> r.getDealer().getIndex() + ":" + r.getHandSize() + ":")
                .reduce("UPDATEROUNDS:" + roundNumber + ":", 
                        (sofar, cString) -> sofar + cString));
    }

    @Override
    public void commandDeal(List<Player> players, Card trump) {
        for (Player p : players) {
            if (p.isKicked()) {
                thread.sendCommand("DEAL:" + p.getIndex());
            } else {
                thread.sendCommand(
                        p.getHand().stream()
                        .map(card -> 
                            (this == p || isKibitzer() || p.isHandRevealed() ? card.toString() : "0") + ":")
                        .reduce("DEAL:" + p.getIndex() + ":", 
                                (sofar, cString) -> sofar + cString));
            }
        }
        thread.sendCommand("TRUMP:" + trump.toString());
    }

    @Override
    public void commandRedeal() {
        thread.sendCommand("REDEAL");
    }

    @Override
    public void commandBid(int index) {
        thread.sendCommand("BID:" + index);
    }

    @Override
    public void commandPlay(int index) {
        thread.sendCommand("PLAY:" + index);
    }

    @Override
    public void commandBidReport(int index, int bid) {
        thread.sendCommand("BIDREPORT:" + index + ":" + bid);
    }

    @Override
    public void commandPlayReport(int index, Card card) {
        thread.sendCommand("PLAYREPORT:" + index + ":" + card);
    }

    @Override
    public void commandTrickWinner(int index, List<Card> trick) {
        thread.sendCommand("TRICKWINNER:" + index);
    }
    
    @Override
    public void commandClaimRequest(int index) {
        thread.sendCommand("CLAIMREQUEST:" + index);
    }
    
    @Override
    public void commandClaimResult(boolean accept) {
        thread.sendCommand("CLAIMRESULT:" + (accept ? "ACCEPT" : "REFUSE"));
    }

    @Override
    public void commandNewScores(List<Integer> scores) {
        thread.sendCommand(scores.stream()
                .map(score -> score == null ? "-:" : score + ":")
                .reduce("REPORTSCORES:", (a, b) -> a + b));
    }

    @Override
    public void commandFinalScores(List<Player> playersSorted) {
        thread.sendCommand(playersSorted.stream()
                .map(p -> "STRING " + p.getName().length() 
                        + ":" + p.getName() + ":" + p.getScore()+":")
                .reduce("FINALSCORES:", (a, b) -> a + b));
    }

    @Override
    public void commandChat(String text) {
        thread.sendCommand("CHAT:STRING " + text.length() + ":" + text);
    }

    @Override
    public void commandKick() {
        thread.sendCommand("KICK");
    }
    
    @Override
    public void commandPoke() {
        thread.sendCommand("POKE");
    }
}
