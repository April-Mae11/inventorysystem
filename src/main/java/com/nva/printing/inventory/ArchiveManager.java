package com.nva.printing.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Archive Manager - Demonstrates OOP Concepts
 * 
 * SINGLETON PATTERN: Ensures only one instance manages archive data
 * ENCAPSULATION: Private constructor and controlled access to archive data
 * ABSTRACTION: Hides file I/O complexity behind simple method calls
 * 
 * This class manages all archive data persistence and provides a clean
 * interface for archive operations while hiding implementation details.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Encapsulation: Private fields and controlled access through public methods
 * - Abstraction: Clean API hiding JSON serialization and file operations
 * - Singleton Pattern: Single point of archive management
 * - Data Integrity: Automatic backup and error handling
 * 
 * @author NVA Printing Services Development Team
 */
public class ArchiveManager {
    
    // SINGLETON PATTERN: Static instance for single point of access
    private static ArchiveManager instance;
    
    // ENCAPSULATION: Private constants for file management
    private static final String ARCHIVE_FILE = "archive_data.json";
    private static final String ARCHIVE_BACKUP_FILE = "archive_data_backup.json";
    
    // ENCAPSULATION: Private fields to protect data integrity
    private final ObservableList<ArchiveItem> archiveItems;
    private final ObjectMapper objectMapper; // JSON serialization abstraction
    private final AtomicInteger nextId; // Thread-safe ID generation
    private static final Logger LOGGER = Logger.getLogger(ArchiveManager.class.getName());
    
    /**
     * SINGLETON PATTERN: Private constructor prevents direct instantiation
     * ENCAPSULATION: Initializes all private fields with proper defaults
     * ABSTRACTION: Hides complex object mapper configuration
     */
    private ArchiveManager() {
        this.archiveItems = FXCollections.observableArrayList();
        this.objectMapper = new ObjectMapper();
        
        // ABSTRACTION: Hide JSON configuration complexity
        this.objectMapper.registerModule(new JavaTimeModule());
        this.nextId = new AtomicInteger(1);
    }
    
    /**
     * SINGLETON PATTERN: Controlled access to single instance
     * ENCAPSULATION: Lazy initialization of singleton instance
     * @return The single ArchiveManager instance
     */
    public static ArchiveManager getInstance() {
        if (instance == null) {
            instance = new ArchiveManager();
        }
        return instance;
    }
    
    /**
     * ENCAPSULATION: Controlled access to archive collection
     * Returns observable list for UI binding while maintaining data integrity
     * @return Observable list of archive items for UI binding
     */
    public ObservableList<ArchiveItem> getArchiveItems() {
        return archiveItems;
    }
    
    /**
     * Get all archive items as a list
     * ABSTRACTION: Simple access to all archive items
     * @return List of all archive items
     */
    public List<ArchiveItem> getAllArchiveItems() {
        return new ArrayList<>(archiveItems);
    }
    
    /**
     * ABSTRACTION: High-level method hiding ID generation and persistence
     * ENCAPSULATION: Manages archive item ID assignment and data consistency
     * @param item The archive item to add
     */
    public void addArchiveItem(ArchiveItem item) {
        // ABSTRACTION: Hide ID generation complexity
        item.setId(nextId.getAndIncrement());
        archiveItems.add(item);
        
        // ABSTRACTION: Automatic persistence
    saveArchiveData();
    LOGGER.info("Added archive item: " + item.getOriginalItemName() + " (Used: " + item.getQuantityUsed() + ")");
    }
    
    /**
     * Archive an inventory item usage
     * ABSTRACTION: High-level method for archiving item usage
     * ENCAPSULATION: Creates archive record from inventory item
     * 
     * @param inventoryItem The inventory item that was used
     * @param quantityUsed Quantity that was used/consumed
     * @param usedBy Username who used the item
     * @param reason Reason for usage
     */
    /**
     * Archive an inventory item usage with explicit type (e.g., "Deleted Item", "Used Item").
     */
    public void archiveItemUsage(InventoryItem inventoryItem, int quantityUsed, String usedBy, String reason, String type) {
        ArchiveItem archiveItem = new ArchiveItem(
            inventoryItem.getName(),
            inventoryItem.getCategory(),
            type,
            quantityUsed,
            inventoryItem.getUnitPrice(),
            inventoryItem.getSupplier(),
            usedBy,
            reason
        );

        // Persist into DB if available. We try DB first and fall back to JSON file.
        try {
            Integer itemId = null;
            try {
                itemId = inventoryItem != null ? inventoryItem.getId() : null;
            } catch (Exception ignored) {}
            boolean ok = DatabaseManager.getInstance().insertArchive(archiveItem, itemId);
            if (ok) {
                // Keep in-memory JSON replica in sync as well
                addArchiveItem(archiveItem);
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("DB archive insert failed, falling back to JSON: " + e.getMessage());
        }

        // JSON fallback
        addArchiveItem(archiveItem);
    }

    /**
     * Backwards-compatible overload: infer type when not provided. Defaults to "Used Item".
     */
    public void archiveItemUsage(InventoryItem inventoryItem, int quantityUsed, String usedBy, String reason) {
        String inferredType = "Used Item";
        // If reason indicates delete or quantity equals full stock, caller may pass explicit type instead.
        addArchiveItem(new ArchiveItem(
            inventoryItem.getName(),
            inventoryItem.getCategory(),
            inferredType,
            quantityUsed,
            inventoryItem.getUnitPrice(),
            inventoryItem.getSupplier(),
            usedBy,
            reason
        ));
    }
    
    // ABSTRACTION: High-level query methods hiding implementation complexity
    
    /**
     * Find archive items by original item name
     * ABSTRACTION: Hides stream processing complexity
     * @param itemName The original item name to search for
     * @return List of archive items matching the name
     */
    public List<ArchiveItem> findArchiveItemsByName(String itemName) {
        return archiveItems.stream()
                .filter(item -> item.getOriginalItemName().toLowerCase().contains(itemName.toLowerCase()))
                .toList();
    }
    
    /**
     * Find archive items by category
     * ABSTRACTION: Hides filtering and collection processing
     * @param category The category to search for
     * @return List of archive items in the specified category
     */
    public List<ArchiveItem> findArchiveItemsByCategory(String category) {
        return archiveItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .toList();
    }
    
    /**
     * Find archive items by user
     * ABSTRACTION: Hides filtering logic
     * @param username The username to search for
     * @return List of archive items used by the specified user
     */
    public List<ArchiveItem> findArchiveItemsByUser(String username) {
        return archiveItems.stream()
                .filter(item -> item.getUsedBy().equalsIgnoreCase(username))
                .toList();
    }
    
    /**
     * Find archive items by date range
     * ABSTRACTION: Hides date filtering complexity
     * @param startDate Start date for the range
     * @param endDate End date for the range
     * @return List of archive items within the date range
     */
    public List<ArchiveItem> findArchiveItemsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return archiveItems.stream()
                .filter(item -> !item.getDateUsed().isBefore(startDate) && !item.getDateUsed().isAfter(endDate))
                .toList();
    }
    
    /**
     * Get all unique categories in the archive
     * ABSTRACTION: Hides stream processing and sorting logic
     * @return Sorted list of unique categories
     */
    public List<String> getAllArchiveCategories() {
        return archiveItems.stream()
                .map(ArchiveItem::getCategory)
                .distinct()
                .sorted()
                .toList();
    }
    
    // ABSTRACTION: File I/O operations hidden behind simple method calls
    
    /**
     * Load archive data from JSON file
     * ABSTRACTION: Hides complex JSON deserialization and error handling
     * ENCAPSULATION: Manages internal state during data loading
     */
    public void loadArchiveData() {
        File archiveFile = new File(ARCHIVE_FILE);
        
        if (!archiveFile.exists()) {
            LOGGER.info("Archive file not found. Starting with empty archive.");
            return;
        }
        
        try {
            // ABSTRACTION: Hide JSON parsing complexity
            List<ArchiveItem> items = objectMapper.readValue(archiveFile, 
                    new TypeReference<List<ArchiveItem>>() {});
            
            // ENCAPSULATION: Safely update internal state
            archiveItems.clear();
            archiveItems.addAll(items);
            
            // ABSTRACTION: Hide ID counter management
            int maxId = items.stream()
                    .mapToInt(ArchiveItem::getId)
                    .max()
                    .orElse(0);
            nextId.set(maxId + 1);
            
            LOGGER.info("Loaded " + items.size() + " archive items from " + ARCHIVE_FILE);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading archive data from " + ARCHIVE_FILE + ": " + e.getMessage(), e);
            // ABSTRACTION: Hide backup recovery process
            loadFromBackup();
        }
    }
    
    /**
     * Save archive data to JSON file with automatic backup
     * ABSTRACTION: Hides JSON serialization and backup creation
     * ENCAPSULATION: Protects data through backup mechanism
     */
    public void saveArchiveData() {
        try {
            // ABSTRACTION: Hide backup creation process
            File archiveFile = new File(ARCHIVE_FILE);
            if (archiveFile.exists()) {
                File backupFile = new File(ARCHIVE_BACKUP_FILE);
                objectMapper.writeValue(backupFile, new ArrayList<>(archiveItems));
            }
            
            // ABSTRACTION: Hide JSON serialization
            objectMapper.writeValue(archiveFile, new ArrayList<>(archiveItems));
            LOGGER.info("Archive data saved successfully to " + ARCHIVE_FILE);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving archive data to " + ARCHIVE_FILE + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * ENCAPSULATION: Private method for backup recovery
     * ABSTRACTION: Hides backup loading complexity from public interface
     */
    private void loadFromBackup() {
        File backupFile = new File(ARCHIVE_BACKUP_FILE);
        
        if (!backupFile.exists()) {
            LOGGER.info("Archive backup file not found. Starting with empty archive.");
            return;
        }
        
        try {
            List<ArchiveItem> items = objectMapper.readValue(backupFile, 
                    new TypeReference<List<ArchiveItem>>() {});
            
            archiveItems.clear();
            archiveItems.addAll(items);
            
            LOGGER.info("Loaded " + items.size() + " archive items from backup file");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading archive backup data: " + e.getMessage(), e);
        }
    }
    
    /**
     * ABSTRACTION: High-level method providing archive analytics
     * ENCAPSULATION: Returns calculated statistics without exposing calculation logic
     * @return ArchiveStats object with computed statistics
     */
    public ArchiveStats getArchiveStats() {
        int totalItems = archiveItems.size();
        double totalValue = archiveItems.stream().mapToDouble(ArchiveItem::getTotalValue).sum();
        int totalQuantity = archiveItems.stream().mapToInt(ArchiveItem::getQuantityUsed).sum();
        
        return new ArchiveStats(totalItems, totalValue, totalQuantity);
    }

    /**
     * Clear all archive items and persist the empty archive to disk.
     * This is a destructive operation; callers should confirm with the user first.
     */
    public void clearArchive() {
        try {
            archiveItems.clear();
            saveArchiveData();
            LOGGER.info("Cleared all archive items");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clear archive: " + e.getMessage(), e);
        }
    }
    
    /**
     * ENCAPSULATION: Inner class to encapsulate archive statistics data
     * ABSTRACTION: Provides a clean interface for accessing archive statistics
     * 
     * This inner class demonstrates composition and encapsulation by grouping
     * related statistical data together with controlled access.
     */
    public static class ArchiveStats {
        // ENCAPSULATION: Private fields with public getters
        private final int totalItems;
        private final double totalValue;
        private final int totalQuantity;
        
        /**
         * Constructor for creating immutable statistics object
         * ENCAPSULATION: All fields are final and set through constructor
         */
        public ArchiveStats(int totalItems, double totalValue, int totalQuantity) {
            this.totalItems = totalItems;
            this.totalValue = totalValue;
            this.totalQuantity = totalQuantity;
        }
        
        // ENCAPSULATION: Read-only access to statistics
        public int getTotalItems() { return totalItems; }
        public double getTotalValue() { return totalValue; }
        public int getTotalQuantity() { return totalQuantity; }
    }
}
