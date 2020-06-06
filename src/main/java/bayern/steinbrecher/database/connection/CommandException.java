package bayern.steinbrecher.database.connection;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class CommandException extends Exception {

    /**
     * @since 0.1
     */
    public CommandException() {
        super();
    }

    /**
     * @since 0.1
     */
    public CommandException(String message) {
        super(message);
    }

    /**
     * @since 0.1
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
