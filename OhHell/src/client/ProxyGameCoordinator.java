package client;

import common.TcpTools;
import core.Card;
import core.GameCoordinator;
import core.GameOptions;
import core.Player;

public class ProxyGameCoordinator extends GameCoordinator {
    ClientReadThread readThread;
    ClientWriteThread writeThread;
    
    public ProxyGameCoordinator(ClientReadThread readThread, ClientWriteThread writeThread) {
        this.readThread = readThread;
        this.writeThread = writeThread;
    }
    
    public void sendCommandToServer(String text) {
        writeThread.write(text);
    }
    
    @Override
    public void disconnectPlayer(Player player) {
        readThread.disconnect();
        sendCommandToServer("CLOSE");
        writeThread.disconnect();
    }
    
    @Override
    public void joinPlayer(Player player, String id) {
        sendCommandToServer("ID:" + TcpTools.encode(id));
    }
    
    @Override
    public void requestEndGame(Player player) {
        sendCommandToServer("STOP");
    }
    
    @Override
    public void setKibitzer(Player player, boolean kibitzer) {
        sendCommandToServer("KIBITZER:" + kibitzer);
    }
    
    @Override
    public void renamePlayer(Player player, String name) {
        sendCommandToServer("RENAME:" + TcpTools.encode(name));
    }
    
    @Override
    public void reteamPlayer(int index, int team) {
        sendCommandToServer("RETEAM:" + index + ":" + team);
    }
    
    @Override
    public void renameTeam(int team, String name) {
        sendCommandToServer("RENAMETEAM:" + TcpTools.encode(name));
    }
    
    @Override
    public void scrambleTeams() {
        sendCommandToServer("TEAMSCRAMBLE");
    }
    
    @Override
    public void updateOptions(GameOptions options) {
        sendCommandToServer("OPTIONS:" + options);
    }
    
    @Override
    public void startGame(GameOptions options) {
        sendCommandToServer("START:" + options);
    }
    
    @Override
    public void makeBid(Player player, int bid) {
        sendCommandToServer("BID:" + bid);
    }
    
    @Override
    public void makePlay(Player player, Card card) {
        sendCommandToServer("PLAY:" + card);
    }
    
    @Override
    public void processUndoBid(Player player) {
        sendCommandToServer("UNDOBID");
    }
    
    @Override
    public void processClaim(Player player) {
        sendCommandToServer("CLAIM");
    }
    
    @Override
    public void processClaimResponse(Player player, boolean accept) {
        sendCommandToServer("CLAIMRESPONSE:" + accept);
    }
    
    @Override
    public void addKickVote(int index, Player fromPlayer) {
        sendCommandToServer("VOTEKICK:" + index);
    }
    
    @Override
    public void sendChat(Player sender, String recipient, String text) {
        sendCommandToServer("CHAT:" + TcpTools.encode(recipient) + ":" + TcpTools.encode(text));
    }
    
    @Override
    public void pokePlayer() {
        sendCommandToServer("POKE");
    }
    
    @Override
    public void startPing() {
        sendCommandToServer("PING");
    }
}
