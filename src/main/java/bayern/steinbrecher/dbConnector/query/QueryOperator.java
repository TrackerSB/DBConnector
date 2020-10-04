package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.DBConnection.Column;
import bayern.steinbrecher.dbConnector.scheme.ColumnParser;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Stefan Huber
 * @since 0.7
 */
public abstract class QueryOperator<T> {
    public static final QueryOperator<String> LIKE
            = new BinaryQueryOperator<>(ArgumentConverter.STRING_ARGUMENT_CONVERTER, "LIKE");

    public static final QueryOperator<Boolean> IS_TRUE
            = new PrefixQueryOperator<>(ArgumentConverter.BOOLEAN_ARGUMENT_CONVERTER, "");
    public static final QueryOperator<Boolean> IS_FALSE
            = new PrefixQueryOperator<>(ArgumentConverter.BOOLEAN_ARGUMENT_CONVERTER, "NOT");

    // FIXME The separation of integer and double prohibits comparison of integer and double columns
    public static final QueryOperator<Integer> IS_SMALLER_I
            = new BinaryQueryOperator<>(ArgumentConverter.INTEGER_ARGUMENT_CONVERTER, "<");
    public static final QueryOperator<Integer> IS_SMALLER_EQUAL_I
            = new BinaryQueryOperator<>(ArgumentConverter.INTEGER_ARGUMENT_CONVERTER, "<=");
    public static final QueryOperator<Integer> IS_EQUAL_I
            = new BinaryQueryOperator<>(ArgumentConverter.INTEGER_ARGUMENT_CONVERTER, "=");
    public static final QueryOperator<Integer> IS_GREATER_EQUAL_I
            = new BinaryQueryOperator<>(ArgumentConverter.INTEGER_ARGUMENT_CONVERTER, ">=");
    public static final QueryOperator<Integer> IS_GREATER_I
            = new BinaryQueryOperator<>(ArgumentConverter.INTEGER_ARGUMENT_CONVERTER, ">");

    public static final QueryOperator<Double> IS_SMALLER_D
            = new BinaryQueryOperator<>(ArgumentConverter.DOUBLE_ARGUMENT_CONVERTER, "<");
    public static final QueryOperator<Double> IS_SMALLER_EQUAL_D
            = new BinaryQueryOperator<>(ArgumentConverter.DOUBLE_ARGUMENT_CONVERTER, "<=");
    public static final QueryOperator<Double> IS_EQUAL_D
            = new BinaryQueryOperator<>(ArgumentConverter.DOUBLE_ARGUMENT_CONVERTER, "=");
    public static final QueryOperator<Double> IS_GREATER_EQUAL_D
            = new BinaryQueryOperator<>(ArgumentConverter.DOUBLE_ARGUMENT_CONVERTER, ">=");
    public static final QueryOperator<Double> IS_GREATER_D
            = new BinaryQueryOperator<>(ArgumentConverter.DOUBLE_ARGUMENT_CONVERTER, ">");

    private final ArgumentConverter<T> argumentConverter;

    protected QueryOperator(@NotNull ArgumentConverter<T> argumentConverter) {
        this.argumentConverter = argumentConverter;
    }

    protected ArgumentConverter<T> getArgumentConverter() {
        return argumentConverter;
    }

    @NotNull
    public abstract QueryCondition<T> generateCondition(
            @NotNull QueryGenerator queryGenerator, @NotNull Object... arguments);

    public static final class ArgumentConverter<T> {
        public static final ArgumentConverter<String> STRING_ARGUMENT_CONVERTER = new ArgumentConverter<>(
                String.class, Column.getTypeDummy(String.class), ColumnParser.STRING_COLUMN_PARSER);
        public static final ArgumentConverter<Boolean> BOOLEAN_ARGUMENT_CONVERTER = new ArgumentConverter<>(
                Boolean.class, Column.getTypeDummy(Boolean.class), ColumnParser.BOOLEAN_COLUMN_PARSER);
        public static final ArgumentConverter<Integer> INTEGER_ARGUMENT_CONVERTER = new ArgumentConverter<>(
                Integer.class, Column.getTypeDummy(Integer.class), ColumnParser.INTEGER_COLUMN_PARSER);
        public static final ArgumentConverter<Double> DOUBLE_ARGUMENT_CONVERTER = new ArgumentConverter<>(
                Double.class, Column.getTypeDummy(Double.class), ColumnParser.DOUBLE_COLUMN_PARSER);

        public final Class<T> runtimeGenericTypeProvider;
        public final Class<Column<T>> runtimeGenericColumnTypeProvider;
        public final ColumnParser<T> columnParser;

        private ArgumentConverter(@NotNull Class<T> runtimeGenericTypeProvider,
                                  @NotNull Class<Column<T>> runtimeGenericColumnTypeProvider,
                                  @NotNull ColumnParser<T> columnParser) {
            this.runtimeGenericTypeProvider = Objects.requireNonNull(runtimeGenericTypeProvider);
            this.runtimeGenericColumnTypeProvider = Objects.requireNonNull(runtimeGenericColumnTypeProvider);
            this.columnParser = Objects.requireNonNull(columnParser);
        }

        @NotNull
        private Optional<String> convertAsColumnArgument(QueryGenerator queryGenerator, @NotNull Object argument) {
            String convertedArgument;
            if (runtimeGenericColumnTypeProvider.isAssignableFrom(argument.getClass())) {
                convertedArgument = queryGenerator.quoteIdentifier(
                        runtimeGenericColumnTypeProvider.cast(argument).getName());
            } else {
                convertedArgument = null;
            }
            return Optional.ofNullable(convertedArgument);
        }

        @NotNull
        private Optional<String> convertAsTypeArgument(@NotNull Object argument) {
            String convertedArgument;
            if (runtimeGenericTypeProvider.isAssignableFrom(argument.getClass())) {
                convertedArgument = columnParser.toString(runtimeGenericTypeProvider.cast(argument));
            } else {
                convertedArgument = null;
            }
            return Optional.ofNullable(convertedArgument);
        }

        @NotNull
        protected String convertArgument(@NotNull QueryGenerator queryGenerator, @NotNull Object argument) {
            return convertAsTypeArgument(argument)
                    .or(() -> convertAsColumnArgument(queryGenerator, argument))
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Only objects of type '%s' and '%s' as arguments accepted",
                                    runtimeGenericTypeProvider.getSimpleName(), Column.class.getSimpleName())));
        }
    }

    private static class BinaryQueryOperator<T> extends QueryOperator<T> {
        private final String operator;

        protected BinaryQueryOperator(
                @NotNull ArgumentConverter<T> argumentConverter, @NotNull String operator) {
            super(argumentConverter);
            this.operator = Objects.requireNonNull(operator);
        }

        @Override
        @NotNull
        public QueryCondition<T> generateCondition(
                @NotNull QueryGenerator queryGenerator, @NotNull Object... arguments) {
            if (arguments.length != 2) {
                throw new IllegalArgumentException("Exactly two arguments required");
            }
            String leftHandArgument = getArgumentConverter()
                    .convertArgument(queryGenerator, arguments[0]);
            String rightHandArgument = getArgumentConverter()
                    .convertArgument(queryGenerator, arguments[1]);
            // FIXME Ensure escaping
            return new QueryCondition<>(String.format("%s %s %s", leftHandArgument, operator, rightHandArgument));
        }
    }

    private static class PrefixQueryOperator<T> extends QueryOperator<T> {
        private final String operator;

        protected PrefixQueryOperator(@NotNull ArgumentConverter<T> argumentConverter, @NotNull String operator) {
            super(argumentConverter);
            this.operator = Objects.requireNonNull(operator);
        }

        @Override
        @NotNull
        public QueryCondition<T> generateCondition(
                @NotNull QueryGenerator queryGenerator, @NotNull Object... arguments) {
            if (arguments.length != 1) {
                throw new IllegalArgumentException("Exactly one argument required");
            }
            String argument = getArgumentConverter()
                    .convertArgument(queryGenerator, arguments[0]);
            // FIXME Ensure escaping
            return new QueryCondition<>(String.format("%s %s", operator, argument));
        }
    }
}
