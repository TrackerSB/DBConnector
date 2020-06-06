package bayern.steinbrecher.database.connection.credentials;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class SimpleCredentials implements DBCredentials {

    private final String username;
    private final String password;

    /**
     * @since 0.1
     */
    public SimpleCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * @since 0.1
     */
    public String getUsername() {
        return username;
    }

    /**
     * @since 0.1
     */
    public String getPassword() {
        return password;
    }
}
