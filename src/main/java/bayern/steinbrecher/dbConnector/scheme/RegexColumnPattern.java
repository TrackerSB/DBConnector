package bayern.steinbrecher.dbConnector.scheme;

import bayern.steinbrecher.dbConnector.utility.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a {@link ColumnPattern} which may match a range of column names instead of a specific one like
 * {@link SimpleColumnPattern}.
 *
 * @param <C> The type of the column content.
 * @param <E> The type of object to set the content of this column to.
 * @param <K> The type of the key to distinguish the columns matching this pattern.
 * @author Stefan Huber
 * @see SimpleColumnPattern
 * @since 0.1
 */
public class RegexColumnPattern<C, E, K> extends ColumnPattern<C, E> {

    private final TriFunction<E, K, C, E> setter;
    private final Function<String, K> keyExtractor;
    private final BiFunction<E, K, C> getter;

    /**
     * Creates a column pattern possibly matching multiple column names. This constructor may be used if {@link E} is an
     * immutable type.
     *
     * @param columnNamePattern The pattern of column names to match.
     * @param parser            The parser to convert values from and to a SQL representation.
     * @param setter            The function used to set a parsed value to a given object. The setter should only
     *                          return a new object of type {@link E} if the handed in one is immutable.
     * @param keyExtractor      Extracts the key for a given column name matching this pattern.
     * @see ColumnPattern#ColumnPattern(String, ColumnParser)
     */
    public RegexColumnPattern(@NotNull String columnNamePattern, @NotNull ColumnParser<C> parser,
                              @NotNull TriFunction<E, K, C, E> setter, @NotNull Function<String, K> keyExtractor,
                              @NotNull BiFunction<E, K, C> getter) {
        super(columnNamePattern, parser);
        Objects.requireNonNull(setter);
        Objects.requireNonNull(keyExtractor);

        this.setter = setter;
        this.keyExtractor = keyExtractor;
        this.getter = getter;
    }

    @Override
    public C getValue(E toGetFrom, @NotNull String columnName) {
        return getter.apply(toGetFrom, keyExtractor.apply(columnName));
    }

    @Override
    protected E combineImpl(@NotNull E toSet, @NotNull String columnName, @Nullable C value) {
        K key = keyExtractor.apply(columnName);
        return setter.accept(toSet, key, value);
    }
}
