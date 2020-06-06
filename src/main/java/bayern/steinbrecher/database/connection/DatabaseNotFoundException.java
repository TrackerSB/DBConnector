package bayern.steinbrecher.database.connection;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class DatabaseNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @since 0.1
     */
    public DatabaseNotFoundException() {
        super();
    }

    /**
     * @since 0.1
     */
    public DatabaseNotFoundException(String message) {
        super(message);
    }

    /**
     * @since 0.1
     */
    public DatabaseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
