package client;

public class IllegalClientStateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalClientStateException(String message) {
        super(message);
    }
}
