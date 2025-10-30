package com.nva.printing.inventory.roles;

import com.nva.printing.inventory.ArchiveItem;
import com.nva.printing.inventory.ArchiveManager;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import com.nva.printing.inventory.DataManager;
import com.nva.printing.inventory.InventoryItem;
import com.nva.printing.inventory.TransactionLogger;
import com.nva.printing.inventory.AuthManager;

public class ArchiveViewController implements Initializable {
    @FXML private TableView<ArchiveItem> archiveTable;
    @FXML private TableColumn<ArchiveItem, Integer> idColumn;
    @FXML private TableColumn<ArchiveItem, String> nameColumn;
    @FXML private TableColumn<ArchiveItem, String> categoryColumn;
    @FXML private TableColumn<ArchiveItem, String> typeColumn;
    @FXML private TableColumn<ArchiveItem, Integer> quantityUsedColumn;
    @FXML private TableColumn<ArchiveItem, Double> unitPriceColumn;
    @FXML private TableColumn<ArchiveItem, String> supplierColumn;
    @FXML private TableColumn<ArchiveItem, String> dateUsedColumn;
    @FXML private TableColumn<ArchiveItem, String> usedByColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("originalItemName"));
    categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
    // Display a friendlier label: map stored "Used Item" to "Item Sold" for clarity
    typeColumn.setCellValueFactory(cellData -> {
        String t = cellData.getValue().getType();
        if (t == null) return new javafx.beans.property.SimpleStringProperty("");
        if (t.equalsIgnoreCase("Used Item")) return new javafx.beans.property.SimpleStringProperty("Item Sold");
        return new javafx.beans.property.SimpleStringProperty(t);
    });
        quantityUsedColumn.setCellValueFactory(new PropertyValueFactory<>("quantityUsed"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        dateUsedColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateUsed() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateUsed().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"))
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        usedByColumn.setCellValueFactory(new PropertyValueFactory<>("usedBy"));

        ObservableList<ArchiveItem> archiveItems = ArchiveManager.getInstance().getArchiveItems();

        // Display newest archive entries first by wrapping the backing list in a SortedList.
        // We sort primarily by dateUsed descending, falling back to id descending when dates are null or equal.
        SortedList<ArchiveItem> sorted = new SortedList<>(archiveItems, (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            if (a.getDateUsed() != null && b.getDateUsed() != null) {
                int cmp = b.getDateUsed().compareTo(a.getDateUsed());
                if (cmp != 0) return cmp;
            } else if (a.getDateUsed() == null && b.getDateUsed() != null) {
                return 1; // put null (older/unknown) last
            } else if (a.getDateUsed() != null && b.getDateUsed() == null) {
                return -1;
            }
            // fallback: newest id first
            return Integer.compare(b.getId(), a.getId());
        });

        archiveTable.setItems(sorted);
    }

    @FXML
    private void handleRetrieveSelected() {
        ArchiveItem selected = archiveTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Please select an archive item to retrieve.", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restore this archive entry back to inventory?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        DataManager dm = DataManager.getInstance();
        // Try to find existing item by name (case-insensitive)
        InventoryItem existing = dm.getAllItems().stream()
                .filter(i -> i.getName().equalsIgnoreCase(selected.getOriginalItemName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            // Add back the quantityUsed
            existing.addStock(selected.getQuantityUsed());
            dm.updateItem(existing);
        } else {
            // Create a new inventory item with basic info from archive
            InventoryItem newItem = new InventoryItem(
                    selected.getOriginalItemName(),
                    selected.getCategory(),
                    selected.getReason() != null ? selected.getReason() : "Restored from archive",
                    selected.getQuantityUsed(),
                    10, // default min stock
                    selected.getUnitPrice(),
                    selected.getSupplier()
            );
            dm.addItem(newItem);
        }

        // Remove archive entry and persist
        ArchiveManager.getInstance().getArchiveItems().remove(selected);
        ArchiveManager.getInstance().saveArchiveData();

        // Log the retrieval in transaction history with the acting user
        try {
            String user = AuthManager.getInstance().getCurrentUser() != null ?
                    AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
            TransactionLogger.getInstance().log(selected.getOriginalItemName(), "RETRIEVED", selected.getQuantityUsed(), user);
        } catch (Exception e) {
            // Don't block UI on logging failure; keep silent (logger will persist when possible)
        }

        Alert ok = new Alert(Alert.AlertType.INFORMATION, "Archive entry restored to inventory.", ButtonType.OK);
        ok.setHeaderText(null);
        ok.showAndWait();
    }

    @FXML
    private void handleBack() {
        if (archiveTable != null && archiveTable.getScene() != null) {
            java.awt.EventQueue.invokeLater(() -> {
                javafx.application.Platform.runLater(() -> {
                    javafx.stage.Window w = archiveTable.getScene().getWindow();
                    if (w instanceof javafx.stage.Stage) {
                        ((javafx.stage.Stage) w).close();
                    }
                });
            });
        }
    }

    @FXML
    private void handleClearArchive() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "This will permanently clear the archive. Continue?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        try {
            ArchiveManager.getInstance().clearArchive();
            archiveTable.getItems().clear();
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Archive cleared.", ButtonType.OK);
            ok.setHeaderText(null);
            ok.showAndWait();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to clear archive: " + e.getMessage(), ButtonType.OK);
            err.setHeaderText(null);
            err.showAndWait();
        }
    }
}
