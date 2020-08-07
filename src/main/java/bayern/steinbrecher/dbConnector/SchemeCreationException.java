package bayern.steinbrecher.dbConnector;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class SchemeCreationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @since 0.1
     */
    public SchemeCreationException() {
        super();
    }

    /**
     * @since 0.1
     */
    public SchemeCreationException(String message) {
        super(message);
    }

    /**
     * @since 0.1
     */
    public SchemeCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
