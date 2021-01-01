package bayern.steinbrecher.dbConnector.query;

/**
 * @author Stefan Huber
 * @since 0.15
 */
public class UnsupportedDBMSException extends Exception {
    public UnsupportedDBMSException() {
    }

    public UnsupportedDBMSException(String message) {
        super(message);
    }

    public UnsupportedDBMSException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedDBMSException(Throwable cause) {
        super(cause);
    }
}
