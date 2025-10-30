package com.nva.printing.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
// no JDBC imports — reverting to JSON file persistence only

/**
 * Data Manager - Demonstrates OOP Concepts
 * 
 * SINGLETON PATTERN: Ensures only one instance manages inventory data
 * ENCAPSULATION: Private constructor and controlled access to data
 * ABSTRACTION: Hides file I/O complexity behind simple method calls
 * 
 * This class manages all inventory data persistence and provides a clean
 * interface for CRUD operations while hiding implementation details.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Encapsulation: Private fields and controlled access through public methods
 * - Abstraction: Clean API hiding JSON serialization and file operations
 * - Singleton Pattern: Single point of data management
 * - Data Integrity: Automatic backup and error handling
 * 
 * @author NVA Printing Services Development Team
 */
public class DataManager {
    
    // SINGLETON PATTERN: Static instance for single point of access
    private static DataManager instance;
    
    // ENCAPSULATION: Private constants for file management
    // Use a per-user application directory (APPDATA on Windows or home dir) to ensure write permissions
    private static final String APP_DIR;
    static {
        String dir;
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            dir = appData + File.separator + "NVAInventory";
        } else {
            dir = System.getProperty("user.home") + File.separator + ".nva_inventory";
        }
        File d = new File(dir);
        if (!d.exists()) d.mkdirs();
        APP_DIR = dir;
    }
    private static final String DATA_FILE = APP_DIR + File.separator + "inventory_data.json";
    private static final String BACKUP_FILE = APP_DIR + File.separator + "inventory_data_backup.json";
    private static final String SUPPLIERS_FILE = APP_DIR + File.separator + "suppliers.json";
    private static final String CATEGORIES_FILE = APP_DIR + File.separator + "categories.json";
    
    // ENCAPSULATION: Private fields to protect data integrity
    // Backing store is a plain ArrayList (primary data structure). We expose an ObservableList view
    // so JavaFX bindings continue to work while the canonical structure is ArrayList.
    private final java.util.List<InventoryItem> inventoryItems;
    private final java.util.List<Supplier> suppliers;
    private final java.util.List<String> persistedCategories;
    private final javafx.collections.ObservableList<InventoryItem> observableInventory;
    private final ObjectMapper objectMapper; // JSON serialization abstraction
    private final AtomicInteger nextId; // Thread-safe ID generation
    private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
    // no DB connection — JSON file persistence only
    
    /**
     * SINGLETON PATTERN: Private constructor prevents direct instantiation
     * ENCAPSULATION: Initializes all private fields with proper defaults
     * ABSTRACTION: Hides complex object mapper configuration
     */
    private DataManager() {
    this.inventoryItems = new java.util.ArrayList<>();
    this.observableInventory = FXCollections.observableList(this.inventoryItems);
        this.objectMapper = new ObjectMapper();
        
        // ABSTRACTION: Hide JSON configuration complexity
        this.objectMapper.registerModule(new JavaTimeModule());
    // suppliers list initialization
    this.suppliers = new java.util.ArrayList<>();
        this.nextId = new AtomicInteger(1);

        // Migration: if project-local data files exist (in working dir) and app dir files don't, copy them
        try {
            File projectData = new File("inventory_data.json");
            File projectBackup = new File("inventory_data_backup.json");
            File targetData = new File(DATA_FILE);
            File targetBackup = new File(BACKUP_FILE);

            if (projectData.exists() && !targetData.exists()) {
                try { Files.copy(projectData.toPath(), targetData.toPath()); LOGGER.info("Migrated inventory_data.json to user app directory"); } catch (IOException e) { LOGGER.warning("Failed to migrate inventory_data.json: " + e.getMessage()); }
            }
            if (projectBackup.exists() && !targetBackup.exists()) {
                try { Files.copy(projectBackup.toPath(), targetBackup.toPath()); LOGGER.info("Migrated inventory_data_backup.json to user app directory"); } catch (IOException e) { LOGGER.warning("Failed to migrate inventory_data_backup.json: " + e.getMessage()); }
            }
        } catch (Exception e) {
            LOGGER.warning("Migration check failed: " + e.getMessage());
        }

        // Load suppliers data (if any)
        loadSuppliers();
    // Load persisted categories (if any)
    persistedCategories = new java.util.ArrayList<>();
    loadCategories();

        // DB integration removed — using JSON files only
    }

    /**
     * Load suppliers from suppliers.json if present
     */
    private void loadSuppliers() {
        File sFile = new File(SUPPLIERS_FILE);
        try {
            // Migrate from project-local file if present
            File projectSuppliers = new File("suppliers.json");
            if (projectSuppliers.exists() && !sFile.exists()) {
                try { Files.copy(projectSuppliers.toPath(), sFile.toPath()); LOGGER.info("Migrated suppliers.json to user app directory"); } catch (IOException e) { LOGGER.warning("Failed to migrate suppliers.json: " + e.getMessage()); }
            }
            if (!sFile.exists()) return;
            List<Supplier> loaded = objectMapper.readValue(sFile, new TypeReference<List<Supplier>>() {});
            if (loaded != null) {
                suppliers.clear();
                suppliers.addAll(loaded);
            }
            LOGGER.info("Loaded " + suppliers.size() + " suppliers from " + SUPPLIERS_FILE);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load suppliers: " + e.getMessage(), e);
        }
    }

    /**
     * Save suppliers list to suppliers.json
     */
    private void saveSuppliers() {
        try {
            File sFile = new File(SUPPLIERS_FILE);
            objectMapper.writeValue(sFile, new ArrayList<>(suppliers));
            LOGGER.info("Suppliers saved to " + SUPPLIERS_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving suppliers: " + e.getMessage(), e);
        }
    }

    /**
     * Public method to restore inventory data from the backup file.
     * Returns true if the restore succeeded, false otherwise.
     */
    public boolean restoreFromBackup() {
        File backupFile = new File(BACKUP_FILE);
        if (!backupFile.exists()) {
            LOGGER.warning("Backup file not found. Cannot restore.");
            return false;
        }

        try {
            // Reuse the private loadFromBackup logic
            loadFromBackup();
            // Persist the restored state as the current data file
            saveData();
            LOGGER.info("Restored inventory data from backup successfully.");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to restore from backup: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * SINGLETON PATTERN: Controlled access to single instance
     * ENCAPSULATION: Lazy initialization of singleton instance
     * @return The single DataManager instance
     */
    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }
    
    /**
     * ENCAPSULATION: Controlled access to inventory collection
     * Returns observable list for UI binding while maintaining data integrity
     * @return Observable list of inventory items for UI binding
     */
    public ObservableList<InventoryItem> getInventoryItems() {
        // Return the ObservableList view so UI bindings can listen for changes
        return observableInventory;
    }
    
    /**
     * Get all inventory items as a list
     * ABSTRACTION: Simple access to all items
     * @return List of all inventory items
     */
    public List<InventoryItem> getAllItems() {
        // Return a defensive copy backed by ArrayList
        return new java.util.ArrayList<>(inventoryItems);
    }
    
    /**
     * ABSTRACTION: High-level method hiding ID generation and persistence
     * ENCAPSULATION: Manages item ID assignment and data consistency
     * @param item The inventory item to add
     */
    public void addItem(InventoryItem item) {
        item.setLastUpdated(LocalDateTime.now());
        try {
            int generatedId = DatabaseManager.getInstance().insertItem(item);
            if (generatedId > 0) {
                item.setId(generatedId);
                observableInventory.add(item);
                String user = AuthManager.getInstance().getCurrentUser() != null ?
                    AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
                com.nva.printing.inventory.TransactionLogger.getInstance().log(item.getName(), "ADD", item.getQuantity(), user);
                saveData();
                LOGGER.info("Added new item to DB: " + item.getName());
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("DB insert failed, falling back to JSON: " + e.getMessage());
        }

        // JSON fallback: assign local id and persist
        item.setId(nextId.getAndIncrement());
        observableInventory.add(item);
        String user = AuthManager.getInstance().getCurrentUser() != null ?
            AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
        com.nva.printing.inventory.TransactionLogger.getInstance().log(item.getName(), "ADD", item.getQuantity(), user);
        saveData();
        LOGGER.info("Added new item (JSON fallback): " + item.getName());
    }
    
    /**
     * ABSTRACTION: Simple update interface hiding timestamp management
     * ENCAPSULATION: Ensures data consistency and automatic saving
     * @param item The inventory item to update
     */
    public void updateItem(InventoryItem item) {
        // ABSTRACTION: Automatic timestamp update
        item.setLastUpdated(LocalDateTime.now());
        
        // Recalculate low stock status after update
        boolean wasLow = item.isLowStock();
        boolean nowLow = item.getQuantity() <= item.getMinStockLevel();
        item.setLowStock(nowLow);
        
        // Attempt DB update first
        try {
            boolean ok = DatabaseManager.getInstance().updateItem(item);
            String user = AuthManager.getInstance().getCurrentUser() != null ?
                    AuthManager.getInstance().getCurrentUser().getUsername().toUpperCase() : "SYSTEM";
            com.nva.printing.inventory.TransactionLogger.getInstance().log(item.getName(), "UPDATE", item.getQuantity(), user);
            if (ok) {
                // If item just transitioned to low stock, insert a stock alert record
                try {
                    if (!wasLow && nowLow) {
                        DatabaseManager.getInstance().insertStockAlert(item.getId(), null, item.getMinStockLevel(), "Automatically generated low stock alert");
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to insert stock alert: " + e.getMessage());
                }
                saveData(); // keep JSON backup in sync
                LOGGER.info("Updated item in DB: " + item.getName());
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("DB update failed, falling back to JSON: " + e.getMessage());
        }

        // JSON fallback save
        saveData();
        LOGGER.info("Updated item (JSON fallback): " + item.getName());
    }
    
    /**
     * ABSTRACTION: Simple removal interface with automatic persistence
     * ENCAPSULATION: Maintains collection integrity
     * @param item The inventory item to remove
     */
    public void removeItem(InventoryItem item) {
        removeItem(item, "");
    }

    /**
     * Remove item with a required reason. This archives the deleted item with the provided reason
     * and then deletes the item from DB/JSON as before.
     */
    public void removeItem(InventoryItem item, String reason) {
        if (item == null) return;
        String usedBy = "SYSTEM";
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            usedBy = currentUser.getUsername().toUpperCase();
        }

        // Ensure reason is non-null
        if (reason == null) reason = "";

        ArchiveManager.getInstance().archiveItemUsage(
            item,
            item.getQuantity(),
            usedBy,
            reason,
            "ARCHIVED"
        );
        // Log delete transaction including reason as part of user field for traceability
    com.nva.printing.inventory.TransactionLogger.getInstance().log(item.getName(), "ARCHIVE", item.getQuantity(), usedBy + (reason.isEmpty() ? "" : " [Reason:" + reason + "]"), item.getUnitPrice());

        // Attempt DB delete first
        try {
            boolean ok = DatabaseManager.getInstance().deleteItem(item.getId());
            if (ok) {
                observableInventory.remove(item);
                saveData(); // keep JSON backup in sync
                LOGGER.info("Removed item from DB: " + item.getName());
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("DB delete failed, falling back to JSON: " + e.getMessage());
        }

        // JSON fallback
        observableInventory.remove(item);
        saveData();
        LOGGER.info("Removed item (JSON fallback): " + item.getName());
    }
    
    /**
     * Reduce item quantity and archive the usage
     * ABSTRACTION: High-level method for quantity reduction with archiving
     * ENCAPSULATION: Manages quantity reduction and archive creation
     * 
     * @param item The inventory item to reduce quantity
     * @param quantityToReduce Amount to reduce from current quantity
     * @param usedBy Username who is using the item
     * @param reason Reason for usage
     * @return true if quantity was successfully reduced
     */
    public boolean reduceItemQuantity(InventoryItem item, int quantityToReduce, String usedBy, String reason) {
        if (item == null || quantityToReduce <= 0) {
            return false;
        }
        
        int currentQuantity = item.getQuantity();
        if (quantityToReduce > currentQuantity) {
            LOGGER.warning("Cannot reduce quantity by " + quantityToReduce + ". Only " + currentQuantity + " available.");
            return false;
        }
        
        // Reduce quantity
        item.setQuantity(currentQuantity - quantityToReduce);

        // Track stock out count
        item.setStockOut(item.getStockOut() + quantityToReduce);

        // Archive the usage
        ArchiveManager archiveManager = ArchiveManager.getInstance();
        archiveManager.archiveItemUsage(item, quantityToReduce, usedBy, reason, "Used Item");
        
        // Update the item (this will also recalculate low stock status)
    updateItem(item);
        
    LOGGER.info("Reduced quantity of " + item.getName() + " by " + quantityToReduce + ". New quantity: " + item.getQuantity());
        return true;
    }
    
    // ABSTRACTION: High-level query methods hiding implementation complexity
    
    /**
     * Find item by unique identifier
     * ABSTRACTION: Hides stream processing complexity
     * @param id The unique item identifier
     * @return Found item or null if not found
     */
    public InventoryItem findItemById(int id) {
        return inventoryItems.stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find items by category using case-insensitive matching
     * ABSTRACTION: Hides filtering and collection processing
     * @param category The category to search for
     * @return List of items in the specified category
     */
    public List<InventoryItem> findItemsByCategory(String category) {
        return inventoryItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .toList();
    }
    
    /**
     * Get all items with low stock status
     * ABSTRACTION: Hides the logic for determining low stock items
     * @return List of items that need restocking
     */
    public List<InventoryItem> getLowStockItems() {
        return inventoryItems.stream()
                .filter(InventoryItem::isLowStock)
                .toList();
    }
    
    /**
     * Search items by name or description (case-insensitive)
     * ABSTRACTION: Hides complex search logic and string processing
     * @param searchText The text to search for
     * @return List of matching items
     */
    public List<InventoryItem> searchItemsByName(String searchText) {
        String lowerSearchText = searchText.toLowerCase();
        return inventoryItems.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerSearchText) ||
                               item.getDescription().toLowerCase().contains(lowerSearchText))
                .toList();
    }
    
    /**
     * Get all unique categories in the inventory
     * ABSTRACTION: Hides stream processing and sorting logic
     * @return Sorted list of unique categories
     */
    public List<String> getAllCategories() {
        // Prefer DB-backed categories if available
        try {
            java.util.List<String> dbCats = DatabaseManager.getInstance().getAllCategories();
            if (dbCats != null && !dbCats.isEmpty()) return dbCats;
        } catch (Exception e) {
            // ignore and fall back to in-memory
        }
        // Merge categories discovered from inventory items with persistedCategories
        java.util.Set<String> merged = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            merged.addAll(persistedCategories);
        } catch (Exception ignored) {}
        for (InventoryItem it : inventoryItems) {
            try { if (it.getCategory() != null && !it.getCategory().trim().isEmpty()) merged.add(it.getCategory().trim()); } catch (Exception ignored) {}
        }
        return new java.util.ArrayList<>(merged);
    }

    /**
     * Load persisted categories from categories.json in the app directory.
     */
    private void loadCategories() {
        File f = new File(CATEGORIES_FILE);
        try {
            // Migrate from project-local file if present
            File projectCats = new File("categories.json");
            if (projectCats.exists() && !f.exists()) {
                try { Files.copy(projectCats.toPath(), f.toPath()); LOGGER.info("Migrated categories.json to user app directory"); } catch (IOException e) { LOGGER.warning("Failed to migrate categories.json: " + e.getMessage()); }
            }
            if (!f.exists()) return;
            List<String> loaded = objectMapper.readValue(f, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            if (loaded != null) {
                persistedCategories.clear();
                persistedCategories.addAll(loaded);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load categories: " + e.getMessage());
        }
    }

    /**
     * Save the persisted categories to categories.json
     */
    private void saveCategories() {
        try {
            File f = new File(CATEGORIES_FILE);
            objectMapper.writeValue(f, new ArrayList<>(persistedCategories));
            LOGGER.info("Categories saved to " + CATEGORIES_FILE);
        } catch (IOException e) {
            LOGGER.warning("Failed to save categories: " + e.getMessage());
        }
    }

    /** Add a category to persisted categories and save. Returns true if added. */
    public boolean addCategory(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String n = name.trim();
        for (String s : persistedCategories) if (s.equalsIgnoreCase(n)) return false;
        persistedCategories.add(n);
        java.util.Collections.sort(persistedCategories, String.CASE_INSENSITIVE_ORDER);
        saveCategories();
        return true;
    }

    /** Remove a category from persisted categories and save. Returns true if removed. */
    public boolean removeCategory(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String n = name.trim();
        String found = null;
        for (String s : persistedCategories) if (s.equalsIgnoreCase(n)) { found = s; break; }
        if (found == null) return false;
        persistedCategories.remove(found);
        saveCategories();
        return true;
    }

    /**
     * Return all suppliers (defensive copy)
     */
    public List<Supplier> getAllSuppliers() {
        return new java.util.ArrayList<>(suppliers);
    }

    /**
     * Add a supplier and persist to disk. If a supplier with same name exists (case-insensitive), update its details.
     */
    public void addSupplier(Supplier s) {
        if (s == null || s.getName() == null || s.getName().trim().isEmpty()) return;
        String name = s.getName().trim();
        // If supplier exists, update details and persist.
        Supplier existing = suppliers.stream()
                .filter(x -> x.getName() != null && x.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setContactNumber(s.getContactNumber());
            existing.setAddress(s.getAddress());
            existing.setSuppliedProduct(s.getSuppliedProduct());
            // try to update DB? currently DB update not implemented, so just persist to JSON
            saveSuppliers();
            return;
        }

        // New supplier: try to insert into DB first, fall back to JSON if DB unavailable.
        boolean dbInserted = false;
        try {
            int generatedId = DatabaseManager.getInstance().insertSupplier(s);
            if (generatedId > 0) {
                dbInserted = true;
                LOGGER.info("Inserted supplier into DB with id: " + generatedId);
            } else {
                LOGGER.fine("DB insert returned no generated id; falling back to JSON persistence.");
            }
        } catch (Exception e) {
            LOGGER.fine("DB unavailable or insert failed; will persist supplier to JSON. Reason: " + e.getMessage());
        }

        // Always keep a local JSON copy so UI can function when DB is down.
        suppliers.add(s);
        saveSuppliers();
        if (!dbInserted) {
            LOGGER.info("Supplier added locally and saved to suppliers.json (DB not used).");
        }
    }

    /**
     * Remove a supplier by name (case-insensitive). Returns true if removed.
     */
    public boolean removeSupplierByName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String targ = name.trim();
        boolean dbDeleted = false;
        try {
            dbDeleted = DatabaseManager.getInstance().deleteSupplierByName(targ);
        } catch (Exception ignored) {}

        Supplier existing = suppliers.stream()
                .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(targ))
                .findFirst().orElse(null);
        boolean jsonDeleted = false;
        if (existing != null) {
            suppliers.remove(existing);
            saveSuppliers();
            jsonDeleted = true;
        }

        if (dbDeleted || jsonDeleted) {
            LOGGER.info("Removed supplier (db=" + dbDeleted + ", json=" + jsonDeleted + "): " + targ);
            return true;
        }
        return false;
    }
    
    // ABSTRACTION: File I/O operations hidden behind simple method calls
    
    /**
     * Load inventory data from JSON file
     * ABSTRACTION: Hides complex JSON deserialization and error handling
     * ENCAPSULATION: Manages internal state during data loading
     */
    public void loadData() {
        // Try loading from DB first (XAMPP / MySQL). If DB unavailable, fall back to JSON files.
        try {
            java.util.List<InventoryItem> dbItems = DatabaseManager.getInstance().loadAllItems();
            if (dbItems != null && !dbItems.isEmpty()) {
                observableInventory.clear();
                observableInventory.addAll(dbItems);
                for (InventoryItem item : observableInventory) {
                    item.setLowStock(item.getQuantity() <= item.getMinStockLevel());
                }
                int maxId = dbItems.stream().mapToInt(InventoryItem::getId).max().orElse(0);
                nextId.set(maxId + 1);
                LOGGER.info("Loaded " + dbItems.size() + " items from DB");
                return;
            }
        } catch (Exception e) {
            LOGGER.info("DB load failed or DB empty, falling back to JSON. Reason: " + e.getMessage());
        }

        // JSON fallback (existing behavior)
        File dataFile = new File(DATA_FILE);
        
        if (!dataFile.exists()) {
            LOGGER.info("Data file not found. Starting with empty inventory.");
            initializeSampleData(); // ABSTRACTION: Hide sample data creation
            return;
        }
        
        try {
            // ABSTRACTION: Hide JSON parsing complexity
            List<InventoryItem> items = objectMapper.readValue(dataFile, 
                    new TypeReference<List<InventoryItem>>() {});
            
            // ENCAPSULATION: Safely update internal state via the observable view
            // Use observableInventory so JavaFX listeners are notified of the changes
            observableInventory.clear();
            observableInventory.addAll(items);
            
            // Recalculate low stock status for all items after loading
            for (InventoryItem item : observableInventory) {
                item.setLowStock(item.getQuantity() <= item.getMinStockLevel());
            }
            
            // ABSTRACTION: Hide ID counter management
            int maxId = items.stream()
                    .mapToInt(InventoryItem::getId)
                    .max()
                    .orElse(0);
            nextId.set(maxId + 1);
            
            LOGGER.info("Loaded " + items.size() + " items from " + DATA_FILE);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading data from " + DATA_FILE + ": " + e.getMessage(), e);
            // ABSTRACTION: Hide backup recovery process
            loadFromBackup();
        }
    }
    
    /**
     * Save inventory data to JSON file with automatic backup
     * ABSTRACTION: Hides JSON serialization and backup creation
     * ENCAPSULATION: Protects data through backup mechanism
     */
    public void saveData() {
        try {
            // ABSTRACTION: Hide backup creation process
            File dataFile = new File(DATA_FILE);
            if (dataFile.exists()) {
                // Copy the existing data file to backup so we preserve the previous state
                File backupFile = new File(BACKUP_FILE);
                try {
                    Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Failed to create file backup by copying existing data file: " + ex.getMessage(), ex);
                    // Fallback: write current in-memory state to backup
                    objectMapper.writeValue(backupFile, new ArrayList<>(inventoryItems));
                }
            }
            
            // ABSTRACTION: Hide JSON serialization
            objectMapper.writeValue(dataFile, new ArrayList<>(inventoryItems));
            LOGGER.info("Data saved successfully to " + DATA_FILE);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving data to " + DATA_FILE + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * ENCAPSULATION: Private method for backup recovery
     * ABSTRACTION: Hides backup loading complexity from public interface
     */
    private void loadFromBackup() {
        File backupFile = new File(BACKUP_FILE);
        
        if (!backupFile.exists()) {
            LOGGER.info("Backup file not found. Starting with sample data.");
            initializeSampleData();
            return;
        }
        
        try {
            List<InventoryItem> items = objectMapper.readValue(backupFile, 
                    new TypeReference<List<InventoryItem>>() {});
            
            // Update via observable view so UI bindings receive change notifications
            observableInventory.clear();
            observableInventory.addAll(items);
            
            // Recalculate low stock status for all items after loading from backup
            for (InventoryItem item : observableInventory) {
                item.setLowStock(item.getQuantity() <= item.getMinStockLevel());
            }
            
            LOGGER.info("Loaded " + items.size() + " items from backup file");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading backup data: " + e.getMessage(), e);
            initializeSampleData();
        }
    }
    
    /**
     * ENCAPSULATION: Private method to initialize sample data
     * ABSTRACTION: Hides the complexity of creating realistic sample inventory
     */
    private void initializeSampleData() {
    LOGGER.info("Initializing with sample data...");
        
        // Printing supplies - demonstrates real-world inventory items
        addItem(new InventoryItem("A4 Paper", "Paper", "Standard A4 printing paper, 80gsm", 500, 100, 0.10, "Office Supplies Co."));
        addItem(new InventoryItem("A3 Paper", "Paper", "A3 size printing paper, 80gsm", 200, 50, 0.20, "Office Supplies Co."));
        addItem(new InventoryItem("Photo Paper", "Paper", "Glossy photo paper, A4 size", 100, 25, 0.50, "Photo Supplies Inc."));
        
        // Inks and Toners
        addItem(new InventoryItem("Black Ink Cartridge", "Ink", "Compatible black ink cartridge", 15, 5, 25.00, "Print Solutions"));
        addItem(new InventoryItem("Color Ink Set", "Ink", "CMY color ink cartridge set", 10, 3, 45.00, "Print Solutions"));
        addItem(new InventoryItem("Toner Cartridge", "Toner", "Laser printer toner cartridge", 8, 2, 75.00, "Laser Tech"));
        
        // Heat Press Materials
        addItem(new InventoryItem("Heat Transfer Vinyl", "Heat Press", "Various colors heat transfer vinyl", 50, 10, 2.50, "Vinyl Crafts"));
        addItem(new InventoryItem("Sublimation Paper", "Heat Press", "Sublimation transfer paper", 200, 50, 0.75, "Sublimation Supplies"));
        addItem(new InventoryItem("Plain T-Shirts", "Heat Press", "Cotton t-shirts for printing", 100, 20, 8.00, "Apparel Wholesale"));
        addItem(new InventoryItem("Ceramic Mugs", "Heat Press", "White ceramic mugs for sublimation", 50, 15, 3.50, "Mug Suppliers"));
        addItem(new InventoryItem("Tumblers", "Heat Press", "Stainless steel tumblers", 30, 10, 12.00, "Drinkware Co."));
        
        // Tarpaulin Materials
        addItem(new InventoryItem("Tarpaulin Material", "Tarpaulin", "Heavy-duty tarpaulin material", 100, 20, 3.00, "Banner Materials"));
        addItem(new InventoryItem("Eyelets", "Tarpaulin", "Metal eyelets for tarpaulin", 1000, 200, 0.05, "Hardware Store"));
        addItem(new InventoryItem("Rope", "Tarpaulin", "Nylon rope for tarpaulin", 50, 10, 1.50, "Hardware Store"));
        
    LOGGER.info("Sample data initialized successfully");
    }
    
    /**
     * ABSTRACTION: High-level method providing inventory analytics
     * ENCAPSULATION: Returns calculated statistics without exposing calculation logic
     * @return InventoryStats object with computed statistics
     */
    public InventoryStats getInventoryStats() {
        int totalItems = inventoryItems.size();
        int lowStockCount = (int) inventoryItems.stream().filter(InventoryItem::isLowStock).count();
        double totalValue = inventoryItems.stream().mapToDouble(InventoryItem::getTotalValue).sum();
        int totalQuantity = inventoryItems.stream().mapToInt(InventoryItem::getQuantity).sum();
        
        return new InventoryStats(totalItems, lowStockCount, totalValue, totalQuantity);
    }
    
    /**
     * ENCAPSULATION: Inner class to encapsulate statistics data
     * ABSTRACTION: Provides a clean interface for accessing inventory statistics
     * 
     * This inner class demonstrates composition and encapsulation by grouping
     * related statistical data together with controlled access.
     */
    public static class InventoryStats {
        // ENCAPSULATION: Private fields with public getters
        private final int totalItems;
        private final int lowStockItems;
        private final double totalValue;
        private final int totalQuantity;
        
        /**
         * Constructor for creating immutable statistics object
         * ENCAPSULATION: All fields are final and set through constructor
         */
        public InventoryStats(int totalItems, int lowStockItems, double totalValue, int totalQuantity) {
            this.totalItems = totalItems;
            this.lowStockItems = lowStockItems;
            this.totalValue = totalValue;
            this.totalQuantity = totalQuantity;
        }
        
        // ENCAPSULATION: Read-only access to statistics
        public int getTotalItems() { return totalItems; }
        public int getLowStockItems() { return lowStockItems; }
        public double getTotalValue() { return totalValue; }
        public int getTotalQuantity() { return totalQuantity; }
    }
}
