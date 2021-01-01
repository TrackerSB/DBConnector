package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.CommandException;
import bayern.steinbrecher.dbConnector.UnsupportedShellException;
import bayern.steinbrecher.dbConnector.credentials.DBCredentials;
import bayern.steinbrecher.javaUtility.IOUtility;
import bayern.steinbrecher.jsch.ChannelExec;
import bayern.steinbrecher.jsch.JSchException;
import bayern.steinbrecher.jsch.Session;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stefan Huber
 * @since 0.15
 */
public abstract class SupportedShell {
    private static final Logger LOGGER = Logger.getLogger(SupportedShell.class.getName());
    private static final SupportedShell POSIX_STANDARD_COMPLIANT_SHELL = new SupportedShell("bash") {
        @Override
        public boolean isCommandAvailable(
                @NotNull String command, @NotNull Session sshSession, @NotNull Charset remoteCharset) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String generateEchoCommand(String ascii) {
            throw new UnsupportedOperationException();
        }
    };
    public static final SupportedShell BASH = new SupportedShell("bash") {
        @Override
        public boolean isCommandAvailable(
                @NotNull String command, @NotNull Session sshSession, @NotNull Charset remoteCharset)
                throws JSchException, CommandException, IOException {
            // Command based on https://stackoverflow.com/a/677212/4863098
            String existenceCheckCommand = String.format(
                    "command -v %s >/dev/null 2>&1 || { echo \"Not installed\"; }", command);
            return !execCommand(existenceCheckCommand, sshSession, remoteCharset).equals("Not installed");
        }

        @Override
        protected String generateEchoCommand(String ascii) {
            return String.format("echo -e '%s'", escapeSingleQuotes(replaceNonAscii(ascii)));
        }
    };
    public static final SupportedShell TCSH = new SupportedShell("tcsh") {
        @Override
        public boolean isCommandAvailable(@NotNull String command, @NotNull Session sshSession,
                                          @NotNull Charset remoteCharset)
                throws JSchException, CommandException, IOException {
            // Command based on https://stackoverflow.com/a/22058620/4863098
            String existenceCheckCommand = String.format(
                    "(where %s == \"\" >/dev/null) || echo \"Not installed\"", command);
            return !execCommand(existenceCheckCommand, sshSession, remoteCharset).equals("Not installed");
        }

        @Override
        protected String generateEchoCommand(String ascii) {
            // FIXME Check availability of dspm option (see https://www.computerhope.com/unix/tcsh.htm)
            return String.format("set dspmbyte=\"utf8\" && printf '%s'", escapeSingleQuotes(replaceNonAscii(ascii)));
        }
    };
    public static final List<SupportedShell> SHELLS = List.of(BASH, TCSH);
    private final String shellCommand;

    private SupportedShell(String shellCommand) {
        this.shellCommand = shellCommand;
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
    private static String replaceNonAscii(@NotNull String nonAscii) {
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

    public abstract boolean isCommandAvailable(
            @NotNull String command, @NotNull Session sshSession, @NotNull Charset remoteCharset)
            throws JSchException, CommandException, IOException;

    /**
     * Generate a command which is capable of printing UTF-8 characters which are escaped in an ASCII {@link String}
     * like {@code "\x00\xfc" => ü}. Using such a command allows to pass UTF-8 characters as input for another command
     * while being on a terminal which does not support UTF-8 characters.
     *
     * @return A command which prints escaped UTF-8 characters in an ASCII encoded {@link String}.
     */
    protected abstract String generateEchoCommand(String ascii);

    @NotNull
    public String execQuery(SupportedDBMS dbms, DBCredentials credentials, String databaseHost, int databasePort,
                            String databaseName, String query, Session sshSession, Charset remoteCharset)
            throws UnsupportedDBMSException, JSchException, CommandException, IOException {
        String queryShellCommand;
        if (dbms == SupportedDBMS.MY_SQL) {
            queryShellCommand = String.format("%s --default-character-set=utf8 -u%s -p%s -h%s -P%d %s",
                    dbms.getShellCommand(), credentials.getDbUsername(), credentials.getDbPassword(),
                    Objects.requireNonNull(databaseHost), databasePort, databaseName);
        } else {
            throw new UnsupportedDBMSException(
                    String.format("Command '%s' does not support command '%s'",
                            this.shellCommand, dbms.getShellCommand()));
        }

        return execCommand(String.format("%s | %s", generateEchoCommand(query), queryShellCommand),
                sshSession, remoteCharset);
    }

    // NOTE Is there any way of automatically detecting the used charset?
    @NotNull
    public String execCommand(@NotNull String command, @NotNull Session sshSession, @NotNull Charset remoteCharset)
            throws JSchException, CommandException, IOException {
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setInputStream(null);
        channel.setCommand(command);

        channel.connect();

        Pair<String, String> streamsContent = IOUtility.readChannelContinuously(channel, remoteCharset);
        String errorStreamContent = streamsContent.getValue();

        if (!errorStreamContent.isBlank()) {
            String errorMessage
                    = String.format("The command '%s' returned the following error:\n%s", command, errorStreamContent);
            if (errorStreamContent.toLowerCase(Locale.ROOT).contains("error")) {
                channel.disconnect();
                throw new CommandException(errorMessage);
            } else {
                LOGGER.log(Level.WARNING,
                        String.format("The error output of command '%s' is not empty:\n%s", command, errorMessage));
            }
        }

        channel.disconnect();
        return streamsContent.getKey();
    }

    @NotNull
    public static SupportedShell determineRemoteShell(
            @NotNull Session sshSession, @NotNull Charset remoteCharset)
            throws JSchException, CommandException, IOException, UnsupportedShellException {
        String detectedShellCommand = POSIX_STANDARD_COMPLIANT_SHELL.execCommand("echo $0", sshSession, remoteCharset)
                .trim();
        return SHELLS.stream()
                /* NOTE The "-" in front of the command name symbolizes a login shell
                 * (see https://stackoverflow.com/a/8451514/4863098)
                 */
                .filter(shell -> detectedShellCommand.matches("^-?" + shell.shellCommand + "$"))
                .findAny()
                .orElseThrow(() -> new UnsupportedShellException(
                        String.format("The shell '%s' is unsupported", detectedShellCommand)));
    }
}
