package com.nva.printing.inventory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Minimal DatabaseManager using DriverManager for XAMPP/MySQL integration.
 * It provides basic load/insert/update/delete for inventory items.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;
    private HikariDataSource ds;

    private DatabaseManager() {
        // Configure HikariCP using DatabaseConfig
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(DatabaseConfig.JDBC_URL);
            cfg.setUsername(DatabaseConfig.DB_USER);
            cfg.setPassword(DatabaseConfig.DB_PASS);
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(1);
            cfg.setPoolName("NVA-Hikari-Pool");
            cfg.addDataSourceProperty("cachePrepStmts", "true");
            cfg.addDataSourceProperty("prepStmtCacheSize", "250");
            cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            this.ds = new HikariDataSource(cfg);
            // Log successful initialization (do not print credentials)
            try {
                LOGGER.info("Successfully connected to database at " + DatabaseConfig.DB_HOST + ":" + DatabaseConfig.DB_PORT + "/" + DatabaseConfig.DB_NAME);
            } catch (Exception _logEx) {
                // Keep silent if logging fails for any reason
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize HikariCP: " + e.getMessage());
            throw e;
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private Connection getConnection() throws SQLException {
        if (ds != null) return ds.getConnection();
        return DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.DB_USER, DatabaseConfig.DB_PASS);
    }

    public List<InventoryItem> loadAllItems() {
        List<InventoryItem> results = new ArrayList<>();
        String sql = "SELECT id, name, category, description, quantity, min_stock_level, unit_price, supplier, last_updated, stock_in, stock_out, low_stock, out_of_stock FROM inventory_items";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventoryItem it = new InventoryItem();
                it.setId(rs.getInt("id"));
                it.setName(rs.getString("name"));
                it.setCategory(rs.getString("category"));
                it.setDescription(rs.getString("description"));
                it.setQuantity(rs.getInt("quantity"));
                it.setMinStockLevel(rs.getInt("min_stock_level"));
                it.setUnitPrice(rs.getDouble("unit_price"));
                it.setSupplier(rs.getString("supplier"));
                Timestamp ts = rs.getTimestamp("last_updated");
                if (ts != null) it.setLastUpdated(ts.toLocalDateTime());
                it.setStockIn(rs.getInt("stock_in"));
                it.setStockOut(rs.getInt("stock_out"));
                it.setLowStock(rs.getBoolean("low_stock"));
                it.setOutOfStock(rs.getBoolean("out_of_stock"));
                results.add(it);
            }
        } catch (SQLException e) {
            LOGGER.severe("loadAllItems error: " + e.getMessage());
        }
        return results;
    }

    public int insertItem(InventoryItem item) {
        String sql = "INSERT INTO inventory_items (name, category, description, quantity, min_stock_level, unit_price, supplier, last_updated, stock_in, stock_out, low_stock, out_of_stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, item.getName());
            ps.setString(idx++, item.getCategory());
            ps.setString(idx++, item.getDescription());
            ps.setInt(idx++, item.getQuantity());
            ps.setInt(idx++, item.getMinStockLevel());
            ps.setDouble(idx++, item.getUnitPrice());
            ps.setString(idx++, item.getSupplier());
            LocalDateTime lu = item.getLastUpdated();
            ps.setTimestamp(idx++, lu == null ? null : Timestamp.valueOf(lu));
            ps.setInt(idx++, item.getStockIn());
            ps.setInt(idx++, item.getStockOut());
            ps.setBoolean(idx++, item.isLowStock());
            ps.setBoolean(idx++, item.isOutOfStock());

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    item.setId(generatedId);
                    return generatedId;
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("insertItem error: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateItem(InventoryItem item) {
        String sql = "UPDATE inventory_items SET name=?, category=?, description=?, quantity=?, min_stock_level=?, unit_price=?, supplier=?, last_updated=?, stock_in=?, stock_out=?, low_stock=?, out_of_stock=? WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, item.getName());
            ps.setString(idx++, item.getCategory());
            ps.setString(idx++, item.getDescription());
            ps.setInt(idx++, item.getQuantity());
            ps.setInt(idx++, item.getMinStockLevel());
            ps.setDouble(idx++, item.getUnitPrice());
            ps.setString(idx++, item.getSupplier());
            LocalDateTime lu = item.getLastUpdated();
            ps.setTimestamp(idx++, lu == null ? null : Timestamp.valueOf(lu));
            ps.setInt(idx++, item.getStockIn());
            ps.setInt(idx++, item.getStockOut());
            ps.setBoolean(idx++, item.isLowStock());
            ps.setBoolean(idx++, item.isOutOfStock());
            ps.setInt(idx++, item.getId());

            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.severe("updateItem error: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteItem(int id) {
        String sql = "DELETE FROM inventory_items WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.severe("deleteItem error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert a transaction record into the transactions table.
     * Attempts to resolve item and user IDs by name. If not found, inserts NULL for those foreign keys
     * and puts a human-readable description into TransactionDescription.
     */
    public boolean insertTransaction(String itemName, String type, int quantity, String username) {
        String findItemSql = "SELECT id FROM inventory_items WHERE name = ? LIMIT 1";
        String findUserSql = "SELECT UserID FROM Users WHERE Username = ? LIMIT 1";
        String insertSql = "INSERT INTO transactions (ItemID, UserID, Quantity, LastUpdated, TransactionDescription) VALUES (?, ?, ?, ?, ?)";

        Integer itemId = null;
        Integer userId = null;
        try (Connection conn = getConnection()) {
            // find item id
            try (PreparedStatement ps = conn.prepareStatement(findItemSql)) {
                ps.setString(1, itemName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) itemId = rs.getInt(1);
                }
            } catch (SQLException ignored) {}

            // find user id
            try (PreparedStatement ps = conn.prepareStatement(findUserSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) userId = rs.getInt(1);
                }
            } catch (SQLException ignored) {}

            // build description
            String desc = (type == null ? "" : type) + (itemName == null ? "" : " - " + itemName);

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                if (itemId != null) ps.setInt(1, itemId); else ps.setNull(1, Types.INTEGER);
                if (userId != null) ps.setInt(2, userId); else ps.setNull(2, Types.INTEGER);
                ps.setInt(3, quantity);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(5, desc);
                int affected = ps.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            LOGGER.severe("insertTransaction error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert a transaction record into the transactions table with a specified timestamp.
     * This preserves original timestamps (useful when migrating in-memory records).
     */
    public boolean insertTransaction(String itemName, String type, int quantity, String username, LocalDateTime when) {
        String findItemSql = "SELECT id FROM inventory_items WHERE name = ? LIMIT 1";
        String findUserSql = "SELECT UserID FROM Users WHERE Username = ? LIMIT 1";
        String insertSql = "INSERT INTO transactions (ItemID, UserID, Quantity, LastUpdated, TransactionDescription) VALUES (?, ?, ?, ?, ?)";

        Integer itemId = null;
        Integer userId = null;
        try (Connection conn = getConnection()) {
            // find item id
            try (PreparedStatement ps = conn.prepareStatement(findItemSql)) {
                ps.setString(1, itemName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) itemId = rs.getInt(1);
                }
            } catch (SQLException ignored) {}

            // find user id
            try (PreparedStatement ps = conn.prepareStatement(findUserSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) userId = rs.getInt(1);
                }
            } catch (SQLException ignored) {}

            // build description
            String desc = (type == null ? "" : type) + (itemName == null ? "" : " - " + itemName);

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                if (itemId != null) ps.setInt(1, itemId); else ps.setNull(1, Types.INTEGER);
                if (userId != null) ps.setInt(2, userId); else ps.setNull(2, Types.INTEGER);
                ps.setInt(3, quantity);
                ps.setTimestamp(4, when == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(when));
                ps.setString(5, desc);
                int affected = ps.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            LOGGER.severe("insertTransaction error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert an archive record into the Archive table.
     * The Archive table schema currently contains: ArchiveID, ItemID, ArchiveDate, ArchiveNote
     * We store a human-readable snapshot of the archived item in ArchiveNote so historical
     * details remain available even if the item row is later deleted.
     */
    public boolean insertArchive(ArchiveItem archiveItem, Integer itemId) {
        String insertSql = "INSERT INTO Archive (ItemID, ArchiveDate, ArchiveNote) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            if (itemId != null) ps.setInt(1, itemId); else ps.setNull(1, Types.INTEGER);
            java.sql.Date d = archiveItem.getDateUsed() == null ? new java.sql.Date(System.currentTimeMillis()) : java.sql.Date.valueOf(archiveItem.getDateUsed().toLocalDate());
            ps.setDate(2, d);
            // Build a compact note preserving useful snapshot information
            String note = String.format("%s - %s - qty:%d - by:%s - reason:%s - price:%.2f",
                    archiveItem.getType(), archiveItem.getOriginalItemName(), archiveItem.getQuantityUsed(),
                    archiveItem.getUsedBy() == null ? "" : archiveItem.getUsedBy(),
                    archiveItem.getReason() == null ? "" : archiveItem.getReason(),
                    archiveItem.getUnitPrice());
            if (note.length() > 250) note = note.substring(0, 250);
            ps.setString(3, note);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.severe("insertArchive error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Return all category names from the categories table (if present).
     */
    public java.util.List<String> getAllCategories() {
        java.util.List<String> cats = new java.util.ArrayList<>();
        String sql = "SELECT CategoryName FROM categories ORDER BY CategoryName";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cats.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.fine("getAllCategories error or table missing: " + e.getMessage());
        }
        return cats;
    }

    /**
     * Insert a category if not exists. Returns generated or existing id, or -1 on error.
     */
    public int insertCategory(String name, String description) {
        if (name == null || name.isBlank()) return -1;
        String find = "SELECT CategoryID FROM categories WHERE CategoryName = ? LIMIT 1";
        String insert = "INSERT INTO categories (CategoryName, Description) VALUES (?, ?)";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(find)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, description == null ? "" : description);
                int affected = ps.executeUpdate();
                if (affected == 0) return -1;
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
            }
        } catch (SQLException e) {
            LOGGER.severe("insertCategory error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Delete a category by name. Returns true if a row was deleted.
     */
    public boolean deleteCategory(String name) {
        if (name == null || name.isBlank()) return false;
        String sql = "DELETE FROM categories WHERE CategoryName = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.severe("deleteCategory error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert a POS sale record into POS_Sales table. Resolves user/item IDs by username/name if possible.
     */
    public boolean insertPosSale(String username, String itemName, int quantitySold, double totalPrice, LocalDateTime when) {
        String findUser = "SELECT UserID FROM Users WHERE Username = ? LIMIT 1";
        String findItem = "SELECT id FROM inventory_items WHERE name = ? LIMIT 1";
        String insert = "INSERT INTO POS_Sales (UserID, ItemID, QuantitySold, TotalPrice, SalesDate) VALUES (?, ?, ?, ?, ?)";
        Integer userId = null; Integer itemId = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(findUser)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) userId = rs.getInt(1); }
            } catch (SQLException ignored) {}
            try (PreparedStatement ps = conn.prepareStatement(findItem)) {
                ps.setString(1, itemName);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) itemId = rs.getInt(1); }
            } catch (SQLException ignored) {}
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                if (userId != null) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
                if (itemId != null) ps.setInt(2, itemId); else ps.setNull(2, Types.INTEGER);
                ps.setInt(3, quantitySold);
                ps.setDouble(4, totalPrice);
                ps.setTimestamp(5, when == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(when));
                int affected = ps.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            LOGGER.severe("insertPosSale error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert a POS sale line and ensure it is linked to a transaction header (TransactionRef).
     * This method will attempt to create a POS_Transactions header table (if missing) and
     * add a TransactionRef column to POS_Sales (best-effort). If those structural operations
     * fail, it will still attempt to insert the POS_Sales row without the TransactionRef.
     *
     * @param transactionRef application-generated transaction identifier (e.g. UUID or receipt no)
     */
    public boolean insertPosSaleWithTransaction(String transactionRef, String username, String itemName, int quantitySold, double totalPrice, LocalDateTime when, String paymentMethod, String paymentRef) {
        if (transactionRef == null) transactionRef = java.util.UUID.randomUUID().toString();
        String findUser = "SELECT UserID FROM Users WHERE Username = ? LIMIT 1";
        String findItem = "SELECT id FROM inventory_items WHERE name = ? LIMIT 1";

        try (Connection conn = getConnection()) {
            Integer userId = null; Integer itemId = null;
            try (PreparedStatement ps = conn.prepareStatement(findUser)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) userId = rs.getInt(1); }
            } catch (SQLException ignored) {}
            try (PreparedStatement ps = conn.prepareStatement(findItem)) {
                ps.setString(1, itemName);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) itemId = rs.getInt(1); }
            } catch (SQLException ignored) {}

            // Ensure POS_Transactions header table exists (best-effort)
            try (Statement stmt = conn.createStatement()) {
                String createHeader = "CREATE TABLE IF NOT EXISTS POS_Transactions (" +
                        "TransactionRef VARCHAR(64) PRIMARY KEY, UserID INT, TotalAmount DOUBLE, TransactionDate DATETIME, FOREIGN KEY (UserID) REFERENCES Users(UserID) ON UPDATE CASCADE ON DELETE SET NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                stmt.execute(createHeader);
            } catch (SQLException ignored) {}

            // Ensure header has payment fields (best-effort)
            try (Statement stmt = conn.createStatement()) {
                try {
                    String alter = "ALTER TABLE POS_Transactions ADD COLUMN IF NOT EXISTS PaymentMethod VARCHAR(32) NULL, ADD COLUMN IF NOT EXISTS PaymentRef VARCHAR(128) NULL, ADD COLUMN IF NOT EXISTS TenderedAmount DOUBLE NULL, ADD COLUMN IF NOT EXISTS ChangeAmount DOUBLE NULL";
                    stmt.executeUpdate(alter);
                } catch (SQLException ignored) {}
            } catch (SQLException ignored) {}

            // Insert header (ignore duplicate key if already present)
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO POS_Transactions (TransactionRef, UserID, TotalAmount, TransactionDate, PaymentMethod, PaymentRef) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE TotalAmount=VALUES(TotalAmount), TransactionDate=VALUES(TransactionDate), PaymentMethod=VALUES(PaymentMethod), PaymentRef=VALUES(PaymentRef)")) {
                if (transactionRef != null) ps.setString(1, transactionRef); else ps.setNull(1, Types.VARCHAR);
                if (userId != null) ps.setInt(2, userId); else ps.setNull(2, Types.INTEGER);
                ps.setDouble(3, totalPrice);
                ps.setTimestamp(4, when == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(when));
                ps.setString(5, paymentMethod == null ? "" : paymentMethod);
                ps.setString(6, paymentRef == null ? "" : paymentRef);
                ps.executeUpdate();
            } catch (SQLException ignored) {}

            // Ensure POS_Sales has a TransactionRef column (best-effort)
            try (Statement stmt = conn.createStatement()) {
                try {
                    stmt.executeUpdate("ALTER TABLE POS_Sales ADD COLUMN IF NOT EXISTS TransactionRef VARCHAR(64) NULL");
                } catch (SQLException ex) {
                    // Some MySQL versions may not support IF NOT EXISTS on ADD COLUMN; ignore errors
                }
            } catch (SQLException ignored) {}

            // Now attempt to insert the sale line including TransactionRef
            String insertWithRef = "INSERT INTO POS_Sales (UserID, ItemID, QuantitySold, TotalPrice, SalesDate, TransactionRef) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertWithRef)) {
                if (userId != null) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
                if (itemId != null) ps.setInt(2, itemId); else ps.setNull(2, Types.INTEGER);
                ps.setInt(3, quantitySold);
                ps.setDouble(4, totalPrice);
                ps.setTimestamp(5, when == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(when));
                ps.setString(6, transactionRef);
                int affected = ps.executeUpdate();
                return affected > 0;
            } catch (SQLException ex) {
                // Fallback: insert without TransactionRef (older schema)
                String insert = "INSERT INTO POS_Sales (UserID, ItemID, QuantitySold, TotalPrice, SalesDate) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(insert)) {
                    if (userId != null) ps2.setInt(1, userId); else ps2.setNull(1, Types.INTEGER);
                    if (itemId != null) ps2.setInt(2, itemId); else ps2.setNull(2, Types.INTEGER);
                    ps2.setInt(3, quantitySold);
                    ps2.setDouble(4, totalPrice);
                    ps2.setTimestamp(5, when == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(when));
                    int affected = ps2.executeUpdate();
                    return affected > 0;
                } catch (SQLException ex2) {
                    LOGGER.severe("insertPosSaleWithTransaction error: " + ex2.getMessage());
                }
            }

        } catch (SQLException e) {
            LOGGER.severe("insertPosSaleWithTransaction error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert a stock alert into Stock_Alert table. Returns true if inserted.
     */
    public boolean insertStockAlert(int itemId, LocalDate alertDate, int minimumStock, String statusDescription) {
        String insert = "INSERT INTO Stock_Alert (ItemID, AlertDate, MinimumStock, StatusDescription) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setInt(1, itemId);
            ps.setDate(2, alertDate == null ? java.sql.Date.valueOf(LocalDate.now()) : java.sql.Date.valueOf(alertDate));
            ps.setInt(3, minimumStock);
            ps.setString(4, statusDescription == null ? "" : statusDescription);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.severe("insertStockAlert error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert a supplier into suppliers table if available. Returns generated SupplierID or -1 on failure.
     * Expected table schema per dbinventory.sql: suppliers(SupplierID PK AUTO_INCREMENT, SupplierName, ContactNumber, Address, SuppliedProduct)
     */
    public int insertSupplier(Supplier s) {
        if (s == null || s.getName() == null || s.getName().trim().isEmpty()) return -1;
        String insert = "INSERT INTO suppliers (SupplierName, ContactNumber, Address, SuppliedProduct) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, s.getName());
            ps.setString(idx++, s.getContactNumber() == null ? "" : s.getContactNumber());
            ps.setString(idx++, s.getAddress() == null ? "" : s.getAddress());
            ps.setString(idx++, s.getSuppliedProduct() == null ? "" : s.getSuppliedProduct());
            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            LOGGER.fine("insertSupplier DB failed: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Delete a supplier by name from the suppliers table. Returns true if a row was deleted.
     */
    public boolean deleteSupplierByName(String name) {
        if (name == null || name.isBlank()) return false;
        String sql = "DELETE FROM suppliers WHERE SupplierName = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.fine("deleteSupplierByName DB failed: " + e.getMessage());
        }
        return false;
    }
}
