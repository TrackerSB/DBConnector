package bayern.steinbrecher.dbConnector.query;

import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Stefan Huber
 * @since 0.15
 */
public abstract class SupportedDBMS {
    /* NOTE MariaDB is a drop-in replacement for MySQL but using a different connection protocol and therefore requires
     * a separate entry for supported DBMS.
     */
    public static final SupportedDBMS MARIADB = new SupportedDBMS(
            "MariaDB", 3306, "mysql",
            new QueryGenerator(
                    Paths.get("templates", "mysql"),
                    HashBiMap.create(Map.of(
                            Boolean.class, new SQLTypeKeyword("TINYINT", 1), // BOOLEAN is an alias for TINYINT(1)
                            Double.class, new SQLTypeKeyword("FLOAT"),
                            Integer.class, new SQLTypeKeyword("INT"), // INTEGER is an alias for INT
                            LocalDate.class, new SQLTypeKeyword("DATE"),
                            String.class, new SQLTypeKeyword("VARCHAR", 255)
                    )),
                    '`')
    ) {
    };
    public static final SupportedDBMS MY_SQL = new SupportedDBMS(
            "MySQL", 3306, "mysql",
            new QueryGenerator(
                    Paths.get("templates", "mysql"),
                    HashBiMap.create(Map.of(
                            Boolean.class, new SQLTypeKeyword("TINYINT", 1), // BOOLEAN is an alias for TINYINT(1)
                            Double.class, new SQLTypeKeyword("FLOAT"),
                            Integer.class, new SQLTypeKeyword("INT"), // INTEGER is an alias for INT
                            LocalDate.class, new SQLTypeKeyword("DATE"),
                            String.class, new SQLTypeKeyword("VARCHAR", 255)
                    )),
                    '`')
    ) {
    };
    public static final List<SupportedDBMS> DBMSs = List.of(MARIADB, MY_SQL);

    private final String displayName;
    private final int defaultPort;
    private final String shellCommand;
    private final QueryGenerator queryGenerator;

    private SupportedDBMS(@NotNull String displayName, int defaultPort, String shellCommand,
                          QueryGenerator queryGenerator) {
        this.displayName = Objects.requireNonNull(displayName);
        this.defaultPort = defaultPort;
        this.shellCommand = shellCommand;
        this.queryGenerator = queryGenerator;
    }

    @Override
    @NotNull
    public String toString() {
        return displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getShellCommand() {
        return shellCommand;
    }

    public QueryGenerator getQueryGenerator() {
        return queryGenerator;
    }
}
