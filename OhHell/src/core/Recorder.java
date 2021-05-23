package core;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Recorder {
    public static final String lineDelimiter = "|";
    public static final String lineDelimiterRegex = "\\|";
    public static final String commandDelimiter1 = ";";
    public static final String commandDelimiter2 = ":";
    /**
     * The following regex is a positive look-behind that checks for an even number of 
     * backslashes. For example, if we ask for str.split(splitPreceder + ";"), the string str will
     * be split at all ; characters that are not preceded by an even number of backslashes.
     */
    public static final String splitPreceder = "(?<=[^\\\\](\\\\\\\\){0,999})";
    
    private volatile LinkedList<String> file;
    private volatile LinkedList<String> round;
    
    public Recorder() {}
    
    public void start() {
        file = new LinkedList<>();
    }
    
    public static String encodeString(String s) {
        return s.replace("\\", "\\\\")
                .replace(lineDelimiter, "\\" + lineDelimiter)
                .replace(commandDelimiter1, "\\" + commandDelimiter1)
                .replace(commandDelimiter2, "\\" + commandDelimiter2);
    }
    
    public static String decodeString(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\" + lineDelimiter, lineDelimiter)
                .replace("\\" + commandDelimiter1, commandDelimiter1)
                .replace("\\" + commandDelimiter2, commandDelimiter2);
    }
    
    public void recordInfo(GameOptions options, List<Player> players, Map<Integer, Team> teams) {
        file.add("decks" + commandDelimiter1 + options.getD());
        if (options.isOregon()) {
            file.add("oregon" + commandDelimiter1 + "true");
        }
        String line1 = "players";
        for (Player player : players) {
            line1 += commandDelimiter1
                    + encodeString(player.getId()) + commandDelimiter2
                    + encodeString(player.getName()) + commandDelimiter2
                    + (player.isHuman() ? "human" : "ai");
        }
        file.add(line1);
        if (options.isTeams()) {
            String line2 = "teams";
            for (Player player : players) {
                line2 += commandDelimiter1 + player.getTeam();
            }
            file.add(line2);
            String line3 = "teaminfo";
            for (int teamNumber : teams.keySet()) {
                Team team = teams.get(teamNumber);
                line3 += commandDelimiter1
                        + teamNumber + commandDelimiter2
                        + encodeString(team.getName());
            }
            file.add(line3);
        }
    }
    
    public void recordRoundInfo(int handSize, int dealer, List<Player> players, Card trump) {
        round = new LinkedList<>();
        round.add("round" + commandDelimiter1 + dealer + commandDelimiter1 + handSize);
        String line = "hands";
        for (Player player : players) {
            line += commandDelimiter1 + player.getHand().get(0);
            for (int i = 1; i < handSize; i++) {
                line += commandDelimiter2 + player.getHand().get(i);
            }
        }
        round.add(line);
        round.add("trump" + commandDelimiter1 + trump);
    }
    
    public void recordBids(List<Player> players, int leader) {
        String line = "bids";
        for (Player player : players) {
            line += commandDelimiter1
                    + (player.getIndex() == leader ? 1 : 0) + commandDelimiter2
                    + player.getBid();
        }
        round.add(line);
    }
    
    public void unrecordBids() {
        round.removeLast();
    }
    
    public void recordTrick(List<Player> players, int leader, int winner) {
        String line = "trick";
        for (Player player : players) {
            line += commandDelimiter1 
                + (player.getIndex() == leader ? 1 : 0) + commandDelimiter2
                + (player.getIndex() == winner ? 1 : 0) + commandDelimiter2
                + player.getTrick();
        }
        round.add(line);
    }
    
    public void recordClaim(int index) {
        round.add("claim" + commandDelimiter1 + index);
    }
    
    public void recordRoundEnd(List<Player> players) {
        String line1 = "takens";
        for (Player player : players) {
            line1 += commandDelimiter1 + player.getTaken();
        }
        round.add(line1);
        String line2 = "scores";
        for (Player player : players) {
            line2 += commandDelimiter1 + player.getScore();
        }
        round.add(line2);
        file.addAll(round);
    }
    
    public void recordFinalScores(List<Player> players) {
        String line = "final scores";
        for (Player player : players) {
            line += commandDelimiter1
                    + player.getScore() + commandDelimiter2
                    + player.getPlace();
        }
        file.add(line);
    }
    
    public void recordKick(int index) {
        file.add("kick" + commandDelimiter1 + index);
    }
    
    public void sendFile(List<Player> players) {
        String finishedFile = file.stream()
                .reduce("", (s1, s2) -> s1 + (s1.isEmpty() ? "" : lineDelimiter) + s2);
        for (Player player : players) {
            player.commandPostGameFile(finishedFile);
        }
    }
}
