package com.nva.printing.inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Inventory Item Model - Demonstrates OOP Concepts
 * 
 * ENCAPSULATION: Private fields with public getters/setters to control access to data
 * ABSTRACTION: Hides internal property implementation from external classes
 * 
 * This class represents a single item in the inventory system and encapsulates
 * all the data and behavior related to an inventory item.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Encapsulation: Private fields with controlled access through public methods
 * - Abstraction: Clean interface hiding JavaFX property implementation details
 * - Data Integrity: Automatic low stock calculation and validation
 * - State Management: Automatic timestamp updates on quantity changes
 * 
 * @author NVA Printing Services Development Team
 */
public class InventoryItem {
    
    // ENCAPSULATION: Private fields to protect data integrity
    // JavaFX Properties for data binding - demonstrates abstraction over simple fields
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty category;
    private final StringProperty description;
    private final IntegerProperty quantity;
    private final IntegerProperty minStockLevel;
    private final DoubleProperty unitPrice;
    private final StringProperty supplier;
    private final ObjectProperty<LocalDateTime> lastUpdated;
    private final BooleanProperty lowStock;
    private final BooleanProperty outOfStock;
    // Stock In/Out tracking
    private final IntegerProperty stockIn;
    private final IntegerProperty stockOut;
    
    /**
     * Default constructor for Jackson deserialization
     * ABSTRACTION: Hides complex property initialization from external code
     * ENCAPSULATION: Initializes all private fields with default values
     */
    public InventoryItem() {
        this.id = new SimpleIntegerProperty();
        this.name = new SimpleStringProperty();
        this.category = new SimpleStringProperty();
        this.description = new SimpleStringProperty();
        this.quantity = new SimpleIntegerProperty();
        this.minStockLevel = new SimpleIntegerProperty(10); // Default minimum stock
        this.unitPrice = new SimpleDoubleProperty();
        this.supplier = new SimpleStringProperty();
        this.lastUpdated = new SimpleObjectProperty<>(LocalDateTime.now());
        this.lowStock = new SimpleBooleanProperty();
        this.outOfStock = new SimpleBooleanProperty();
        
        // ABSTRACTION: Automatic behavior - hide implementation details
    // Initialize all properties with appropriate default values
    this.stockIn = new SimpleIntegerProperty();
    this.stockOut = new SimpleIntegerProperty();
        // Add listeners to update low stock status automatically when values change
        this.quantity.addListener((obs, oldVal, newVal) -> updateLowStockStatus());
        this.minStockLevel.addListener((obs, oldVal, newVal) -> updateLowStockStatus());
    }
    
    /**
     * Parameterized constructor for creating new inventory items
     * ABSTRACTION: Provides a simple interface for object creation
     * ENCAPSULATION: Validates and sets all necessary data
     */
    public InventoryItem(String name, String category, String description, int quantity, int minStockLevel, double unitPrice, String supplier) {
        this(); // Call default constructor for property initialization
        this.name.set(name);
        this.category.set(category);
        this.description.set(description);
        this.quantity.set(quantity);
        this.minStockLevel.set(minStockLevel);
        this.unitPrice.set(unitPrice);
        this.supplier.set(supplier);
        this.stockIn.set(0);
        this.stockOut.set(0);
        updateLowStockStatus();
    }

    /**
     * Stock In Property Accessors
     */
    @JsonProperty("stockIn")
    public int getStockIn() { return stockIn.get(); }
    public void setStockIn(int stockIn) { this.stockIn.set(stockIn); }
    public IntegerProperty stockInProperty() { return stockIn; }

    /**
     * Stock Out Property Accessors
     */
    @JsonProperty("stockOut")
    public int getStockOut() { return stockOut.get(); }
    public void setStockOut(int stockOut) { this.stockOut.set(stockOut); }
    public IntegerProperty stockOutProperty() { return stockOut; }
    
    /**
     * ENCAPSULATION: Private method to maintain data consistency
     * ABSTRACTION: Hides the logic for determining low stock status
     */
    private void updateLowStockStatus() {
        if (quantity.get() == 0) {
            outOfStock.set(true);
            lowStock.set(false);
        } else if (quantity.get() <= minStockLevel.get()) {
            outOfStock.set(false);
            lowStock.set(true);
        } else {
            outOfStock.set(false);
            lowStock.set(false);
        }
    }
    
    // ENCAPSULATION: Controlled access to private fields through public methods
    // Jackson annotations for JSON serialization/deserialization
    
    /**
     * ID Property Accessors - ENCAPSULATION
     * Provides controlled access to the ID field
     */
    @JsonProperty("id")
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; } // For JavaFX binding
    
    /**
     * Name Property Accessors - ENCAPSULATION
     * Provides controlled access to the name field with validation potential
     */
    @JsonProperty("name")
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }
    
    /**
     * Category Property Accessors - ENCAPSULATION
     * Allows categorization while maintaining data integrity
     */
    @JsonProperty("category")
    public String getCategory() { return category.get(); }
    public void setCategory(String category) { this.category.set(category); }
    public StringProperty categoryProperty() { return category; }
    
    /**
     * Description Property Accessors - ENCAPSULATION
     * Optional field with controlled access
     */
    @JsonProperty("description")
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }
    
    /**
     * Quantity Property Accessors - ENCAPSULATION with BUSINESS LOGIC
     * Automatically updates timestamp when quantity changes
     */
    @JsonProperty("quantity")
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int quantity) { 
        this.quantity.set(quantity);
        // ABSTRACTION: Hide automatic timestamp update from caller
        this.lastUpdated.set(LocalDateTime.now());
    }
    public IntegerProperty quantityProperty() { return quantity; }
    
    /**
     * Minimum Stock Level Accessors - ENCAPSULATION
     * Controls the threshold for low stock alerts
     */
    @JsonProperty("minStockLevel")
    public int getMinStockLevel() { return minStockLevel.get(); }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel.set(minStockLevel); }
    public IntegerProperty minStockLevelProperty() { return minStockLevel; }
    
    /**
     * Unit Price Property Accessors - ENCAPSULATION
     * Manages pricing information with data integrity
     */
    @JsonProperty("unitPrice")
    public double getUnitPrice() { return unitPrice.get(); }
    public void setUnitPrice(double unitPrice) { this.unitPrice.set(unitPrice); }
    public DoubleProperty unitPriceProperty() { return unitPrice; }
    
    /**
     * Supplier Property Accessors - ENCAPSULATION
     * Tracks supplier information for each item
     */
    @JsonProperty("supplier")
    public String getSupplier() { return supplier.get(); }
    public void setSupplier(String supplier) { this.supplier.set(supplier); }
    public StringProperty supplierProperty() { return supplier; }
    
    /**
     * Last Updated Property Accessors - ENCAPSULATION
     * Tracks when the item was last modified
     */
    @JsonProperty("lastUpdated")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getLastUpdated() { return lastUpdated.get(); }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated.set(lastUpdated); }
    public ObjectProperty<LocalDateTime> lastUpdatedProperty() { return lastUpdated; }
    
    /**
     * Low Stock Status Accessors - ENCAPSULATION with COMPUTED STATE
     * Provides read access to automatically calculated low stock status
     */
    @JsonProperty("lowStock")
    public boolean isLowStock() { return lowStock.get(); }
    public void setLowStock(boolean lowStock) { this.lowStock.set(lowStock); }
    public BooleanProperty lowStockProperty() { return lowStock; }
    
    /**
     * Out Of Stock Status Accessors - ENCAPSULATION with COMPUTED STATE
     * Provides read access to automatically calculated out of stock status
     */
    @JsonProperty("outOfStock")
    public boolean isOutOfStock() { return outOfStock.get(); }
    public void setOutOfStock(boolean value) { outOfStock.set(value); }
    public BooleanProperty outOfStockProperty() { return outOfStock; }
    
    // ABSTRACTION: Business logic methods that hide implementation complexity
    
    /**
     * Calculate total value of this inventory item
     * ABSTRACTION: Hides the calculation logic from the caller
     * @return Total monetary value (quantity × unit price)
     */
    @JsonIgnore
    public double getTotalValue() {
        return quantity.get() * unitPrice.get();
    }
    
    /**
     * Update stock quantity with automatic timestamp
     * ABSTRACTION: Combines quantity update with timestamp management
     * @param newQuantity The new quantity value
     */
    public void updateStock(int newQuantity) {
        setQuantity(newQuantity); // Uses setter to trigger timestamp update
    }
    
    /**
     * Add stock to current quantity
     * ABSTRACTION: Provides a semantic operation for stock increases
     * @param amount Amount to add to current stock
     */
    public void addStock(int amount) {
        setQuantity(getQuantity() + amount);
    }
    
    /**
     * Remove stock from current quantity
     * ABSTRACTION: Provides a semantic operation for stock decreases
     * ENCAPSULATION: Ensures quantity never goes below zero
     * @param amount Amount to remove from current stock
     */
    public void removeStock(int amount) {
        int newQuantity = Math.max(0, getQuantity() - amount);
        setQuantity(newQuantity);
    }
    
    /**
     * POLYMORPHISM: Override Object.equals() for proper object comparison
     * Compares items based on their unique ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InventoryItem that = (InventoryItem) obj;
        return getId() == that.getId();
    }
    
    /**
     * POLYMORPHISM: Override Object.hashCode() for proper hashing
     * Uses ID as the basis for hash code calculation
     */
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    /**
     * POLYMORPHISM: Override Object.toString() for meaningful string representation
     * ABSTRACTION: Provides a clean string representation hiding internal complexity
     */
    @Override
    public String toString() {
        return String.format("InventoryItem{id=%d, name='%s', category='%s', quantity=%d, lowStock=%s, outOfStock=%s}",
                getId(), getName(), getCategory(), getQuantity(), isLowStock(), isOutOfStock());
    }
}
