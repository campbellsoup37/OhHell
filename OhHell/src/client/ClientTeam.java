package client;

import java.util.LinkedList;
import java.util.List;

public class ClientTeam extends ClientPlayer {
    private List<ClientPlayer> members = new LinkedList<>();
    
    public ClientTeam() {}
    
    @Override
    public boolean isTeam() {
        return true;
    }
    
    @Override
    public List<Integer> getScores() {
        return members == null || members.isEmpty() ? new LinkedList<>() : members.get(0).getScores();
    }
    
    @Override
    public int getScore() {
        return members == null || members.isEmpty() ? 0 : members.get(0).getScore();
    }
    
    @Override
    public int getBid() {
        return members == null ? 0 : members.stream().map(ClientPlayer::getBid).reduce(0, (a, b) -> a + b);
    }
    
    @Override
    public int getTaken() {
        return members == null ? 0 : members.stream().map(ClientPlayer::getTaken).reduce(0, (a, b) -> a + b);
    }

    public List<ClientPlayer> getMembers() {
        return members;
    }

    public void setMembers(List<ClientPlayer> members) {
        this.members = members;
    }
}
