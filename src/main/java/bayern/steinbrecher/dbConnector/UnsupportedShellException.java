package bayern.steinbrecher.dbConnector;

/**
 * @author Stefan Huber
 * @since 0.15
 */
public class UnsupportedShellException extends Exception {
    public UnsupportedShellException() {
    }

    public UnsupportedShellException(String message) {
        super(message);
    }

    public UnsupportedShellException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedShellException(Throwable cause) {
        super(cause);
    }
}
