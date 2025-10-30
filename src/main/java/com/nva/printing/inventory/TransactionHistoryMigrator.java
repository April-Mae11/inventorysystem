package com.nva.printing.inventory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One-time utility to migrate current in-memory TransactionLogger records into the database.
 * Run from the application (or main) to persist existing UI history.
 */
public class TransactionHistoryMigrator {
    public static void migrate() {
        int migrated = migrateAndCount();
        System.out.println("TransactionHistoryMigrator: migrated " + migrated + " records.");
    }

    public static int migrateAndCount() {
        TransactionLogger logger = TransactionLogger.getInstance();
        List<TransactionLogger.TransactionRecord> records = logger.getRecords();
        DatabaseManager db = DatabaseManager.getInstance();
        int success = 0;
        for (TransactionLogger.TransactionRecord r : records) {
            try {
                LocalDateTime when = r.getDate();
                boolean ok = db.insertTransaction(r.getItemName(), r.getType(), r.getQuantity(), r.getUser(), when);
                if (ok) success++;
            } catch (Exception e) {
                // ignore single failures but continue
            }
        }
        return success;
    }

    public static void main(String[] args) {
        // ensure data and users are loaded so lookups succeed
        AuthManager.getInstance().loadUsers();
        DataManager.getInstance().loadData();
        migrate();
        // optional: shutdown executor
        TransactionLogger.getInstance().shutdown();
    }
}
