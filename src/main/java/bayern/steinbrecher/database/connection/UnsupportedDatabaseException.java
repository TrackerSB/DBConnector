package bayern.steinbrecher.database.connection;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class UnsupportedDatabaseException extends Exception {

    /**
     * @since 0.1
     */
    public UnsupportedDatabaseException() {
        super();
    }

    /**
     * @since 0.1
     */
    public UnsupportedDatabaseException(String message) {
        super(message);
    }

    /**
     * @since 0.1
     */
    public UnsupportedDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
