package bayern.steinbrecher.database.connection.credentials;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class SshCredentials implements DBCredentials {

    private final String dbUsername;
    private final String dbPassword;
    private final String sshUsername;
    private final String sshPassword;

    /**
     * @since 0.1
     */
    public SshCredentials(String dbUsername, String dbPassword, String sshUsername, String sshPassword) {
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.sshUsername = sshUsername;
        this.sshPassword = sshPassword;
    }

    /**
     * @since 0.1
     */
    public String getDbUsername() {
        return dbUsername;
    }

    /**
     * @since 0.1
     */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * @since 0.1
     */
    public String getSshUsername() {
        return sshUsername;
    }

    /**
     * @since 0.1
     */
    public String getSshPassword() {
        return sshPassword;
    }
}
