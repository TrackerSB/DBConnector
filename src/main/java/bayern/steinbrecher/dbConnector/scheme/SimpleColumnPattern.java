package bayern.steinbrecher.dbConnector.scheme;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a {@link ColumnPattern} representing a <strong>specific</strong> column name instead of an actual pattern
 * for column names.
 *
 * @param <C> The type of the contents this column holds.
 * @param <E> The type of object to set the content of this column to.
 * @author Stefan Huber
 * @since 0.1
 */
public class SimpleColumnPattern<C, E> extends ColumnPattern<C, E> {
    private final String realColumnName;
    private final Optional<Optional<C>> defaultValue;
    private final BiFunction<E, C, E> setter;
    private final Function<E, C> getter;
    private final boolean isPrimaryKey;
    private final boolean allowNull;

    /**
     * Creates a new simple column pattern, i.e. a pattern which specifies a specific column name. This constructor may
     * be used if {@link E} is an immutable type.
     *
     * @param realColumnName The exact name of the column to match.
     * @param parser         The parser to convert values from and to a SQL representation.
     * @param setter         The function used to set a parsed value to a given object. The setter should only return
     *                       a new object of type {@link E} if the handed in one is immutable.
     * @since 0.5
     */
    public SimpleColumnPattern(@NotNull String realColumnName, @NotNull ColumnParser<C> parser,
                               @NotNull BiFunction<E, C, E> setter, @NotNull Function<E, C> getter) {
        this(realColumnName, parser, setter, getter, Optional.empty(), false, false);
    }

    /**
     * Creates a new simple column pattern, i.e. a pattern which specifies a specific column name. This constructor may
     * be used if {@link E} is an immutable type.
     *
     * @param realColumnName The exact name of the column to match.
     * @param parser         The parser to convert values from and to a SQL representation.
     * @param setter         The function used to set a parsed value to a given object. The setter should only return
     *                       a new object of type {@link E} if the handed in one is immutable.
     * @param defaultValue   The default value of this column. {@link Optional#empty()} represents explicitely no
     *                       default value. An {@link Optional} of an {@link Optional#empty()} represents {@code null}
     *                       as default value. Otherwise the value of the inner {@link Optional} represents the default
     *                       value.
     * @see ColumnPattern#ColumnPattern(String, ColumnParser)
     * @since 0.5
     */
    public SimpleColumnPattern(@NotNull String realColumnName, @NotNull ColumnParser<C> parser,
                               @NotNull BiFunction<E, C, E> setter, @NotNull Function<E, C> getter,
                               @NotNull Optional<Optional<C>> defaultValue,
                               boolean isPrimaryKey, boolean allowNull) {
        super("^\\Q" + realColumnName + "\\E$", parser);
        if (realColumnName.length() < 1) {
            throw new IllegalArgumentException("The column name must have at least a single character");
        }

        this.realColumnName = Objects.requireNonNull(realColumnName);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.setter = Objects.requireNonNull(setter);
        this.getter = Objects.requireNonNull(getter);
        this.isPrimaryKey = isPrimaryKey;
        this.allowNull = allowNull;
    }

    @Override
    public C getValue(E toGetFrom, @NotNull String columnName) {
        return getter.apply(toGetFrom);
    }

    @Override
    @NotNull
    protected E combineImpl(@NotNull E toSet, @NotNull String columnName, @Nullable C value) {
        return setter.apply(toSet, value);
    }

    /**
     * Returns the real column name of this column.
     *
     * @return The real column name of this column.
     */
    @NotNull
    public String getRealColumnName() {
        return realColumnName;
    }

    /**
     * Checks whether a default value is set for this column
     *
     * @return {@code true} only if a default value is associated with this column.
     */
    public boolean hasDefaultValue() {
        return getDefaultValue().isPresent();
    }

    /**
     * Returns the default value to set when creating a table containing this column.
     *
     * @return The default value to set when creating a table containing this column. See description of constructors.
     */
    @NotNull
    public Optional<Optional<C>> getDefaultValue() {
        return defaultValue;
    }

    public String getSQLDefaultValue() {
        return getDefaultValue()
                .map(value -> getParser().toString(value.orElse(null)))
                .orElseThrow();
    }

    /**
     * @since 0.5
     */
    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    /**
     * @since 0.5
     */
    public boolean isAllowNull() {
        return allowNull;
    }
}
