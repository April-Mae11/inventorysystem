package com.nva.printing.inventory.roles;

import com.nva.printing.inventory.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import java.time.ZoneId;
import java.util.Date;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import javafx.scene.input.KeyCombination;

/**
 * Manager Controller - Demonstrates OOP Concepts
 * 
 * INHERITANCE: Implements Initializable interface from JavaFX framework
 * ENCAPSULATION: Private fields with controlled access through FXML injection
 * ABSTRACTION: Hides complex UI management behind simple event handler methods
 * POLYMORPHISM: Overrides initialize() method and uses callback interfaces
 * 
 * This controller manages the manager interface with full system access and
 * demonstrates how OOP principles are applied in role-based applications.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Inheritance: Extends JavaFX framework capabilities through interface implementation
 * - Encapsulation: Private fields and methods to control access to UI components
 * - Abstraction: Clean public interface hiding complex UI management
 * - Polymorphism: Method overriding and interface implementations
 * 
 * @author NVA Printing Services Development Team
 */
public class ManagerController implements Initializable {
    @FXML
    private TableColumn<InventoryItem, Integer> stockInColumn;
    @FXML
    private TableColumn<InventoryItem, Integer> stockOutColumn;

    @FXML
    private void handleStockIn() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("No Selection", "Please select an item to stock in.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Stock In");
        dialog.setHeaderText("Stock In Item");
        dialog.setContentText("Enter amount to stock in:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int amount = Integer.parseInt(result.get());
                if (amount <= 0) {
                    showAlert("Invalid Input", "Please enter a positive number.");
                    return;
                }
                selectedItem.setQuantity(selectedItem.getQuantity() + amount);
                selectedItem.setStockIn(selectedItem.getStockIn() + amount);
                DataManager.getInstance().updateItem(selectedItem);
                // Force table update for instant status change
                inventoryTable.refresh();
                // Log stock in transaction
                String user = AuthManager.getInstance().getCurrentUser() != null ?
                    AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
                TransactionLogger.getInstance().log(selectedItem.getName(), "STOCK IN", amount, user);
                showAlert("Stock In", "Stock increased by " + amount + ".");
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter a valid number.");
            }
        }
    }

    @FXML
    private void handleStockOut() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("No Selection", "Please select an item to stock out.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Stock Out");
        dialog.setHeaderText("Stock Out Item");
        dialog.setContentText("Enter amount to stock out:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int amount = Integer.parseInt(result.get());
                if (amount <= 0) {
                    showAlert("Invalid Input", "Please enter a positive number.");
                    return;
                }
                if (selectedItem.getQuantity() < amount) {
                    showAlert("Stock Out", "Not enough stock to remove that amount.");
                    return;
                }
                selectedItem.setQuantity(selectedItem.getQuantity() - amount);
                selectedItem.setStockOut(selectedItem.getStockOut() + amount);
                DataManager.getInstance().updateItem(selectedItem);
                // Force table update for out of stock
                inventoryTable.refresh();
                // Log stock out transaction
                String user = AuthManager.getInstance().getCurrentUser() != null ?
                    AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
                TransactionLogger.getInstance().log(selectedItem.getName(), "STOCK OUT", amount, user);
                showAlert("Stock Out", "Stock decreased by " + amount + ".");
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter a valid number.");
            }
        }
    }

    @FXML
    private void handleViewArchive() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/archive-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Archive - Used/Deleted Items");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(true);
            // Open archive maximized so manager sees full workspace immediately
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open archive view: " + e.getMessage());
        }
    }


    @FXML
    private void handleEndOfDay() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("End of Day");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to perform End of Day? This will save data and log the action.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Reset daily counters (stockIn / stockOut) for all items
            for (InventoryItem item : dataManager.getInventoryItems()) {
                item.setStockIn(0);
                item.setStockOut(0);
            }
            // Persist changes once and refresh UI
            dataManager.saveData();
            inventoryTable.refresh();
            // Log end of day event
            String user = AuthManager.getInstance().getCurrentUser() != null ?
                AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
            TransactionLogger.getInstance().log("END_OF_DAY", "RESET STOCK IN/OUT COUNTERS", 0, user);
            showAlert("End of Day", "Stock In/Out counters reset to 0 for all items and data saved.");
        }
    }

    @FXML
    private MenuItem undoEndOfDayMenuItem;

    @FXML
    private void handleUndoEndOfDay() {
        // Confirm undo
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Undo End of Day");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to undo the last End of Day? This will restore data from the most recent backup.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean ok = DataManager.getInstance().restoreFromBackup();
            if (ok) {
                // Refresh UI data
                dataManager.loadData();

                // Recreate filtered and sorted views and bind comparator to the table so sorting persists
                javafx.collections.transformation.FilteredList<InventoryItem> refreshedFiltered = new javafx.collections.transformation.FilteredList<>(dataManager.getInventoryItems(), p -> true);
                javafx.collections.transformation.SortedList<InventoryItem> refreshedSorted = new javafx.collections.transformation.SortedList<>(refreshedFiltered);
                // Bind the sorted list comparator to the table's comparator so user sorting remains effective
                refreshedSorted.comparatorProperty().bind(inventoryTable.comparatorProperty());
                inventoryTable.setItems(refreshedSorted);

                // Restore default alphabetical sort by name (A → Z)
                inventoryTable.getSortOrder().clear();
                inventoryTable.getSortOrder().add(nameColumn);
                nameColumn.setSortType(TableColumn.SortType.ASCENDING);

                updateCategoryFilter();
                updateStatistics();
                // Notify the user that restore succeeded (blocking alert)
                showAlert("Success", "End of Day undone — data restored from backup.");
            } else {
                showAlert("Error", "Could not restore data from backup. No backup available or restore failed.");
            }
        }
    }

    @FXML
    private void handleArchiveSelected() {
        var selectedItems = inventoryTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showAlert("No Selection", "Please select item(s) to archive.");
            return;
        }

        String message = selectedItems.size() == 1 ?
            "Are you sure you want to archive the selected item?" :
            "Are you sure you want to archive " + selectedItems.size() + " selected items?";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Archive");
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Archive Reason");
            reasonDialog.setHeaderText("Provide a reason for archiving the selected item(s)");
            reasonDialog.setContentText("Reason:");
            Optional<String> reasonRes = reasonDialog.showAndWait();
            if (reasonRes.isEmpty() || reasonRes.get().trim().isEmpty()) {
                showAlert("Reason Required", "You must provide a reason for archiving.");
                return;
            }
            String reason = reasonRes.get().trim();

            var itemsToArchive = FXCollections.observableArrayList(selectedItems);
            for (InventoryItem item : itemsToArchive) {
                DataManager.getInstance().removeItem(item, reason);
            }
            updateCategoryFilter();
        }
    }

    @FXML
    private void handleViewTransactions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/transaction-history-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Transaction History");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(true);
            // Open transaction history maximized for full workspace
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open transaction history: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard-overview.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Dashboard Overview");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(true);
            stage.setMaximized(false);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open dashboard overview: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangeUser() {
        // Show user selection dialog
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Switch User");
        dialog.setHeaderText("Select a user to switch to:");

        ComboBox<User> userCombo = new ComboBox<>();
        userCombo.setPrefWidth(300);
        // Only show active users except current
        User currentUser = AuthManager.getInstance().getCurrentUser();
        userCombo.getItems().addAll(
            AuthManager.getInstance().getUsers().filtered(u -> u.isActive() && !u.equals(currentUser) && u.getRole() == com.nva.printing.inventory.User.UserRole.CASHIER)
        );
        userCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getFullName() + " (" + user.getRole().getDisplayName() + ")");
            }
        });
        userCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getFullName() + " (" + user.getRole().getDisplayName() + ")");
            }
        });

        dialog.getDialogPane().setContent(userCombo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return userCombo.getValue();
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            User selectedUser = result.get();
            // Switch session to selected user
            AuthManager.getInstance().setCurrentUser(selectedUser);
            // Open corresponding dashboard
            try {
                String fxml;
                String title;
                switch (selectedUser.getRole()) {
                    case CASHIER:
                        fxml = "/cashier-view.fxml";
                        title = "Cashier Dashboard";
                        break;
                    default:
                        showAlert("Error", "Only Cashier role is allowed for switching.");
                        return;
                }
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle(title);
                stage.setScene(new Scene(root));
                stage.initModality(Modality.WINDOW_MODAL);
                stage.setResizable(true);
                // Open dashboard maximized so the user sees full workspace immediately
                stage.setMaximized(true);
                stage.show();
                // Close current window
                Stage currentStage = (Stage) changeUserButton.getScene().getWindow();
                currentStage.close();
            } catch (IOException e) {
                showAlert("Error", "Could not open dashboard: " + e.getMessage());
            }

            if (selectedUser.getRole() == com.nva.printing.inventory.User.UserRole.CASHIER) {
                com.nva.printing.inventory.roles.CashierController.previousManagerName = "MANAGER";
            } else {
                com.nva.printing.inventory.roles.CashierController.previousManagerName = null;
            }
        }
    }
    @FXML private Button stockInButton;
    @FXML private Button stockOutButton;
    @FXML private Button viewArchiveButton;
    @FXML private Button viewHistoryButton; // Now labeled as View Transactions in FXML
    @FXML private Button changeUserButton;
    
    // ENCAPSULATION: FXML-injected UI components with controlled access
    // These fields are automatically injected by JavaFX framework
    
    // Table and Column Components - ENCAPSULATION through private access
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, String> nameColumn;
    @FXML private TableColumn<InventoryItem, String> categoryColumn;
    @FXML private TableColumn<InventoryItem, String> descriptionColumn;
    @FXML private TableColumn<InventoryItem, Integer> quantityColumn;
    @FXML private TableColumn<InventoryItem, Integer> minStockColumn;
    @FXML private TableColumn<InventoryItem, Double> priceColumn;
    @FXML private TableColumn<InventoryItem, String> supplierColumn;
    @FXML private TableColumn<InventoryItem, String> lastUpdatedColumn;
    @FXML private TableColumn<InventoryItem, String> statusColumn;
    
    // Filter and Search Components - ENCAPSULATION
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private CheckBox lowStockFilter;
    @FXML private CheckBox outOfStockFilter;
    
    // Action Button Components - ENCAPSULATION
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button userManagementButton;
    @FXML private Button logoutButton;
    
    // Statistics Display Components - ENCAPSULATION
    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockItemsLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label totalQuantityLabel;
    @FXML private Label userInfoLabel;
    @FXML private MenuItem endOfDayMenuItem;
    
    // ENCAPSULATION: Private fields for internal state management
    private FilteredList<InventoryItem> filteredData;
    private DataManager dataManager; // COMPOSITION: Uses DataManager for data access
    private AuthManager authManager; // COMPOSITION: Uses AuthManager for authentication
    
    /**
     * POLYMORPHISM: Override Initializable.initialize()
     * ABSTRACTION: Hides complex initialization behind simple method call
     * This method is called automatically by JavaFX after FXML loading
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // COMPOSITION: Use DataManager and AuthManager singletons for data access
        dataManager = DataManager.getInstance();
        authManager = AuthManager.getInstance();
        
        // ABSTRACTION: Break down initialization into manageable methods
        setupTableColumns();
        setupFilters();
        setupButtons();
        // Setup accelerator for End of Day menu item (programmatically to avoid FXML parsing issues)
        try {
            if (endOfDayMenuItem != null) {
                endOfDayMenuItem.setAccelerator(KeyCombination.valueOf("Alt+E"));
                // Only enable if current user is manager
                endOfDayMenuItem.setDisable(!AuthManager.getInstance().hasManagerAccess());
            }
            if (undoEndOfDayMenuItem != null) {
                undoEndOfDayMenuItem.setAccelerator(KeyCombination.valueOf("Alt+U"));
                // Only enable if current user is manager
                undoEndOfDayMenuItem.setDisable(!AuthManager.getInstance().hasManagerAccess());
            }
        } catch (Exception e) {
            Logger.getLogger(ManagerController.class.getName()).log(Level.WARNING, "Could not set End of Day accelerator: " + e.getMessage(), e);
        }
        loadData();
        updateStatistics();
        updateUserInfo();
        
        // ABSTRACTION: Hide listener complexity behind lambda expression
        // Refresh statistics when data changes
        dataManager.getInventoryItems().addListener((javafx.collections.ListChangeListener<InventoryItem>) c -> {
            Platform.runLater(this::updateStatistics);
        });
    }
    
    /**
     * ENCAPSULATION: Private method to configure table columns
     * ABSTRACTION: Hides complex table setup behind simple method call
     * POLYMORPHISM: Uses anonymous classes and lambda expressions for callbacks
     */
    private void setupTableColumns() {
        // ABSTRACTION: PropertyValueFactory hides reflection complexity
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
    descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));
    priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
    supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
    stockInColumn.setCellValueFactory(new PropertyValueFactory<>("stockIn"));
    stockOutColumn.setCellValueFactory(new PropertyValueFactory<>("stockOut"));
        
        // ABSTRACTION: Custom cell value factory hiding date formatting complexity
        lastUpdatedColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLastUpdated() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getLastUpdated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"))
                );
            }
            return new SimpleStringProperty("");
        });
        
        // ABSTRACTION: Custom status column with business logic
        statusColumn.setCellValueFactory(cellData -> {
            InventoryItem item = cellData.getValue();
            if (item.getQuantity() == 0) {
                return new SimpleStringProperty("OUT OF STOCK");
            } else if (item.isLowStock()) {
                return new SimpleStringProperty("LOW STOCK");
            } else {
                return new SimpleStringProperty("NORMAL STOCK");
            }
        });

        // Stack level column removed

        // POLYMORPHISM: Anonymous class implementing Callback interface
        // ABSTRACTION: Hide complex cell rendering logic
        // Use shared StatusCellFactory to centralize rendering and styling
        statusColumn.setCellFactory(new com.nva.printing.inventory.utils.StatusCellFactory());
        
        // POLYMORPHISM: Lambda expression implementing callback interface
        // ABSTRACTION: Hide currency formatting complexity
        priceColumn.setCellFactory(column -> new TableCell<InventoryItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%.2f", price));
                }
            }
        });
        
        // ABSTRACTION: Hide column width configuration
        nameColumn.setPrefWidth(150);
        categoryColumn.setPrefWidth(120);
        descriptionColumn.setPrefWidth(200);
        quantityColumn.setPrefWidth(80);
        minStockColumn.setPrefWidth(90);
        priceColumn.setPrefWidth(100);
        supplierColumn.setPrefWidth(150);
        lastUpdatedColumn.setPrefWidth(130);
        statusColumn.setPrefWidth(100);
    // stack level column removed
        
        // Allow multiple selection for batch operations
        inventoryTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
    
    // Stack level logic removed
    
    private void setupFilters() {
        // Initialize filtered list
        filteredData = new FilteredList<>(dataManager.getInventoryItems(), p -> true);
        
        // Setup search filter
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Setup category filter
        categoryFilter.setItems(FXCollections.observableArrayList("All Categories"));
        categoryFilter.setValue("All Categories");
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Stack level filter removed
        
        // Setup low stock filter
        lowStockFilter.selectedProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Setup out of stock filter
        if (outOfStockFilter != null) {
            outOfStockFilter.selectedProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
        
    // Create sorted list and apply a programmatic alphabetical comparator (A → Z)
    // We intentionally DO NOT bind the sorted list to the table comparator so the
    // name column can be forced to appear alphabetized without showing the
    // header sort arrow. This provides a clean A→Z visual while keeping other
    // UI behaviors stable.
    SortedList<InventoryItem> sortedData = new SortedList<>(filteredData);
    sortedData.setComparator(Comparator.comparing(
        item -> item.getName() == null ? "" : item.getName(),
        String.CASE_INSENSITIVE_ORDER
    ));

    // Disable the name column's sortability so the header arrow is not shown
    nameColumn.setSortable(false);

    inventoryTable.setItems(sortedData);
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
    String selectedCategory = categoryFilter.getValue();
    boolean showOnlyLowStock = lowStockFilter.isSelected();
    boolean showOnlyOutOfStock = outOfStockFilter != null && outOfStockFilter.isSelected();
        
        filteredData.setPredicate(item -> {
            // Search filter
            if (searchText != null && !searchText.isEmpty()) {
                if (!item.getName().toLowerCase().contains(searchText) &&
                    !item.getDescription().toLowerCase().contains(searchText) &&
                    !item.getSupplier().toLowerCase().contains(searchText)) {
                    return false;
                }
            }
            
            // Category filter (treat "Ink & Toner" as matching both 'Ink' and 'Toner')
            if (selectedCategory != null && !"All Categories".equals(selectedCategory)) {
                if ("Ink & Toner".equalsIgnoreCase(selectedCategory)) {
                    String cat = item.getCategory();
                    if (cat == null || (!cat.equalsIgnoreCase("ink") && !cat.equalsIgnoreCase("toner") && !cat.equalsIgnoreCase("Ink & Toner"))) {
                        return false;
                    }
                } else {
                    if (!item.getCategory().equals(selectedCategory)) {
                        return false;
                    }
                }
            }
            
            // Stack level filter removed
            
            // Low stock / Out of stock filters
            if (showOnlyLowStock || showOnlyOutOfStock) {
                // Explicit low-stock match: low but not out-of-stock
                boolean matchesLow = item.getQuantity() > 0 && item.getQuantity() <= item.getMinStockLevel();
                boolean matchesOut = item.getQuantity() == 0;
                // Show if it matches either selected filter
                if (showOnlyLowStock && !matchesLow && !showOnlyOutOfStock) return false;
                if (showOnlyOutOfStock && !matchesOut && !showOnlyLowStock) return false;
                if (showOnlyLowStock && showOnlyOutOfStock && !(matchesLow || matchesOut)) return false;
            }
            
            return true;
        });
    }
    
    private void setupButtons() {
        // Edit and Delete buttons should be disabled when no item is selected
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
            // No archiveWholeButton present; maintain other button state
            // Maintain selection-dependent button enable/disable for other buttons if needed
        });
    }
    
    private void loadData() {
        // Update category filter options
        updateCategoryFilter();
    }
    
    private void updateCategoryFilter() {
        String currentSelection = categoryFilter.getValue();
        var categories = FXCollections.observableArrayList("All Categories");
        // Normalize categories: merge 'Ink' and 'Toner' into 'Ink & Toner'
        var normalized = dataManager.getAllCategories().stream()
                .map(c -> {
                    if (c == null) return "";
                    String t = c.trim();
                    if (t.equalsIgnoreCase("ink") || t.equalsIgnoreCase("toner")) return "Ink & Toner";
                    return t;
                })
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
        categories.addAll(normalized);
        categoryFilter.setItems(categories);
        
        // Restore selection if it still exists
        if (categories.contains(currentSelection)) {
            categoryFilter.setValue(currentSelection);
        } else {
            categoryFilter.setValue("All Categories");
        }
    }
    
    private void updateStatistics() {
        var stats = dataManager.getInventoryStats();
        totalItemsLabel.setText(String.valueOf(stats.getTotalItems()));
        lowStockItemsLabel.setText(String.valueOf(stats.getLowStockItems()));
        totalValueLabel.setText(String.format("₱%.2f", stats.getTotalValue()));
        totalQuantityLabel.setText(String.valueOf(stats.getTotalQuantity()));
    }
    
    private void updateUserInfo() {
        User currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            userInfoLabel.setText("Logged in as: " + currentUser.getFullName() + " (" + currentUser.getRole().getDisplayName() + ")");
        }
    }
    
    @FXML
    private void handleAddItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/item-form.fxml"));
            Parent root = loader.load();
            
            ItemFormController controller = loader.getController();
            controller.setMode(ItemFormController.Mode.ADD);
            
            Stage stage = new Stage();
            stage.setTitle("Add New Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(false);
            
            stage.showAndWait();
            
            updateCategoryFilter();
            
        } catch (IOException e) {
            showAlert("Error", "Could not open add item dialog: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleEditItem() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("No Selection", "Please select an item to edit.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/item-form.fxml"));
            Parent root = loader.load();
            
            ItemFormController controller = loader.getController();
            controller.setMode(ItemFormController.Mode.EDIT);
            controller.setItem(selectedItem);
            
            Stage stage = new Stage();
            stage.setTitle("Edit Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(false);
            
            stage.showAndWait();
            
            updateCategoryFilter();
            
        } catch (IOException e) {
            showAlert("Error", "Could not open edit item dialog: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteItem() {
        var selectedItems = inventoryTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showAlert("No Selection", "Please select item(s) to delete.");
            return;
        }
        
        String message = selectedItems.size() == 1 ? 
            "Are you sure you want to delete the selected item?" :
            "Are you sure you want to delete " + selectedItems.size() + " selected items?";
            
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Create a copy of the list to avoid ConcurrentModificationException
            var itemsToDelete = FXCollections.observableArrayList(selectedItems);
            // Ask for delete reason
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Delete Reason");
            reasonDialog.setHeaderText("Provide a reason for deleting the selected item(s)");
            reasonDialog.setContentText("Reason:");
            Optional<String> reasonRes = reasonDialog.showAndWait();
            if (reasonRes.isEmpty() || reasonRes.get().trim().isEmpty()) {
                showAlert("Reason Required", "You must provide a reason for deletion.");
                return;
            }
            String reason = reasonRes.get().trim();

            for (InventoryItem item : itemsToDelete) {
                dataManager.removeItem(item, reason);
            }
            updateCategoryFilter();
        }
    }

    

    @FXML
    private void handleArchiveItem() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("No Selection", "Please select an item to archive.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Archive Item");
        dialog.setHeaderText("Archive selected item: " + selectedItem.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField qtyField = new TextField("1");
        TextField reasonField = new TextField();
        CheckBox deleteWhenZero = new CheckBox("Delete item when fully archived");

        grid.add(new Label("Quantity to archive:"), 0, 0);
        grid.add(qtyField, 1, 0);
        grid.add(new Label("Reason (required):"), 0, 1);
        grid.add(reasonField, 1, 1);
        grid.add(deleteWhenZero, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();
            if (reason.isEmpty()) {
                showAlert("Reason Required", "You must provide a reason for archiving the item.");
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert("Invalid Quantity", "Please enter a valid integer quantity.");
                return;
            }

            if (qty <= 0) {
                showAlert("Invalid Quantity", "Quantity must be greater than zero.");
                return;
            }

            if (qty > selectedItem.getQuantity()) {
                showAlert("Invalid Quantity", "Cannot archive more than available quantity.");
                return;
            }

            String user = AuthManager.getInstance().getCurrentUser() != null ? AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";

            boolean ok = dataManager.reduceItemQuantity(selectedItem, qty, user, reason);
            if (!ok) {
                showAlert("Archive Failed", "Could not archive the item. See logs for details.");
                return;
            }

            // If user requested delete when zero and item quantity reached zero, remove it with reason
            if (deleteWhenZero.isSelected() && selectedItem.getQuantity() == 0) {
                dataManager.removeItem(selectedItem, reason);
            }

            updateCategoryFilter();
            inventoryTable.refresh();
            showAlert("Archived", "Successfully archived " + qty + " of " + selectedItem.getName() + ".");
        }
    }
    
    @FXML
    private void handleRefresh() {
        dataManager.loadData();
        updateCategoryFilter();
        showAlert("Success", "Data refreshed successfully.");
    }

    @FXML
    private void handleFlushHistory() {
        // Confirm action
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Flush Transaction History");
        confirm.setHeaderText(null);
        confirm.setContentText("This will persist in-memory transaction history to the database. Continue?");
        Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            // Run migration
            int migrated = TransactionHistoryMigrator.migrateAndCount();
            showAlert("Migration Complete", "Migrated " + migrated + " transaction records into the database.");
        }
    }
    
    @FXML
    private void handleUserManagement() {
        try {
            // Create user management dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("User Management");
            dialog.setHeaderText("Manage System Users");
            
            // Create table view for users
            TableView<User> userTable = new TableView<>();
            
            // Create columns
            TableColumn<User, String> usernameCol = new TableColumn<>("Username");
            usernameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
            
            TableColumn<User, String> roleCol = new TableColumn<>("Role");
            roleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().toString()));
            
            TableColumn<User, String> fullNameCol = new TableColumn<>("Full Name");
            fullNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
            
            TableColumn<User, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
            
            userTable.getColumns().add(usernameCol);
            userTable.getColumns().add(roleCol);
            userTable.getColumns().add(fullNameCol);
            userTable.getColumns().add(emailCol);
            userTable.setItems(FXCollections.observableArrayList(authManager.getAllUsers()));
            
            // Create buttons
            Button addUserBtn = new Button("Add User");
            Button editUserBtn = new Button("Edit User");
            Button deleteUserBtn = new Button("Delete User");
            Button refreshBtn = new Button("Refresh");
            
            // Button actions
            addUserBtn.setOnAction(e -> showAddUserDialog(userTable));
            editUserBtn.setOnAction(e -> showEditUserDialog(userTable));
            deleteUserBtn.setOnAction(e -> deleteUser(userTable));
            refreshBtn.setOnAction(e -> userTable.setItems(FXCollections.observableArrayList(authManager.getAllUsers())));
            
            // Layout
            VBox content = new VBox(10);
            HBox buttonBox = new HBox(10, addUserBtn, editUserBtn, deleteUserBtn, refreshBtn);
            content.getChildren().addAll(buttonBox, userTable);
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            dialog.showAndWait();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to open user management: " + e.getMessage());
        }
    }
    
    private void showAddUserDialog(TableView<User> userTable) {
        // Create add user dialog
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Enter user details");
        
        // Create form fields
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<User.UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(User.UserRole.values());
        TextField fullNameField = new TextField();
        TextField emailField = new TextField();
        
        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleCombo, 1, 2);
        grid.add(new Label("Full Name:"), 0, 3);
        grid.add(fullNameField, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Validate and create user
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    User newUser = new User(
                        usernameField.getText(),
                        passwordField.getText(),
                        roleCombo.getValue(),
                        fullNameField.getText(),
                        emailField.getText()
                    );
                    authManager.addUser(newUser);
                    userTable.setItems(FXCollections.observableArrayList(authManager.getAllUsers()));
                    showAlert("Success", "User added successfully!");
                    return newUser;
                } catch (Exception e) {
                    showAlert("Error", "Failed to add user: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void showEditUserDialog(TableView<User> userTable) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to edit.");
            return;
        }
        
        // Create edit user dialog
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user details");
        
        // Create form fields with current values
        TextField usernameField = new TextField(selectedUser.getUsername());
        usernameField.setEditable(false); // Username cannot be changed
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter new password (leave blank to keep current)");
        ComboBox<User.UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(User.UserRole.values());
        roleCombo.setValue(selectedUser.getRole());
        TextField fullNameField = new TextField(selectedUser.getFullName());
        TextField emailField = new TextField(selectedUser.getEmail());
        
        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleCombo, 1, 2);
        grid.add(new Label("Full Name:"), 0, 3);
        grid.add(fullNameField, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Validate and update user
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    selectedUser.setRole(roleCombo.getValue());
                    selectedUser.setFullName(fullNameField.getText());
                    selectedUser.setEmail(emailField.getText());
                    
                    if (!passwordField.getText().isEmpty()) {
                        selectedUser.setPassword(passwordField.getText());
                    }
                    
                    authManager.updateUser(selectedUser);
                    userTable.setItems(FXCollections.observableArrayList(authManager.getAllUsers()));
                    showAlert("Success", "User updated successfully!");
                    return selectedUser;
                } catch (Exception e) {
                    showAlert("Error", "Failed to update user: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void deleteUser(TableView<User> userTable) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to delete.");
            return;
        }
        
        // Prevent deletion of current user
        if (selectedUser.equals(authManager.getCurrentUser())) {
            showAlert("Cannot Delete", "You cannot delete your own account.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete User");
        confirmAlert.setContentText("Are you sure you want to delete user: " + selectedUser.getUsername() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                authManager.deleteUser(selectedUser);
                userTable.setItems(FXCollections.observableArrayList(authManager.getAllUsers()));
                showAlert("Success", "User deleted successfully!");
            } catch (Exception e) {
                showAlert("Error", "Failed to delete user: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            authManager.logout();
            
            // Return to login screen
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login-view.fxml"));
                Parent root = loader.load();
                
                LoginController loginController = loader.getController();
                loginController.setLoginStage(InventoryApplication.getPrimaryStage());
                
                Scene scene = new Scene(root, 500, 600);
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                
                InventoryApplication.getPrimaryStage().setTitle("NVA Printing Services - Login");
                InventoryApplication.getPrimaryStage().setScene(scene);
                InventoryApplication.getPrimaryStage().setResizable(false);
                InventoryApplication.getPrimaryStage().centerOnScreen();
                InventoryApplication.getPrimaryStage().show();
                
            } catch (IOException e) {
                java.util.logging.Logger.getLogger(ManagerController.class.getName()).log(java.util.logging.Level.SEVERE, "Error returning to login screen: " + e.getMessage(), e);
                Platform.exit();
            }
        }
    }
    
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue("All Categories");
        lowStockFilter.setSelected(false);
        if (outOfStockFilter != null) outOfStockFilter.setSelected(false);
    }
    
    @FXML
    private void handleExport() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Inventory Data");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            // Set default folder to Downloads
            String userHome = System.getProperty("user.home");
            File downloadsDir = new File(userHome, "Downloads");
            if (downloadsDir.exists() && downloadsDir.isDirectory()) {
                fileChooser.setInitialDirectory(downloadsDir);
            }
            fileChooser.setInitialFileName("inventory_export_" + java.time.LocalDate.now().toString() + ".xlsx");
            File file = fileChooser.showSaveDialog(inventoryTable.getScene().getWindow());
            if (file != null) {
                try {
                    exportToExcel(file);
                    if (file.exists()) {
                        showAlert("Export Successful", "Inventory data exported to: " + file.getAbsolutePath());
                    } else {
                        showAlert("Export Error", "File was not created. Please check permissions and try a different folder.");
                    }
                } catch (Exception ex) {
                    showAlert("Export Error", "Failed to save file: " + ex.getMessage() + "\nPlease check folder permissions and ensure the file is not open in another program.");
                }
            } else {
                showAlert("Export Cancelled", "No file was selected for export.");
            }
        } catch (Exception e) {
            showAlert("Export Error", "Failed to export data: " + e.getMessage());
        }
    }

    private void exportToExcel(File file) throws IOException {
        List<InventoryItem> items = new ArrayList<>(dataManager.getAllItems());
        // Sort items by name (A-Z), then by ID (ascending)
        items.sort(Comparator.comparing(InventoryItem::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(InventoryItem::getId));
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventory");
            // Header style: bold and center
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // Data style: center
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"No.", "Item Name", "Category", "Quantity", "Min Stock", "Unit Price", "Supplier", "Last Updated", "Stock In", "Stock Out"};
            // Explicitly reference org.apache.poi.ss.usermodel.Cell in exportToExcel
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            // Data rows
            int rowNum = 1;
            for (InventoryItem item : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1); // No. column: sequential number starting from 1
                row.createCell(1).setCellValue(item.getName()); // Item Name
                row.createCell(2).setCellValue(item.getCategory());
                row.createCell(3).setCellValue(item.getQuantity());
                row.createCell(4).setCellValue(item.getMinStockLevel());
                row.createCell(5).setCellValue(item.getUnitPrice());
                row.createCell(6).setCellValue(item.getSupplier());
                // Last Updated column (index 7)
                if (item.getLastUpdated() != null) {
                    Date lastUpdatedDate = Date.from(item.getLastUpdated().atZone(ZoneId.systemDefault()).toInstant());
                    org.apache.poi.ss.usermodel.Cell lastUpdatedCell = row.createCell(7);
                    lastUpdatedCell.setCellValue(lastUpdatedDate);
                    lastUpdatedCell.setCellStyle(workbook.createCellStyle()); // placeholder, will set below
                } else {
                    row.createCell(7).setCellValue("");
                }
                // Stock In/Out columns
                row.createCell(8).setCellValue(item.getStockIn());
                row.createCell(9).setCellValue(item.getStockOut());
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(centerStyle);
                }
            }
            // Create a date cell style with 12-hour AM/PM format and apply to the Last Updated column
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            short dateFormat = createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm AM/PM");
            dateStyle.setDataFormat(dateFormat);
            
            // Apply dateStyle to all cells in the Last Updated column (index 7)
            for (int r = 1; r < rowNum; r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow != null) {
                    org.apache.poi.ss.usermodel.Cell c = dataRow.getCell(7);
                    if (c != null && c.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        c.setCellStyle(dateStyle);
                    }
                }
            }

            // After writing all data rows, add TOTAL row at the bottom
            int totalRowNum = rowNum;
            Row totalRow = sheet.createRow(totalRowNum);
            org.apache.poi.ss.usermodel.Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("TOTAL:");
            totalLabelCell.setCellStyle(headerStyle);
            // Merge cells for TOTAL label (A16:B16 for 15 items)
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(totalRowNum, totalRowNum, 0, 1));
            // Calculate totals for Stock In and Stock Out
            int stockInTotal = items.stream().mapToInt(InventoryItem::getStockIn).sum();
            int stockOutTotal = items.stream().mapToInt(InventoryItem::getStockOut).sum();
            // Place Stock In total in I column, Stock Out total in J column
            org.apache.poi.ss.usermodel.Cell stockInTotalCell = totalRow.createCell(8);
            stockInTotalCell.setCellValue(stockInTotal);
            stockInTotalCell.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell stockOutTotalCell = totalRow.createCell(9);
            stockOutTotalCell.setCellValue(stockOutTotal);
            stockOutTotalCell.setCellStyle(headerStyle);
            // Auto-size columns for better appearance
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // Set row height for header and total rows for emphasis
            headerRow.setHeightInPoints(22);
            totalRow.setHeightInPoints(22);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }
    
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("NVA Printing Services - Inventory Management System");
        alert.setContentText(
            "Version 1.0.0\n\n" +
            "Developed by:\n" +
            "• Allen Kirk A. Cailing\n" +
            "• April Mae O. Cape\n" +
            "• Loreen Angel Culanculan\n" +
            "• Vanessa V. Embornas\n" +
            "• Bernadine E. Suson\n" +
            "• Gerome L. Velasco\n\n" +
            "A simple digital inventory management system\n" +
            "for NVA Printing Services with role-based access."
        );
        alert.showAndWait();
    }
    
    @FXML
    private void handleExit() {
        // Save data before exit
        dataManager.saveData();
        authManager.saveUsers();
        
        // Close application
        Platform.exit();
        System.exit(0);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
