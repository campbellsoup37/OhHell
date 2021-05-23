package core;

import java.util.List;

public class Team extends Player {
    private List<Player> members;
    
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
        return members.stream().map(Player::getBid).reduce(0, (a, b) -> a + b);
    }
    
    @Override
    public int getTaken() {
        return members.stream().map(Player::getTaken).reduce(0, (a, b) -> a + b);
    }

    public List<Player> getMembers() {
        return members;
    }

    public void setMembers(List<Player> members) {
        this.members = members;
    }
    
    public void addMember(Player player) {
        this.members.add(player);
    }
}
