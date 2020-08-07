package bayern.steinbrecher.dbConnector.utility;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a {@link HashMap} where no entry can be put manually. All entries are lazily generated when trying to
 * access them using {@link HashMap#get(Object)}.
 *
 * @author Stefan Huber
 * @param <K> The type of the keys.
 * @param <V> The type of the associated valued.
 * @since 0.1
 */
public class PopulatingMap<K, V> extends HashMap<K, V> {

    private final Function<K, V> populator;

    /**
     * Creates a {@link PopulatingMap} whose entries are generated automatically when trying to access them.
     *
     * @param populator The function to use for populating the map.
     * @since 0.1
     */
    public PopulatingMap(Function<K, V> populator) {
        this.populator = populator;
    }

    /**
     * @since 0.1
     */
    @Override
    @SuppressWarnings({"element-type-mismatch", "unchecked"})
    public V get(@NotNull Object key) {
        /*
         * NOTE Use "if containsKey(...)" instead of putIfAbsent(...) since it is lacking lazy evaluation for the second
         * argument.
         */
        if (!containsKey(key)) {
            K keyK = (K) key;
            super.put(keyK, populator.apply(keyK));
        }
        return super.get(key);
    }

    /**
     * Unsupported operation.
     *
     * @param key ignored.
     * @param defaultValue ignored.
     * @return Throws {@link UnsupportedOperationException} always.
     * @throws UnsupportedOperationException
     * @since 0.1
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        throw new UnsupportedOperationException("This map can not be changed manually.");
    }

    /**
     * Unsupported operation.
     *
     * @param key ignored.
     * @param value ignored.
     * @return Throws {@link UnsupportedOperationException} always.
     * @throws UnsupportedOperationException
     * @since 0.1
     */
    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("This map can not be changed manually.");
    }

    /**
     * Unsupported operation.
     *
     * @throws UnsupportedOperationException
     * @since 0.1
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("This map can not be changed manually.");
    }

    /**
     * Unsupported operation.
     *
     * @param key ignored.
     * @param value ignored.
     * @return Throws {@link UnsupportedOperationException} always.
     * @throws UnsupportedOperationException
     * @since 0.1
     */
    @Override
    public V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException("This map can not be changed manually.");
    }
}
