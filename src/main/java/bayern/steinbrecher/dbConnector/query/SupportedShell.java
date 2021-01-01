package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.CommandException;
import bayern.steinbrecher.dbConnector.UnsupportedShellException;
import bayern.steinbrecher.javaUtility.IOUtility;
import bayern.steinbrecher.jsch.ChannelExec;
import bayern.steinbrecher.jsch.JSchException;
import bayern.steinbrecher.jsch.Session;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
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
    };
    public static final List<SupportedShell> SHELLS = List.of(BASH, TCSH);
    private final String shellCommand;

    private SupportedShell(String shellCommand) {
        this.shellCommand = shellCommand;
    }

    public abstract boolean isCommandAvailable(
            @NotNull String command, @NotNull Session sshSession, @NotNull Charset remoteCharset)
            throws JSchException, CommandException, IOException;

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
                .filter(shell -> detectedShellCommand.matches("^-?bash" + shell.shellCommand + "$"))
                .findAny()
                .orElseThrow(() -> new UnsupportedShellException(
                        String.format("The shell '%s' is unsupported", detectedShellCommand)));
    }
}
