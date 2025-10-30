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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TransactionLogger - keeps an in-memory list of transaction records and persists them to disk
 * so history survives application restarts. It still attempts to insert into the DB asynchronously
 * (when available) to keep parity with existing behaviour.
 */
public class TransactionLogger {
    private static TransactionLogger instance;
    private final ObservableList<TransactionRecord> records;
    private final ObjectMapper objectMapper;
    private final File transactionsFile;
    private static final Logger LOGGER = Logger.getLogger(TransactionLogger.class.getName());

    private TransactionLogger() {
        this.records = FXCollections.observableArrayList();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TransactionLogger-DB");
            t.setDaemon(true);
            return t;
        });

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Determine per-user app dir (same logic as DataManager)
        String dir;
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            dir = appData + File.separator + "NVAInventory";
        } else {
            dir = System.getProperty("user.home") + File.separator + ".nva_inventory";
        }
        File d = new File(dir);
        if (!d.exists()) d.mkdirs();
        this.transactionsFile = new File(dir + File.separator + "transactions.json");

        // Load persisted records if any
        loadFromDisk();
    }

    public static TransactionLogger getInstance() {
        if (instance == null) {
            instance = new TransactionLogger();
        }
        return instance;
    }

    /**
     * Log a transaction (in-memory), persist to DB asynchronously (if available), and save to disk.
     */
    public void log(String itemName, String type, int quantity, String user) {
        TransactionRecord rec = new TransactionRecord(LocalDateTime.now(), itemName, type, quantity, user, 0.0, 0.0);
        records.add(rec);

        // Persist to DB asynchronously 
        try {
            executor.submit(() -> {
                try {
                    DatabaseManager.getInstance().insertTransaction(itemName, type, quantity, user);
                } catch (Exception e) {
                    // best-effort: ignore DB failures
                }
            });
        } catch (Exception ignored) {}

        // Persist to disk asynchronously to avoid blocking UI
        try {
            executor.submit(() -> saveToDisk());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule transactions save: " + e.getMessage(), e);
        }
    }

    /**
     * Overload: log with unit price (total price inferred as unitPrice * quantity).
     */
    public void log(String itemName, String type, int quantity, String user, double unitPrice) {
        double total = unitPrice * Math.max(0, quantity);
        TransactionRecord rec = new TransactionRecord(LocalDateTime.now(), itemName, type, quantity, user, unitPrice, total);
        records.add(rec);

        // Persist to DB asynchronously (best-effort) - reuse existing DB insertion which doesn't store prices
        try {
            executor.submit(() -> {
                try {
                    DatabaseManager.getInstance().insertTransaction(itemName, type, quantity, user);
                } catch (Exception e) {
                    // best-effort: ignore DB failures
                }
            });
        } catch (Exception ignored) {}

        // Persist to disk asynchronously to avoid blocking UI
        try {
            executor.submit(() -> saveToDisk());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule transactions save: " + e.getMessage(), e);
        }
    }

    public ObservableList<TransactionRecord> getRecords() {
        return records;
    }

    /**
     * Clear all in-memory records and persist the empty list to disk.
     * This is a destructive action and should be guarded by a confirmation in the UI.
     */
    public void clearAllRecords() {
        try {
            records.clear();
            saveToDisk();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to clear transaction records: " + e.getMessage(), e);
        }
    }

    /**
     * Load records from transactions.json into the in-memory list.
     */
    private void loadFromDisk() {
        if (!transactionsFile.exists()) return;
        try {
            List<TransactionRecord> loaded = objectMapper.readValue(transactionsFile, new TypeReference<List<TransactionRecord>>() {});
            if (loaded != null) {
                records.clear();
                records.addAll(loaded);
            }
            LOGGER.info("Loaded " + records.size() + " transaction records from " + transactionsFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load transactions from disk: " + e.getMessage(), e);
        }
    }

    /**
     * Save current in-memory records to transactions.json.
     */
    private void saveToDisk() {
        try {
            List<TransactionRecord> copy = new ArrayList<>(records);
            objectMapper.writeValue(transactionsFile, copy);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save transactions to disk: " + e.getMessage(), e);
        }
    }

    public static class TransactionRecord {
        private LocalDateTime date;
        private String itemName;
        private String type;
        private int quantity;
        private String user;
        private double unitPrice;
        private double totalPrice;

        // Jackson requires a default constructor
        public TransactionRecord() {}

        public TransactionRecord(LocalDateTime date, String itemName, String type, int quantity, String user) {
            this(date, itemName, type, quantity, user, 0.0, 0.0);
        }

        public TransactionRecord(LocalDateTime date, String itemName, String type, int quantity, String user, double unitPrice, double totalPrice) {
            this.date = date;
            this.itemName = itemName;
            this.type = type;
            this.quantity = quantity;
            this.user = user;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }
        
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
    }

    // Executor to persist logs without blocking the JavaFX thread
    private final ExecutorService executor;

    // Shutdown hook (optional) to allow graceful termination when app closes
    public void shutdown() {
        // Ensure latest records are flushed to disk
        try {
            saveToDisk();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save transactions during shutdown: " + e.getMessage(), e);
        }
        try {
            executor.shutdownNow();
        } catch (Exception ignored) {}
    }
}
