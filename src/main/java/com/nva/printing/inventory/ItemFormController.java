package com.nva.printing.inventory;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.geometry.Side;
import javafx.scene.layout.GridPane;
import javafx.scene.Node;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class ItemFormController implements Initializable {
    public enum Mode {
        ADD, EDIT
    }

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button addCategoryButton;
    @FXML private Button removeCategoryButton;
    @FXML private TextArea descriptionArea;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Spinner<Integer> minStockSpinner;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> supplierComboBox;
    @FXML private Button addSupplierButton;
    @FXML private Button removeSupplierButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label titleLabel;

    private Mode currentMode;
    private InventoryItem currentItem;
    private DataManager dataManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dataManager = DataManager.getInstance();
        setupForm();
        setupValidation();
    }

    private void setupForm() {
        // Initial population of category combo box
        List<String> categories = buildCategoryList();
        categoryComboBox.getItems().setAll(categories);
        categoryComboBox.setEditable(true);

        // Suggestion-only autocomplete for nameField
        ContextMenu nameSuggestions = new ContextMenu();
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if ((currentMode == Mode.ADD || nameField.isFocused()) && newText.length() > 0) {
                nameSuggestions.getItems().clear();
                for (InventoryItem item : dataManager.getInventoryItems()) {
                    if (item.getName().toLowerCase().startsWith(newText.toLowerCase())) {
                        MenuItem suggestion = new MenuItem(item.getName());
                        suggestion.setOnAction(e -> {
                            nameField.setText(item.getName());
                            nameSuggestions.hide();
                        });
                        nameSuggestions.getItems().add(suggestion);
                    }
                }
                if (!nameSuggestions.getItems().isEmpty()) {
                    nameSuggestions.show(nameField, Side.BOTTOM, 0, 0);
                } else {
                    nameSuggestions.hide();
                }
            } else {
                nameSuggestions.hide();
            }
        });

        // Populate supplierComboBox with unique supplier names and make it editable
        supplierComboBox.setEditable(true);
        Set<String> suppliers = new HashSet<>();
        // Prefer canonical supplier list from DataManager
        try {
            for (Supplier s : dataManager.getAllSuppliers()) {
                if (s != null && s.getName() != null && !s.getName().trim().isEmpty()) suppliers.add(s.getName().trim());
            }
        } catch (Exception ignored) {}
        // Also include suppliers found on existing inventory items as a fallback
        for (InventoryItem item : dataManager.getInventoryItems()) {
            String sup = item.getSupplier();
            if (sup != null && !sup.trim().isEmpty()) suppliers.add(sup.trim());
        }
        List<String> supplierList = new ArrayList<>(suppliers);
        Collections.sort(supplierList, String.CASE_INSENSITIVE_ORDER);
        supplierComboBox.getItems().setAll(supplierList);

        // Suggestion-only autocomplete for supplierComboBox editor
        ContextMenu supplierSuggestions = new ContextMenu();
        supplierComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 0) {
                supplierSuggestions.getItems().clear();
                Set<String> seen = new HashSet<>();
                String query = newText.toLowerCase();
                for (String sup : supplierComboBox.getItems()) {
                    if (sup == null) continue;
                    String supLower = sup.toLowerCase();
                    if (supLower.startsWith(query) && seen.add(supLower)) {
                        MenuItem suggestion = new MenuItem(sup);
                        suggestion.setOnAction(e -> {
                            supplierComboBox.getEditor().setText(sup);
                            supplierSuggestions.hide();
                        });
                        supplierSuggestions.getItems().add(suggestion);
                    }
                }
                if (!supplierSuggestions.getItems().isEmpty()) {
                    supplierSuggestions.show(supplierComboBox.getEditor(), Side.BOTTOM, 0, 0);
                } else {
                    supplierSuggestions.hide();
                }
            } else {
                supplierSuggestions.hide();
            }
        });

        // Suggestion-only autocomplete for categoryComboBox
        ContextMenu categorySuggestions = new ContextMenu();
        categoryComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 0) {
                categorySuggestions.getItems().clear();
                for (String cat : categoryComboBox.getItems()) {
                    if (cat.toLowerCase().startsWith(newText.toLowerCase())) {
                        MenuItem suggestion = new MenuItem(cat);
                        suggestion.setOnAction(e -> {
                            categoryComboBox.getEditor().setText(cat);
                            categorySuggestions.hide();
                            // Hide suggestions after selection or typing
                            categorySuggestions.getItems().clear();
                        });
                        categorySuggestions.getItems().add(suggestion);
                    }
                }
                if (!categorySuggestions.getItems().isEmpty()) {
                    categorySuggestions.show(categoryComboBox.getEditor(), Side.BOTTOM, 0, 0);
                } else {
                    categorySuggestions.hide();
                }
            } else {
                categorySuggestions.hide();
            }
        });
        // Hide suggestions when focus is lost or Enter is pressed
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) nameSuggestions.hide(); });
    supplierComboBox.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) supplierSuggestions.hide(); });
        categoryComboBox.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) categorySuggestions.hide(); });
        categoryComboBox.getEditor().setOnAction(e -> categorySuggestions.hide());
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999999, 0));
        quantitySpinner.setEditable(true);
        minStockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999999, 10));
        minStockSpinner.setEditable(true);
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                priceField.setText(oldValue);
            }
        });
    }

    /**
     * Build the full category list from DataManager plus extras, normalize and sort.
     * Public refresh method uses this to repopulate the ComboBox when categories change.
     */
    private List<String> buildCategoryList() {
        List<String> extras = Arrays.asList(
            ""
        );
        List<String> allCats = new ArrayList<>();
        // collect categories from current data (DataManager may prefer DB-backed categories)
        try {
            allCats.addAll(dataManager.getAllCategories());
        } catch (Exception ignored) {
            // fall back to empty list
        }
        // add extras (they may duplicate; we'll dedupe next)
        allCats.addAll(extras);

        // Normalize and deduplicate (merge Ink + Toner -> Ink & Toner)
        Set<String> normalized = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String c : allCats) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (t.equalsIgnoreCase("ink") || t.equalsIgnoreCase("toner")) {
                normalized.add("Ink & Toner");
            } else {
                normalized.add(t);
            }
        }

        List<String> categories = new ArrayList<>(normalized);
        Collections.sort(categories, String.CASE_INSENSITIVE_ORDER);
        return categories;
    }

    /**
     * Public API to refresh categories in the ComboBox from DataManager/DB. Call this after
     * categories are added/removed externally.
     */
    public void refreshCategories() {
        List<String> categories = buildCategoryList();
        categoryComboBox.getItems().setAll(categories);
    }

    private void setupValidation() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());
        priceField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    supplierComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());
    supplierComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }

    private void validateForm() {
    boolean isValid = !nameField.getText().trim().isEmpty() &&
             categoryComboBox.getValue() != null &&
             !categoryComboBox.getValue().trim().isEmpty() &&
             !priceField.getText().trim().isEmpty() &&
             supplierComboBox.getEditor().getText() != null && !supplierComboBox.getEditor().getText().trim().isEmpty();
        
        saveButton.setDisable(!isValid);
    }
    
    public void setMode(Mode mode) {
        this.currentMode = mode;
        switch (mode) {
            case ADD:
                titleLabel.setText("Add New Item");
                clearForm();
                quantitySpinner.setDisable(false); // Enable quantity in ADD mode
                minStockSpinner.getValueFactory().setValue(10);
                priceField.clear();
                if (supplierComboBox.getEditor() != null) supplierComboBox.getEditor().clear();
                break;
            case EDIT:
                titleLabel.setText("Edit Item");
                quantitySpinner.setDisable(true); // Disable quantity in EDIT mode
                break;
        }
    }
    
    private void populateForm(InventoryItem item) {
        nameField.setText(item.getName());
        categoryComboBox.setValue(item.getCategory());
        descriptionArea.setText(item.getDescription());
        quantitySpinner.getValueFactory().setValue(item.getQuantity());
        minStockSpinner.getValueFactory().setValue(item.getMinStockLevel());
        priceField.setText(String.format("%.2f", item.getUnitPrice()));
    supplierComboBox.getEditor().setText(item.getSupplier());
    }

    // Call this before showing the edit dialog
    public void setItem(InventoryItem item) {
        this.currentItem = item;
        populateForm(item);
    }
    
    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            String name = nameField.getText().trim();
            String category = categoryComboBox.getValue().trim();
            String description = descriptionArea.getText().trim();
            int quantity = quantitySpinner.getValue();
            int minStock = minStockSpinner.getValue();
            double price = Double.parseDouble(priceField.getText().trim());
            String supplier = supplierComboBox.getEditor().getText().trim();

            if (currentMode == Mode.ADD) {
                // Check if item with same name exists
                InventoryItem existing = dataManager.getInventoryItems().stream()
                        .filter(item -> item.getName().equalsIgnoreCase(name))
                        .findFirst().orElse(null);
                if (existing != null) {
                    // Add quantity to existing item and update price
                    existing.setQuantity(existing.getQuantity() + quantity);
                    existing.setUnitPrice(price);
                    existing.setMinStockLevel(minStock);
                    dataManager.updateItem(existing);
                    showAlert("Success", "Quantity, price, and min stock updated for existing item!");
                } else {
                    // Create new item
                    InventoryItem newItem = new InventoryItem(name, category, description, quantity, minStock, price, supplier);
                    dataManager.addItem(newItem);
                    showAlert("Success", "Item added successfully!");
                    // Ensure supplier shows up in the dropdown for future entries
                    addSupplierToComboIfMissing(supplier);
                }
            } else if (currentMode == Mode.EDIT && currentItem != null) {
                // Update existing item
                currentItem.setName(name);
                currentItem.setCategory(category);
                currentItem.setDescription(description);
                currentItem.setQuantity(quantity);
                currentItem.setMinStockLevel(minStock);
                currentItem.setUnitPrice(price);
                currentItem.setSupplier(supplier);

                dataManager.updateItem(currentItem);
                showAlert("Success", "Item updated successfully!");
                // Ensure supplier shows up in the dropdown
                addSupplierToComboIfMissing(supplier);
            }

            closeDialog();

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid price.");
        } catch (Exception e) {
            showAlert("Error", "An error occurred while saving: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddCategory() {
        // Build a custom dialog with Name + Description
        Dialog<java.util.Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add Category");
        dialog.setHeaderText(null);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Category name");
        TextField descField = new TextField();
        descField.setPromptText("Description (required)");

        grid.add(new javafx.scene.control.Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable Add button based on validation
    Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        nameField.textProperty().addListener((obs, oldV, newV) -> {
            addButton.setDisable(newV == null || newV.trim().isEmpty() || descField.getText() == null || descField.getText().trim().isEmpty());
        });
        descField.textProperty().addListener((obs, oldV, newV) -> {
            addButton.setDisable(newV == null || newV.trim().isEmpty() || nameField.getText() == null || nameField.getText().trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                java.util.Map<String, String> result = new java.util.HashMap<>();
                result.put("name", nameField.getText().trim());
                result.put("description", descField.getText().trim());
                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(map -> {
            String cat = map.getOrDefault("name", "").trim();
            String desc = map.getOrDefault("description", "").trim();
            if (cat.isEmpty() || desc.isEmpty()) {
                showAlert("Validation", "Name and description are required.");
                return;
            }
            boolean addedToDb = false;
            try {
                int id = DatabaseManager.getInstance().insertCategory(cat, desc);
                if (id > 0) addedToDb = true;
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(ItemFormController.class.getName()).warning("Failed to insert category into DB: " + e.getMessage());
            }
            // Persist to categories.json fallback and DataManager
            boolean addedToJson = DataManager.getInstance().addCategory(cat);

            if (!addedToDb && !addedToJson) {
                showAlert("Add Category", "Category already exists or could not be added.");
            } else {
                refreshCategories();
                categoryComboBox.setValue(cat);
                showAlert("Added", "Category added.");
            }
        });
    }

    @FXML
    private void handleAddSupplier() {
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle("Add Supplier");
        dialog.setHeaderText(null);

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Supplier name");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact number");
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        TextField suppliedProductField = new TextField();
        suppliedProductField.setPromptText("Supplied product (optional)");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Contact:"), 0, 1);
        grid.add(contactField, 1, 1);
        grid.add(new Label("Address:"), 0, 2);
        grid.add(addressField, 1, 2);
        grid.add(new Label("Product:"), 0, 3);
        grid.add(suppliedProductField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node addNode = dialog.getDialogPane().lookupButton(addBtn);
        addNode.setDisable(true);

        nameField.textProperty().addListener((obs, oldV, newV) -> {
            addNode.setDisable(newV == null || newV.trim().isEmpty());
        });

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                Supplier s = new Supplier(nameField.getText().trim(), contactField.getText().trim(), addressField.getText().trim(), suppliedProductField.getText().trim());
                return s;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(sup -> {
            // Persist supplier via DataManager
            DataManager.getInstance().addSupplier(sup);
            // Update combo box items
            if (supplierComboBox != null) {
                if (!supplierComboBox.getItems().contains(sup.getName())) {
                    supplierComboBox.getItems().add(sup.getName());
                    supplierComboBox.getItems().sort(String.CASE_INSENSITIVE_ORDER);
                }
                supplierComboBox.getEditor().setText(sup.getName());
            }
        });
    }

    @FXML
    private void handleRemoveSupplier() {
        String selected = supplierComboBox.getEditor().getText();
        if (selected == null || selected.trim().isEmpty()) {
            showAlert("Remove Supplier", "Please select a supplier to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Remove Supplier");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to remove supplier '" + selected + "'? This will remove it from the supplier list.");

        java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) {
            boolean ok = DataManager.getInstance().removeSupplierByName(selected);
            if (ok) {
                // remove from combo box and clear editor if it was selected
                supplierComboBox.getItems().removeIf(s -> s.equalsIgnoreCase(selected));
                if (supplierComboBox.getEditor().getText().equalsIgnoreCase(selected)) {
                    supplierComboBox.getEditor().clear();
                }
                showAlert("Removed", "Supplier removed successfully.");
            } else {
                showAlert("Remove Supplier", "Supplier not found or could not be removed.");
            }
        }
    }

    @FXML
    private void handleRemoveCategory() {
        String selected = categoryComboBox.getValue();
        if (selected == null || selected.trim().isEmpty()) {
            showAlert("Remove Category", "Please select a category to remove.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Remove");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete category '" + selected + "'? This will only remove the category name from the categories list.");
        java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) {
            // Best-effort: remove from DB then from UI list
            boolean deletedFromDb = false;
            try {
                deletedFromDb = DatabaseManager.getInstance().deleteCategory(selected);
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(ItemFormController.class.getName()).warning("Failed to delete category from DB: " + e.getMessage());
            }
            boolean deletedFromJson = DataManager.getInstance().removeCategory(selected);
            // Refresh the category list to reflect change
            refreshCategories();
            if (deletedFromDb || deletedFromJson) {
                if (selected.equals(categoryComboBox.getValue())) categoryComboBox.setValue(null);
                showAlert("Removed", "Category removed successfully.");
            } else {
                showAlert("Remove Category", "Category not found or could not be removed.");
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        closeDialog();
    }
    
    private boolean validateInput() {
        // Check required fields
        if (nameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Item name is required.");
            nameField.requestFocus();
            return false;
        }
        
        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().trim().isEmpty()) {
            showAlert("Validation Error", "Category is required.");
            categoryComboBox.requestFocus();
            return false;
        }
        
        if (priceField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Unit price is required.");
            priceField.requestFocus();
            return false;
        }
        
        String supplierText = supplierComboBox.getEditor().getText();
        if (supplierText == null || supplierText.trim().isEmpty()) {
            showAlert("Validation Error", "Supplier is required.");
            supplierComboBox.requestFocus();
            return false;
        }
        
        // Validate price
        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price < 0) {
                showAlert("Validation Error", "Price cannot be negative.");
                priceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid price.");
            priceField.requestFocus();
            return false;
        }
        
        // Validate quantity and min stock
        if (quantitySpinner.getValue() < 0) {
            showAlert("Validation Error", "Quantity cannot be negative.");
            quantitySpinner.requestFocus();
            return false;
        }
        
        if (minStockSpinner.getValue() < 0) {
            showAlert("Validation Error", "Minimum stock level cannot be negative.");
            minStockSpinner.requestFocus();
            return false;
        }
        
        // Only check for duplicate names when editing (excluding current item)
        if (currentMode == Mode.EDIT && currentItem != null) {
            // Check for duplicate names when editing (excluding current item)
            String newName = nameField.getText().trim();
            boolean nameExists = dataManager.getInventoryItems().stream()
                    .filter(item -> item.getId() != currentItem.getId())
                    .anyMatch(item -> item.getName().equalsIgnoreCase(newName));
            
            if (nameExists) {
                showAlert("Validation Error", "An item with this name already exists.");
                nameField.requestFocus();
                return false;
            }
        }
        
        return true;
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Removed duplicate setupForm()

    private void clearForm() {
        nameField.clear();
        categoryComboBox.setValue("");
        descriptionArea.clear();
        quantitySpinner.getValueFactory().setValue(0);
        minStockSpinner.getValueFactory().setValue(10);
    priceField.clear();
    if (supplierComboBox.getEditor() != null) supplierComboBox.getEditor().clear();
    }

    // Helper: add supplier to combo list (sorted) if not present
    private void addSupplierToComboIfMissing(String supplier) {
        if (supplier == null) return;
        String sup = supplier.trim();
        if (sup.isEmpty()) return;
        if (!supplierComboBox.getItems().contains(sup)) {
            supplierComboBox.getItems().add(sup);
            // sort case-insensitively
            supplierComboBox.getItems().sort(String.CASE_INSENSITIVE_ORDER);
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
