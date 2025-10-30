CREATE DATABASE IF NOT EXISTS `dbinventory` CHARACTER SET = 'utf8mb4' COLLATE = 'utf8mb4_unicode_ci';
USE `dbinventory`;

/* Users table */
DROP TABLE IF EXISTS `Users`;
CREATE TABLE `Users` (
  `UserID` INT PRIMARY KEY AUTO_INCREMENT,
  `Username` VARCHAR(50) UNIQUE NOT NULL,
  `PassWord` VARCHAR(255) NOT NULL,
  `Role` VARCHAR(50) DEFAULT 'user'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `Users` (Username, PassWord, Role) VALUES
  ('manager', 'manager', 'manager'),
  ('headproduction', 'headproduction', 'headproduction'),
  ('cashier', 'cashier', 'cashier');

/* Categories */
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `CategoryID` INT PRIMARY KEY AUTO_INCREMENT,
  `CategoryName` VARCHAR(100) UNIQUE NOT NULL,
  `Description` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `categories` (CategoryName, Description) VALUES
  ('Paper', 'Bond and specialty papers'),
  ('Ink', 'Ink cartridges and toner'),
  ('Heat Press', 'Heat transfer materials');

/* Suppliers */
DROP TABLE IF EXISTS `suppliers`;
CREATE TABLE `suppliers` (
  `SupplierID` INT PRIMARY KEY AUTO_INCREMENT,
  `SupplierName` VARCHAR(150) NOT NULL,
  `ContactNumber` VARCHAR(50) DEFAULT '',
  `Address` VARCHAR(255) DEFAULT '',
  `SuppliedProduct` VARCHAR(255) DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `suppliers` (SupplierName, ContactNumber, Address, SuppliedProduct) VALUES
  ('ABC Supplies', '09088184444', 'Bukidnon', 'Paper'),
  ('InkWorld', '09123654789', 'Davao', 'Ink');

/* Inventory items */
DROP TABLE IF EXISTS `inventory_items`;
CREATE TABLE `inventory_items` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `category` VARCHAR(100),
  `description` TEXT,
  `quantity` INT DEFAULT 0,
  `min_stock_level` INT DEFAULT 10,
  `unit_price` DOUBLE DEFAULT 0.0,
  `supplier` VARCHAR(255),
  `last_updated` DATETIME,
  `stock_in` INT DEFAULT 0,
  `stock_out` INT DEFAULT 0,
  `low_stock` TINYINT(1) DEFAULT 0,
  `out_of_stock` TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `inventory_items` (name, category, description, quantity, min_stock_level, unit_price, supplier, last_updated) VALUES
  ('Bond Paper A4', 'Paper', 'Standard A4 printing paper, 80gsm', 500, 100, 0.10, 'ABC Supplies', '2025-05-13 13:24:04'),
  ('Ink Cartridge Black', 'Ink', 'Compatible black ink cartridge', 15, 5, 25.00, 'InkWorld', '2025-06-01 09:12:00');

/* POS Transactions header: groups sale lines under a transaction reference */
DROP TABLE IF EXISTS `POS_Transactions`;
CREATE TABLE `POS_Transactions` (
  `TransactionRef` VARCHAR(64) PRIMARY KEY,
  `UserID` INT,
  `TotalAmount` DOUBLE NOT NULL,
  `TransactionDate` DATETIME NOT NULL,
  `PaymentMethod` VARCHAR(32) DEFAULT NULL,
  `PaymentRef` VARCHAR(128) DEFAULT NULL,
  `TenderedAmount` DOUBLE DEFAULT NULL,
  `ChangeAmount` DOUBLE DEFAULT NULL,
  FOREIGN KEY (UserID) REFERENCES Users(UserID) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* POS Sales: individual sale lines that reference a transaction header via TransactionRef */
DROP TABLE IF EXISTS `POS_Sales`;
CREATE TABLE `POS_Sales` (
  `SaleID` INT PRIMARY KEY AUTO_INCREMENT,
  `TransactionRef` VARCHAR(64) DEFAULT NULL,
  `UserID` INT,
  `ItemID` INT,
  `QuantitySold` INT NOT NULL,
  `TotalPrice` DOUBLE NOT NULL,
  `SalesDate` DATETIME NOT NULL,
  FOREIGN KEY (UserID) REFERENCES Users(UserID) ON UPDATE CASCADE ON DELETE SET NULL,
  FOREIGN KEY (ItemID) REFERENCES inventory_items(id) ON UPDATE CASCADE ON DELETE SET NULL,
  FOREIGN KEY (TransactionRef) REFERENCES POS_Transactions(TransactionRef) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `POS_Sales` (UserID, ItemID, QuantitySold, TotalPrice, SalesDate) VALUES
  (1, 1, 120, 30000.00, '2025-05-13 13:24:04');

/* Transactions (history) */
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
  `TransactionID` INT PRIMARY KEY AUTO_INCREMENT,
  `ItemID` INT,
  `UserID` INT,
  `Quantity` INT NOT NULL DEFAULT 0,
  `LastUpdated` DATETIME,
  `TransactionDescription` VARCHAR(255) DEFAULT '',
  FOREIGN KEY (ItemID) REFERENCES inventory_items(id) ON UPDATE CASCADE ON DELETE SET NULL,
  FOREIGN KEY (UserID) REFERENCES Users(UserID) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `transactions` (ItemID, UserID, Quantity, LastUpdated, TransactionDescription) VALUES
  (1, 1, 120, '2025-05-13 13:24:04', 'Bond Paper Sold');

/* Stock Alert */
DROP TABLE IF EXISTS `Stock_Alert`;
CREATE TABLE `Stock_Alert` (
  `AlertID` INT PRIMARY KEY AUTO_INCREMENT,
  `ItemID` INT,
  `AlertDate` DATE,
  `MinimumStock` INT NOT NULL,
  `StatusDescription` VARCHAR(255) NOT NULL,
  FOREIGN KEY (ItemID) REFERENCES inventory_items(id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `Stock_Alert` (ItemID, AlertDate, MinimumStock, StatusDescription) VALUES
  (2, '2025-08-22', 9, 'Low Stock');

/* Archive */
DROP TABLE IF EXISTS `Archive`;
CREATE TABLE `Archive` (
  `ArchiveID` INT PRIMARY KEY AUTO_INCREMENT,
  `ItemID` INT,
  `ArchiveDate` DATE NOT NULL,
  `ArchiveNote` VARCHAR(255) DEFAULT '',
  FOREIGN KEY (ItemID) REFERENCES inventory_items(id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `Archive` (ItemID, ArchiveDate) VALUES
  (1, '2025-05-13'),
  (2, '2025-08-22');

/* Indexes */
CREATE INDEX idx_item_name ON inventory_items(name(80));
CREATE INDEX idx_suppliers_name ON suppliers(SupplierName(80));
CREATE INDEX idx_categories_name ON categories(CategoryName(80));

