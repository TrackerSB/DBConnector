package bayern.steinbrecher.dbConnector;

import bayern.steinbrecher.dbConnector.scheme.ColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.TableCreationKeywords;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public enum SupportedDatabases {
    /**
     * @since 0.1
     */
    MY_SQL("MySQL", 3306,
            HashBiMap.create(Map.of(
                    TableCreationKeywords.DEFAULT, "DEFAULT",
                    TableCreationKeywords.NOT_NULL, "NOT NULL",
                    TableCreationKeywords.PRIMARY_KEY, "PRIMARY KEY"
            )),
            HashBiMap.create(Map.of(
                    Boolean.class, new SQLTypeKeyword("TINYINT", 1), //BOOLEAN is an alias for TINYINT(1)
                    Double.class, new SQLTypeKeyword("FLOAT"),
                    Integer.class, new SQLTypeKeyword("INT"), //INTEGER is an alias for INT
                    LocalDate.class, new SQLTypeKeyword("DATE"),
                    String.class, new SQLTypeKeyword("VARCHAR", 255)
            )),
            '`');

    private final String displayName;
    private final int defaultPort;
    private final BiMap<TableCreationKeywords, String> keywordRepresentations;
    private final BiMap<Class<?>, SQLTypeKeyword> types;
    private final char identifierQuoteSymbol;

    /**
     * @param keywordRepresentations The mapping of the keywords to the database specific keywords. NOTE Only use
     *                               resolved alias otherwise the mapping from a SQL type keyword to a class may not
     *                               work since
     *                               {@code information_schema} stores only resolved aliases.
     * @param identifierQuoteSymbol  The symbol to use for quoting columns, tables,...
     */
    SupportedDatabases(@NotNull String displayName, int defaultPort,
                       @NotNull BiMap<TableCreationKeywords, String> keywordRepresentations,
                       @NotNull BiMap<Class<?>, SQLTypeKeyword> types, char identifierQuoteSymbol) {
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(keywordRepresentations);
        Objects.requireNonNull(types);

        this.displayName = displayName;
        this.defaultPort = defaultPort;
        this.keywordRepresentations = keywordRepresentations;
        this.types = types;
        this.identifierQuoteSymbol = identifierQuoteSymbol;

        String missingKeywords = keywordRepresentations.keySet()
                .stream()
                .filter(keyword -> !keywordRepresentations.containsKey(keyword))
                .map(TableCreationKeywords::toString)
                .collect(Collectors.joining(", "));
        if (!missingKeywords.isEmpty()) {
            Logger.getLogger(SupportedDatabases.class.getName())
                    .log(Level.WARNING, "The database {0} does not define following table creation keywords:\n",
                            new Object[]{displayName, missingKeywords});
        }
    }

    /**
     * Returns the appropriate SQL keyword for the given keyword representation.
     *
     * @param keyword The keyword to get a database specific keyword for.
     * @return The database specific keyword.
     * @since 0.1
     */
    @NotNull
    public Optional<String> getKeyword(@NotNull TableCreationKeywords keyword) {
        return Optional.ofNullable(keywordRepresentations.getOrDefault(keyword, null));
    }

    /**
     * Returns the keyword representing the given database specific keyword.
     *
     * @param sqlKeyword The SQL keyword to get a {@link TableCreationKeywords} from.
     * @return The representing keyword. {@link Optional#empty()} only if this database does not associate a keyword for
     * the given SQL keyword.
     * @since 0.1
     */
    @NotNull
    public Optional<TableCreationKeywords> getKeyword(@NotNull String sqlKeyword) {
        return Optional.ofNullable(keywordRepresentations.inverse().get(sqlKeyword));
    }

    /**
     * Returns the appropriate SQL type keyword for the given column.
     *
     * @param <T>    The type of the values hold by {@code column}.
     * @param column The column to get the type for.
     * @return The SQL type representing the type of {@code column}.
     * @since 0.1
     */
    @NotNull
    public <T> Optional<String> getType(@NotNull ColumnPattern<T, ?> column) {
        Class<T> type = column.getParser().getType();
        return Optional.ofNullable(types.getOrDefault(type, null))
                .map(SQLTypeKeyword::getSqlTypeKeyword);
    }

    /**
     * Returns the class used for representing values of the given SQL type.
     *
     * @param sqlType The type to get a class for.
     * @return An {@link Optional} containing the {@link Class} representing the appropriate SQL type. Returns
     * {@link Optional#empty()} if and only if for {@code sqlType} no class is defined.
     * @since 0.1
     */
    @NotNull
    public Optional<Class<?>> getType(@NotNull String sqlType) {
        return Optional.ofNullable(types.inverse().get(new SQLTypeKeyword(sqlType)));
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
     * @since 0.1
     */
    public char getIdentifierQuoteSymbol() {
        return identifierQuoteSymbol;
    }

    /**
     * Represents a wrapper for a {@link String} which ignores at every point the case of characters of the wrapped
     * keyword. This includes {@link Object#equals(Object)}, {@link Comparable#compareTo(Object)},
     * etc. NOTE: Specified parameters are ignored.
     */
    private static class SQLTypeKeyword implements Comparable<SQLTypeKeyword> {

        private final String keyword; //NOPMD - It is access over getSqlTypeKeyword()
        private final String parameterSuffix; //NOPMD - It is access over getSqlTypeKeyword()

        /**
         * Creates a new {@link SQLTypeKeyword}.
         *
         * @param keyword   The keyword is always saved and handled in uppercase. This keyword must represent the type
         *                  saved in {@code information_schema.columns}. Be careful with aliases.
         * @param parameter Additional parameters related to the keyword. These are ignored concerning
         *                  {@link Object#equals(Object)}, {@link Comparable#compareTo(Object)},
         *                  etc.
         */
        SQLTypeKeyword(@NotNull String keyword, @NotNull Object... parameter) {
            this.keyword = keyword.toUpperCase(Locale.ROOT);
            this.parameterSuffix = parameter.length > 0
                    ? Arrays.stream(parameter)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "(", ")"))
                    : "";
        }

        /**
         * @since 0.1
         */
        @Override
        public boolean equals(@Nullable Object other) {
            boolean areEqual;
            if (other instanceof SQLTypeKeyword) {
                areEqual = keyword.equalsIgnoreCase(((SQLTypeKeyword) other).keyword);
            } else {
                areEqual = false;
            }
            return areEqual;
        }

        /**
         * @since 0.1
         */
        @Override
        public int hashCode() {
            return keyword.hashCode();
        }

        /**
         * @since 0.1
         */
        @Override
        public int compareTo(@NotNull SQLTypeKeyword other) {
            return keyword.compareToIgnoreCase(other.keyword);
        }

        /**
         * Returns the SQL type keyword in upper case and appends a comma separated list of parameters in parenthesis.
         *
         * @return The SQL type keyword in upper case and appends a comma separated list of parameters in parenthesis.
         * @since 0.1
         */
        @NotNull
        public String getSqlTypeKeyword() {
            return keyword + parameterSuffix;
        }
    }
}
