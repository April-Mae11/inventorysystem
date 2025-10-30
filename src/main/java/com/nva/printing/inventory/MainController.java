package com.nva.printing.inventory;

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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Main Controller - Demonstrates OOP Concepts
 * 
 * INHERITANCE: Implements Initializable interface from JavaFX framework
 * ENCAPSULATION: Private fields with controlled access through FXML injection
 * ABSTRACTION: Hides complex UI management behind simple event handler methods
 * POLYMORPHISM: Overrides initialize() method and uses callback interfaces
 * 
 * This controller manages the main user interface and demonstrates how OOP
 * principles are applied in a real-world JavaFX application.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Inheritance: Extends JavaFX framework capabilities through interface implementation
 * - Encapsulation: Private fields and methods to control access to UI components
 * - Abstraction: Clean public interface hiding complex UI management
 * - Polymorphism: Method overriding and interface implementations
 * 
 * @author NVA Printing Services Development Team
 */
public class MainController implements Initializable {
    
    // ENCAPSULATION: FXML-injected UI components with controlled access
    // These fields are automatically injected by JavaFX framework
    
    // Table and Column Components - ENCAPSULATION through private access
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, String> nameColumn;
    @FXML private TableColumn<InventoryItem, String> categoryColumn;
    @FXML private TableColumn<InventoryItem, String> descriptionColumn;
    @FXML private TableColumn<InventoryItem, Integer> quantityColumn;
    @FXML private TableColumn<InventoryItem, Integer> stockInColumn;
    @FXML private TableColumn<InventoryItem, Integer> stockOutColumn;
    @FXML private TableColumn<InventoryItem, Integer> minStockColumn;
    @FXML private TableColumn<InventoryItem, Double> priceColumn;
    @FXML private TableColumn<InventoryItem, String> supplierColumn;
    @FXML private TableColumn<InventoryItem, String> lastUpdatedColumn;
    @FXML private TableColumn<InventoryItem, String> statusColumn;
    
    // Filter and Search Components - ENCAPSULATION
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private CheckBox lowStockFilter;
    
    // Action Button Components - ENCAPSULATION
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button archiveButton;
    @FXML private Button refreshButton;
    @FXML private Button stockInButton;
    @FXML private Button stockOutButton;
    
    // Statistics Display Components - ENCAPSULATION
    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockItemsLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label totalQuantityLabel;
    
    // ENCAPSULATION: Private fields for internal state management
    private FilteredList<InventoryItem> filteredData;
    private DataManager dataManager; // COMPOSITION: Uses DataManager for data access
    
    /**
     * POLYMORPHISM: Override Initializable.initialize()
     * ABSTRACTION: Hides complex initialization behind simple method call
     * This method is called automatically by JavaFX after FXML loading
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // COMPOSITION: Use DataManager singleton for data access
        dataManager = DataManager.getInstance();
        
        // ABSTRACTION: Break down initialization into manageable methods
        setupTableColumns();
        setupFilters();
        setupButtons();
        loadData();
        updateStatistics();
        
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
    stockInColumn.setCellValueFactory(new PropertyValueFactory<>("stockIn"));
    stockOutColumn.setCellValueFactory(new PropertyValueFactory<>("stockOut"));
        minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        
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
            return new SimpleStringProperty(cellData.getValue().isLowStock() ? "LOW STOCK" : "NORMAL STOCK");
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
        
        // Allow multiple selection for batch operations
        inventoryTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
    
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
        
        // Setup low stock filter
        lowStockFilter.selectedProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Create sorted list and bind to table
        SortedList<InventoryItem> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(inventoryTable.comparatorProperty());
        inventoryTable.setItems(sortedData);
    }
    
    /**
     * Determine stack level based on quantity
     * ABSTRACTION: Hides business logic for stack level calculation
     * @param item Inventory item to analyze
     * @return Stack level string
     */
    private String getStackLevel(InventoryItem item) {
        int quantity = item.getQuantity();
        int minStock = item.getMinStockLevel();
        
        if (quantity <= minStock) {
            return "LOW";
        } else if (quantity <= minStock * 2) {
            return "NORMAL STOCK";
        } else {
            return "FULL";
        }
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryFilter.getValue();
        boolean showOnlyLowStock = lowStockFilter.isSelected();
        
        filteredData.setPredicate(item -> {
            // Search filter
            if (searchText != null && !searchText.isEmpty()) {
                if (!item.getName().toLowerCase().contains(searchText) &&
                    !item.getDescription().toLowerCase().contains(searchText) &&
                    !item.getSupplier().toLowerCase().contains(searchText)) {
                    return false;
                }
            }
            
            // Category filter 
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
            
            // Low stock filter: show only items that are low but NOT out of stock
            if (showOnlyLowStock && !(item.isLowStock() && item.getQuantity() > 0)) {
                return false;
            }
            
            return true;
        });
    }
    
    private void setupButtons() {
        stockInButton.setOnAction(e -> handleStockIn());
        stockOutButton.setOnAction(e -> handleStockOut());
        // Edit and Delete buttons should be disabled when no item is selected
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        });
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
                dataManager.updateItem(selectedItem);
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
                dataManager.updateItem(selectedItem);
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
            // Ask for delete reason (required)
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

            // Create a copy of the list to avoid ConcurrentModificationException
            var itemsToDelete = FXCollections.observableArrayList(selectedItems);
            for (InventoryItem item : itemsToDelete) {
                dataManager.removeItem(item, reason);
            }
            updateCategoryFilter();
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

            // Create a copy to avoid ConcurrentModificationException
            var itemsToArchive = FXCollections.observableArrayList(selectedItems);
            for (InventoryItem item : itemsToArchive) {
                // DataManager.removeItem archives the item and removes it
                dataManager.removeItem(item, reason);
            }
            updateCategoryFilter();
        }
    }
    
    @FXML
    private void handleRefresh() {
        dataManager.loadData();
        updateCategoryFilter();
        showAlert("Success", "Data refreshed successfully.");
    }
    
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue("All Categories");
        lowStockFilter.setSelected(false);
    }
    
    @FXML
    private void handleExport() {
        try {
            // Create file chooser for export location
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Inventory Data");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            fileChooser.setInitialFileName("inventory_export_" + 
                java.time.LocalDate.now().toString() + ".csv");
            
            File file = fileChooser.showSaveDialog(inventoryTable.getScene().getWindow());
            if (file != null) {
                exportToCSV(file);
                showAlert("Export Successful", "Inventory data exported to: " + file.getName());
            }
        } catch (Exception e) {
            showAlert("Export Error", "Failed to export data: " + e.getMessage());
        }
    }
    
    private void exportToCSV(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write CSV header
            writer.append("ID,Name,Category,Description,Quantity,Min Stock,Unit Price,Supplier,Last Updated,Status,Stack Level\n");
            
            // Write data rows
            for (InventoryItem item : dataManager.getAllItems()) {
                writer.append(String.valueOf(item.getId())).append(",");
                writer.append(escapeCSV(item.getName())).append(",");
                writer.append(escapeCSV(item.getCategory())).append(",");
                writer.append(escapeCSV(item.getDescription())).append(",");
                writer.append(String.valueOf(item.getQuantity())).append(",");
                writer.append(String.valueOf(item.getMinStockLevel())).append(",");
                writer.append(String.valueOf(item.getUnitPrice())).append(",");
                writer.append(escapeCSV(item.getSupplier())).append(",");
                writer.append(item.getLastUpdated().toString()).append(",");
                writer.append(item.isLowStock() ? "LOW STOCK" : "NORMAL STOCK").append(",");
                writer.append(getStackLevel(item)).append("\n");
            }
        }
    }
    
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
            "for NVA Printing Services."
        );
        alert.showAndWait();
    }
    
    @FXML
    private void handleExit() {
        // Save data before exit
        dataManager.saveData();
        
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
