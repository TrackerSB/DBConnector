package bayern.steinbrecher.dbConnector.utility;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

/**
 * @author Stefan Huber
 * @since 0.16
 */
public class DatePickerTableCell<E, C> extends TableCell<E, C> {
    private final Callback<Integer, Property<LocalDate>> getSelectedPropertyCallback;

    public DatePickerTableCell(@NotNull Callback<Integer, Property<LocalDate>> getSelectedPropertyCallback) {
        this.getSelectedPropertyCallback = getSelectedPropertyCallback;
    }

    private Property<LocalDate> getSelectedProperty() {
        return getSelectedPropertyCallback.call(getIndex());
    }

    @Override
    protected void updateItem(C item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            var datePicker = new DatePicker();
            datePicker.valueProperty().bindBidirectional(getSelectedProperty());
            datePicker.disableProperty().bind(
                    Bindings.not(
                            getTableView().editableProperty()
                                    .and(getTableColumn().editableProperty())
                                    .and(editableProperty())
                    ));
            setGraphic(datePicker);
        }
    }
}
