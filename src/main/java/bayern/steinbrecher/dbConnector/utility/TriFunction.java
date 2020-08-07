package bayern.steinbrecher.dbConnector.utility;

/**
 * Represents a {@link java.util.function.BiFunction} but accepting three different input parameter.
 *
 * @param <T> The type of the first argument.
 * @param <U> The type of the second argument.
 * @param <V> The type of the third argument.
 * @param <R> The type of the output.
 * @author Stefan Huber
 * @see java.util.function.BiFunction
 * @since 0.1
 */
@FunctionalInterface
@SuppressWarnings("PMD.ShortVariable")
public interface TriFunction<T, U, V, R> {

    /**
     * Performs the given operation on the passed arguments.
     *
     * @param t The first input argument.
     * @param u The second input argument.
     * @param v The third input argument.
     * @return The resulting object.
     */
    R accept(T t, U u, V v);
}
