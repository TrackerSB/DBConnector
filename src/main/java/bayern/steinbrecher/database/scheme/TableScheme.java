package bayern.steinbrecher.database.scheme;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a table and all patterns for its required and optional columns.
 *
 * @param <T> The type representing the whole table.
 * @param <E> The type of an entry of the table.
 * @author Stefan Huber
 * @since 0.1
 */
public class TableScheme<T, E> {
    private static final Logger LOGGER = Logger.getLogger(TableScheme.class.getName());
    private final String tableName;
    private final Collection<SimpleColumnPattern<?, E>> requiredColumns;
    private final Collection<ColumnPattern<?, E>> optionalColumns;
    private final Supplier<E> emptyEntrySupplier;
    private final Function<Stream<E>, T> reducer;

    public TableScheme(@NotNull String tableName, @NotNull Collection<SimpleColumnPattern<?, E>> requiredColumns,
                       @NotNull Collection<ColumnPattern<?, E>> optionalColumns,
                       @NotNull Supplier<E> emptyEntrySupplier, @NotNull Function<Stream<E>, T> reducer) {
        this.tableName = Objects.requireNonNull(tableName);
        this.requiredColumns = Objects.requireNonNull(requiredColumns);
        this.optionalColumns = Objects.requireNonNull(optionalColumns);
        this.emptyEntrySupplier = Objects.requireNonNull(emptyEntrySupplier);
        this.reducer = Objects.requireNonNull(reducer);
    }

    @NotNull
    private Stream<ColumnPattern<?, E>> streamAllColumns() {
        return Stream.concat(getRequiredColumns().stream(), getOptionalColumns().stream());
    }

    /**
     * @since 0.1
     */
    @NotNull
    public T parseFrom(@NotNull List<List<String>> queryResult) {
        List<String> headings = queryResult.get(0);
        Map<ColumnPattern<?, E>, Collection<Integer>> patternToColumnMapping = streamAllColumns()
                .map(pattern -> {
                    Collection<Integer> targetIndices = new ArrayList<>();
                    for (int i = 0; i < headings.size(); i++) {
                        if (pattern.matches(headings.get(i))) {
                            targetIndices.add(i);
                        }
                    }
                    return new Pair<>(pattern, targetIndices);
                })
                .filter(pair -> !pair.getValue().isEmpty())
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        //Check duplicate target column indices
        Set<Integer> mappedTargetIndices = new HashSet<>();
        if (!patternToColumnMapping.values()
                .stream()
                .flatMap(Collection::stream)
                .allMatch(mappedTargetIndices::add)) {
            throw new IllegalStateException("Table " + getTableName() + " contains intersecting column patterns.");
        }

        return reducer.apply(queryResult.stream()
                .skip(1) //Skip headings
                .map(row -> {
                    E rowRepresentation = emptyEntrySupplier.get();
                    for (Map.Entry<ColumnPattern<?, E>, Collection<Integer>> columnMapping
                            : patternToColumnMapping.entrySet()) {
                        ColumnPattern<?, E> pattern = columnMapping.getKey();
                        Collection<Integer> targetIndices = columnMapping.getValue();
                        if (targetIndices.size() <= 0) {
                            LOGGER.log(Level.WARNING,
                                    "Pattern {0} is registered for {1} but there is no matching column",
                                    new Object[]{pattern.getColumnNamePattern().pattern(), getTableName()});
                        }
                        if (pattern instanceof SimpleColumnPattern<?, ?>
                                && targetIndices.size() > 1) { //NOPMD - Check whether association is ambiguous.
                            LOGGER.log(Level.WARNING, "The simple column {0} is associated to more than "
                                            + "1 column name matches. Only the last match is applied.",
                                    pattern.getColumnNamePattern().pattern());
                        }
                        for (Integer index : targetIndices) {
                            rowRepresentation = pattern.combine(rowRepresentation, headings.get(index), row.get(index));
                        }
                    }
                    return rowRepresentation;
                }));
    }

    /**
     * @since 0.1
     */
    @NotNull
    public String getTableName() {
        return tableName;
    }

    /**
     * @since 0.1
     */
    @NotNull
    public Collection<SimpleColumnPattern<?, E>> getRequiredColumns() {
        return requiredColumns;
    }

    /**
     * @since 0.1
     */
    @NotNull
    public Collection<ColumnPattern<?, E>> getOptionalColumns() {
        return optionalColumns;
    }
}
