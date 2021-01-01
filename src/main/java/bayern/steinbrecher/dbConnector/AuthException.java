package bayern.steinbrecher.dbConnector;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class AuthException extends Exception {

    public AuthException() {
    }

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthException(Throwable cause) {
        super(cause);
    }
}
