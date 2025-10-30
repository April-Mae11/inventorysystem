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
// GridPane import removed (unused)
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Comparator;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.HBox;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class CashierController implements Initializable {
    
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
    @FXML private TableColumn<InventoryItem, String> statusColumn;
    
    // ENCAPSULATION: Private fields for internal state management
    // Filter and Search Components - ENCAPSULATION
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private CheckBox lowStockFilter;
    @FXML private CheckBox outOfStockFilter;

    // Action Button Components - ENCAPSULATION (Limited access for Cashier)
    @FXML private Button useItemButton;
    @FXML private Button viewArchiveButton;
    @FXML private Button refreshButton;
    @FXML private Button logoutButton;
    @FXML private Button stockInButton;
    @FXML private Button stockOutButton;

    // Statistics Display Components - ENCAPSULATION
    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockItemsLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label totalQuantityLabel;
    @FXML private Label userInfoLabel;
    @FXML private Label timeLabel;

    // ENCAPSULATION: Private fields for internal state management
    private FilteredList<InventoryItem> filteredData;
    private DataManager dataManager; // COMPOSITION: Uses DataManager for data access
    private AuthManager authManager; // COMPOSITION: Uses AuthManager for authentication
    private ArchiveManager archiveManager; // COMPOSITION: Uses ArchiveManager for archive access
    private javafx.animation.Timeline clockTimeline;
    // PauseTransition used to auto-hide inline messages (e.g., checkout success)
    private PauseTransition msgHideTimer;
    
    public static boolean wasManager = false;
    public static String previousManagerName = null;
    
        // Removed unused PreviousRoleTracker class

        // Simple inner class representing a line in the POS cart
        public static class POSLine {
            private final javafx.beans.property.SimpleStringProperty name;
            private final double unitPrice;
            private final javafx.beans.property.SimpleIntegerProperty quantity;

            public POSLine(String name, double unitPrice, int quantity) {
                this.name = new javafx.beans.property.SimpleStringProperty(name);
                this.unitPrice = unitPrice;
                this.quantity = new javafx.beans.property.SimpleIntegerProperty(quantity);
            }

            public String getName() { return name.get(); }
            public javafx.beans.property.SimpleStringProperty nameProperty() { return name; }

            public double getUnitPrice() { return unitPrice; }

            public int getQuantity() { return quantity.get(); }
            public void setQuantity(int q) { this.quantity.set(q); }
            public javafx.beans.property.SimpleIntegerProperty quantityProperty() { return quantity; }

            public double getTotal() { return unitPrice * getQuantity(); }
        }
    
    /**
     * POLYMORPHISM: Override Initializable.initialize()
     * ABSTRACTION: Hides complex initialization behind simple method call
     * This method is called automatically by JavaFX after FXML loading
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // COMPOSITION: Use DataManager, AuthManager, and ArchiveManager singletons for data access
        dataManager = DataManager.getInstance();
        authManager = AuthManager.getInstance();
        archiveManager = ArchiveManager.getInstance();
        
        // Load archive data
        archiveManager.loadArchiveData();
        
        // ABSTRACTION: Break down initialization into manageable methods
        setupTableColumns();
        setupFilters();
        setupButtons();
        loadData();
        updateStatistics();
    updateUserInfo();
    startClock();
        
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

        // Add spacing/padding for textual columns so the table looks less cramped
        nameColumn.setCellFactory(col -> new TableCell<InventoryItem, String>() {
            private final Label lbl = new Label();
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(lbl);
            {
                box.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                lbl.setStyle("-fx-text-fill: -fx-text-base-color;");
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setEllipsisString("...");
                javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else { lbl.setText(item); setGraphic(box); }
            }
        });

        categoryColumn.setCellFactory(col -> new TableCell<InventoryItem, String>() {
            private final Label lbl = new Label();
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(lbl);
            {
                box.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                lbl.setStyle("-fx-text-fill: -fx-text-base-color;");
                lbl.setMaxWidth(Double.MAX_VALUE);
                javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else { lbl.setText(item); setGraphic(box); }
            }
        });

        descriptionColumn.setCellFactory(col -> new TableCell<InventoryItem, String>() {
            private final Label lbl = new Label();
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(lbl);
            {
                box.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                lbl.setStyle("-fx-text-fill: -fx-text-base-color;");
                lbl.setWrapText(true);
                lbl.setMaxWidth(Double.MAX_VALUE);
                javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else { lbl.setText(item); setGraphic(box); }
            }
        });

        supplierColumn.setCellFactory(col -> new TableCell<InventoryItem, String>() {
            private final Label lbl = new Label();
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(lbl);
            {
                box.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                lbl.setStyle("-fx-text-fill: -fx-text-base-color;");
                lbl.setMaxWidth(Double.MAX_VALUE);
                javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else { lbl.setText(item); setGraphic(box); }
            }
        });
        
    // ABSTRACTION: Hide column width configuration — adjusted to match desired spacing
    nameColumn.setPrefWidth(180);
    categoryColumn.setPrefWidth(140);
    descriptionColumn.setPrefWidth(360);
    quantityColumn.setPrefWidth(80);
    minStockColumn.setPrefWidth(80);
    priceColumn.setPrefWidth(100);
    supplierColumn.setPrefWidth(180);
    statusColumn.setPrefWidth(110);

    // Give table rows a bit more vertical space to match the visual design
    try { inventoryTable.setFixedCellSize(34); } catch (Exception ignored) {}
        
        // Allow single selection for Cashier role
        inventoryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Align numeric columns to the right for readability
        quantityColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        minStockColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Prevent users from reordering columns (keeps consistent layout)
        nameColumn.setReorderable(false);
        categoryColumn.setReorderable(false);
        descriptionColumn.setReorderable(false);
        quantityColumn.setReorderable(false);
        minStockColumn.setReorderable(false);
        priceColumn.setReorderable(false);
        supplierColumn.setReorderable(false);
        statusColumn.setReorderable(false);

        // Enforce column order to match the screenshot: Item Name, Category, Description,
        // Quantity, Min Stock, Unit Price, Supplier, Status
        javafx.collections.ObservableList<javafx.scene.control.TableColumn<InventoryItem, ?>> cols = javafx.collections.FXCollections.observableArrayList();
        cols.add(nameColumn);
        cols.add(categoryColumn);
        cols.add(descriptionColumn);
        cols.add(quantityColumn);
        cols.add(minStockColumn);
        cols.add(priceColumn);
        cols.add(supplierColumn);
        cols.add(statusColumn);
        inventoryTable.getColumns().setAll(cols);

        // Rename the visible header from 'Name' to 'Item Name' for clarity
        try {
            nameColumn.setText("Item Name");
        } catch (Exception ignored) {}
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

        // Setup out of stock filter
        if (outOfStockFilter != null) {
            outOfStockFilter.selectedProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
        
    // Create sorted list and apply a programmatic alphabetical comparator (A → Z)
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
        // Keep Use Item button enabled at all times (allow adding items manually)
        useItemButton.setDisable(false);
    }
    
    private void loadData() {
        // Update category filter options
        updateCategoryFilter();
    }
    
    private void updateCategoryFilter() {
        String currentSelection = categoryFilter.getValue();
        List<String> extraCategories = List.of(
            "Sticker", "Special Paper", "Rush ID", "Lamination Supplies", "Document print", "Lamination", "Sintra board", "Tarpaulin"
        );
        var categories = FXCollections.observableArrayList("All Categories");
        List<String> allCategories = new ArrayList<>(dataManager.getAllCategories());
        allCategories.addAll(extraCategories);
        // Normalize categories: merge 'Ink' and 'Toner' into 'Ink & Toner'
        Set<String> uniqueCategories = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String c : allCategories) {
            if (c == null) continue;
            String t = c.trim();
            if (t.equalsIgnoreCase("ink") || t.equalsIgnoreCase("toner")) {
                uniqueCategories.add("Ink & Toner");
            } else if (!t.isEmpty()) {
                uniqueCategories.add(t);
            }
        }
        List<String> sortedCategories = new ArrayList<>(uniqueCategories);
        Collections.sort(sortedCategories, String.CASE_INSENSITIVE_ORDER);
        categories.addAll(sortedCategories);
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
    totalValueLabel.setText(String.format("₱%,.2f", stats.getTotalValue()));
        totalQuantityLabel.setText(String.valueOf(stats.getTotalQuantity()));
    }
    
    private void updateUserInfo() {
        User currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            userInfoLabel.setText("Logged in as: " + currentUser.getFullName() + " (" + currentUser.getRole().getDisplayName().toUpperCase() + ")");
        }
    }

    private void startClock() {
        if (timeLabel == null) return;
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a");
        // Initialize immediately with current time
        timeLabel.setText(java.time.LocalDateTime.now().format(fmt));

        // Use a 1-second polling timeline but only update the label when the
        // formatted minute/AMPM string actually changes. This ensures the
        // displayed minute matches the system clock without showing seconds.
        clockTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                String formatted = java.time.LocalDateTime.now().format(fmt);
                if (!formatted.equals(timeLabel.getText())) {
                    timeLabel.setText(formatted);
                }
            })
        );
        clockTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clockTimeline.play();
    }
    
    @FXML
    private void handleUseItem() {
        // POS-style window: allow adding multiple items, show cart, total, payment and change
        // We'll open the POS in a maximized Stage instead of using a Dialog.

    // Main layout - increase default size so POS dialog isn't compressed
    javafx.scene.layout.BorderPane pane = new javafx.scene.layout.BorderPane();
    pane.setPrefSize(1300, 820);

    // Top header for POS dialog: centered label. The Back button will be placed on the footer's right side.
    javafx.scene.control.Button backBtn = new javafx.scene.control.Button("Back");
    // Make the back button visually neutral (gray) and accessible
    backBtn.setStyle("-fx-background-color: #d0d0d0; -fx-text-fill: black; -fx-border-color: #bfbfbf; -fx-border-width:1px; -fx-padding:6 12 6 12;");
    // Close the window when Back is pressed (resolve Stage from node)
    backBtn.setOnAction(ev -> {
        try {
            javafx.stage.Window w = backBtn.getScene().getWindow();
            if (w instanceof javafx.stage.Stage) ((javafx.stage.Stage) w).close();
        } catch (Exception ignored) {}
    });

    javafx.scene.control.Label headerLabel = new javafx.scene.control.Label("NVA Printing Services | POS Dashboard");
    headerLabel.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill: white;");

    javafx.scene.layout.VBox headerBox = new javafx.scene.layout.VBox(6);
    headerBox.setPadding(new javafx.geometry.Insets(10, 8, 8, 8));
    headerBox.setStyle("-fx-background-color: #a70104;");
    javafx.scene.layout.HBox labelRow = new javafx.scene.layout.HBox();
    labelRow.setAlignment(javafx.geometry.Pos.CENTER);
    labelRow.getChildren().add(headerLabel);
    javafx.scene.layout.HBox backRow = new javafx.scene.layout.HBox();
    backRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    backRow.getChildren().add(backBtn);
    headerBox.getChildren().addAll(labelRow, backRow);
    pane.setTop(headerBox);

        // Left: product selector and add controls
        javafx.scene.layout.VBox left = new javafx.scene.layout.VBox(10);
        left.setPadding(new javafx.geometry.Insets(8));
        left.setPrefWidth(300);

        javafx.scene.control.Label selectLabel = new javafx.scene.control.Label("Select Item:");
        // Build an observable list of item names and back the ListView with a FilteredList
        javafx.collections.ObservableList<String> baseItemNames = javafx.collections.FXCollections.observableArrayList();
        for (InventoryItem it : dataManager.getInventoryItems()) {
            if (it.getName() != null) baseItemNames.add(it.getName());
        }
        // Sort suggestions A -> Z (case-insensitive)
        FXCollections.sort(baseItemNames, String.CASE_INSENSITIVE_ORDER);
        javafx.collections.transformation.FilteredList<String> filteredItems = new javafx.collections.transformation.FilteredList<>(baseItemNames, s -> true);

    // Text field used as the editor for typing/searching
    javafx.scene.control.TextField itemField = new javafx.scene.control.TextField();
    // bind width to the left panel so it never overflows and the cart stays visible
    itemField.prefWidthProperty().bind(left.widthProperty().subtract(16));
    itemField.setPromptText("Type to search or select an item");

    // Inline ListView used for showing suggestions (placed under the TextField)
    javafx.scene.control.ListView<String> itemListView = new javafx.scene.control.ListView<>(filteredItems);
    // bind list width to the left panel so it matches the search field
    itemListView.prefWidthProperty().bind(left.widthProperty().subtract(16));
        itemListView.setPrefHeight(280);
    // start hidden; use managed property so layout collapses when hidden
    itemListView.setVisible(false);
    itemListView.setManaged(false);
    // Show stock remaining in the suggestion list using a two-column HBox (name + right-aligned stock)
    itemListView.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
        @Override
        protected void updateItem(String name, boolean empty) {
            super.updateItem(name, empty);
            if (empty || name == null) {
                setText(null);
                setGraphic(null);
            } else {
                InventoryItem it = dataManager.getAllItems().stream()
                        .filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(name))
                        .findFirst().orElse(null);
                String stockTxt = it == null ? "" : "(" + it.getQuantity() + " left)";
                javafx.scene.control.Label nameLbl = new javafx.scene.control.Label(name);
                javafx.scene.control.Label stockLbl = new javafx.scene.control.Label(stockTxt);
                stockLbl.setStyle("-fx-text-fill: -fx-text-base-color; -fx-opacity: 0.8;");
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8, nameLbl, new javafx.scene.layout.Region(), stockLbl);
                javafx.scene.layout.HBox.setHgrow(box.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
                setGraphic(box);
                setText(null);
            }
        }
    });

    // Reference holder for performAdd so early handlers can call it without forward reference
    final java.util.concurrent.atomic.AtomicReference<java.lang.Runnable> performAddRef = new java.util.concurrent.atomic.AtomicReference<>();
    // When true, the text listener will not reopen the suggestion list (used during programmatic clears)
    final java.util.concurrent.atomic.AtomicBoolean suppressSuggestionShow = new java.util.concurrent.atomic.AtomicBoolean(false);

    // Filter suggestions as the user types
        itemField.textProperty().addListener((obs, oldV, newV) -> {
            String filter = newV == null ? "" : newV.toLowerCase();
            filteredItems.setPredicate(s -> {
                if (filter.isEmpty()) return true;
                return s != null && s.toLowerCase().contains(filter);
            });
            if (!filteredItems.isEmpty()) {
                if (!itemListView.isVisible() && !suppressSuggestionShow.get()) {
                    javafx.application.Platform.runLater(() -> {
                        itemListView.setVisible(true);
                        itemListView.setManaged(true);
                    });
                }
            } else {
                if (itemListView.isVisible()) {
                    itemListView.setVisible(false);
                    itemListView.setManaged(false);
                }
            }
        });

        // Show popup on click and focus
        itemField.setOnMouseClicked(e -> {
            itemField.requestFocus();
            if (!filteredItems.isEmpty() && !itemListView.isVisible()) {
                javafx.application.Platform.runLater(() -> {
                    itemListView.setVisible(true);
                    itemListView.setManaged(true);
                });
            }
        });

        

    // Keyboard navigation for TextField -> navigate the ListView and accept on Enter
        itemField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ke -> {
            javafx.scene.input.KeyCode code = ke.getCode();
            if (code == javafx.scene.input.KeyCode.DOWN) {
                if (filteredItems.isEmpty()) return;
                int sel = itemListView.getSelectionModel().getSelectedIndex();
                if (sel < 0) sel = 0; else sel = Math.min(filteredItems.size() - 1, sel + 1);
                itemListView.getSelectionModel().select(sel);
                itemListView.scrollTo(sel);
                if (!itemListView.isVisible()) {
                    itemListView.setVisible(true);
                    itemListView.setManaged(true);
                }
                ke.consume();
            } else if (code == javafx.scene.input.KeyCode.UP) {
                if (filteredItems.isEmpty()) return;
                int sel = itemListView.getSelectionModel().getSelectedIndex();
                if (sel < 0) sel = filteredItems.size() - 1; else sel = Math.max(0, sel - 1);
                itemListView.getSelectionModel().select(sel);
                itemListView.scrollTo(sel);
                if (!itemListView.isVisible()) {
                    itemListView.setVisible(true);
                    itemListView.setManaged(true);
                }
                ke.consume();
            } else if (code == javafx.scene.input.KeyCode.ENTER) {
                // Apply the currently highlighted suggestion (if any). Do NOT call performAdd here
                // to avoid duplicate adds; the canonical add is performed by itemField.setOnAction
                String picked = itemListView.getSelectionModel().getSelectedItem();
                if (picked == null && !filteredItems.isEmpty()) picked = filteredItems.get(0);
                if (picked != null) {
                    itemField.setText(picked);
                }
                if (itemListView.isVisible()) { itemListView.setVisible(false); itemListView.setManaged(false); }
                // do not consume; allow TextField onAction to run once
            }
        });

        // Note: we rely on itemField.setOnAction to perform the add when Enter is pressed

        // Mouse select from the list
        itemListView.setOnMouseClicked(me -> {
            String sel = itemListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                itemField.setText(sel);
            }
            if (itemListView.isVisible()) { itemListView.setVisible(false); itemListView.setManaged(false); }
            itemField.requestFocus();
        });

        javafx.scene.control.Label qtyLabel = new javafx.scene.control.Label("Quantity:");
        javafx.scene.control.Spinner<Integer> qtySpinner = new javafx.scene.control.Spinner<>(1, 9999, 1);
        qtySpinner.setMaxWidth(90);
        // Allow typing into the spinner editor and commit on focus lost or Enter
        qtySpinner.setEditable(true);
        javafx.util.StringConverter<Integer> intConv = new javafx.util.converter.IntegerStringConverter();
        qtySpinner.getValueFactory().setConverter(intConv);
        // Commit typed value when Enter is pressed in the editor
        qtySpinner.getEditor().setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.ENTER) {
                try {
                    String txt = qtySpinner.getEditor().getText().trim();
                    int v = Integer.parseInt(txt);
                    if (v < 1) v = 1;
                    if (v > 9999) v = 9999;
                    qtySpinner.getValueFactory().setValue(v);
                } catch (NumberFormatException ex) {
                    // revert to last value
                    qtySpinner.getEditor().setText(String.valueOf(qtySpinner.getValue()));
                }
            }
        });
        // Commit on focus lost as well
        qtySpinner.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                try {
                    String txt = qtySpinner.getEditor().getText().trim();
                    int v = Integer.parseInt(txt);
                    if (v < 1) v = 1;
                    if (v > 9999) v = 9999;
                    qtySpinner.getValueFactory().setValue(v);
                } catch (NumberFormatException ex) {
                    qtySpinner.getEditor().setText(String.valueOf(qtySpinner.getValue()));
                }
            }
        });

    javafx.scene.control.Button addButton = new javafx.scene.control.Button("Add to Cart");
    javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("Remove Selected");
    // small emoji graphics for buttons
    javafx.scene.control.Label addIcon = new javafx.scene.control.Label("🛒");
    addIcon.setStyle("-fx-font-size:14px; -fx-padding:0 6 0 0;");
    addButton.setGraphic(addIcon);
    javafx.scene.control.Label remIcon = new javafx.scene.control.Label("X");
    remIcon.setStyle("-fx-font-size:13px; -fx-padding:0 6 0 0; -fx-text-fill: black; -fx-font-weight:bold;");
    removeBtn.setGraphic(remIcon);

    // Apply POS-specific styles
    addButton.getStyleClass().add("pos-add");
    removeBtn.getStyleClass().add("pos-remove");

    left.getChildren().addAll(selectLabel, itemField, itemListView, qtyLabel, qtySpinner, addButton, removeBtn);

        // Center: cart table
    javafx.scene.control.TableView<POSLine> cartTable = new javafx.scene.control.TableView<>();
    // Use a lambda-based policy to avoid deprecated constant usage while keeping constrained behavior
    cartTable.setColumnResizePolicy(table -> true);
    cartTable.setPrefWidth(880);
    cartTable.setPrefHeight(760);
        javafx.scene.control.TableColumn<POSLine, String> colName = new javafx.scene.control.TableColumn<>("Item");
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        javafx.scene.control.TableColumn<POSLine, Integer> colQty = new javafx.scene.control.TableColumn<>("Qty");
        colQty.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        javafx.scene.control.TableColumn<POSLine, String> colPrice = new javafx.scene.control.TableColumn<>("Unit Price");
        colPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.format("₱%.2f", c.getValue().getUnitPrice())));
        javafx.scene.control.TableColumn<POSLine, String> colTotal = new javafx.scene.control.TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.format("₱%.2f", c.getValue().getTotal())));

    // Give explicit preferred widths to improve layout on larger dialog
    // Limit the Item column so it doesn't push other columns out of view
    colName.setPrefWidth(320);
    colName.setMaxWidth(360);
    colName.setMinWidth(120);
    // Make the Qty column slightly wider to fit the +/- buttons comfortably
    colQty.setPrefWidth(110);
    colQty.setMinWidth(90);
    colQty.setMaxWidth(140);
    colPrice.setPrefWidth(120);
    colTotal.setPrefWidth(120);

    // Use a cell factory that ellipsizes long names and shows full name in a tooltip
    colName.setCellFactory(column -> new TableCell<POSLine, String>() {
        private final Label lbl = new Label();
        private final Tooltip tip = new Tooltip();
        {
            lbl.setMaxWidth(300);
            lbl.setEllipsisString("...");
            // default style
            lbl.setStyle("-fx-text-fill: -fx-text-base-color;");
        }

            @Override
            protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setTooltip(null);
            } else {
                    // display item name and remaining stock in two-column layout (name + right-aligned stock)
                    InventoryItem it = dataManager.getAllItems().stream().filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(item)).findFirst().orElse(null);
                    String stockTxt = it == null ? "" : "(" + it.getQuantity() + " left)";
                    // build HBox with name label, spacer region, and stock label
                    javafx.scene.control.Label nameLbl = new javafx.scene.control.Label(item);
                    nameLbl.setMaxWidth(300);
                    nameLbl.setEllipsisString("...");
                    javafx.scene.control.Label stockLbl = new javafx.scene.control.Label(stockTxt);
                    stockLbl.setStyle("-fx-text-fill: -fx-text-base-color; -fx-opacity: 0.85;");
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12, nameLbl, spacer, stockLbl);
                        row.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
                        tip.setText(item + (stockTxt.isEmpty() ? "" : " " + stockTxt));
                        // color labels according to selection
                        TableRow<POSLine> r = getTableRow();
                        javafx.scene.paint.Color fill = (r != null && r.isSelected()) ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK;
                        nameLbl.setTextFill(fill);
                        stockLbl.setTextFill(fill);
                        setGraphic(row);
                        setText(null);
                        setTooltip(tip);
            }
        }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                // update labels' text fill when selection changes
                if (getGraphic() instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox hb = (javafx.scene.layout.HBox) getGraphic();
                    if (hb.getChildren().size() >= 3) {
                        java.lang.Object left = hb.getChildren().get(0);
                        java.lang.Object right = hb.getChildren().get(2);
                        if (left instanceof javafx.scene.control.Label) ((javafx.scene.control.Label) left).setTextFill(selected ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
                        if (right instanceof javafx.scene.control.Label) ((javafx.scene.control.Label) right).setTextFill(selected ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
                    }
                }
            }
    });

    cartTable.getColumns().add(colName);
    cartTable.getColumns().add(colQty);
    cartTable.getColumns().add(colPrice);
    cartTable.getColumns().add(colTotal);

    // Make other columns respect selection text color as well
    // Make Qty editable: use TextFieldTableCell so user may type to adjust quantity
    colQty.setCellFactory(col -> new TableCell<POSLine, Integer>() {
        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else {
                setText(String.valueOf(item));
                TableRow<POSLine> r = getTableRow();
                setTextFill(r != null && r.isSelected() ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
                setStyle("-fx-alignment: CENTER; -fx-padding: 0 6 0 6;");
            }
        }
    });
    // (Qty editing will be enabled after the cart and updateTotals are declared below)
    colPrice.setCellFactory(col -> new TableCell<POSLine, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else {
                setText(item);
                TableRow<POSLine> r = getTableRow();
                setTextFill(r != null && r.isSelected() ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
            }
        }
    });
    colTotal.setCellFactory(col -> new TableCell<POSLine, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else {
                setText(item);
                TableRow<POSLine> r = getTableRow();
                setTextFill(r != null && r.isSelected() ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
            }
        }
    });

        // Right/bottom: payment controls
    javafx.scene.layout.VBox right = new javafx.scene.layout.VBox(10);
        right.setPadding(new javafx.geometry.Insets(8));
            right.setPrefWidth(260);

        javafx.scene.control.Label totalLabel = new javafx.scene.control.Label("Total: ₱0.00");
    totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        javafx.scene.control.Label paymentLabel = new javafx.scene.control.Label("Payment Method:");
        javafx.scene.control.ComboBox<String> paymentCombo = new javafx.scene.control.ComboBox<>();
    paymentCombo.getItems().addAll("Cash", "E-wallet");
        paymentCombo.setValue("Cash");
    // show icons in payment combo
    paymentCombo.setCellFactory(list -> new javafx.scene.control.ListCell<String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else {
                javafx.scene.control.Label ic = new javafx.scene.control.Label(item.equals("Cash") ? "💵" : "💳");
                ic.setStyle("-fx-font-size:12px; -fx-padding:0 6 0 0; -fx-text-fill:black;");
                javafx.scene.control.Label txt = new javafx.scene.control.Label(item);
                txt.setStyle("-fx-text-fill:black;");
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, ic, txt);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
            }
        }
    });
    paymentCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else {
                javafx.scene.control.Label ic = new javafx.scene.control.Label(item.equals("Cash") ? "💵" : "💳");
                ic.setStyle("-fx-font-size:12px; -fx-padding:0 6 0 0; -fx-text-fill:black;");
                javafx.scene.control.Label txt = new javafx.scene.control.Label(item);
                txt.setStyle("-fx-text-fill:black;");
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, ic, txt);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
            }
        }
    });

    javafx.scene.control.Label tenderedLabel = new javafx.scene.control.Label("Tendered Amount:");
    javafx.scene.control.TextField tenderedField = new javafx.scene.control.TextField();
    tenderedField.setPromptText("0.00");

    javafx.scene.control.Label ewalletLabel = new javafx.scene.control.Label("E-wallet Ref:");
    javafx.scene.control.TextField ewalletField = new javafx.scene.control.TextField();
    ewalletField.setPromptText("Reference / Transaction ID");
    ewalletField.setDisable(true);

        javafx.scene.control.Label changeLabel = new javafx.scene.control.Label("Change: ₱0.00");
        changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

    javafx.scene.control.Button checkoutBtn = new javafx.scene.control.Button("Checkout");
    javafx.scene.control.Label checkoutIcon = new javafx.scene.control.Label("✅");
    checkoutIcon.setStyle("-fx-font-size:13px; -fx-padding:0 6 0 0;");
    checkoutBtn.setGraphic(checkoutIcon);
    // Apply POS checkout style
    checkoutBtn.getStyleClass().add("pos-checkout");
        // Inline message area for checkout results (replaces popup)
        javafx.scene.control.Label msgLabel = new javafx.scene.control.Label("");
    msgLabel.setWrapText(true);
    msgLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 12px;");

    right.getChildren().addAll(totalLabel, paymentLabel, paymentCombo, tenderedLabel, tenderedField, ewalletLabel, ewalletField, changeLabel, checkoutBtn, msgLabel);

    // Give the three regions reasonable max widths so they layout predictably
    left.setMaxWidth(360);
    cartTable.setMaxWidth(980);
    right.setMaxWidth(360);

    // Place nodes
    pane.setLeft(left);
    // Center: cart table with date/time label underneath
    // Center: cart table
    javafx.scene.layout.VBox centerBox = new javafx.scene.layout.VBox(6, cartTable);
    centerBox.setPadding(new javafx.geometry.Insets(8));
    pane.setCenter(centerBox);
    pane.setRight(right);

        // Footer: bottom-left branding similar to cashier dashboard (no date) and colored per theme
    javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox();
    // Increase padding and set a preferred height so the footer is more prominent
    footer.setPadding(new javafx.geometry.Insets(12, 12, 12, 12));
    footer.setStyle("-fx-background-color: #a70104;");
    footer.setPrefHeight(56);
    footer.setMinHeight(48);
    javafx.scene.control.Label footerLeft = new javafx.scene.control.Label("© NVA Printing Services");
    footerLeft.setStyle("-fx-font-size:12px; -fx-text-fill: white; -fx-font-weight:bold;");
    javafx.scene.layout.Region footerSpacer = new javafx.scene.layout.Region();
    javafx.scene.layout.HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
    // Right-aligned footer time label
    javafx.scene.control.Label footerTime = new javafx.scene.control.Label("");
    footerTime.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 0 8 0 8;");
    footer.getChildren().addAll(footerLeft, footerSpacer, footerTime);
    // Place Back button at the bottom-right of the POS window inside the footer
    // Ensure it is right-aligned and visually separated
    javafx.scene.layout.HBox backContainer = new javafx.scene.layout.HBox();
    backContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
    backContainer.getChildren().add(backBtn);
    backContainer.setPadding(new javafx.geometry.Insets(0, 8, 0, 0));
    // Add a small spacer and then the back button container so it's pinned to the right
    footer.getChildren().add(backContainer);
    pane.setBottom(footer);

    // Instead of using a Dialog, open a full window Stage so POS Dashboard can be maximized
    javafx.stage.Stage posStage = new javafx.stage.Stage();
    posStage.setTitle("POS Dashboard");
    javafx.scene.Scene posScene = new javafx.scene.Scene(pane);
    // Ensure POS Scene uses the same stylesheet so pos-* style classes take effect
    try {
        posScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    } catch (Exception ignored) {}
    posStage.setScene(posScene);
    posStage.initOwner(InventoryApplication.getPrimaryStage());
    posStage.setResizable(true);
    posStage.setMaximized(true);
    posStage.show();

    // Start a small timeline to update the footer time every second while POS is open
    javafx.animation.Timeline posFooterClock = new javafx.animation.Timeline(
        new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> {
            try {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
                footerTime.setText(java.time.LocalDateTime.now().format(fmt));
            } catch (Exception ignored) {}
        })
    );
    posFooterClock.setCycleCount(javafx.animation.Animation.INDEFINITE);
    posFooterClock.play();

    // Ensure the timeline is stopped when the POS stage is closed
    posStage.setOnHidden(ev -> {
        try { posFooterClock.stop(); } catch (Exception ignored) {}
    });

        javafx.collections.ObservableList<POSLine> cart = javafx.collections.FXCollections.observableArrayList();
        cartTable.setItems(cart);

        // helper: update totals
        Runnable updateTotals = () -> {
            double tot = 0.0;
            for (POSLine l : cart) tot += l.getTotal();
            totalLabel.setText(String.format("Total: ₱%.2f", tot));
            try {
                double tendered = Double.parseDouble(tenderedField.getText());
                double change = tendered - tot;
                if (change < 0) changeLabel.setText(String.format("Change: -₱%.2f", Math.abs(change)));
                else changeLabel.setText(String.format("Change: ₱%.2f", change));
            } catch (Exception ex) {
                changeLabel.setText("Change: ₱0.00");
            }
        };

        // Enable editing support on the Qty column now that cart and updateTotals are available
        colQty.setEditable(true);
        cartTable.setEditable(true);
        // Use a text input dialog when double-clicking to edit a quantity (keeps UI simple)
        colQty.setOnEditStart(evt -> {
            // no-op placeholder (we handle edits via a simple double-click handler below)
        });
        // Allow double-click to edit quantity via dialog for simplicity and better numeric entry on touch
        cartTable.setRowFactory(tv -> {
            TableRow<POSLine> row = new TableRow<>();
            row.setOnMouseClicked(me -> {
                if (me.getClickCount() == 2 && !row.isEmpty()) {
                    POSLine line = row.getItem();
                    TextInputDialog dlg = new TextInputDialog(String.valueOf(line.getQuantity()));
                    dlg.setTitle("Edit Quantity");
                    dlg.setHeaderText("Edit quantity for: " + line.getName());
                    dlg.setContentText("Enter new quantity:");
                    java.util.Optional<String> r = dlg.showAndWait();
                    if (r.isPresent()) {
                        try {
                            int newQty = Integer.parseInt(r.get().trim());
                            if (newQty <= 0) {
                                cart.remove(line);
                            } else {
                                InventoryItem inv = dataManager.getAllItems().stream().filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(line.getName())).findFirst().orElse(null);
                                if (inv != null && newQty > inv.getQuantity()) {
                                    showAlert("Insufficient stock", "Only " + inv.getQuantity() + " available.");
                                } else {
                                    line.setQuantity(newQty);
                                }
                            }
                            cartTable.refresh();
                            updateTotals.run();
                        } catch (NumberFormatException ex) {
                            showAlert("Invalid input", "Please enter a valid integer.");
                        }
                    }
                }
            });
            return row;
        });

        // Add smaller +/- quick buttons in the Qty column for fast adjustments
        colQty.setCellFactory(column -> new TableCell<POSLine, Integer>() {
            private final Button dec = new Button("-");
            private final Button inc = new Button("+");
            private final Label lbl = new Label();
            private final HBox box = new HBox(4, dec, lbl, inc);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                // make the +/- buttons compact
                dec.setFocusTraversable(false);
                inc.setFocusTraversable(false);
                dec.setPrefSize(26, 18);
                inc.setPrefSize(26, 18);
                dec.setMinSize(22, 18);
                inc.setMinSize(22, 18);
                dec.setStyle("-fx-font-size:11px; -fx-padding:0 2 0 2;");
                inc.setStyle("-fx-font-size:11px; -fx-padding:0 2 0 2;");
                lbl.setMinWidth(28);
                lbl.setAlignment(javafx.geometry.Pos.CENTER);
                dec.setOnAction(ae -> {
                    POSLine p = getTableRow() == null ? null : getTableRow().getItem();
                    if (p == null) return;
                    int q = p.getQuantity() - 1;
                    if (q <= 0) {
                        // remove line
                        cart.remove(p);
                    } else {
                        p.setQuantity(q);
                    }
                    cartTable.refresh();
                    updateTotals.run();
                });
                inc.setOnAction(ae -> {
                    POSLine p = getTableRow() == null ? null : getTableRow().getItem();
                    if (p == null) return;
                    p.setQuantity(p.getQuantity() + 1);
                    cartTable.refresh();
                    updateTotals.run();
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(String.valueOf(item));
                    setGraphic(box);
                }
            }
        });

    // Guard to prevent duplicate adds when multiple handlers fire (Enter may trigger several events)
    final java.util.concurrent.atomic.AtomicBoolean addInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Helper to perform add (declared after cart/updateTotals so it can access them)
        java.lang.Runnable performAdd = () -> {
            // prevent re-entrant/duplicate adds (multiple handlers firing on Enter)
            if (!addInProgress.compareAndSet(false, true)) return;
            try {
                String name = itemField.getText();
                int qty = qtySpinner.getValue();
                // If a cart row is selected and the user didn't type a different name,
                // increment the selected row's quantity directly. This allows quick
                // adjustments by selecting a line and using the Quantity control.
                POSLine selLine = cartTable.getSelectionModel().getSelectedItem();
                if (selLine != null && (name == null || name.isBlank() || selLine.getName().equalsIgnoreCase(name))) {
                    // find inventory item for stock check
                    InventoryItem invSel = null;
                    for (InventoryItem it : dataManager.getInventoryItems()) {
                        if (it.getName() != null && selLine.getName().equalsIgnoreCase(it.getName())) { invSel = it; break; }
                    }
                    if (invSel != null && invSel.getQuantity() < qty) {
                        showAlert("Insufficient stock", "Only " + invSel.getQuantity() + " available.");
                        return;
                    }
                    selLine.setQuantity(selLine.getQuantity() + qty);
                    cartTable.refresh();
                    updateTotals.run();
                } else {
                    if (name == null || name.isBlank()) {
                        showAlert("No item", "Please select an item to add.");
                        return;
                    }
                    InventoryItem inv = null;
                    for (InventoryItem it : dataManager.getInventoryItems()) {
                        if (it.getName() != null && name.equalsIgnoreCase(it.getName())) { inv = it; break; }
                    }
                    if (inv == null) { showAlert("Not found", "Selected item not found."); return; }
                    if (inv.getQuantity() < qty) { showAlert("Insufficient stock", "Only " + inv.getQuantity() + " available."); return; }

                    POSLine existing = null;
                    for (POSLine l : cart) if (l.getName().equals(name)) { existing = l; break; }
                    if (existing != null) {
                        existing.setQuantity(existing.getQuantity() + qty);
                        cartTable.refresh();
                    } else {
                        cart.add(new POSLine(name, inv.getUnitPrice(), qty));
                    }
                    updateTotals.run();
                }

                // After adding, reset qty to 1, clear the search box and keep focus there for rapid scanning
                qtySpinner.getValueFactory().setValue(1);
                // prevent the suggestion list from popping back up during our programmatic clear
                suppressSuggestionShow.set(true);
                itemField.clear();
                javafx.application.Platform.runLater(() -> {
                    itemField.requestFocus();
                    // allow suggestions to show again after the current pulse
                    suppressSuggestionShow.set(false);
                });
            } finally {
                addInProgress.set(false);
            }
        };

    // expose performAdd to earlier handlers
    performAddRef.set(performAdd);

        // Add button action
        addButton.setOnAction(ae -> performAdd.run());

        // Also allow Enter key on the itemField to trigger Add (useful for scanners)
        itemField.setOnAction(evt -> performAdd.run());
        // Ensure pressing Enter while the suggestion popup is visible also adds the item
        itemField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.ENTER) {
                // If popup was visible and a suggestion selected, ensure the selection is applied
                String picked = itemListView.getSelectionModel().getSelectedItem();
                if (picked == null && !filteredItems.isEmpty()) picked = filteredItems.get(0);
                if (picked != null) itemField.setText(picked);
                if (itemListView.isVisible()) { itemListView.setVisible(false); itemListView.setManaged(false); }
                performAdd.run();
                ke.consume();
            }
        });

        // remove button: prompt how many units to remove from the selected cart line
        removeBtn.setOnAction(e -> {
            POSLine sel = cartTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("No selection", "Please select a cart line to adjust.");
                return;
            }
            int current = sel.getQuantity();
            TextInputDialog dlg = new TextInputDialog("1");
            dlg.setTitle("Adjust Quantity");
            dlg.setHeaderText("Remove quantity from: " + sel.getName());
            dlg.setContentText("Enter amount to remove (1 - " + current + ") or 0 to remove the line:");
            Optional<String> res = dlg.showAndWait();
            if (res.isPresent()) {
                try {
                    int remove = Integer.parseInt(res.get().trim());
                    if (remove < 0) { showAlert("Invalid input", "Please enter a non-negative number."); return; }
                    if (remove == 0) {
                        // remove entire line
                        cart.remove(sel);
                        updateTotals.run();
                        return;
                    }
                    if (remove > current) { showAlert("Invalid amount", "You cannot remove more than current quantity (" + current + ")."); return; }
                    int newQty = current - remove;
                    if (newQty <= 0) {
                        cart.remove(sel);
                    } else {
                        sel.setQuantity(newQty);
                        cartTable.refresh();
                    }
                    updateTotals.run();
                } catch (NumberFormatException ex) {
                    showAlert("Invalid input", "Please enter a valid integer.");
                }
            }
        });

        // Ensure ListView selection via keyboard or mouse triggers add when performed
        itemListView.setOnMouseClicked(me -> {
            String sel = itemListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                itemField.setText(sel);
            }
            if (itemListView.isVisible()) { itemListView.setVisible(false); itemListView.setManaged(false); }
            // performAdd is available here
            performAdd.run();
        });

        itemListView.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.ENTER) {
                String sel = itemListView.getSelectionModel().getSelectedItem();
                if (sel != null) itemField.setText(sel);
                if (itemListView.isVisible()) { itemListView.setVisible(false); itemListView.setManaged(false); }
                performAdd.run();
                ke.consume();
            }
        });

        // update change when tendered or payment changes
        tenderedField.textProperty().addListener((obs, o, n) -> updateTotals.run());
        paymentCombo.valueProperty().addListener((obs, o, n) -> {
            updateTotals.run();
            boolean isE = "E-wallet".equals(n);
            ewalletField.setDisable(!isE);
            tenderedField.setDisable(isE);
            // If e-wallet selected, auto-fill tendered amount with the cart total
            if (isE) {
                double curTot = 0.0;
                for (POSLine l : cart) curTot += l.getTotal();
                tenderedField.setText(String.format("%.2f", curTot));
            }
        });

        // checkout behavior
        checkoutBtn.setOnAction(e -> {
            if (cart.isEmpty()) { showAlert("Empty cart", "Please add items before checkout."); return; }
            double tot = 0.0; for (POSLine l : cart) tot += l.getTotal();
            String payment = paymentCombo.getValue();
            double tendered = 0.0;
            if ("Cash".equals(payment)) {
                try { tendered = Double.parseDouble(tenderedField.getText()); } catch (Exception ex) { showAlert("Invalid amount", "Please enter a valid tendered amount."); return; }
                if (tendered < tot) { showAlert("Insufficient payment", "Tendered amount is less than total."); return; }
            } else if ("E-wallet".equals(payment)) {
                if (ewalletField.getText() == null || ewalletField.getText().isBlank()) {
                    showAlert("E-wallet required", "Please enter the e-wallet transaction/reference number.");
                    return;
                }
            }

            // Snapshot the cart so we can update the UI immediately and persist in background
            final List<POSLine> snapshot = new java.util.ArrayList<>(cart);

            // Basic validation and prepare transaction metadata
            String user = AuthManager.getInstance().getCurrentUser() != null ? AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "CASHIER";
            final String transactionRef = java.util.UUID.randomUUID().toString();
            final java.time.LocalDateTime when = java.time.LocalDateTime.now();

            // Perform quick, in-memory inventory reductions and logging so the cashier sees updated stock immediately.
            for (POSLine l : snapshot) {
                // find inventory item
                InventoryItem inv = null;
                for (InventoryItem it : dataManager.getInventoryItems()) if (it.getName().equals(l.getName())) { inv = it; break; }
                if (inv == null) continue;

                boolean success = DataManager.getInstance().reduceItemQuantity(inv, l.getQuantity(), user, "Sold via POS");
                if (!success) {
                    showAlert("Error", "Failed to update inventory for " + l.getName());
                    return;
                }

                // Log transaction locally (no DB call here)
                String userDesc = user + " [Tx:" + transactionRef + "]" + ("E-wallet".equals(payment) ? " [ERef:" + (ewalletField.getText() == null ? "" : ewalletField.getText()) + "]" : "");
                TransactionLogger.getInstance().log(l.getName(), "SALE", l.getQuantity(), userDesc, l.getUnitPrice());
            }

            // Build and show the summary message immediately
            // Show formatted summary and change in inline message area instead of popup
            double change = tendered - tot;
            java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            String dateOnly = when.format(dateFmt);
            String timeOnly = when.format(timeFmt);

            StringBuilder sb = new StringBuilder();
            if ("Cash".equals(payment)) {
                sb.append("✅ Payment Successful!\n");
                sb.append(String.format("Total: ₱%.2f | Change: ₱%.2f\n", tot, Math.max(0.0, change)));
                sb.append("Payment Method: CASH\n");
                sb.append(String.format("Date: %s\nTime: %s\n\n", dateOnly, timeOnly));
                sb.append("Transaction saved successfully.");
            } else { // E-wallet
                sb.append("✅ Payment Successful!\n");
                sb.append(String.format("Total: ₱%.2f\n", tot));
                // Display provider as GCash 
                sb.append("Payment Method: GCash\n");
                sb.append(String.format("Transaction ID: %s\n", ewalletField.getText() == null ? "" : ewalletField.getText()));
                sb.append(String.format("Date: %s\nTime: %s\n", dateOnly, timeOnly));
                sb.append("Transaction saved successfully.");
            }
            msgLabel.setText(sb.toString());
            // Auto-hide the inline success message after 8 seconds.
            try {
                if (msgHideTimer != null) {
                    msgHideTimer.stop();
                }
                msgHideTimer = new PauseTransition(Duration.seconds(8));
                msgHideTimer.setOnFinished(ev -> {
                    // Clear the message on JavaFX Application Thread
                    Platform.runLater(() -> msgLabel.setText(""));
                });
                msgHideTimer.playFromStart();
            } catch (Exception ex) {
                // If anything goes wrong with the timer, just ignore and keep the message visible
                java.util.logging.Logger.getLogger(CashierController.class.getName()).fine("Failed to schedule msg hide: " + ex.getMessage());
            }
            updateCategoryFilter();

            // After successful payment, reset the POS for the next transaction on FX thread
            javafx.application.Platform.runLater(() -> {
                cart.clear();
                updateTotals.run();
                // reset inputs
                qtySpinner.getValueFactory().setValue(1);
                tenderedField.clear();
                ewalletField.clear();
                paymentCombo.setValue("Cash");
                itemField.clear();
                // keep the success message visible for cashier confirmation
            });

            // Persist the snapshot to DB and save data off the FX thread so UI is not blocked
            new Thread(() -> {
                try {
                    // Persist each snapshot line with transaction metadata
                    for (POSLine l : snapshot) {
                        try {
                            DatabaseManager.getInstance().insertPosSaleWithTransaction(transactionRef, user, l.getName(), l.getQuantity(), l.getTotal(), when, payment, ewalletField.getText());
                        } catch (Exception ex) {
                            java.util.logging.Logger.getLogger(CashierController.class.getName()).warning("Failed to record POS sale with transaction: " + ex.getMessage());
                        }
                    }

                    // Save in-memory changes to JSON/archives (best-effort)
                    try {
                        dataManager.saveData();
                        archiveManager.saveArchiveData();
                    } catch (Exception ex) {
                        java.util.logging.Logger.getLogger(CashierController.class.getName()).warning("Failed to save data after checkout: " + ex.getMessage());
                        // Notify cashier non-blockingly that a background save failed
                        Platform.runLater(() -> {
                            msgLabel.setText(msgLabel.getText() + "\n(Warning: failed to persist data immediately; will retry in background)");
                        });
                    }
                } catch (Throwable t) {
                    java.util.logging.Logger.getLogger(CashierController.class.getName()).log(java.util.logging.Level.SEVERE, "Unexpected error persisting POS transaction: " + t.getMessage(), t);
                    Platform.runLater(() -> showAlert("Persistence Error", "An error occurred saving transaction data in background: " + t.getMessage()));
                }
            }).start();

            // keep POS open so cashier can see transaction ID and continue with new sale
        });

    // POS is shown via the maximized Stage (posStage.show()).
    }
    
    @FXML
    private void handleViewArchive() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/archive-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Archive - Used Items");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(true);
            // Open archive maximized so cashier sees full workspace immediately
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to open archive view: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard-overview.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("POS Overview");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.NONE);
            stage.initOwner(InventoryApplication.getPrimaryStage());
            stage.setResizable(true);
            // Open overview maximized (full window)
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open POS Overview: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        dataManager.loadData();
        archiveManager.loadArchiveData();
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
                java.util.logging.Logger.getLogger(CashierController.class.getName()).log(java.util.logging.Level.SEVERE, "Error returning to login screen: " + e.getMessage(), e);
                stopClock();
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
            "• Bernadine E. Suson\n" +

            "A simple digital inventory management system\n" +
            "for NVA Printing Services with role-based access.\n\n" +
            "Cashier Role: Track inventory usage and view archive."
        );
        alert.showAndWait();
    }
    
    @FXML
    private void handleExit() {
        // Save data before exit
        dataManager.saveData();
        archiveManager.saveArchiveData();
        authManager.saveUsers();
        stopClock();

        // Close application
        Platform.exit();
        System.exit(0);
    }

    private void stopClock() {
        try {
            if (clockTimeline != null) {
                clockTimeline.stop();
                clockTimeline = null;
            }
            // no initial delay to stop when using polling timeline
        } catch (Exception e) {
            // ignore
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
                // Use DataManager to perform reduction and archive the usage (includes stockOut increment)
                String user = AuthManager.getInstance().getCurrentUser() != null ?
                    AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";

                boolean success = DataManager.getInstance().reduceItemQuantity(selectedItem, amount, user, "Sold via Cashier");
                if (success) {
                    // Persist POS sale to DB (best-effort)
                    try {
                        double total = selectedItem.getUnitPrice() * amount;
                        java.time.LocalDateTime when = java.time.LocalDateTime.now();
                        DatabaseManager.getInstance().insertPosSale(user, selectedItem.getName(), amount, total, when);
                    } catch (Exception e) {
                        java.util.logging.Logger.getLogger(CashierController.class.getName()).warning("Failed to record POS sale: " + e.getMessage());
                    }

                    // Refresh UI and log
                    DataManager.getInstance().updateItem(selectedItem);
                    inventoryTable.refresh();
                    TransactionLogger.getInstance().log(selectedItem.getName(), "STOCK OUT", amount, user);
                    showAlert("Stock Out", "Stock decreased by " + amount + ".");
                } else {
                    showAlert("Stock Out", "Failed to stock out item. Please try again.");
                }
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter a valid number.");
            }
        }
    }
}
