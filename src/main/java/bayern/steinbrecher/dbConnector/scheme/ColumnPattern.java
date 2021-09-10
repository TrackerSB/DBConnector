package bayern.steinbrecher.dbConnector.scheme;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @param <C> The datatype of the values stored in the column.
 * @param <E> The type of objects the content of this column belongs to.
 * @author Stefan Huber
 * @since 0.1
 */
public abstract class ColumnPattern<C, E> {

    private static final Logger LOGGER = Logger.getLogger(ColumnPattern.class.getName());
    private final Pattern columnNamePattern;
    private final ColumnParser<C> parser;

    /**
     * @param columnNamePattern The RegEx column names have to match.
     * @param parser            The parser for converting values from and to their SQL representation.
     */
    protected ColumnPattern(@NotNull String columnNamePattern, @NotNull ColumnParser<C> parser) {
        Objects.requireNonNull(columnNamePattern, "The pattern for the column name is required");
        Objects.requireNonNull(parser, "A parser which handles the column content is required");

        if (columnNamePattern.charAt(0) != '^' || !columnNamePattern.endsWith("$")) {
            LOGGER.log(Level.WARNING, "The pattern \"{0}\" is not encapsulated in \"^\" and \"$\".", columnNamePattern);
        }
        this.columnNamePattern = Pattern.compile(columnNamePattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        this.parser = parser;
    }

    @NotNull
    public Pattern getColumnNamePattern() {
        return columnNamePattern;
    }

    public boolean matches(@NotNull String columnName) {
        return getColumnNamePattern()
                .matcher(Objects.requireNonNull(columnName))
                .matches();
    }

    @NotNull
    public ColumnParser<C> getParser() {
        return parser;
    }

    /**
     * Return the value of the given object in the column having the given name.
     *
     * @param toGetFrom  The item to get the value from.
     * @param columnName The name of the column of the value to get.
     * @since 0.16
     */
    public abstract C getValue(E toGetFrom, @NotNull String columnName);

    /**
     * Parses the given value and sets it to the object of type {@link E}.
     *
     * @param toSet      The object to set the parsed value to.
     * @param columnName The column name matching this pattern to extract the key from.
     * @param value      The value to parse and to set.
     * @return The resulting object of type {@link E}.
     */
    public final E combine(@NotNull E toSet, @NotNull String columnName, @Nullable String value) {
        if (getColumnNamePattern().matcher(columnName).matches()) {
            String valueToParse;
            if (value == null || value.equalsIgnoreCase("null")) {
                valueToParse = null;
            } else {
                valueToParse = value;
            }
            C parsedValue;
            try {
                parsedValue = getParser()
                        .parse(valueToParse);
            } catch (ParseException ex) {
                LOGGER.log(Level.WARNING,
                        String.format("Could not parse value '%s' for column '%s'. The value is skipped.",
                                valueToParse, columnName));
                parsedValue = null;
            }
            return combineImpl(toSet, columnName, parsedValue);
        } else {
            throw new IllegalArgumentException("The given column name does not match this pattern.");
        }
    }

    /**
     * @see #combine(Object, String, String)
     */
    protected abstract E combineImpl(@NotNull E toSet, @NotNull String columnName, @Nullable C parsedValue);

    /**
     * Checks whether this pattern reflects the same column names as the given object. NOTE It is only checked whether
     * their regex are identical not whether they express the same column names.
     *
     * @param obj The object to compare this column pattern with.
     * @return {@code true} only if this pattern reflects the same column names as the given object.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        boolean isEqual = false;
        if (obj instanceof ColumnPattern) {
            isEqual = ((ColumnPattern<?, ?>) obj).getColumnNamePattern()
                    .pattern()
                    .equals(columnNamePattern.pattern());
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        //CHECKSTYLE.OFF: MagicNumber - This is the default implementation of NetBeans.
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.columnNamePattern);
        //CHECKSTYLE.ON: MagicNumber
        return hash;
    }

    @Override
    @NotNull
    public String toString() {
        return getColumnNamePattern()
                .pattern();
    }
}
