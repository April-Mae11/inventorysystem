package com.nva.printing.inventory.roles;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TransactionHistoryController implements Initializable {
    @FXML private TableView<com.nva.printing.inventory.TransactionLogger.TransactionRecord> transactionTable;
    @FXML private TableColumn<com.nva.printing.inventory.TransactionLogger.TransactionRecord, String> dateColumn;
    @FXML private TableColumn<com.nva.printing.inventory.TransactionLogger.TransactionRecord, String> itemColumn;
    @FXML private TableColumn<com.nva.printing.inventory.TransactionLogger.TransactionRecord, String> typeColumn;
    @FXML private TableColumn<com.nva.printing.inventory.TransactionLogger.TransactionRecord, String> quantityColumn;
    @FXML private TableColumn<com.nva.printing.inventory.TransactionLogger.TransactionRecord, String> userColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"))));
        itemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        // Map certain internal types to friendlier display labels
        typeColumn.setCellValueFactory(cellData -> {
            String t = cellData.getValue().getType();
            if (t == null) return new SimpleStringProperty("");
            if (t.equalsIgnoreCase("SALE")) return new SimpleStringProperty("ITEM SOLD");
            return new SimpleStringProperty(t);
        });
        quantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));
        // Show user but strip any appended transaction markers like [Tx:...] or [ERef:...]
        userColumn.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().getUser();
            if (raw == null) return new SimpleStringProperty("");
            try {
                // remove bracketed Tx and ERef markers and trim
                String cleaned = raw.replaceAll("\\[Tx:.*?\\]", "").replaceAll("\\[ERef:.*?\\]", "").trim();
                return new SimpleStringProperty(cleaned);
            } catch (Exception e) {
                return new SimpleStringProperty(raw);
            }
        });

        // Filter out DELETE transactions and show newest first by wrapping in a SortedList.
        javafx.collections.ObservableList<com.nva.printing.inventory.TransactionLogger.TransactionRecord> backing = com.nva.printing.inventory.TransactionLogger.getInstance().getRecords();

        // Build a filtered view (exclude DELETE) backed by the live list so additions are reflected.
        javafx.collections.transformation.FilteredList<com.nva.printing.inventory.TransactionLogger.TransactionRecord> filtered =
                new javafx.collections.transformation.FilteredList<>(backing, r -> r == null || !"DELETE".equalsIgnoreCase(r.getType()));

        // SortedList to order by date descending (newest first). If date is null, treat as older.
        javafx.collections.transformation.SortedList<com.nva.printing.inventory.TransactionLogger.TransactionRecord> sorted =
                new javafx.collections.transformation.SortedList<>(filtered, (a, b) -> {
                    if (a == null && b == null) return 0;
                    if (a == null) return 1;
                    if (b == null) return -1;
                    if (a.getDate() != null && b.getDate() != null) {
                        return b.getDate().compareTo(a.getDate());
                    } else if (a.getDate() == null && b.getDate() != null) {
                        return 1;
                    } else if (a.getDate() != null && b.getDate() == null) {
                        return -1;
                    }
                    return 0;
                });

        transactionTable.setItems(sorted);
    }

    @FXML
    private void handleBack() {
        if (transactionTable != null && transactionTable.getScene() != null) {
            javafx.stage.Window w = transactionTable.getScene().getWindow();
            if (w instanceof javafx.stage.Stage) {
                ((javafx.stage.Stage) w).close();
            }
        }
    }

    @FXML
    private void handleClearHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "This will permanently clear all transaction history. Continue?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        confirm.setHeaderText(null);
        java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.YES) return;

        try {
            com.nva.printing.inventory.TransactionLogger.getInstance().clearAllRecords();
            transactionTable.getItems().clear();
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Transaction history cleared.", javafx.scene.control.ButtonType.OK);
            ok.setHeaderText(null);
            ok.showAndWait();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to clear transaction history: " + e.getMessage(), javafx.scene.control.ButtonType.OK);
            err.setHeaderText(null);
            err.showAndWait();
        }
    }

    // TransactionRecord now comes from TransactionLogger
}
