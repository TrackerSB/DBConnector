package bayern.steinbrecher.database.connection;

import bayern.steinbrecher.database.scheme.ColumnPattern;
import bayern.steinbrecher.database.scheme.TableScheme;

/**
 * Represents a concrete column that actually exists in an existing database. In contrast {@link ColumnPattern} only
 * represents patterns of column names in a {@link TableScheme}.
 *
 * @author Stefan Huber
 * @param <T> The type of Java objects this column represents.
 * @since 0.1
 */
public class Column<T> {

    private final String name;
    private final Class<T> columnType;

    /**
     * Creates a concrete column that exists in an existing database.
     *
     * @param name The name of the column.
     * @param columnType The class of Java objects this column represents. Since this class represents existing columns
     * this type can only be determined at runtime.
     * @since 0.1
     */
    // NOTE Only DBConnection and its subclasses should be allowed to create such column objects.
    Column(String name, Class<T> columnType) {
        this.name = name;
        this.columnType = columnType;
    }

    /**
     * @since 0.1
     */
    public String getName() {
        return name;
    }

    /**
     * @since 0.1
     */
    public Class<T> getColumnType() {
        return columnType;
    }
}
