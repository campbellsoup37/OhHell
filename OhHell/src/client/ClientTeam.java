package client;

import java.util.List;

public class ClientTeam extends ClientPlayer {
    private int number;
    private List<ClientPlayer> members;
    
    public ClientTeam(int number, List<ClientPlayer> members) {
        this.setNumber(number);
        this.setMembers(members);
    }
    
    @Override
    public String getName() {
        return "Team " + number;
    }
    
    @Override
    public boolean isTeam() {
        return true;
    }
    
    @Override
    public List<Integer> getScores() {
        return members.get(0).getScores();
    }
    
    @Override
    public int getScore() {
        return members.get(0).getScore();
    }
    
    @Override
    public int getBid() {
        return members.stream().map(ClientPlayer::getBid).reduce(0, (a, b) -> a + b);
    }
    
    @Override
    public int getTaken() {
        return members.stream().map(ClientPlayer::getTaken).reduce(0, (a, b) -> a + b);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<ClientPlayer> getMembers() {
        return members;
    }

    public void setMembers(List<ClientPlayer> members) {
        this.members = members;
    }
}
