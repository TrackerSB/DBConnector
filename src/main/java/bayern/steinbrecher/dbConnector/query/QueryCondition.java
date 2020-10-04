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
    public static final QueryCondition<String> LIKE = new BinaryStringQueryCondition("LIKE");

    private final Class<T> runtimeGenericTypeProvider;
    private final Class<Column<T>> runtimeGenericColumnTypeProvider;
    private final ColumnParser<T> columnParser;

    protected QueryCondition(@NotNull Class<T> runtimeGenericTypeProvider,
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

    @NotNull
    public abstract String generateSQLExpression(@NotNull QueryGenerator queryGenerator, @NotNull Object... arguments);

    private static abstract class BinaryQueryCondition<T> extends QueryCondition<T> {
        private final String operator;

        protected BinaryQueryCondition(@NotNull Class<T> runtimeGenericTypeProvider,
                                       @NotNull Class<Column<T>> runtimeGenericColumnTypeProvider,
                                       @NotNull ColumnParser<T> columnParser, @NotNull String operator) {
            super(runtimeGenericTypeProvider, runtimeGenericColumnTypeProvider, columnParser);
            this.operator = Objects.requireNonNull(operator);
        }

        @Override
        public @NotNull String generateSQLExpression(
                @NotNull QueryGenerator queryGenerator, @NotNull Object... arguments) {
            if (arguments.length != 2) {
                throw new IllegalArgumentException("Exactly two arguments required");
            }
            String leftHandArgument = convertArgument(queryGenerator, arguments[0]);
            String rightHandArgument = convertArgument(queryGenerator, arguments[1]);
            // FIXME Ensure escaping
            return String.format("%s %s %s", leftHandArgument, operator, rightHandArgument);
        }
    }

    private static class BinaryStringQueryCondition extends BinaryQueryCondition<String> {

        protected BinaryStringQueryCondition(@NotNull String operator) {
            super(String.class, Column.getTypeDummy(String.class), ColumnParser.STRING_COLUMN_PARSER, operator);
        }
    }
}
