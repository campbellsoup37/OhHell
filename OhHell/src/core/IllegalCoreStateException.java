package core;

public class IllegalCoreStateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalCoreStateException(String message) {
        super(message);
    }
}
