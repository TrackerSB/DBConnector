package bayern.steinbrecher.database.connection;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class AuthException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @since 0.1
     */
    public AuthException() {
        super();
    }

    /**
     * @since 0.1
     */
    public AuthException(String message) {
        super(message);
    }

    /**
     * @since 0.1
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
