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

    private void bindTextToDate() {
        datePicker = null;
        textProperty().bind(
                Bindings.createStringBinding(
                        () -> CONVERTER.toString(getSelectedProperty().getValue()), getSelectedProperty()));
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
            datePicker = new DatePicker();
            datePicker.valueProperty().bindBidirectional(getSelectedProperty());
            datePicker.setOnAction(aevt -> commitEdit((C) datePicker.getValue()));
            textProperty().unbind();
            setText(null);
            setGraphic(datePicker);
        }
    }

    @Override
    public void commitEdit(C newValue) {
        super.commitEdit(newValue);
        bindTextToDate();
        setGraphic(null);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        bindTextToDate();
        setGraphic(null);
    }

    @Override
    protected void updateItem(C item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty && !isEditing()) {
            bindTextToDate();
        }
    }
}
