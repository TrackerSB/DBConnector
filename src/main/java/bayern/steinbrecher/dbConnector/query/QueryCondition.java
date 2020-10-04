package bayern.steinbrecher.dbConnector.query;

import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.DBConnection.Column;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Stefan Huber
 * @since 0.5
 */
public abstract class QueryCondition<T> {
    public static final QueryCondition<String> LIKE = new BinaryQueryCondition<>(String.class, DBConnection.Column.getTypeDummy(String.class), "LIKE") {
        @Override
        protected String convertArgumentImpl(String argument) {
            return String.format("'%s'", argument);
        }
    };

    @NotNull
    public abstract String generateSQLExpression(@NotNull QueryGenerator queryGenerator, @NotNull Object... arguments);

    private static abstract class BinaryQueryCondition<T> extends QueryCondition<T> {
        private final Class<T> runtimeGenericTypeProvider;
        private final Class<Column<T>> runtimeGenericColumnTypeProvider;
        private final String operator;

        protected BinaryQueryCondition(@NotNull Class<T> runtimeGenericTypeProvider,
                                       Class<Column<T>> runtimeGenericColumnTypeProvider, @NotNull String operator) {
            this.runtimeGenericTypeProvider = Objects.requireNonNull(runtimeGenericTypeProvider);
            this.runtimeGenericColumnTypeProvider = runtimeGenericColumnTypeProvider;
            this.operator = Objects.requireNonNull(operator);
        }

        protected abstract String convertArgumentImpl(T argument);

        protected String convertArgument(QueryGenerator queryGenerator, Object argument) {
            if (runtimeGenericTypeProvider.isAssignableFrom(argument.getClass())) {
                return convertArgumentImpl(runtimeGenericTypeProvider.cast(argument));
            }
            if (runtimeGenericColumnTypeProvider.isAssignableFrom(argument.getClass())) {
                return queryGenerator.quoteIdentifier(runtimeGenericColumnTypeProvider.cast(argument).getName());
            }
            throw new IllegalArgumentException(
                    String.format("Only objects of type '%s' and '%s' as arguments accepted",
                            runtimeGenericTypeProvider.getSimpleName(), Column.class.getSimpleName()));
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
}
