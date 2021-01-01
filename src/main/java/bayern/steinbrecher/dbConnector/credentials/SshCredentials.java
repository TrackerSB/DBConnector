package bayern.steinbrecher.dbConnector.credentials;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class SshCredentials extends DBCredentials {

    private final String sshUsername;
    private final String sshPassword;

    public SshCredentials(@NotNull String dbUsername, @NotNull String dbPassword, @NotNull String sshUsername,
                          @NotNull String sshPassword) {
        super(dbUsername, dbPassword);
        this.sshUsername = Objects.requireNonNull(sshUsername);
        this.sshPassword = Objects.requireNonNull(sshPassword);
    }

    @NotNull
    public String getSshUsername() {
        return sshUsername;
    }

    @NotNull
    public String getSshPassword() {
        return sshPassword;
    }
}
