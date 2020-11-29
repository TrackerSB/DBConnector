package bayern.steinbrecher.dbConnector;

import bayern.steinbrecher.dbConnector.credentials.SshCredentials;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.query.SupportedDatabases;
import bayern.steinbrecher.javaUtility.IOUtility;
import bayern.steinbrecher.jsch.ChannelExec;
import bayern.steinbrecher.jsch.JSch;
import bayern.steinbrecher.jsch.JSchException;
import bayern.steinbrecher.jsch.Session;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public final class SshConnection extends DBConnection {

    private static final Logger LOGGER = Logger.getLogger(SshConnection.class.getName());
    private static final Map<SupportedDatabases, String> COMMANDS = Map.of(SupportedDatabases.MY_SQL, "mysql");
    private final Map<SupportedDatabases, Function<String, String>> sqlCommands = new HashMap<>();
    /**
     * The SSH session used to connect to the database over a secure channel.
     */
    private final Session sshSession;
    /**
     * The charset used by SSH response.
     */
    private final Charset charset;
    private final SupportedDatabases dbms;

    static {
        //Configurations which are applied to all sessions.
        //NOTE Config values must be separated with comma but WITHOUT space
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
    public SshConnection(@NotNull SupportedDatabases dbms, @NotNull String databaseHost, int databasePort,
                         @NotNull String databaseName, @NotNull String sshHost, int sshPort,
                         @NotNull Charset sshCharset, @NotNull SshCredentials credentials)
            throws AuthException, UnknownHostException, UnsupportedDatabaseException {
        super(databaseName, dbms);
        this.dbms = dbms;
        this.sshSession = createSshSession(
                Objects.requireNonNull(credentials), Objects.requireNonNull(sshHost), sshPort);
        this.charset = Objects.requireNonNull(sshCharset);

        //NOTE The echo command is needed for handling UTF8 chars on non UTF8 terminals.
        sqlCommands.put(dbms, query -> "echo -e '" + escapeSingleQuotes(replaceNonAscii(query))
                + "' | "
                + COMMANDS.get(dbms)
                + " --default-character-set=utf8"
                + " -u" + credentials.getDbUsername()
                + " -p" + credentials.getDbPassword()
                + " -h" + Objects.requireNonNull(databaseHost)
                + " -P" + databasePort
                + " " + databaseName);

        try {
            this.sshSession.connect();

            String result;
            try {
                result = execCommand(
                        "command -v " + COMMANDS.get(dbms) + " >/dev/null 2>&1 || { echo \"Not installed\"; }");
            } catch (CommandException | IOException ex) {
                throw new UnsupportedDatabaseException(
                        "The command to check existence of the correct database failed.", ex);
            }
            if (result.contains("Not installed")) {
                throw new UnsupportedDatabaseException("The configured database is not supported by the SSH host.");
            }

            //Check sql-host connection
            execQuery("SELECT 1");
        } catch (QueryFailedException | JSchException ex) {
            close();
            /*
             * A simple instanceof check is not sufficient => It can not be replaced by an additional catch clause.
             */
            if (ex instanceof JSchException && !ex.getMessage().contains("Auth")) { //NOPMD
                throw new UnknownHostException(ex.getMessage()); //NOPMD - UnknownHostException does not accept a cause.
            } else {
                throw new AuthException("Auth fail", ex);
            }
        }
    }

    /**
     * Escapes every single quote in such way that the resulting {@link String} can be inserted between single quotes.
     *
     * @param nonEscaped The {@link String} whose single quotes to escape.
     * @return The {@link String} who can be quoted in single quotes itself.
     */
    @NotNull
    private static String escapeSingleQuotes(@NotNull String nonEscaped) {
        return nonEscaped.replace("'", "'\"'\"'");
    }

    @NotNull
    private String replaceNonAscii(@NotNull String nonAscii) {
        StringBuilder ascii = new StringBuilder();
        nonAscii.chars()
                .forEach(codePoint -> {
                    Character character = (char) codePoint;
                    //CHECKSTYLE.OFF: MagicNumber - 32-126 is the printable ascii range
                    if (codePoint > 31 && codePoint < 127) {
                        //CHECKSTYLE.ON: MagicNumber
                        ascii.append(character);
                    } else {
                        byte[] bytes = String.valueOf(character).getBytes(StandardCharsets.UTF_8);
                        for (byte utf8byte : bytes) {
                            //CHECKSTYLE.OFF: MagicNumber - May add 256 to make sure the byte has a positive value.
                            int toConvert = utf8byte < 0 ? utf8byte + 256 : utf8byte;
                            //CHECKSTYLE.ON: MagicNumber
                            String asciiRepresentation = Integer.toHexString(toConvert);
                            String zeroLeftPad = (asciiRepresentation.length() < 2) ? "0" : "";
                            ascii.append("\\x")
                                    .append(zeroLeftPad)
                                    .append(asciiRepresentation);
                        }
                    }
                });
        return ascii.toString();
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

    @NotNull
    private String execCommand(@NotNull String command) throws JSchException, CommandException, IOException {
        String result;
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setInputStream(null);
        channel.setCommand(command);
        InputStream inStream = channel.getInputStream();
        InputStream errStream = channel.getErrStream();

        channel.connect();

        String errorStreamContent = IOUtility.readAll(errStream, charset).trim();
        if (!errorStreamContent.isBlank()) {
            String errorMessage
                    = String.format("The command '%s' returned the following error:\n%s", command, errorStreamContent);
            if (errorStreamContent.toLowerCase(Locale.ROOT).contains("error")) {
                channel.disconnect();
                throw new CommandException(errorMessage);
            } else {
                LOGGER.log(Level.WARNING, errorMessage);
            }
        }
        result = IOUtility.readAll(inStream, charset);

        channel.disconnect();
        return result;
    }

    private String generateQueryCommand(String sqlCode) {
        return sqlCommands.get(dbms).apply(sqlCode);
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
            result = execCommand(generateQueryCommand(Objects.requireNonNull(sqlCode)));
        } catch (JSchException | CommandException | IOException ex) {
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
            execCommand(generateQueryCommand(Objects.requireNonNull(sqlCode)));
        } catch (JSchException | CommandException | IOException ex) {
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
