package com.nva.printing.inventory;

import java.util.List;
import java.util.logging.Logger;

/**
 * Small utility to migrate existing JSON inventory_data.json into the database.
 * Run this once after starting XAMPP and creating the DB/table.
 */
public class JsonToDbMigrator {
    private static final Logger LOGGER = Logger.getLogger(JsonToDbMigrator.class.getName());

    public static void main(String[] args) {
        DataManager dm = DataManager.getInstance();
        dm.loadData(); // this will load from JSON if DB is empty/unavailable
        List<InventoryItem> items = dm.getAllItems();
        LOGGER.info("Found " + items.size() + " items locally to migrate.");

        int migrated = 0;
        for (InventoryItem item : items) {
            try {
                int id = DatabaseManager.getInstance().insertItem(item);
                if (id > 0) migrated++;
            } catch (Exception e) {
                LOGGER.warning("Failed to migrate item " + item.getName() + ": " + e.getMessage());
            }
        }

        LOGGER.info("Migration complete. Migrated count: " + migrated);
    }
}
