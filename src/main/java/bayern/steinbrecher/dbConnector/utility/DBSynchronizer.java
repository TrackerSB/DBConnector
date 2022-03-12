package bayern.steinbrecher.dbConnector.utility;

import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.query.GenerationFailedException;
import bayern.steinbrecher.dbConnector.query.QueryCondition;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.query.QueryGenerator;
import bayern.steinbrecher.dbConnector.query.QueryOperator;
import bayern.steinbrecher.dbConnector.scheme.ColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.SimpleColumnPattern;
import bayern.steinbrecher.dbConnector.scheme.TableScheme;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stefan Huber
 * @since 0.16
 */
public record DBSynchronizer<E>(
        TableScheme<?, E> scheme,
        DBConnection connection
) {
    private static final Logger LOGGER = Logger.getLogger(DBSynchronizer.class.getName());

    /**
     * Generate a set of {@link QueryCondition}s that identify the given entry within the set of given columns.
     */
    @NotNull
    private <C> Collection<QueryCondition<?>> getPrimaryKeyConditions(
            @NotNull Set<DBConnection.Column<E, ?>> columns, @NotNull E entry) {
        QueryGenerator queryGenerator = connection().getDbms().getQueryGenerator();
        Collection<QueryCondition<?>> primaryKeyConditions = new ArrayList<>();
        for (DBConnection.Column<E, ?> column : columns) {
            @SuppressWarnings("unchecked")
            var typedColumn = (DBConnection.Column<E, C>) column;
            typedColumn.pattern().ifPresent(pattern -> {
                if (pattern instanceof SimpleColumnPattern<C, E> scp && scp.isPrimaryKey()) {
                    QueryOperator<C> equalityOperator
                            = QueryOperator.getEqualityOperator(scp.getParser().getType());
                    C oldCellValue = scp.getValue(entry, scp.getRealColumnName());
                    QueryCondition<C> condition
                            = equalityOperator.generateCondition(queryGenerator, column, oldCellValue);
                    primaryKeyConditions.add(condition);
                }
            });
        }
        return primaryKeyConditions;
    }

    /**
     * Reflect any changes made to the given entries via the initially given connection to the DB.
     * NOTE 2022-02-22: Changes in the database are NOT reflected to the given entries.
     */
    public <C> void synchronize(@NotNull ObservableList<E> entries) throws InvalidSyncTargetException {
        DBConnection.Table<?, E> table;
        Set<DBConnection.Column<E, ?>> columns;
        try {
            table = connection().getTable(scheme()).orElseThrow(() -> new InvalidSyncTargetException(
                    "Could not find any table matching the given scheme using the given connection"));
            columns = table.getColumns();
        } catch (QueryFailedException ex) {
            throw new InvalidSyncTargetException(ex);
        }

        if (scheme().getRequiredColumns().stream().noneMatch(SimpleColumnPattern::isPrimaryKey)) {
            throw new InvalidSyncTargetException("Cannot synchronize table having no primary key");
        }

        entries.addListener((ListChangeListener<? super E>) change -> {
            while (change.next()) {
                List<? extends E> removedEntries = change.getRemoved();
                List<? extends E> addedEntries = change.getAddedSubList();
                List<Pair<E, E>> updatedEntries = new ArrayList<>();

                QueryGenerator queryGenerator = connection().getDbms().getQueryGenerator();

                for (E entry : removedEntries) {
                    int addedIndex = addedEntries.indexOf(entry);
                    if (addedIndex > -1) {
                        updatedEntries.add(new Pair<>(entry, addedEntries.get(addedIndex)));
                    } else {
                        System.out.println("TODO Remove entry from database");
                    }
                }

                for (E entry : addedEntries) {
                    if (!removedEntries.contains(entry)) {
                        System.out.println("TODO Add a new entry to the database");
                    }
                }

                for (Pair<E, E> entry : updatedEntries) {
                    Map<String, String> fieldChanges = new HashMap<>();
                    for (DBConnection.Column<E, ?> column : columns) {
                        @SuppressWarnings("unchecked")
                        Optional<ColumnPattern<C, E>> columnPattern = ((DBConnection.Column<E, C>) column).pattern();
                        columnPattern.ifPresent(cp -> {
                            C oldCellValue = cp.getValue(entry.getKey(), column.name());
                            C newCellValue = cp.getValue(entry.getValue(), column.name());
                            if (!Objects.equals(oldCellValue, newCellValue)) {
                                String newCellSQLValue = cp.getParser().toString(newCellValue);
                                fieldChanges.put(column.name(), newCellSQLValue);
                            }
                        });
                    }

                    Collection<QueryCondition<?>> primaryKeyConditions
                            = getPrimaryKeyConditions(columns, entry.getKey());

                    try {
                        String updateQueryStatement = queryGenerator.generateUpdateQueryStatement(
                                connection().getDatabaseName(), table, fieldChanges, primaryKeyConditions);
                        connection().execUpdate(updateQueryStatement);
                    } catch (GenerationFailedException | QueryFailedException ex) {
                        LOGGER.log(Level.SEVERE,
                                String.format("Failed to synchronize changes for %s to %s",
                                        entry.getValue(), connection().getDatabaseName()), ex);
                    }
                }
            }
        });
    }
}
