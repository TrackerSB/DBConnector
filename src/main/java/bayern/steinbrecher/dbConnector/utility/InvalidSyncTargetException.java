package bayern.steinbrecher.dbConnector.utility;

/**
 * @author Stefan Huber
 * @since 0.16
 */
public class InvalidSyncTargetException extends Exception {
    public InvalidSyncTargetException() {
    }

    public InvalidSyncTargetException(String message) {
        super(message);
    }

    public InvalidSyncTargetException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSyncTargetException(Throwable cause) {
        super(cause);
    }
}
