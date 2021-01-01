package bayern.steinbrecher.dbConnector.credentials;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Stefan Huber
 * @since 0.15
 */
public abstract class DBCredentials {

    private final String dbUsername;
    private final String dbPassword;

    public DBCredentials(@NotNull String username, @NotNull String password) {
        this.dbUsername = Objects.requireNonNull(username);
        this.dbPassword = Objects.requireNonNull(password);
    }

    @NotNull
    public String getDbUsername() {
        return dbUsername;
    }

    @NotNull
    public String getDbPassword() {
        return dbPassword;
    }
}
