package bayern.steinbrecher.database.scheme;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Represents a {@link ColumnPattern} representing a <strong>specific</strong> column name instead of an actual pattern
 * for column names.
 *
 * @param <T> The type of the contents this column holds.
 * @param <U> The type of object to set the content of this column to.
 * @author Stefan Huber
 * @since 0.1
 */
public class SimpleColumnPattern<T, U> extends ColumnPattern<T, U> {

    private final String realColumnName;
    private final Optional<Optional<T>> defaultValue;
    private final Set<TableCreationKeywords> keywords;
    private final BiFunction<U, T, U> setter;

    /**
     * Creates a new simple column pattern, i.e. a pattern which specifies a specific column name. This constructor may
     * be used if {@link U} is an immutable type.
     *
     * @param realColumnName The exact name of the column to match.
     * @param keywords       The keywords to specify when creating a column matching this pattern.
     * @param parser         The parser to convert values from and to a SQL representation.
     * @param setter         The function used to set a parsed value to a given object. The setter should only return
     *                       a new object of type {@link U} if the handed in one is immutable.
     * @since 0.1
     */
    public SimpleColumnPattern(@NotNull String realColumnName, @NotNull Set<TableCreationKeywords> keywords,
                               @NotNull ColumnParser<T> parser, @NotNull BiFunction<U, T, U> setter) {
        this(realColumnName, keywords, parser, setter, Optional.empty());
    }

    /**
     * Creates a new simple column pattern, i.e. a pattern which specifies a specific column name. This constructor may
     * be used if {@link U} is an immutable type.
     *
     * @param realColumnName The exact name of the column to match.
     * @param keywords       The keywords to specify when creating a column matching this pattern.
     * @param parser         The parser to convert values from and to a SQL representation.
     * @param setter         The function used to set a parsed value to a given object. The setter should only return
     *                       a new object of type {@link U} if the handed in one is immutable.
     * @param defaultValue   The default value of this column. {@link Optional#empty()} represents explicitely no
     *                       default value. An {@link Optional} of an {@link Optional#empty()} represents {@code null}
     *                       as default value. Otherwise the value of the inner {@link Optional} represents the default
     *                       value.
     * @see ColumnPattern#ColumnPattern(String, ColumnParser)
     * @since 0.1
     */
    public SimpleColumnPattern(@NotNull String realColumnName, @NotNull Set<TableCreationKeywords> keywords,
                               @NotNull ColumnParser<T> parser, @NotNull BiFunction<U, T, U> setter,
                               @NotNull Optional<Optional<T>> defaultValue) {
        super("^\\Q" + realColumnName + "\\E$", parser);
        if (realColumnName.length() < 1) {
            throw new IllegalArgumentException("The column name must have at least a single character");
        }

        Set<TableCreationKeywords> keywordsCopy = new HashSet<>(Objects.requireNonNull(keywords));
        // Make sure DEFAULT keyword is present when a default value is specified.
        if (defaultValue.isPresent()) {
            keywordsCopy.add(TableCreationKeywords.DEFAULT);
        }
        this.realColumnName = realColumnName;
        this.defaultValue = defaultValue;
        this.keywords = keywordsCopy;
        this.setter = Objects.requireNonNull(setter);
    }

    /**
     * @since 0.1
     */
    @Override
    @NotNull
    public U combineImpl(@NotNull U toSet, @NotNull String columnName, @Nullable String valueToParse) {
        T parsedValue = getParser()
                .parse(valueToParse)
                .orElseThrow(
                        () -> new IllegalArgumentException(getRealColumnName() + " can not parse " + valueToParse));
        return setter.apply(toSet, parsedValue);
    }

    /**
     * Checks whether a default value is set for this column
     *
     * @return {@code true} only if a default value is associated with this column.
     * @since 0.1
     */
    public boolean hasDefaultValue() {
        return getDefaultValue().isPresent();
    }

    /**
     * Returns the {@link String} representation of the default value suitable for SQL.
     *
     * @return The {@link String} representation of the default value suitable for SQL. If the default value is
     * {@code null} the {@link String} "NULL" (without quotes) is returned.
     * @see #getDefaultValue()
     * @since 0.1
     */
    @NotNull
    public String getDefaultValueSql() {
        return getDefaultValue()
                .map(value -> getParser().toString(value.orElse(null)))
                .orElseThrow();
    }

    /**
     * Returns the default value to set when creating a table containing this column.
     *
     * @return The default value to set when creating a table containing this column. See description of constructors.
     * @since 0.1
     */
    @NotNull
    public Optional<Optional<T>> getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the real column name of this column.
     *
     * @return The real column name of this column.
     * @since 0.1
     */
    @NotNull
    public String getRealColumnName() {
        return realColumnName;
    }

    /**
     * Returns the SQL keywords associated with columns matching this pattern name.
     *
     * @return The SQL keywords associated with columns matching this pattern name.
     * @since 0.1
     */
    @NotNull
    public Set<TableCreationKeywords> getKeywords() {
        return keywords;
    }
}
