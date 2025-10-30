package com.nva.printing.inventory.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nva.printing.inventory.InventoryItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Utility to migrate inventory_data.json into SQL INSERT statements.
 * Writes a file named db_from_json.sql in the per-user app directory (same place DataManager stores JSON).
 */
public class DataMigrator {
    private static final Logger LOGGER = Logger.getLogger(DataMigrator.class.getName());

    public static void main(String[] args) {
        try {
            String appDir;
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                appDir = appData + File.separator + "NVAInventory";
            } else {
                appDir = System.getProperty("user.home") + File.separator + ".nva_inventory";
            }
            File dir = new File(appDir);
            if (!dir.exists()) dir.mkdirs();

            File dataFile = new File(appDir + File.separator + "inventory_data.json");
            if (!dataFile.exists()) {
                // fallback: current working dir
                File cwdFile = new File("inventory_data.json");
                if (cwdFile.exists()) {
                    dataFile = cwdFile;
                } else {
                    System.err.println("inventory_data.json not found in app dir or working directory: " + dataFile.getAbsolutePath());
                    return;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            // Handle Java 8 date/time types (LocalDateTime, LocalDate, etc.)
            mapper.registerModule(new JavaTimeModule());
            List<InventoryItem> items = mapper.readValue(dataFile, new TypeReference<List<InventoryItem>>() {});

            // Maintain insertion order
            Map<String, Integer> categoryIds = new LinkedHashMap<>();
            Map<String, Integer> supplierIds = new LinkedHashMap<>();
            AtomicInteger catSeq = new AtomicInteger(1);
            AtomicInteger supSeq = new AtomicInteger(1);
            AtomicInteger itemSeq = new AtomicInteger(1);

            StringBuilder sql = new StringBuilder();
            sql.append("-- Generated SQL from inventory_data.json\n");
            sql.append("USE dbinventory;\n\n");

            // Create minimal tables if desired (optional)
            sql.append("-- Inserts for categories\n");
            for (InventoryItem it : items) {
                String cat = it.getCategory() == null ? "" : it.getCategory().trim();
                if (!cat.isEmpty() && !categoryIds.containsKey(cat)) {
                    int id = catSeq.getAndIncrement();
                    categoryIds.put(cat, id);
                    sql.append(String.format("INSERT INTO categories (CategoryID, CategoryName) VALUES (%d, %s);\n", id, sqlEscape(cat)));
                }
            }

            sql.append("\n-- Inserts for suppliers\n");
            for (InventoryItem it : items) {
                String sup = it.getSupplier() == null ? "" : it.getSupplier().trim();
                if (!sup.isEmpty() && !supplierIds.containsKey(sup)) {
                    int id = supSeq.getAndIncrement();
                    supplierIds.put(sup, id);
                    sql.append(String.format("INSERT INTO suppliers (SupplierID, SupplierName) VALUES (%d, %s);\n", id, sqlEscape(sup)));
                }
            }

            sql.append("\n-- Inserts for items\n");
            for (InventoryItem it : items) {
                int id = itemSeq.getAndIncrement();
                String name = it.getName() == null ? "" : it.getName().trim();
                String cat = it.getCategory() == null ? "" : it.getCategory().trim();
                String sup = it.getSupplier() == null ? "" : it.getSupplier().trim();
                Integer catId = categoryIds.get(cat);
                Integer supId = supplierIds.get(sup);
                int qty = it.getQuantity();
                double price = it.getUnitPrice();
                sql.append(String.format("INSERT INTO item (ItemID, ItemName, CategoryID, SupplierID, Quantity, UnitPrice) VALUES (%d, %s, %s, %s, %d, %.2f);\n",
                        id,
                        sqlEscape(name),
                        catId == null ? "NULL" : String.valueOf(catId),
                        supId == null ? "NULL" : String.valueOf(supId),
                        qty,
                        price
                ));
            }

            File out = new File(appDir + File.separator + "db_from_json.sql");
            try (FileWriter fw = new FileWriter(out)) {
                fw.write(sql.toString());
            }

            System.out.println("Generated SQL file: " + out.getAbsolutePath());
            LOGGER.info("Generated SQL file: " + out.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sqlEscape(String s) {
        if (s == null) return "''";
        String escaped = s.replace("'", "''");
        return "'" + escaped + "'";
    }
}
