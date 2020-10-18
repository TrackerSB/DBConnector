package bayern.steinbrecher.dbConnector.query;

/**
 * @author Stefan Huber
 * @since 0.10
 */
public class QueryFailedException extends Exception{
    public QueryFailedException() {
    }

    public QueryFailedException(String message) {
        super(message);
    }

    public QueryFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryFailedException(Throwable cause) {
        super(cause);
    }
}
