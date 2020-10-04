package bayern.steinbrecher.dbConnector.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Represents a wrapper for a {@link String} which ignores at every point the case of characters of the wrapped
 * keyword. This includes {@link Object#equals(Object)}, {@link Comparable#compareTo(Object)},
 * etc. NOTE: Specified parameters are ignored.
 */
class SQLTypeKeyword implements Comparable<SQLTypeKeyword> {

    private final String keyword; //NOPMD - It is access over getSqlTypeKeyword()
    private final String parameterSuffix; //NOPMD - It is access over getSqlTypeKeyword()

    /**
     * Creates a new {@link SQLTypeKeyword}.
     *
     * @param keyword   The keyword is always saved and handled in uppercase. This keyword must represent the type
     *                  saved in {@code information_schema.columns}. Be careful with aliases.
     * @param parameter Additional parameters related to the keyword. These are ignored concerning
     *                  {@link Object#equals(Object)}, {@link Comparable#compareTo(Object)},
     *                  etc.
     */
    public SQLTypeKeyword(@NotNull String keyword, @NotNull Object... parameter) {
        this.keyword = keyword.toUpperCase(Locale.ROOT);
        this.parameterSuffix = parameter.length > 0
                ? Arrays.stream(parameter)
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "(", ")"))
                : "";
    }

    /**
     * @since 0.1
     */
    @Override
    public boolean equals(@Nullable Object other) {
        boolean areEqual;
        if (other instanceof SQLTypeKeyword) {
            areEqual = keyword.equalsIgnoreCase(((SQLTypeKeyword) other).keyword);
        } else {
            areEqual = false;
        }
        return areEqual;
    }

    /**
     * @since 0.1
     */
    @Override
    public int hashCode() {
        return keyword.hashCode();
    }

    /**
     * @since 0.1
     */
    @Override
    public int compareTo(@NotNull SQLTypeKeyword other) {
        return keyword.compareToIgnoreCase(other.keyword);
    }

    /**
     * Returns the SQL type keyword in upper case and appends a comma separated list of parameters in parenthesis.
     *
     * @return The SQL type keyword in upper case and appends a comma separated list of parameters in parenthesis.
     * @since 0.1
     */
    @NotNull
    public String getSqlTypeKeyword() {
        return keyword + parameterSuffix;
    }
}
