package server;

import core.Player;

public class DummyPlayer extends Player {
    public DummyPlayer() {
        setName("Bot");
        setId("Dummy");
    }
    
    public boolean isHuman() {
        return false;
    }
    
    public String realName() {
        return "Dummy";
    }
}
