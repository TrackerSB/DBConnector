package bayern.steinbrecher.dbConnector.query;

/**
 * @author Stefan Huber
 * @since 0.5
 */
public class GenerationFailedException extends Exception {

    public GenerationFailedException() {
        super();
    }

    public GenerationFailedException(String message) {
        super(message);
    }

    public GenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
