package bayern.steinbrecher.dbConnector.credentials;

import org.jetbrains.annotations.NotNull;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public class SimpleCredentials extends DBCredentials {
    public SimpleCredentials(@NotNull String username, @NotNull String password) {
        super(username, password);
    }
}
