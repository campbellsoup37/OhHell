package common;

public class TcpTools {
    public static String encode(String text) {
        return text.isEmpty() ? "" : "STRING " + text.length() + ":" + text;
    }
}
