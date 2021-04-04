package core;

public class DummyPlayer extends Player {
    public DummyPlayer(long id) {
        setName("Bot");
        setId("Dummy" + id);
    }
    
    public boolean isHuman() {
        return false;
    }
    
    public String realName() {
        return "Dummy";
    }
}
