package bayern.steinbrecher.dbConnector.scheme;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Contains singletons for converting objects from and to their SQL representation.
 *
 * @param <T> The type to convert from and to a SQL representation.
 * @author Stefan Huber
 * @since 0.1
 */
//TODO Wait for generic enums
public abstract /* final */ class ColumnParser<T> {

    /**
     * @since 0.1
     */
    public static final ColumnParser<String> STRING_COLUMN_PARSER = new ColumnParser<>() {
        @Override
        @Nullable
        public String parse(@Nullable String value) {
            return value;
        }

        @Override
        @NotNull
        protected String toStringImpl(@NotNull String value) {
            /*
             * Single quotes for Strings should be preferred since single quotes always work in ANSI SQL. In MySQL they
             * may be quoted in double quotes which is only working if ANSI_QUOTES is NOT enabled (it is per default
             * disabled).
             */
            return "'" + value + "'";
        }

        @Override
        @NotNull
        public Class<String> getType() {
            return String.class;
        }
    };
    /**
     * @since 0.1
     */
    public static final ColumnParser<Integer> INTEGER_COLUMN_PARSER = new ColumnParser<>() {
        @Override
        @Nullable
        public Integer parse(@Nullable String value) throws ParseException {
            Integer parsedValue = null;
            if (value != null) {
                try {
                    parsedValue = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw new ParseException(ex);
                }
            }
            return parsedValue;
        }

        @Override
        @NotNull
        public Class<Integer> getType() {
            return Integer.class;
        }
    };
    /**
     * @since 0.1
     */
    public static final ColumnParser<Boolean> BOOLEAN_COLUMN_PARSER = new ColumnParser<>() {
        @Override
        @Nullable
        public Boolean parse(@Nullable String value) {
            Boolean parsedValue = null;
            if (value != null) {
                parsedValue = "1".equalsIgnoreCase(value);
            }
            return parsedValue;
        }

        @Override
        @NotNull
        protected String toStringImpl(@NotNull Boolean value) {
            return value ? "TRUE" : "FALSE";
        }

        @Override
        @NotNull
        public Class<Boolean> getType() {
            return Boolean.class;
        }
    };
    /**
     * @since 0.1
     */
    public static final ColumnParser<LocalDate> LOCALDATE_COLUMN_PARSER = new ColumnParser<>() {
        @Override
        @Nullable
        public LocalDate parse(@Nullable String value) throws ParseException {
            LocalDate date = null;
            if (value != null) {
                try {
                    date = LocalDate.parse(value);
                } catch (DateTimeParseException ex) {
                    throw new ParseException(ex);
                }
            }
            return date;
        }

        @Override
        @NotNull
        protected String toStringImpl(@NotNull LocalDate value) {
            return "'" + value + "'";
        }

        @Override
        @NotNull
        public Class<LocalDate> getType() {
            return LocalDate.class;
        }
    };
    /**
     * @since 0.1
     */
    public static final ColumnParser<Double> DOUBLE_COLUMN_PARSER = new ColumnParser<>() {
        @Override
        @Nullable
        public Double parse(@Nullable String value) throws ParseException {
            Double parsedValue = null;
            if (value != null) {
                try {
                    parsedValue = Double.parseDouble(value);
                } catch (NumberFormatException ex) {
                    throw new ParseException(ex);
                }
            }
            return parsedValue;
        }

        @Override
        @NotNull
        public Class<Double> getType() {
            return Double.class;
        }
    };

    private ColumnParser() {
        //Prohibit construction of additional parser outside this class
    }

    /**
     * @since 0.14
     */
    @Nullable
    public abstract T parse(@Nullable String value) throws ParseException;

    /**
     * Returns the {@link String} representation of the given value suitable for SQL. NOTE: For implementation it can be
     * assumed that the value is not {@code null} since this is handled by {@link #toString(Object)}. The
     * default implementation just calls {@link String#valueOf(Object)}.
     *
     * @param value The value to convert.
     * @return The {@link String} representation of the given value suitable for SQL.
     * @see #toString(Object)
     * @since 0.1
     */
    @NotNull
    protected String toStringImpl(@NotNull T value) {
        return String.valueOf(value);
    }

    /**
     * Parses the given value into a {@link String} representation suitable for SQL. Returns the {@link String} "NULL"
     * (without quotes) if {@code value} is {@code null}.
     *
     * @param value The value to convert.
     * @return A {@link String} representation of the given value suitable for SQL.
     * @since 0.1
     */
    @NotNull
    public final String toString(@Nullable T value) {
        String valueSql;
        if (value == null) {
            valueSql = "NULL";
        } else {
            valueSql = toStringImpl(value);
        }
        return valueSql;
    }

    /**
     * Returns the generic type of the class. This method is needed since type ereasure takes place.
     *
     * @return The generic type of the class.
     * @since 0.1
     */
    @NotNull
    public abstract Class<T> getType();
}
