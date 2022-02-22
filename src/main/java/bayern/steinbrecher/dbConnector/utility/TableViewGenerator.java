package bayern.steinbrecher.dbConnector.utility;

import bayern.steinbrecher.dbConnector.DBConnection;
import bayern.steinbrecher.dbConnector.query.QueryFailedException;
import bayern.steinbrecher.dbConnector.scheme.ColumnParser;
import bayern.steinbrecher.dbConnector.scheme.ColumnPattern;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
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

    @NotNull
    private static <E, C> Optional<TableCell<E, C>> createTableCell(
            @NotNull DBConnection.Column<E, C> dbColumn, @NotNull TableColumn<E, C> tableColumn) {
        if (dbColumn.pattern().isPresent()) {
            if (Boolean.class.isAssignableFrom(dbColumn.columnType())) {
                return Optional.of(new CheckBoxTableCell<>(
                        index -> (ObservableValue<Boolean>) tableColumn.getCellObservableValue(index)));
            }
            if (LocalDate.class.isAssignableFrom(dbColumn.columnType())) {
                return Optional.of(new DatePickerTableCell<>(
                        index -> (Property<LocalDate>) tableColumn.getCellObservableValue(index)));
            }
            if (String.class.isAssignableFrom(dbColumn.columnType())) {
                return Optional.of(new TextFieldTableCell<>(new StringConverter<>() {
                    @Override
                    public String toString(C object) {
                        return (String) object;
                    }

                    @Override
                    public C fromString(String string) {
                        return (C) string;
                    }
                }));
            }
        }

        // Either type of column is not supported or the column is not part of the scheme
        return Optional.empty();
    }

    @NotNull
    private static <E, C> ChangeListener<C> createItemChangeListener(
            @NotNull DBConnection.Column<E, C> dbColumn, @NotNull TableCell<E, C> cell) {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends C> obs, C previousCellValue, C currentCellValue) {
                Optional<ColumnPattern<C, E>> optPattern = dbColumn.pattern();
                assert optPattern.isPresent() : "If there is no pattern associated with the "
                        + "column there is no item associated which could be updated";

                ObservableList<E> resolvedItems = cell.getTableView().getItems();
                int resolvedRowIndex = cell.getIndex();

                // Resolve items and index if they are wrapped by SortedList, FilteredList etc.
                while (resolvedItems instanceof TransformationList<?, ?> resolvedTransList) {
                    resolvedRowIndex = resolvedTransList.getSourceIndex(resolvedRowIndex);
                    resolvedItems = (ObservableList<E>) resolvedTransList.getSource();
                }

                E currentItem = resolvedItems.get(resolvedRowIndex);
                /* NOTE 2022-02-05: If editing was cancelled on a cell before this listener may be attached multiple
                 * times and thus called multiple times. Avoid setting the current cell value multiple times.
                 */
                C currentItemValue = optPattern.get().getValue(currentItem, dbColumn.name());
                if (currentItemValue != currentCellValue) {
                    E updatedItem = optPattern.get().combine(currentItem, dbColumn.name(),
                            String.valueOf(currentCellValue));
                    resolvedItems.set(resolvedRowIndex, updatedItem);
                }
                cell.itemProperty().removeListener(this);
            }
        };
    }

    /**
     * Return a column which can be used for a {@link TableView} that extracts a certain value of
     * items of the given type and parses it with this {@link ColumnParser}.
     *
     * @return A {@link TableColumn} with a cell value factory but no name.
     */
    @NotNull
    private static <E, C> TableColumn<E, C> createTableViewColumn(@NotNull DBConnection.Column<E, C> dbColumn) {
        var warnedAboutPatternlessColumn = new AtomicBoolean(false);

        TableColumn<E, C> viewColumn = new TableColumn<>();
        viewColumn.setCellValueFactory(features -> {
            if (dbColumn.pattern().isPresent()) {
                C cellValue = dbColumn.pattern().get().getValue(features.getValue(), dbColumn.name());

                // NOTE 2022-01-09: Required to ensure CheckBoxTableCell shows value
                if (Boolean.class.isAssignableFrom(dbColumn.columnType())) {
                    return (ObservableValue<C>) new SimpleBooleanProperty((Boolean) cellValue);
                }
                return new SimpleObjectProperty<>(cellValue);
            }

            String columnName = features.getTableColumn().getText();
            if (!warnedAboutPatternlessColumn.get()) {
                LOGGER.log(Level.WARNING, String.format(
                        "There's no pattern for %s. The column will be empty.", columnName));
                warnedAboutPatternlessColumn.set(true);
            }

            return null;
        });
        viewColumn.setCellFactory(tableColumn -> {
            Optional<TableCell<E, C>> optCell = createTableCell(dbColumn, tableColumn);

            if (optCell.isPresent()) {
                TableCell<E, C> cell = optCell.get();
                tableColumn.setEditable(true);

                ChangeListener<C> itemChangeListener = createItemChangeListener(dbColumn, cell);

                cell.editingProperty().addListener((obs, wasEditing, isEditing) -> {
                    if (isEditing) {
                        cell.itemProperty().addListener(itemChangeListener);
                    }
                    // FIXME 2022-01-05: Remove listener on edit cancellation
                });
                return cell;
            }

            tableColumn.setEditable(false);
            return new TextFieldTableCell<>();
        });
        return viewColumn;
    }

    /**
     * Return an empty {@link TableView} that has columns for all columns of the given table and allows visualizing as
     * well as editing any items of type {@code E} that are set to it.
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
