package bayern.steinbrecher.dbConnector.query;

import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public enum SupportedDatabases {
    /**
     * @since 0.1
     */
    MY_SQL("MySQL", 3306,
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
    );

    private final String displayName;
    private final int defaultPort;
    private final QueryGenerator queryGenerator;

    SupportedDatabases(@NotNull String displayName, int defaultPort, QueryGenerator queryGenerator) {
        this.displayName = Objects.requireNonNull(displayName);
        this.defaultPort = defaultPort;
        this.queryGenerator = queryGenerator;
    }

    /**
     * @since 0.1
     */
    @Override
    @NotNull
    public String toString() {
        return displayName;
    }

    /**
     * @since 0.1
     */
    public int getDefaultPort() {
        return defaultPort;
    }

    /**
     * @since 0.5
     */
    public QueryGenerator getQueryGenerator() {
        return queryGenerator;
    }
}
