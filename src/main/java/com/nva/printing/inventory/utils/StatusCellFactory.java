package com.nva.printing.inventory.utils;

import com.nva.printing.inventory.Status;
import com.nva.printing.inventory.InventoryItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class StatusCellFactory implements Callback<TableColumn<InventoryItem, String>, TableCell<InventoryItem, String>> {

    @Override
    public TableCell<InventoryItem, String> call(TableColumn<InventoryItem, String> param) {
        return new TableCell<InventoryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("status-out", "status-low", "status-normal");
                } else {
                    setText(item);
                    Status status = Status.fromItem(java.util.Optional.of(item));
                    // Clean existing status classes, then add the correct one
                    getStyleClass().removeAll("status-out", "status-low", "status-normal");
                    getStyleClass().add(status.getCssClass());
                }
            }
        };
    }
}
