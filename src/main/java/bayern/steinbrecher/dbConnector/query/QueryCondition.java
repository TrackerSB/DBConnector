package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.DBConnection.Column;
import bayern.steinbrecher.dbConnector.scheme.ColumnParser;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Stefan Huber
 * @since 0.5
 */
public abstract class QueryCondition<T> {
    public static final QueryCondition<String> LIKE
            = new BinaryQueryCondition<>(ArgumentConverter.STRING_ARGUMENT_CONVERTER, "LIKE");
    public static final QueryCondition<Boolean> IS_TRUE
            = new UnaryPrefixQueryCondition<>(ArgumentConverter.BOOLEAN_ARGUMENT_CONVERTER, "");
    public static final QueryCondition<Boolean> IS_FALSE
            = new UnaryPrefixQueryCondition<>(ArgumentConverter.BOOLEAN_ARGUMENT_CONVERTER, "NOT");

    private final ArgumentConverter<T> argumentConverter;

    protected QueryCondition(@NotNull ArgumentConverter<T> argumentConverter) {
        this.argumentConverter = argumentConverter;
    }

    protected ArgumentConverter<T> getArgumentConverter() {
        return argumentConverter;
    }

    @NotNull
    public abstract String generateSQLExpression(@NotNull QueryGenerator queryGenerator, @NotNull Object... arguments);

    public static final class ArgumentConverter<T> {
        public static final ArgumentConverter<String> STRING_ARGUMENT_CONVERTER = new ArgumentConverter<>(
                String.class, Column.getTypeDummy(String.class), ColumnParser.STRING_COLUMN_PARSER);
        public static final ArgumentConverter<Boolean> BOOLEAN_ARGUMENT_CONVERTER = new ArgumentConverter<>(
                Boolean.class, Column.getTypeDummy(Boolean.class), ColumnParser.BOOLEAN_COLUMN_PARSER);

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

    private static class BinaryQueryCondition<T> extends QueryCondition<T> {
        private final String operator;

        protected BinaryQueryCondition(
                @NotNull ArgumentConverter<T> argumentConverter, @NotNull String operator) {
            super(argumentConverter);
            this.operator = Objects.requireNonNull(operator);
        }

        @Override
        @NotNull
        public String generateSQLExpression(
                @NotNull QueryGenerator queryGenerator, @NotNull Object... arguments) {
            if (arguments.length != 2) {
                throw new IllegalArgumentException("Exactly two arguments required");
            }
            String leftHandArgument = getArgumentConverter()
                    .convertArgument(queryGenerator, arguments[0]);
            String rightHandArgument = getArgumentConverter()
                    .convertArgument(queryGenerator, arguments[1]);
            // FIXME Ensure escaping
            return String.format("%s %s %s", leftHandArgument, operator, rightHandArgument);
        }
    }

    private static class UnaryPrefixQueryCondition<T> extends QueryCondition<T> {
        private final String operator;

        protected UnaryPrefixQueryCondition(@NotNull ArgumentConverter<T> argumentConverter, @NotNull String operator) {
            super(argumentConverter);
            this.operator = Objects.requireNonNull(operator);
        }

        @Override
        public @NotNull String generateSQLExpression(
                @NotNull QueryGenerator queryGenerator, @NotNull Object... arguments) {
            if (arguments.length != 1) {
                throw new IllegalArgumentException("Exactly one argument required");
            }
            String argument = getArgumentConverter()
                    .convertArgument(queryGenerator, arguments[0]);
            // FIXME Ensure escaping
            return String.format("%s %s", operator, argument);
        }
    }
}
