package bayern.steinbrecher.dbConnector;

import bayern.steinbrecher.dbConnector.query.GenerationFailedException;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.query.QueryGenerator;
import bayern.steinbrecher.dbConnector.query.SupportedDBMS;
import bayern.steinbrecher.dbConnector.scheme.ColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.SimpleColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.TableScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Stefan Huber
 * @since 0.1
 */
public abstract class DBConnection implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private final String databaseName;
    private final SupportedDBMS dbms;

    /**
     * @since 0.1
     */
    public DBConnection(@NotNull String databaseName, @NotNull SupportedDBMS dbms) {
        this.databaseName = Objects.requireNonNull(databaseName);
        this.dbms = Objects.requireNonNull(dbms);
    }

    /**
     * @since 0.1
     */
    @Override
    public abstract void close();

    /**
     * Executes a query and returns the result.
     *
     * @param sqlCode The sql code to execute.
     * @return Table containing the results AND the headings of each column. First dimension rows; second columns.
     * @throws QueryFailedException Thrown if the sql code is invalid.
     * @since 0.1
     */
    @NotNull
    public abstract List<List<String>> execQuery(@NotNull String sqlCode) throws QueryFailedException;

    /**
     * Executes a command like INSERT INTO, UPDATE or CREATE.
     *
     * @param sqlCode The sql code to execute.
     * @throws QueryFailedException Thrown if the sql code is invalid.
     * @since 0.1
     */
    public abstract void execUpdate(@NotNull String sqlCode) throws QueryFailedException;

    /**
     * Checks if the connected database exists.
     *
     * @return {@code true} only if the connected database exists.
     * @since 0.1
     */
    public boolean databaseExists() throws QueryFailedException {
        try {
            QueryGenerator queryGenerator = getDbms()
                    .getQueryGenerator();
            List<List<String>> result
                    = execQuery(queryGenerator.generateCheckDatabaseExistenceStatement(getDatabaseName()));
            return result.size() > 1
                    && !result.get(1).isEmpty()
                    && Integer.parseInt(result.get(1).get(0)) > 0;
        } catch (GenerationFailedException | NumberFormatException ex) {
            throw new QueryFailedException(String
                    .format("Could not check existence of database '%s'", getDatabaseName()), ex);
        }
    }

    /**
     * @since 0.1
     */
    public void createTableIfNotExists(@NotNull TableScheme<?, ?> scheme) throws QueryFailedException {
        if (!tableExists(scheme)) {
            //FIXME Check rights for creating tables
            try {
                execUpdate(getDbms().getQueryGenerator().generateCreateTableStatement(getDatabaseName(), scheme));
            } catch (GenerationFailedException ex) {
                throw new QueryFailedException(String.format("Could not create table '%s'", scheme.getTableName()), ex);
            }
        }
    }

    /**
     * Returns an object representing all current entries of the given table.
     *
     * @param <T>         The type that represents the whole content of the given table.
     * @param tableScheme The table to query all its data from.
     * @return The table to request all data from.
     * @since 0.1
     */
    public <T, E> T getTableContent(@NotNull TableScheme<T, E> tableScheme)
            throws GenerationFailedException, QueryFailedException {
        Set<SimpleColumnPattern<?, E>> missingColumns = getMissingColumns(tableScheme);
        if (missingColumns.isEmpty()) {
            T tableContent;
            String searchQuery = getDbms()
                    .getQueryGenerator()
                    .generateSearchQueryStatement(getDatabaseName(), getTable(tableScheme).orElseThrow(),
                            Collections.EMPTY_LIST, Collections.emptyList());
            try {
                tableContent = tableScheme.parseFrom(execQuery(searchQuery));
            } catch (QueryFailedException ex) {
                throw new GenerationFailedException(
                        String.format("Could not parse query results to a representation for '%s'",
                                tableScheme.getTableName()),
                        ex);
            }
            return tableContent;
        } else {
            throw new IllegalStateException("The table scheme misses columns: " + missingColumns);
        }
    }

    /**
     * @since 0.5
     */
    @NotNull
    public <T, E> Set<SimpleColumnPattern<?, E>> getMissingColumns(@NotNull TableScheme<T, E> scheme)
            throws QueryFailedException {
        Set<Column<E, ?>> cachedColumns = getAllColumns(scheme);
        return scheme.getRequiredColumns()
                .stream()
                .filter(scp -> cachedColumns.stream().noneMatch(column -> scp.matches(column.name())))
                .collect(Collectors.toSet());
    }

    /**
     * @since 0.5
     */
    @NotNull
    public <T, E> Set<Column<E, ?>> getAllColumns(@NotNull TableScheme<T, E> tableScheme) throws QueryFailedException {
        Optional<Table<T, E>> table;
        try {
            table = getTable(tableScheme);
        } catch (QueryFailedException ex) {
            throw new QueryFailedException("Could not request existing columns", ex);
        }
        if (table.isPresent()) {
            return table.get()
                    .getColumns();
        } else {
            throw new QueryFailedException(
                    String.format(
                            "Could not return existing columns since there is no table corresponding to the given "
                                    + "scheme for '%s'", tableScheme.getTableName()));
        }
    }

    /**
     * @since 0.6
     */
    @NotNull
    public <T, E> Optional<Table<T, E>> getTable(@NotNull TableScheme<T, E> scheme) throws QueryFailedException {
        QueryGenerator queryGenerator = getDbms()
                .getQueryGenerator();
        String query;
        try {
            query = queryGenerator.generateQueryTableNamesStatement(getDatabaseName());
        } catch (GenerationFailedException ex) {
            throw new QueryFailedException("Could not check existence of table", ex);
        }
        boolean tableExists = execQuery(query)
                .stream()
                .skip(1) // Skip headings
                .anyMatch(row -> row.get(0).equalsIgnoreCase(scheme.getTableName()));
        return Optional.ofNullable(tableExists ? new Table<>(scheme) : null);
    }

    /**
     * @since 0.1
     */
    public boolean tableExists(@NotNull TableScheme<?, ?> tableScheme) throws QueryFailedException {
        return getTable(tableScheme)
                .isPresent();
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @since 0.3
     */
    @NotNull
    public SupportedDBMS getDbms() {
        return dbms;
    }

    /**
     * Represents a table which exists and is accessible by this connection. In contrast {@link TableScheme} only
     * represents a scheme of a table which might have been created.
     *
     * @author Stefan Huber
     * @since 0.5
     */
    public class Table<T, E> {
        private final TableScheme<T, E> scheme;
        /**
         * NOTE Should be a static local variable in {@link #getColumns()}
         */
        private final Set<Column<E, ?>> cachedColumns = new HashSet<>();

        // NOTE Only DBConnection should be allowed to create Table objects
        private Table(@NotNull TableScheme<T, E> scheme) {
            this.scheme = Objects.requireNonNull(scheme);
        }

        @NotNull
        public TableScheme<T, E> getTableScheme() {
            return scheme;
        }

        @NotNull
        private <C> Optional<ColumnPattern<C, E>> findColumnPattern(
                @NotNull Class<C> columnType, @NotNull String columnName) {
            return Stream.concat(
                            getTableScheme().getRequiredColumns().stream(),
                            getTableScheme().getOptionalColumns().stream())
                    .filter(cp -> cp.matches(columnName))
                    .map(cp -> (ColumnPattern<C, E>) cp)
                    .findAny();
        }

        @NotNull
        @Unmodifiable
        public <C> Set<Column<E, ?>> getColumns() throws QueryFailedException {
            if (cachedColumns.isEmpty()) {
                try {
                    QueryGenerator queryGenerator = getDbms()
                            .getQueryGenerator();
                    String query = queryGenerator.generateQueryColumnNamesAndTypesStatement(
                            getDatabaseName(), this);
                    List<List<String>> result = execQuery(query);
                    result.remove(0); // Skip headings
                    for (List<String> row : result) {
                        String columnName = row.get(0);
                        String columnTypeName = row.get(1);
                        Optional<Class<C>> columnType = queryGenerator.getType(columnTypeName);
                        if (columnType.isPresent()) {
                            int ordinalPosition = Integer.parseInt(row.get(3));
                            boolean nullable = row.get(2).equalsIgnoreCase("YES");
                            // FIXME Associate column patterns where available
                            cachedColumns.add(new Column<E, C>(columnName, columnType.get(), ordinalPosition, nullable,
                                    findColumnPattern(columnType.get(), columnName)));
                        } else {
                            LOGGER.log(Level.INFO, String.format(
                                    "Skip column '%s' of table '%s' since it has an unsupported SQL type ('%s')",
                                    columnName, getTableScheme().getTableName(), columnTypeName));
                        }
                    }
                } catch (GenerationFailedException ex) {
                    throw new QueryFailedException(
                            String.format("Could not request existing columns of table '%s'",
                                    getTableScheme().getTableName()), ex);
                }
            }
            return Collections.unmodifiableSet(cachedColumns);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Table<?, ?> table = (Table<?, ?>) o;
            return getTableScheme().equals(table.getTableScheme());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTableScheme());
        }
    }

    /**
     * Represents a column that exists in a table which is accessible by this connection. In contrast
     * {@link ColumnPattern} only represents patterns of column names in a {@link TableScheme} of a table which might
     * have been created.
     *
     * @param <E> The type of Java objects the containing table entry represents.
     * @param <C> The type of Java objects this column represents.
     * @since 0.1
     */
    // FIXME Unify the order of generic template parameters (TableColumn/Column vs ColumnPattern)
    public record Column<E, C>(
            String name,
            Class<C> columnType,
            int index,
            boolean nullable,
            Optional<ColumnPattern<C, E>> pattern) {

        /**
         * @param columnType The class of Java objects this column represents. Since this class represents existing
         *                   columns this type can only be determined at runtime.
         * @param index      The index representing the ordering of the column in the table (starting with 0 for the
         *                   first column)
         * @since 0.16
         */
        // NOTE Only the class Table should be allowed to create Column objects
        public Column {
            if (index < 0) {
                throw new IllegalArgumentException("The index must not be negative");
            }
        }

        /**
         * Required for referencing the generic type of {@link Column} during runtime.
         */
        @NotNull
        @SuppressWarnings("unchecked")
        public static <C> Class<Column<?, C>> getTypeDummy(Class<C> runtimeGenericTypeProvider) {
            //noinspection InstantiatingObjectToGetClassObject
            return (Class<Column<?, C>>)
                    new Column<>("nonExistingColumnName", runtimeGenericTypeProvider, 0, false, null).getClass();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Column<?, ?> column = (Column<?, ?>) o;
            return name().equals(column.name());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name());
        }
    }
}
