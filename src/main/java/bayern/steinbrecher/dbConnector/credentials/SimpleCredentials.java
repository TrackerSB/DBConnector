package bayern.steinbrecher.dbConnector.credentials;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
    public SimpleCredentials(@NotNull String username, @NotNull String password) {
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getPassword() {
        return password;
    }
}
