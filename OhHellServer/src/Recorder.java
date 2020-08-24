import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Recorder {
    private BufferedWriter writer;
    private boolean ready = false;
    private LinkedList<String> recordQueue = new LinkedList<String>();
    private boolean queueStarted = false;
    
    public Recorder() {}
    
    public void start() {
        try {
            writer = new BufferedWriter(new FileWriter(
                    "OhHellServerStats/" + System.currentTimeMillis() + ".txt",
                    true));
            ready = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void recordPlayers(List<String> addresses) {
        write(addresses.stream()
                .reduce("", (sofar, s) -> sofar + s + ":"));
    }
    
    public void recordTrump(Card card) {
        write("TRUMP:" + card);
    }
    
    public void recordDealer(int index) {
        write("DEALER:" + index);
    }
    
    public void recordBids(List<Integer> bids) {
        write(bids.stream()
                .map(b -> b.toString())
                .reduce("BIDS:", (sofar, s) -> sofar + s + ":"));
    }
    
    public void recordTrick(List<Card> cards, int winner) {
        write(cards.stream()
                .map(c -> c.toString())
                .reduce("TRICK:", (sofar, s) -> sofar + s + ":")
                + winner);
    }
    
    public void recordResults(List<Integer> results) {
        write(results.stream()
                .map(r -> r.toString())
                .reduce("RESULTS:", (sofar, s) -> sofar + s + ":"));
    }
    
    public void recordKick(int index) {
        write("KICK:" + index);
    }
    
    public void write(String text) {
        recordQueue.add(text + "\n");
        if (!queueStarted && ready) {
            queueStarted = true;
            while (recordQueue.size() > 0) {
                String line = recordQueue.remove();
                try {
                    writer.write(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            recordQueue = new LinkedList<String>();
            queueStarted = false;
        }
    }
    
    public void stop() {
        try {
            writer.flush();
            writer.close();
            ready = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
