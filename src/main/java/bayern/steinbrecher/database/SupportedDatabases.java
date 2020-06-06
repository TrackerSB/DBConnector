package bayern.steinbrecher.database;

import bayern.steinbrecher.database.scheme.ColumnPattern;
import bayern.steinbrecher.database.scheme.SimpleColumnPattern;
import bayern.steinbrecher.database.scheme.TableCreationKeywords;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Returns a line which can be used in a CREATE statement appropriate for this type of database.
     *
     * @param column The column for which a line should be created which can be used in CREATE statements.
     * @return A list of the appropriate SQL keywords for the given ones.
     * @since 0.1
     */
    @NotNull
    public String generateCreateLine(@NotNull SimpleColumnPattern<?, ?> column) {
        String realColumnName = column.getRealColumnName();
        return Stream.concat(
                Stream.of(realColumnName, getType(column)),
                column.getKeywords().stream()
                        .map(keyword -> {
                            if (this.keywordRepresentations.containsKey(keyword)) {
                                StringBuilder keywordString = new StringBuilder(this.keywordRepresentations
                                        .get(keyword));
                                if (keyword == TableCreationKeywords.DEFAULT) {
                                    keywordString.append(' ')
                                            .append(column.getDefaultValueSql());
                                }
                                return keywordString;
                            } else {
                                Logger.getLogger(SupportedDatabases.class.getName())
                                        .log(Level.WARNING, "Keyword {0} is not defined by {1}",
                                                new Object[]{keyword, this});
                                return null;
                            }
                        })
                        .filter(Objects::nonNull))
                .collect(Collectors.joining(" "));
    }

    /**
     * Returns the appropriate SQL keyword for the given keyword representation.
     *
     * @param keyword The keyword to get a database specific keyword for.
     * @return The database specific keyword.
     * @since 0.1
     */
    @NotNull
    public String getKeyword(@NotNull TableCreationKeywords keyword) {
        if (keywordRepresentations.containsKey(keyword)) {
            return keywordRepresentations.get(keyword);
        } else {
            throw new NoSuchElementException(
                    "For the database " + displayName + " no SQL keyword for keyword " + keyword + " is defined.");
        }
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
        Optional<TableCreationKeywords> keyword = Optional.ofNullable(keywordRepresentations.inverse().get(sqlKeyword));
        if (keyword.isEmpty()) {
            Logger.getLogger(SupportedDatabases.class.getName())
                    .log(Level.WARNING, "The database {0} does not define a keyword for {1}.",
                            new Object[]{displayName, sqlKeyword});
        }
        return keyword;
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
    public <T> String getType(@NotNull ColumnPattern<T, ?> column) {
        Class<T> type = column.getParser().getType();
        if (types.containsKey(type)) {
            return types.get(type).getSqlTypeKeyword();
        } else {
            throw new NoSuchElementException(
                    "For the database " + displayName + " no SQL type for type " + type + " is defined.");
        }
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
        Optional<Class<?>> type = Optional.ofNullable(types.inverse().get(new SQLTypeKeyword(sqlType)));
        if (type.isEmpty()) {
            Logger.getLogger(SupportedDatabases.class.getName())
                    .log(Level.WARNING, "The database {0} does not define a class for SQL type {1}.",
                            new Object[]{displayName, sqlType});
        }
        return type;
    }

    /**
     * Returns the given identifier quoted with the database specific quote symbol. It also escapes occurrences of the
     * quote symbol within the identifier. NOTE: It is not checked whether the column, table,... described by the
     * identifier exists somewhere.
     *
     * @param identifier The identifier to quote. If an identifier {@code first_part.second_part} contains a dot it is
     *                   quoted like (e.g. quoted with double quotes) {@code "first_part"."second_part"}.
     * @return The quoted identifier.
     * @since 0.1
     */
    @NotNull
    public String quoteIdentifier(@NotNull String identifier) {
        return Arrays.stream(identifier.split("\\."))
                .map(
                        i -> identifierQuoteSymbol
                                + i.replaceAll(String.valueOf(identifierQuoteSymbol), "\\" + identifierQuoteSymbol)
                                + identifierQuoteSymbol
                )
                .collect(Collectors.joining("."));
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

        @Override
        public int hashCode() {
            return keyword.hashCode();
        }

        @Override
        public int compareTo(SQLTypeKeyword other) {
            return keyword.compareToIgnoreCase(other.keyword);
        }

        /**
         * Returns the SQL type keyword in upper case and appends a comma separated list of parameters in braces.
         *
         * @return The SQL type keyword in upper case and appends a comma separated list of parameters in braces.
         */
        @NotNull
        public String getSqlTypeKeyword() {
            return keyword + parameterSuffix;
        }
    }
}
