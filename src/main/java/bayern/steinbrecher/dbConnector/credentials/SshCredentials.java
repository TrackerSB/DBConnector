package bayern.steinbrecher.dbConnector.credentials;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
    public SshCredentials(@NotNull String dbUsername, @NotNull String dbPassword, @NotNull String sshUsername,
                          @NotNull String sshPassword) {
        this.dbUsername = Objects.requireNonNull(dbUsername);
        this.dbPassword = Objects.requireNonNull(dbPassword);
        this.sshUsername = Objects.requireNonNull(sshUsername);
        this.sshPassword = Objects.requireNonNull(sshPassword);
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getDbUsername() {
        return dbUsername;
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getSshUsername() {
        return sshUsername;
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getSshPassword() {
        return sshPassword;
    }
}
