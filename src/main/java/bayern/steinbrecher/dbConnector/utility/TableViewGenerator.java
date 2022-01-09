package bayern.steinbrecher.dbConnector.utility;

import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.scheme.ColumnParser;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stefan Huber
 * @since 0.16
 */
public final class TableViewGenerator {
    private static final Logger LOGGER = Logger.getLogger(TableViewGenerator.class.getName());

    private TableViewGenerator() {
        throw new UnsupportedOperationException("The construction of instances is prohibited");
    }

    /**
     * Return a column which can be used for a {@link TableView} that extracts a certain value of
     * items of the given type and parses it with this {@link ColumnParser}.
     *
     * @return A {@link TableColumn} with a cell value factory but no name.
     */
    @NotNull
    public static <E, C> TableColumn<E, C> createTableViewColumn(@NotNull DBConnection.Column<E, C> column) {
        AtomicBoolean warnedAboutPatternlessColumn = new AtomicBoolean(false);

        TableColumn<E, C> viewColumn = new TableColumn<>();
        viewColumn.setCellValueFactory(features -> {
            if (column.pattern().isPresent()) {
                C value = column.pattern().get().getValue(features.getValue(), column.name());

                // NOTE 2022-01-09: Required for CheckBoxTableCell
                if (Boolean.class.isAssignableFrom(column.columnType())) {
                    return (ObservableValue<C>) new SimpleBooleanProperty((Boolean) value);
                }
                return new SimpleObjectProperty<>(value);
            }

            String columnName = features.getTableColumn().getText();
            if (!warnedAboutPatternlessColumn.get()) {
                LOGGER.log(Level.WARNING, String.format(
                        "There's no pattern for %s. The column will be empty.", columnName));
                warnedAboutPatternlessColumn.set(true);
            }
            return null;
        });
        viewColumn.setCellFactory(c -> {
            if (column.pattern().isPresent()) {
                c.setEditable(true);
                if (Boolean.class.isAssignableFrom(column.columnType())) {
                    return new CheckBoxTableCell<>(
                            index -> (ObservableValue<Boolean>) c.getCellObservableValue(index));
                }
                if (LocalDate.class.isAssignableFrom(column.columnType())) {
                    return new DatePickerTableCell<>(
                            index -> (Property<LocalDate>) c.getCellObservableValue(index));
                }
            }

            // Either the type of the column is not supported or it is not part of the scheme
            c.setEditable(false);
            return new TextFieldTableCell<>();
        });
        return viewColumn;
    }

    /**
     * Return an empty {@link TableView} showing all columns of the given table and allowing to visualize any objects of
     * type {@code E}.
     */
    @NotNull
    public static <E> TableView<E> createTableView(@NotNull DBConnection.Table<?, E> table)
            throws QueryFailedException {
        TableView<E> tableView = new TableView<>();
        table.getColumns()
                .stream()
                .sorted(Comparator.comparingInt(DBConnection.Column::index))
                .forEachOrdered(c -> {
                    TableColumn<E, ?> tableViewColumn = createTableViewColumn(c);
                    tableViewColumn.setText(c.name());
                    tableView.getColumns()
                            .add(tableViewColumn);
                });
        return tableView;
    }
}
