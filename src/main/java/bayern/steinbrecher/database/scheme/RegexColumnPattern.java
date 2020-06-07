package bayern.steinbrecher.database.scheme;

import bayern.steinbrecher.utility.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a {@link ColumnPattern} which may match a range of column names instead of a specific one like
 * {@link SimpleColumnPattern}.
 *
 * @param <T> The type of the column content.
 * @param <U> The type of object to set the content of this column to.
 * @param <K> The type of the key to distinguish the columns matching this pattern.
 * @author Stefan Huber
 * @see SimpleColumnPattern
 * @since 0.1
 */
public class RegexColumnPattern<T, U, K> extends ColumnPattern<T, U> {

    private final TriFunction<U, K, T, U> setter;
    private final Function<String, K> keyExtractor;

    /**
     * Creates a column pattern possibly matching multiple column names. This constructor may be used if {@link U} is an
     * immutable type.
     *
     * @param columnNamePattern The pattern of column names to match.
     * @param parser            The parser to convert values from and to a SQL representation.
     * @param setter            The function used to set a parsed value to a given object. The setter should only
     *                          return a new object of type {@link U} if the handed in one is immutable.
     * @param keyExtractor      Extracts the key for a given column name matching this pattern.
     * @see ColumnPattern#ColumnPattern(String, ColumnParser)
     * @since 0.1
     */
    public RegexColumnPattern(@NotNull String columnNamePattern, @NotNull ColumnParser<T> parser,
                              @NotNull TriFunction<U, K, T, U> setter, @NotNull Function<String, K> keyExtractor) {
        super(columnNamePattern, parser);
        Objects.requireNonNull(setter);
        Objects.requireNonNull(keyExtractor);

        this.setter = setter;
        this.keyExtractor = keyExtractor;
    }

    /**
     * @since 0.1
     */
    @Override
    public U combineImpl(@NotNull U toSet, @NotNull String columnName, @Nullable String valueToParse) {
        K key = keyExtractor.apply(columnName);
        T parsedValue = getParser()
                .parse(valueToParse)
                .orElseThrow(() -> new IllegalArgumentException("Can not parse " + valueToParse));
        return setter.accept(toSet, key, parsedValue);
    }
}
