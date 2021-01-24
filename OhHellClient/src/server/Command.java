package server;
public class Command {
    private String text;
    private long id;
    
    public Command(String text, long id) {
        this.text = text;
        this.id = id;
    }
    
    @Override
    public String toString() {
        return id + ":" + text;
    }
    
    public long getId() {
        return id;
    }
}