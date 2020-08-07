package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.SupportedDatabases;
import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.scheme.SimpleColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.TableCreationKeywords;
import bayern.steinbrecher.dbConnector.scheme.TableScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// FIXME Instead of returning strings return statement which associates a result type
// FIXME Should each method also return the required rights for it?
/*
 * @since 0.1
 */
public final class QueryGenerator {
    private static final Logger LOGGER = Logger.getLogger(QueryGenerator.class.getName());

    private QueryGenerator() {
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
    public static String quoteIdentifier(@NotNull SupportedDatabases dbms, @NotNull String identifier) {
        return Arrays.stream(identifier.split("\\."))
                .map(i -> dbms.getIdentifierQuoteSymbol()
                        + i.replaceAll(
                        String.valueOf(dbms.getIdentifierQuoteSymbol()),
                        "\\" + dbms.getIdentifierQuoteSymbol())
                        + dbms.getIdentifierQuoteSymbol()
                )
                .collect(Collectors.joining("."));
    }

    /**
     * Returns a line which can be used in a CREATE statement appropriate for this type of database.
     *
     * @param column The column for which a line should be created which can be used in CREATE statements.
     * @return A list of the appropriate SQL keywords for the given ones.
     * @since 0.1
     */
    @NotNull
    private static String generateCreateLine(
            @NotNull SupportedDatabases dbms, @NotNull SimpleColumnPattern<?, ?> column) {
        String type = dbms.getType(column).orElseThrow(() -> new NoSuchElementException(
                String.format("%s does not support the type of %s", dbms, column)));
        return Stream.concat(
                Stream.of(column.getRealColumnName(), type),
                column.getKeywords()
                        .stream()
                        .map(keyword -> {
                            Optional<String> dbmsKeyword = dbms.getKeyword(keyword)
                                    .map(k -> {
                                        String outCreateElement;
                                        if (keyword == TableCreationKeywords.DEFAULT) {
                                            outCreateElement = k + " " + column.getDefaultValueSql();
                                        } else {
                                            outCreateElement = k;
                                        }
                                        return outCreateElement;
                                    });
                            if (dbmsKeyword.isEmpty()) {
                                Logger.getLogger(SupportedDatabases.class.getName())
                                        .log(Level.WARNING, "Keyword {0} is not defined by {1}",
                                                new Object[]{keyword, dbms});
                            }
                            return dbmsKeyword;
                        })
                        .filter(Optional::isPresent)
                        .map(Optional::get))
                .collect(Collectors.joining(" "));
    }

    /*
     * @since 0.1
     */
    @NotNull
    public static String generateCreateTableStatement(
            @NotNull SupportedDatabases dbms, @NotNull String databaseName, @NotNull TableScheme<?, ?> scheme) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /*
     * @since 0.1
     */
    @NotNull
    public static String generateRequestForColumnNamesAndTypes(
            @NotNull SupportedDatabases dbms, @NotNull String databaseName, @NotNull TableScheme<?, ?> scheme) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /*
     * @since 0.1
     */
    @NotNull
    public static String generateRequestForTableNames(@NotNull SupportedDatabases dbms, @NotNull String databaseName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /*
     * @since 0.1
     */
    @NotNull
    public static String generateRequestForExistenceOfDatabase(
            @NotNull SupportedDatabases dbms,@NotNull String databaseName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns a {@link String} containing a statement with SELECT, FROM and WHERE, but with no semicolon and no
     * trailing space at the end. The select statement contains only columns existing and associated with {@code table}.
     * The where clause excludes conditions references a column contained in {@code columnsToSelect} which do not exist.
     *
     * @param tableScheme     The table to select from.
     * @param columnsToSelect The columns to select if they exist. If it is empty all available columns are queried.
     * @param conditions      The conditions every row has to satisfy. NOTE Occurrences of identifiers with conditions are
     *                        not automatically quoted by this method.
     * @return The statement selecting all columns given in {@code columnsToSelect} satisfying all {@code conditions}.
     * Returns {@link Optional#empty()} if {@code columnsToSelect} contains no column.
     * @since 0.1
     */
    @NotNull
    // FIXME The method does not tell which conditions were excluded.
    public static Optional<String> generateSearchQueryFromColumns(
            @NotNull SupportedDatabases dbms, @NotNull String databaseName,
            @NotNull TableScheme<?, ?> tableScheme, @NotNull Collection<DBConnection.Column<?>> columnsToSelect,
            @Nullable Collection<String> conditions) {
        Optional<String> searchQuery;
        if (columnsToSelect.isEmpty()) {
            LOGGER.log(Level.WARNING, "Generating search query without selecting any existing column.");
            searchQuery = Optional.empty();
        } else {
            StringBuilder sqlString = new StringBuilder("SELECT ")
                    .append(
                            columnsToSelect.stream()
                                    .map(DBConnection.Column::getName)
                                    .map(name -> quoteIdentifier(dbms, name))
                                    .collect(Collectors.joining(", "))
                    )
                    .append(" FROM ")
                    .append(quoteIdentifier(dbms, tableScheme.getTableName()));
            if (conditions != null && !conditions.isEmpty()) {
                String conditionString = conditions.stream()
                        // FIXME This method does not check whether all the column identifiers in any conditions exist.
                        .collect(Collectors.joining(" AND "));
                sqlString.append(" WHERE ")
                        .append(conditionString);
            }
            searchQuery = Optional.of(sqlString.toString());
        }
        return searchQuery;
    }
}
