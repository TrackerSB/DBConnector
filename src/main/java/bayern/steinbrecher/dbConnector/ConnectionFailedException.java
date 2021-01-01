package bayern.steinbrecher.dbConnector;

/**
 * @author Stefan Huber
 * @since 0.15
 */
public class ConnectionFailedException extends Exception{
    public ConnectionFailedException() {
    }

    public ConnectionFailedException(String message) {
        super(message);
    }

    public ConnectionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionFailedException(Throwable cause) {
        super(cause);
    }
}
