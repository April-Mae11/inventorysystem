package com.nva.printing.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Archive Item Model - Demonstrates OOP Concepts
 * 
 * ENCAPSULATION: Private fields with controlled access through getters/setters
 * ABSTRACTION: Hides archive data management complexity
 * 
 * This class represents an archived item that was used/consumed from inventory.
 * It tracks the original item details and usage information for historical records.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Encapsulation: Private fields with controlled access
 * - Abstraction: Clean interface for archive management
 * - Data Integrity: Immutable archive records with usage tracking
 * 
 * @author NVA Printing Services Development Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArchiveItem {
    
    // ENCAPSULATION: Private fields to protect archive data
    private int id;
    private String originalItemName;
    private String category;
    private String type;
    private int quantityUsed;
    private double unitPrice;
    private String supplier;
    private LocalDateTime dateUsed;
    private String usedBy; // Username who used the item
    private String reason; // Reason for usage (e.g., "Printing job", "Customer order")
    
    /**
     * Default constructor for Jackson deserialization
     */
    public ArchiveItem() {
        this.dateUsed = LocalDateTime.now();
    }
    
    /**
     * Parameterized constructor for creating archive records
     * 
     * @param originalItemName Name of the original inventory item
     * @param category Category of the item
    * @param type Type of archive record (e.g., "Deleted Item", "Used Item")
     * @param quantityUsed Quantity that was used/consumed
     * @param unitPrice Unit price at time of usage
     * @param supplier Supplier of the item
     * @param usedBy Username who used the item
     * @param reason Reason for usage
     */
    public ArchiveItem(String originalItemName, String category, String type, 
                      int quantityUsed, double unitPrice, String supplier, 
                      String usedBy, String reason) {
        this.originalItemName = originalItemName;
        this.category = category;
        this.type = type;
        this.quantityUsed = quantityUsed;
        this.unitPrice = unitPrice;
        this.supplier = supplier;
        this.usedBy = usedBy;
        this.reason = reason;
        this.dateUsed = LocalDateTime.now();
    }
    
    // ENCAPSULATION: Controlled access to archive data
    
    @JsonProperty("id")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    @JsonProperty("originalItemName")
    public String getOriginalItemName() { return originalItemName; }
    public void setOriginalItemName(String originalItemName) { this.originalItemName = originalItemName; }
    
    @JsonProperty("category")
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    @JsonProperty("type")
    @JsonAlias({"description"})
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    @JsonProperty("quantityUsed")
    public int getQuantityUsed() { return quantityUsed; }
    public void setQuantityUsed(int quantityUsed) { this.quantityUsed = quantityUsed; }
    
    @JsonProperty("unitPrice")
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    
    @JsonProperty("supplier")
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    
    @JsonProperty("dateUsed")
    public LocalDateTime getDateUsed() { return dateUsed; }
    public void setDateUsed(LocalDateTime dateUsed) { this.dateUsed = dateUsed; }
    
    @JsonProperty("usedBy")
    public String getUsedBy() { return usedBy; }
    public void setUsedBy(String usedBy) { this.usedBy = usedBy; }
    
    @JsonProperty("reason")
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    /**
     * Calculate total value of used items
     * ABSTRACTION: Hides calculation logic
     * @return Total value (quantity * unit price)
     */
    public double getTotalValue() {
        return quantityUsed * unitPrice;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ArchiveItem that = (ArchiveItem) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    @Override
    public String toString() {
        return String.format("ArchiveItem{id=%d, originalItemName='%s', quantityUsed=%d, usedBy='%s', dateUsed=%s}",
                id, originalItemName, quantityUsed, usedBy, dateUsed);
    }
}
