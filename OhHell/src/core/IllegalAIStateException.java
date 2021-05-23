package core;

public class IllegalAIStateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalAIStateException(String message) {
        super(message);
    }
}
