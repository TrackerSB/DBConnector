package bayern.steinbrecher.database.connection;

import bayern.steinbrecher.database.SupportedDatabases;
import bayern.steinbrecher.database.query.QueryGenerator;
import bayern.steinbrecher.database.scheme.SimpleColumnPattern;
import bayern.steinbrecher.database.scheme.TableScheme;
import bayern.steinbrecher.utility.PopulatingMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Caches the all really existing columns on every supported {@link TableScheme}. The cache is refreshed
     * whenever the currently loaded profile changes.
     */
    private final Map<TableScheme<?, ?>, List<Column<?>>> columnsCache;
    private final Set<String> tablesCache = new HashSet<>();

    /**
     * @since 0.1
     */
    public DBConnection(String databaseName, SupportedDatabases dbms) {
        this.databaseName = databaseName;
        this.dbms = dbms;

        columnsCache = new PopulatingMap<>(tableScheme -> {
            if (tableScheme == null) {
                throw new IllegalArgumentException("Can not generate query controls for table null.");
            }
            List<Column<?>> entry;
            try {
                String createTableStatement
                        = QueryGenerator.generateRequestForColumnNamesAndTypes(dbms, databaseName, tableScheme);
                List<List<String>> result = execQuery(createTableStatement);
                entry = result.stream()
                        .skip(1) //Skip headings
                        .map(list -> {
                            return dbms.getType(list.get(1))
                                    .map(ct -> new Column<>(list.get(0), ct));
                        })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                entry = new ArrayList<>();
            }
            return entry;
        });
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
     * @throws SQLException Thrown if the sql code is invalid.
     * @since 0.1
     */
    public abstract List<List<String>> execQuery(String sqlCode) throws SQLException;

    /**
     * Executes a command like INSERT INTO, UPDATE or CREATE.
     *
     * @param sqlCode The sql code to execute.
     * @throws SQLException Thrown if the sql code is invalid.
     * @since 0.1
     */
    public abstract void execUpdate(String sqlCode) throws SQLException;

    private void populateTablesCache() {
        synchronized (tablesCache) {
            if (tablesCache.isEmpty()) {
                List<List<String>> result;
                try {
                    result = execQuery(QueryGenerator.generateRequestForTableNames(dbms, databaseName));
                    tablesCache.addAll(result.stream()
                            //Skip column name
                            .skip(1)
                            .map(list -> list.get(0))
                            .collect(Collectors.toList()));
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Checks if the connected database exists.
     *
     * @return {@code true} only if the connected database exists.
     * @since 0.1
     */
    public boolean databaseExists() {
        List<List<String>> result;
        try {
            result = execQuery(QueryGenerator.generateRequestForExistenceOfDatabase(dbms, databaseName));
            return !result.isEmpty() && !result.get(0).isEmpty();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     * Checks whether the given table exists. It DOES NOT check whether it has all needed columns and is configured
     * right.
     *
     * @param tableScheme The table to search for.
     * @return {@code true} only if the given table exist.
     * @since 0.1
     */
    public boolean tableExists(TableScheme<?, ?> tableScheme) {
        populateTablesCache();
        return tablesCache.contains(tableScheme.getTableName());
    }

    /**
     * @since 0.1
     */
    public void createTableIfNotExists(TableScheme<?, ?> scheme) throws SchemeCreationException {
        if (!tableExists(scheme)) {
            //FIXME Check rights for creating tables
            try {
                execUpdate(QueryGenerator.generateCreateTableStatement(dbms, databaseName, scheme));
            } catch (SQLException ex) {
                throw new SchemeCreationException("Could not create table " + scheme.getTableName(), ex);
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
    public <T> T getTableContent(TableScheme<T, ?> tableScheme) {
        Optional<String> missingColumns = getMissingColumnsString(tableScheme);
        if (missingColumns.isPresent()) {
            throw new IllegalStateException("The table scheme misses columns: " + missingColumns);
        } else {
            T tableContent;
            Optional<String> searchQuery = QueryGenerator.generateSearchQueryFromColumns(
                    dbms, databaseName, tableScheme, columnsCache.get(tableScheme), Optional.empty());
            if (searchQuery.isPresent()) {
                try {
                    tableContent = tableScheme.parseFrom(execQuery(searchQuery.get()));
                } catch (SQLException ex) {
                    throw new Error("Generated SQL-Code invalid", ex); //NOPMD - Indicates bug in hardcoded SQL.
                }
            } else {
                throw new Error("The cache of the columns of the table " + tableScheme.getTableName()
                        + " contains no columns that are part of the actual table.");
            }
            return tableContent;
        }
    }

    /**
     * Checks whether all needed tables are accessible using this connection and have all required columns which are
     * also accessible.
     *
     * @return {@link Optional#empty()} if all tables have all required columns. Otherwise returns an {@link Optional}
     * mapping invalid tables to the required columns missing.
     * @since 0.1
     */
    public List<SimpleColumnPattern<?, ?>> getMissingColumns(TableScheme<?, ?> scheme) {
        List<Column<?>> cachedColumns = columnsCache.get(scheme);
        return scheme.getRequiredColumns()
                .stream()
                .filter(scp -> cachedColumns.stream().noneMatch(column -> scp.matches(column.getName())))
                .collect(Collectors.toList());
    }

    /**
     * @see #getMissingColumns(TableScheme)
     * @since 0.1
     */
    public Optional<String> getMissingColumnsString(TableScheme<?, ?> scheme) {
        List<SimpleColumnPattern<?, ?>> missingColumns = getMissingColumns(scheme);
        Optional<String> missingColumnsString;
        if (missingColumns.isEmpty()) {
            missingColumnsString = Optional.empty();
        } else {
            missingColumnsString = Optional.of(scheme.getTableName() + ": "
                    + missingColumns.parallelStream()
                    .map(SimpleColumnPattern::getRealColumnName)
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        return missingColumnsString;
    }

    /**
     * Returns a {@link List} of all existing columns (not only that ones declared in the scheme) and their
     * <strong>actual</strong> types in the database which may defer from the one declared in the scheme of the table.
     *
     * @param tableScheme The table to get the columns for.
     * @return A {@link List} of all existing columns (not only that ones declared in the scheme).
     * @see TableScheme#getAllColumns()
     * @since 0.1
     */
    public List<Column<?>> getAllColumns(TableScheme<?, ?> tableScheme) {
        return columnsCache.get(tableScheme);
    }

    /**
     * Returns all available table names of accessible over this connection.
     *
     * @return All available table names of accessible over this connection.
     * @since 0.1
     */
    public Set<String> getAllTables() {
        populateTablesCache();
        return tablesCache;
    }

    /**
     * @since 0.1
     */
    public String getDatabaseName() {
        return databaseName;
    }
}
