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
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Comparator;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;

/**
 * Head Production Controller - Demonstrates OOP Concepts
 * 
 * INHERITANCE: Implements Initializable interface from JavaFX framework
 * ENCAPSULATION: Private fields with controlled access through FXML injection
 * ABSTRACTION: Hides complex UI management behind simple event handler methods
 * POLYMORPHISM: Overrides initialize() method and uses callback interfaces
 * 
 * This controller manages the head production interface with production-focused access
 * and demonstrates how OOP principles are applied in role-based applications.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Inheritance: Extends JavaFX framework capabilities through interface implementation
 * - Encapsulation: Private fields and methods to control access to UI components
 * - Abstraction: Clean public interface hiding complex UI management
 * - Polymorphism: Method overriding and interface implementations
 * 
 * @author NVA Printing Services Development Team
 */
public class HeadProductionController implements Initializable {
    @FXML private Button deleteButton;
    @FXML
    private TableColumn<InventoryItem, Integer> stockInColumn;
    @FXML
    private TableColumn<InventoryItem, Integer> stockOutColumn;

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
            // Open archive view maximized for better visibility
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open archive view: " + e.getMessage());
        }
    }
    @FXML private Button stockInButton;
    @FXML private Button stockOutButton;
    @FXML private Button viewArchiveButton;
    
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
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open transaction history: " + e.getMessage());
        }
    }
    
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
    
    // Action Button Components - ENCAPSULATION (Limited access for Head Production)
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button refreshButton;
    @FXML private Button logoutButton;
    
    // Statistics Display Components - ENCAPSULATION
    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockItemsLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label totalQuantityLabel;
    @FXML private Label userInfoLabel;
    
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
        try {
            // COMPOSITION: Use DataManager and AuthManager singletons for data access
            dataManager = DataManager.getInstance();
            authManager = AuthManager.getInstance();

            // ABSTRACTION: Break down initialization into manageable methods
            setupTableColumns();
            setupFilters();
            setupButtons();
            loadData();
            updateStatistics();
            updateUserInfo();

            // ABSTRACTION: Hide listener complexity behind lambda expression
            // Refresh statistics when data changes
            dataManager.getInventoryItems().addListener((javafx.collections.ListChangeListener<InventoryItem>) c -> {
                Platform.runLater(this::updateStatistics);
            });
        } catch (Exception e) {
            // Log and show a detailed alert so the user/developer can see the root cause
            java.util.logging.Logger.getLogger(HeadProductionController.class.getName()).log(java.util.logging.Level.SEVERE, "Initialization error: " + e.getMessage(), e);

            // Build stacktrace for display
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Head Production - Initialization Error");
                alert.setHeaderText("An error occurred while initializing the Head Production view");
                alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage());

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(new Label("The exception stacktrace was:"), 0, 0);
                expContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expContent);
                alert.showAndWait();
            });
        }
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
        
        // Allow single selection for Head Production role
        inventoryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    /**
     * Determine stack level based on quantity
     * ABSTRACTION: Hides business logic for stack level calculation
     * @param item Inventory item to analyze
     * @return Stack level string
     */
    // Stack level logic removed; not used anymore
    
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
    // Do not bind to the table comparator so the name column header arrow is not shown
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
                boolean matchesLow = item.getQuantity() > 0 && item.getQuantity() <= item.getMinStockLevel();
                boolean matchesOut = item.getQuantity() == 0;
                if (showOnlyLowStock && !matchesLow && !showOnlyOutOfStock) return false;
                if (showOnlyOutOfStock && !matchesOut && !showOnlyLowStock) return false;
                if (showOnlyLowStock && showOnlyOutOfStock && !(matchesLow || matchesOut)) return false;
            }
            
            return true;
        });
    }
    
    private void setupButtons() {
        deleteButton.setOnAction(e -> handleDeleteItem());
        // Edit button should be disabled when no item is selected
        editButton.setDisable(true);
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editButton.setDisable(!hasSelection);
        });
        stockInButton.setOnAction(e -> handleStockIn());
        stockOutButton.setOnAction(e -> handleStockOut());
    }

    @FXML
    private void handleDeleteItem() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("No Selection", "Please select an item to delete.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete the selected item?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Delete Reason");
            reasonDialog.setHeaderText("Provide a reason for deleting the selected item");
            reasonDialog.setContentText("Reason:");
            Optional<String> reasonRes = reasonDialog.showAndWait();
            if (reasonRes.isEmpty() || reasonRes.get().trim().isEmpty()) {
                showAlert("Reason Required", "You must provide a reason for deletion.");
                return;
            }
            String reason = reasonRes.get().trim();
            dataManager.removeItem(selectedItem, reason);
            updateCategoryFilter();
        }
    }

    @FXML
    private void handleArchiveSelected() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("No Selection", "Please select an item to archive.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Archive");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to archive the selected item?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Archive Reason");
            reasonDialog.setHeaderText("Provide a reason for archiving the selected item");
            reasonDialog.setContentText("Reason:");
            Optional<String> reasonRes = reasonDialog.showAndWait();
            if (reasonRes.isEmpty() || reasonRes.get().trim().isEmpty()) {
                showAlert("Reason Required", "You must provide a reason for archiving.");
                return;
            }
            String reason = reasonRes.get().trim();
            dataManager.removeItem(selectedItem, reason);
            updateCategoryFilter();
        }
    }

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
    private void handleRefresh() {
        dataManager.loadData();
        updateCategoryFilter();
        showAlert("Success", "Data refreshed successfully.");
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
                java.util.logging.Logger.getLogger(HeadProductionController.class.getName()).log(java.util.logging.Level.SEVERE, "Error returning to login screen: " + e.getMessage(), e);
                Platform.exit();
            }
        }
    }
    
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue("All Categories");
        lowStockFilter.setSelected(false);
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
            "• Bernadine E. Suson\n\n" +
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
