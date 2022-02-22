package bayern.steinbrecher.dbConnector.utility;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.util.Callback;
import javafx.util.converter.LocalDateStringConverter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

/**
 * @author Stefan Huber
 * @since 0.16
 */
public class DatePickerTableCell<E, C> extends TableCell<E, C> {
    private static final LocalDateStringConverter CONVERTER = new LocalDateStringConverter();
    private final Callback<Integer, Property<LocalDate>> getSelectedPropertyCallback;
    private DatePicker datePicker;

    public DatePickerTableCell(@NotNull Callback<Integer, Property<LocalDate>> getSelectedPropertyCallback) {
        this.getSelectedPropertyCallback = getSelectedPropertyCallback;
    }

    private Property<LocalDate> getSelectedProperty() {
        return getSelectedPropertyCallback.call(getIndex());
    }

    private void bindCellToSelectedProperty() {
        datePicker = null;
        textProperty().bind(
                Bindings.createStringBinding(
                        () -> CONVERTER.toString(getSelectedProperty().getValue()), getSelectedProperty()));
        setGraphic(null);
    }

    private void unbindCellFromSelectedProperty() {
        textProperty().unbind();
        setText(null);
    }

    @Override
    public void startEdit() {
        if (isEditing()) {
            // Editing already started
            return;
        }

        super.startEdit();

        // Editing started successfully
        if (isEditing()) {
            unbindCellFromSelectedProperty();
            datePicker = new DatePicker();
            datePicker.valueProperty().bindBidirectional(getSelectedProperty());
            datePicker.setOnAction(aevt -> commitEdit((C) datePicker.getValue()));
            setGraphic(datePicker);
        }
    }

    @Override
    public void commitEdit(C newValue) {
        super.commitEdit(newValue);
        bindCellToSelectedProperty();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        bindCellToSelectedProperty();
    }

    @Override
    protected void updateItem(C item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || isEditing()) {
            unbindCellFromSelectedProperty();
        } else {
            bindCellToSelectedProperty();
        }
    }
}
