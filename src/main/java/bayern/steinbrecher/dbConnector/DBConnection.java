package bayern.steinbrecher.dbConnector;

import bayern.steinbrecher.dbConnector.query.GenerationFailedException;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.query.QueryGenerator;
import bayern.steinbrecher.dbConnector.query.SupportedDatabases;
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

/**
 * @author Stefan Huber
 * @since 0.1
 */
public abstract class DBConnection implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private final String databaseName;
    private final SupportedDatabases dbms;

    /**
     * @since 0.1
     */
    public DBConnection(@NotNull String databaseName, @NotNull SupportedDatabases dbms) {
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
            return !result.isEmpty()
                    && !result.get(0).isEmpty()
                    && Integer.parseInt(result.get(0).get(0)) > 0;
        } catch (GenerationFailedException ex) {
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
    public <T> T getTableContent(@NotNull TableScheme<T, ?> tableScheme)
            throws GenerationFailedException, QueryFailedException {
        Set<SimpleColumnPattern<?, ?>> missingColumns = getMissingColumns(tableScheme);
        if (missingColumns.isEmpty()) {
            T tableContent;
            String searchQuery = getDbms()
                    .getQueryGenerator()
                    .generateSearchQueryStatement(getDatabaseName(), getTable(tableScheme).orElseThrow(),
                            Collections.emptyList(), Collections.emptyList());
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
    public Set<SimpleColumnPattern<?, ?>> getMissingColumns(@NotNull TableScheme<?, ?> scheme)
            throws QueryFailedException {
        Set<Column<?>> cachedColumns = getAllColumns(scheme);
        return scheme.getRequiredColumns()
                .stream()
                .filter(scp -> cachedColumns.stream().noneMatch(column -> scp.matches(column.getName())))
                .collect(Collectors.toSet());
    }

    /**
     * @since 0.5
     */
    @NotNull
    public Set<Column<?>> getAllColumns(@NotNull TableScheme<?, ?> tableScheme) throws QueryFailedException {
        Optional<Table<?, ?>> table;
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
     * @since 0.5
     */
    @NotNull
    public Set<Table<?, ?>> getAllTables() throws QueryFailedException {
        QueryGenerator queryGenerator = getDbms()
                .getQueryGenerator();
        String query;
        try {
            query = queryGenerator.generateQueryTableNamesStatement(getDatabaseName());
        } catch (GenerationFailedException ex) {
            throw new QueryFailedException("Could not request existing tables", ex);
        }
        return execQuery(query)
                .stream()
                .map(row -> new Table<>(row.get(0)))
                .collect(Collectors.toSet());
    }

    /**
     * NOTE In theory the generic parameters of input and output have to match. Due to type erasure of generic types
     * this can not be guaranteed.
     *
     * @since 0.6
     */
    @NotNull
    public Optional<Table<?, ?>> getTable(@NotNull TableScheme<?, ?> scheme) throws QueryFailedException {
        try {
            return getAllTables()
                    .stream()
                    .filter(table -> table.getTableName()
                            .equalsIgnoreCase(Objects.requireNonNull(scheme).getTableName()))
                    .findAny();
        } catch (QueryFailedException ex) {
            throw new QueryFailedException(
                    String.format(
                            "Could not check existence of table for table scheme '%s'", scheme.getTableName()), ex);
        }
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
    public SupportedDatabases getDbms() {
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
        private final String tableName;
        /**
         * NOTE Should be a static local variable in {@link #getColumns()}
         */
        private final Set<Column<?>> cachedColumns = new HashSet<>();

        // NOTE Only DBConnection should be allowed to create Table objects
        private Table(@NotNull String tableName) {
            this.tableName = Objects.requireNonNull(tableName);
        }

        @NotNull
        public String getTableName() {
            return tableName;
        }

        @NotNull
        @Unmodifiable
        public Set<Column<?>> getColumns() throws QueryFailedException {
            if (cachedColumns.isEmpty()) {
                try {
                    QueryGenerator queryGenerator = getDbms()
                            .getQueryGenerator();
                    String query = queryGenerator.generateQueryColumnNamesAndTypesStatement(
                            getDatabaseName(), this);
                    List<List<String>> result = execQuery(query);
                    for (List<String> row : result) {
                        String columnName = row.get(0);
                        String columnTypeName = row.get(1);
                        Optional<Class<?>> columnType = queryGenerator.getType(columnTypeName);
                        if (columnType.isPresent()) {
                            cachedColumns.add(new Column<>(columnName, columnType.get()));
                        } else {
                            LOGGER.log(Level.INFO, String.format(
                                    "Skip column '%s' of table '%s' since it has an unsupported SQL type ('%s')",
                                    columnName, getTableName(), columnTypeName));
                        }
                    }
                } catch (GenerationFailedException ex) {
                    throw new QueryFailedException(
                            String.format("Could not request existing columns of table '%s'", getTableName()), ex);
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
            return getTableName().equals(table.getTableName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTableName());
        }
    }

    /**
     * Represents a column that exists in a table which is accessible by this connection. In contrast
     * {@link ColumnPattern} only represents patterns of column names in a {@link TableScheme} of a table which might
     * have been created.
     *
     * @param <T> The type of Java objects this column represents.
     * @since 0.1
     */
    public static class Column<T> {

        private final String name;
        private final Class<T> columnType;

        /**
         * @param columnType The class of Java objects this column represents. Since this class represents existing
         *                   columns this type can only be determined at runtime.
         */
        // NOTE Only Table should be allowed to create Column objects
        private Column(@NotNull String name, @NotNull Class<T> columnType) {
            this.name = Objects.requireNonNull(name);
            this.columnType = Objects.requireNonNull(columnType);
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public Class<T> getColumnType() {
            return columnType;
        }

        /**
         * Required for referencing the generic type of {@link Column} during runtime.
         */
        @NotNull
        @SuppressWarnings("unchecked")
        public static <C> Class<Column<C>> getTypeDummy(Class<C> runtimeGenericTypeProvider) {
            return (Class<Column<C>>) new Column<C>("nonExistingColumnName", runtimeGenericTypeProvider).getClass();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Column<?> column = (Column<?>) o;
            return getName().equals(column.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName());
        }
    }
}
