# NVA Printing Services - Inventory Management System

A simple JavaFX-based inventory management system designed specifically for NVA Printing Services to replace their manual paper### Technical Architecture

This system is built using Object-Oriented Programming principles and demonstrates the four fundamental OOP concepts:

#### OOP Concepts Implementation

**1. ENCAPSULATION**
- Private fields in `InventoryItem` with controlled access through getters/setters
- `DataManager` singleton with private constructor and controlled data access
- Form validation and UI state management in controllers

**2. INHERITANCE**
- `InventoryApplication` extends JavaFX `Application` class
- Controllers implement `Initializable` interface for framework integration
- Custom cell factories inherit from JavaFX table cell classes

**3. POLYMORPHISM**
- Method overriding: `equals()`, `hashCode()`, `toString()` in `InventoryItem`
- Interface implementations: Lambda expressions for event handlers
- Callback interfaces for custom table cell rendering

**4. ABSTRACTION**
- Complex data operations hidden behind simple method calls in `DataManager`
- UI complexity abstracted through modular controller methods
- Business logic (low stock calculation, statistics) automated and hidden

#### Technologies Used
- **JavaFX**: User interface framework demonstrating inheritance and polymorphism
- **Maven**: Build and dependency management
- **Jackson**: JSON data processing with abstraction for complex serialization
- **SLF4J**: Logging frameworkinventory tracking.

## Features

- **Digital Inventory Management**: Replace paper-based tracking with a modern digital solution
- **Low Stock Alerts**: Automatic visual indicators when items fall below minimum stock levels
- **Search and Filter**: Easy searching by name, category, or stock status
- **Real-time Statistics**: Live dashboard showing total items, low stock count, and inventory value
- **Data Persistence**: Automatic saving and loading of inventory data using JSON format
- **User-friendly Interface**: Clean, intuitive design optimized for printing shop operations
- **Role-based Access Control**: Different user roles with appropriate permissions
- **Inventory Usage Tracking**: Cashier role for tracking item consumption with archive system

## User Roles and Authentication

The system supports three distinct user roles with different access levels:

### 1. Manager Role
- **Username**: `manager`
- **Password**: `manager`
- **Access**: Full system access including user management, inventory management, and all operations
- **Features**: 
  - Add/edit/delete items
  - Stock In/Stock Out operations (track additions and removals)
  - Transaction logging for all inventory changes
  - Archive viewing and usage tracking (view all item usage history)
  - Role-based UI updates for Manager
  - Manage users
  - View all statistics
  - Export data

### 2. Head Production Role
- **Username**: `headproduction`
- **Password**: `headproduction`
- **Access**: Production-focused access for managing inventory
- **Features**: 
  - Add/edit items
  - Stock In/Stock Out operations (track additions and removals)
  - Transaction logging for production-related inventory changes
  - Archive viewing and usage tracking (view production item usage history)
  - Role-based UI updates for Head Production
  - View inventory
  - Manage stock levels
  - Refresh data

### 3. Cashier Role
- **Username**: `cashier`
- **Password**: `cashier`
- **Access**: Inventory usage tracking and archive management
- **Features**: 
  - Use items (reduce quantities with reason tracking)
  - Stock In/Stock Out operations (track additions and removals)
  - Transaction logging for cashier-related inventory changes
  - Archive viewing and usage tracking (view cashier item usage history)
  - Role-based UI updates for Cashier
  - View archive of all used items
  - Track inventory consumption
  - View real-time statistics
  - Search and filter inventory

### Archive System
The cashier role includes a comprehensive archive system that tracks:
- All items used/consumed from inventory
- Who used the items (user tracking)
- When items were used (timestamp)
- Reason for usage (business context)
- Complete usage history with statistics

## Project Information

**Developed by:**
- Allen Kirk A. Cailing
- April Mae O. Cape
- Loreen Angel Culanculan
- Vanessa V. Embornas
- Bernadine E. Suson
- Gerome L. Velasco

**Submitted to:** Love Jean Nadayag Villanueva

## System Requirements

- **Java**: Version 17 or higher
- **Maven**: Version 3.6 or higher
- **Operating System**: Windows, macOS, or Linux
- **Memory**: Minimum 512MB RAM
- **Storage**: At least 100MB free disk space

## Installation and Setup

### Prerequisites

1. **Install Java 17 or higher**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Verify installation: `java -version`

2. **Install Apache Maven**
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn --version`

### Running the Application

1. **Using the Setup Batch File (Recommended for Windows)**
   ```batch
   setup.bat
   ```
   This will automatically:
   - Check Java and Maven installation
   - Build the application if needed
   - Start the inventory system

2. **Using Maven Commands**
   ```bash
   # Build the project
   mvn clean compile package
   
   # Run the application
   mvn javafx:run
   ```

3. **Using Java directly (after building)**
   ```bash
   java --module-path target/dependency --add-modules javafx.controls,javafx.fxml -cp "target/classes;target/dependency/*" com.nva.printing.inventory.InventoryApplication
   ```

## Usage Guide

### Main Interface

The main window displays:
- **Inventory Table**: Shows all items with their details
- **Search Bar**: Filter items by name, description, or supplier
- **Category Filter**: Filter by specific item categories
- **Low Stock Filter**: Show only items that need restocking
- **Statistics Panel**: Real-time inventory statistics

### Managing Items

#### Adding New Items
1. Click "Add Item" button or use File → Inventory → Add New Item
2. Fill in the required information:
   - Item Name (required)
   - Category (required)
   - Description (optional)
   - Quantity
   - Minimum Stock Level
   - Unit Price (required)
   - Supplier (required)
3. Click "Save" to add the item

#### Editing Items
1. Select an item from the table
2. Click "Edit Item" or double-click the row
3. Modify the information as needed
4. Click "Save" to update

#### Deleting Items
1. Select one or more items from the table
2. Click "Delete Item" or use the Delete key
3. Confirm the deletion

### Categories

The system includes predefined categories for printing supplies:
- **Paper**: A4, A3, Photo paper, etc.
- **Ink**: Ink cartridges, color sets
- **Toner**: Laser printer toners
- **Heat Press**: Vinyl, sublimation materials, t-shirts, mugs, tumblers
- **Tarpaulin**: Tarpaulin material, eyelets, rope
- **Office Supplies**: General office items
- **Equipment**: Machinery and tools
- **Other**: Miscellaneous items

You can also create custom categories by typing a new category name.

### Stock Management

- **Low Stock Alerts**: Items with quantity at or below the minimum stock level are highlighted in red
- **Stock Status**: Visual indicators show "LOW STOCK" or "NORMAL STOCK" status
- **Automatic Updates**: Last updated timestamp is automatically recorded when quantities change

## Data Storage

- **File Format**: JSON (inventory_data.json)
- **Backup**: Automatic backup creation (inventory_data_backup.json)
- **Location**: Same folder as the application
- **Auto-save**: Data is saved automatically when changes are made

## Sample Data

On first run, the system initializes with sample data representing typical printing shop inventory:

### Paper Products
- A4 Paper (500 sheets)
- A3 Paper (200 sheets)
- Photo Paper (100 sheets)

### Printing Supplies
- Black Ink Cartridge (15 units)
- Color Ink Set (10 units)
- Toner Cartridge (8 units)

### Heat Press Materials
- Heat Transfer Vinyl (50 rolls)
- Sublimation Paper (200 sheets)
- Plain T-Shirts (100 pieces)
- Ceramic Mugs (50 pieces)
- Tumblers (30 pieces)

### Tarpaulin Materials
- Tarpaulin Material (100 meters)
- Eyelets (1000 pieces)
- Rope (50 meters)

## Troubleshooting

### Common Issues

1. **Application won't start**
   - Verify Java 17+ is installed: `java -version`
   - Check Maven installation: `mvn --version`
   - Run `setup.bat` for automatic diagnostics

2. **Data not saving**
   - Check file permissions in the application directory
   - Ensure sufficient disk space
   - Check console for error messages

3. **Performance issues**
   - Close unnecessary applications
   - Increase available memory for Java
   - Consider reducing the number of items displayed

### Error Messages

- **"Java is not installed"**: Install Java 17 or higher
- **"Maven is not installed"**: Install Apache Maven
- **"Failed to build"**: Check internet connection for dependency downloads
- **"Could not load data"**: Check if inventory_data.json is corrupted

## Technical Architecture

### Technologies Used
- **JavaFX**: User interface framework
- **Maven**: Build and dependency management
- **Jackson**: JSON data processing
- **SLF4J**: Logging framework

### Project Structure
```
inventorysystem/
├── pom.xml                          # Maven configuration
├── setup.bat                       # Windows setup script
├── src/main/java/com/nva/printing/inventory/
│   ├── InventoryApplication.java    # Main application class
│   ├── MainController.java          # Main UI controller
│   ├── ItemFormController.java      # Add/edit form controller
│   ├── InventoryItem.java           # Data model
│   ├── DataManager.java             # Data persistence
│   ├── ArchiveManager.java          # Archive management
│   ├── ArchiveItem.java             # Archive item model
│   ├── AuthManager.java             # Authentication manager
│   ├── LoginController.java         # Login screen controller
│   ├── TransactionLogger.java       # Transaction logging
│   ├── User.java                    # User model
│   └── roles/
│       ├── ManagerController.java             # Manager role 
│       ├── HeadProductionController.java      # Head Production role
│       ├── CashierController.java             # Cashier role 
│       ├── ArchiveViewController.java         # Archive view 
│       └── TransactionHistoryController.java  # Transaction history
│ 
└── src/main/resources/
    ├── main-view.fxml                  # Main window layout
    ├── item-form.fxml                  # Add/edit form layout
    ├── manager-view.fxml               # Manager dashboard layout
    ├── headproduction-view.fxml        # Head Production dashboard layout
    ├── cashier-view.fxml               # Cashier dashboard layout
    ├── archive-view.fxml               # Archive viewing layout
    ├── transaction-history-view.fxml   # Transaction history layout
    ├── login-view.fxml                 # Login screen layout
    └── styles.css                      # Application styles
```

### Design Patterns
- **Singleton**: DataManager for centralized data access
- **Observer**: JavaFX properties for automatic UI updates
- **MVC**: Separation of concerns with controllers and models

## Future Enhancements

Potential features for future versions:
- **User Management**: Multi-user support with different access levels
- **Supplier Management**: Track supplier information and purchase history
- **Backup & Restore**: Cloud-based backup solutions
- **Mobile App**: Companion mobile application
- **Purchase Orders**: Automated ordering when stock is low

## Support

For technical support or questions about the system:
1. Check this README for common solutions
2. Review the console output for error messages
3. Contact the development team with specific error details

## License

This project is developed as part of an academic requirement for NVA Printing Services. All rights reserved to the development team and the institution.

---

**Version**: 1.0.0  
**Last Updated**: August 31, 2025  
**Developed for**: NVA Printing Services, Pabayo-T. Chavez St., Cagayan de Oro City
