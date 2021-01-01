package bayern.steinbrecher.dbConnector;

import bayern.steinbrecher.dbConnector.credentials.SshCredentials;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.query.SupportedDBMS;
import bayern.steinbrecher.dbConnector.query.SupportedShell;
import bayern.steinbrecher.dbConnector.query.UnsupportedDBMSException;
import bayern.steinbrecher.jsch.JSch;
import bayern.steinbrecher.jsch.JSchException;
import bayern.steinbrecher.jsch.Session;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public final class SshConnection extends DBConnection {

    private static final Logger LOGGER = Logger.getLogger(SshConnection.class.getName());
    private final String databaseHost;
    private final int databasePort;
    private final SshCredentials credentials;
    /**
     * The SSH session used to connect to the database over a secure channel.
     */
    private final Session sshSession;
    /**
     * The charset used by SSH response.
     */
    private final Charset remoteShellCharset;
    private final SupportedShell remoteShell;

    static {
        /* NOTE Config values must be separated with comma but WITHOUT space. Otherwise misleading exceptions like
         * UnknownHostException may occur
         */
        JSch.setConfig("kex", "diffie-hellman-group-exchange-sha1,diffie-hellman-group1-sha1,"
                + "diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha256,ecdh-sha2-nistp256,"
                + "ecdh-sha2-nistp384,ecdh-sha2-nistp521");
        JSch.setConfig("server_host_key", "ssh-dss,ssh-rsa,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,"
                + "ecdsa-sha2-nistp521");
        JSch.setConfig("StrictHostKeyChecking", "no");
        JSch.setConfig("lang.s2c", "");
        JSch.setConfig("lang.c2s", "");
        JSch.setConfig("cipher.s2c", "blowfish-cbc,3des-cbc,aes128-cbc,aes192-cbc,aes256-cbc,aes128-ctr,"
                + "aes192-ctr,aes256-ctr,3des-ctr,arcfour,arcfour128,arcfour256");
        JSch.setConfig("cipher.c2s", "blowfish-cbc,3des-cbc,aes128-cbc,aes192-cbc,aes256-cbc,aes128-ctr,"
                + "aes192-ctr,aes256-ctr,3des-ctr,arcfour,arcfour128,arcfour256");
        JSch.setConfig("mac.s2c", "hmac-md5,hmac-sha1,hmac-md5-96,hmac-sha1-96");
        JSch.setConfig("mac.c2s", "hmac-md5,hmac-sha1,hmac-md5-96,hmac-sha1-96");
    }

    /**
     * @since 0.1
     */
    public SshConnection(@NotNull SupportedDBMS dbms, @NotNull String databaseHost, int databasePort,
                         @NotNull String databaseName, @NotNull String sshHost, int sshPort,
                         @NotNull Charset sshCharset, @NotNull SshCredentials credentials)
            throws ConnectionFailedException, AuthException, UnknownHostException {
        super(databaseName, dbms);
        this.databaseHost = databaseHost;
        this.databasePort = databasePort;
        this.credentials = credentials;
        this.sshSession = createSshSession(
                Objects.requireNonNull(credentials), Objects.requireNonNull(sshHost), sshPort);
        try {
            // FIXME Close connection at any potential exception point
            this.sshSession.connect();
        } catch (JSchException ex) {
            if (ex.getMessage().contains("Auth")) { //NOPMD
                throw new AuthException("Authentication failed", ex);
            } else {
                //NOPMD - UnknownHostException does not accept a cause.
                throw new UnknownHostException(ex.getMessage());
            }
        }
        this.remoteShellCharset = Objects.requireNonNull(sshCharset);
        try {
            this.remoteShell = SupportedShell.determineRemoteShell(sshSession, remoteShellCharset);
        } catch (JSchException | CommandException | IOException | UnsupportedShellException ex) {
            throw new ConnectionFailedException("Failed to determine SSH remote shell", ex);
        }

        // FIXME Check DBMS command availability (see SupportedShell#isCommandAvailable(...))

        try {
            //Check sql-host connection
            execQuery("SELECT 1");
        } catch (QueryFailedException ex) {
            throw new ConnectionFailedException("Cannot execute SQL commands", ex);
        }
    }

    @NotNull
    private Session createSshSession(@NotNull SshCredentials credentials, @NotNull String sshHost, int sshPort)
            throws AuthException {
        try {
            Session session = new JSch().getSession(credentials.getSshUsername(), sshHost, sshPort);
            session.setPassword(credentials.getSshPassword());
            session.setDaemonThread(true);
            return session;
        } catch (JSchException ex) {
            throw new AuthException("SSH-Login failed.", ex);
        }
    }

    /**
     * @since 0.1
     */
    @Override
    @NotNull
    public List<List<String>> execQuery(@NotNull String sqlCode) throws QueryFailedException {
        LOGGER.log(Level.FINE, "Execute query: \"{0}\"", sqlCode);
        String result;
        try {
            result = remoteShell.execQuery(
                    getDbms(), credentials, databaseHost, databasePort, getDatabaseName(), sqlCode, sshSession,
                    remoteShellCharset);
        } catch (JSchException | CommandException | IOException | UnsupportedDBMSException ex) {
            throw new QueryFailedException(ex);
        }
        String[] rows = result.split("\n");
        LOGGER.log(Level.FINE, "Query result has {0} rows", rows.length);

        return Arrays.stream(rows)
                .map(row -> splitUp(row, '\t'))
                .map(rowFields -> rowFields.stream()
                        .map(f -> "0000-00-00".equals(f) ? null : f)
                        .map(f -> (f == null || "NULL".equalsIgnoreCase(f)) ? null : f)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * @since 0.1
     */
    @Override
    public void execUpdate(@NotNull String sqlCode) throws QueryFailedException {
        try {
            remoteShell.execQuery(
                    getDbms(), credentials, databaseHost, databasePort, getDatabaseName(), sqlCode, sshSession,
                    remoteShellCharset);
        } catch (JSchException | CommandException | IOException | UnsupportedDBMSException ex) {
            throw new QueryFailedException(ex);
        }
    }

    /**
     * Splits up a string on the given regex. The regex itself won´t show up in any element of the returned list. When
     * two or more regex are right in a row an empty {@link String} will be added. (This is the main difference to
     * {@link String#split(String)})
     */
    @NotNull
    private List<String> splitUp(@NotNull String row, char regex) {
        List<String> columns = new ArrayList<>();
        StringBuilder lastCol = new StringBuilder();
        for (char c : row.toCharArray()) {
            if (c == regex) {
                columns.add(lastCol.toString());
                lastCol.setLength(0);
            } else {
                lastCol.append(c);
            }
        }
        columns.add(lastCol.toString());

        return columns;
    }

    /**
     * @since 0.1
     */
    @Override
    public void close() {
        this.sshSession.disconnect();
    }
}
