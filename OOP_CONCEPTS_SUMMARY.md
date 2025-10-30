# OOP Concepts Implementation Summary
## NVA Printing Services Inventory Management System

### Overview
This document outlines how the four fundamental Object-Oriented Programming (OOP) concepts are implemented throughout the NVA Printing Services Inventory Management System.

---

## 1. ENCAPSULATION

### Definition
Encapsulation is the bundling of data and methods that operate on that data within a single unit (class), while restricting direct access to some of the object's components.

### Implementation Examples:

#### InventoryItem.java
- **Private Fields**: All data fields (id, name, category, etc.) are declared as private JavaFX properties
- **Controlled Access**: Public getter/setter methods provide controlled access to private fields
- **Data Integrity**: Automatic timestamp updates when quantity changes
- **Business Logic Protection**: Private `updateLowStockStatus()` method maintains data consistency

```java
// ENCAPSULATION: Private fields with controlled access
private final IntegerProperty quantity;
private final ObjectProperty<LocalDateTime> lastUpdated;

// ENCAPSULATION: Public method with automatic business logic
public void setQuantity(int quantity) { 
    this.quantity.set(quantity);
    // ABSTRACTION: Hide automatic timestamp update from caller
    this.lastUpdated.set(LocalDateTime.now());
}
```

#### DataManager.java
- **Singleton Pattern**: Private constructor prevents direct instantiation
- **Private Fields**: Data collection and configuration are encapsulated
- **Controlled Data Access**: Public methods provide safe access to inventory data

#### Controllers
- **FXML Injection**: Private fields for UI components with controlled access
- **Internal State Management**: Private fields for filters, data managers, and UI state
- **Stock In/Stock Out Functions**: Private methods for stock management in controllers
- **Archive Viewing and Usage Tracking**: Private fields and methods for archive data and UI state
- **Role-Based UI Updates**: Private methods for updating UI based on user role

---

## 2. INHERITANCE

### Definition
Inheritance allows classes to inherit properties and methods from other classes, promoting code reuse and establishing relationships between classes.

### Implementation Examples:

#### JavaFX Framework Integration
- **InventoryApplication extends Application**: Inherits JavaFX application lifecycle management
- **Controllers implement Initializable**: Inherit framework initialization capabilities

```java
/**
 * INHERITANCE: Extends JavaFX Application class to inherit framework capabilities
 */
public class InventoryApplication extends Application {
    /**
     * POLYMORPHISM: Override Application.start() method
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Custom application startup logic
    }
}
```

#### Interface Implementation
- **Initializable Interface**: Controllers inherit standardized initialization behavior
- **Callback Interfaces**: Cell factories inherit framework rendering capabilities
- **Stock In/Stock Out and Archive Features**: Controllers extend/implement methods for inventory and archive management

---

## 3. POLYMORPHISM

### Definition
Polymorphism allows objects of different types to be treated as objects of a common base type, enabling methods to behave differently based on the object type.

### Implementation Examples:

#### Method Overriding
- **Object Methods**: `equals()`, `hashCode()`, and `toString()` methods are overridden in InventoryItem
- **Framework Methods**: `initialize()` and `start()` methods are overridden for custom behavior

```java
/**
 * POLYMORPHISM: Override Object.equals() for proper object comparison
 */
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    InventoryItem that = (InventoryItem) obj;
    return getId() == that.getId();
}
```

#### Interface Implementation
- **Anonymous Classes**: Cell factories use anonymous classes implementing Callback interface
- **Lambda Expressions**: Event handlers and listeners use lambda expressions for polymorphic behavior
- **Role-Based UI Updates**: Event handlers and UI callbacks adapt to user role

```java
// POLYMORPHISM: Lambda expression implementing ChangeListener interface
priceField.textProperty().addListener((observable, oldValue, newValue) -> {
    if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
        priceField.setText(oldValue);
    }
});
```

#### Enum Usage
- **Mode Enum**: ItemFormController uses enum for type-safe operation modes
- **Stock In/Stock Out Operations**: Use of enums for transaction types

---

## 4. ABSTRACTION

### Definition
Abstraction hides complex implementation details while exposing only the necessary functionality through a simplified interface.

### Implementation Examples:

#### High-Level Data Operations
- **DataManager Methods**: Complex file I/O and JSON operations are hidden behind simple method calls
- **Search and Filter Operations**: Complex stream processing is abstracted into simple method calls
- **Stock In/Stock Out and Archive Operations**: Complex validation and transaction logic hidden behind controller methods

```java
/**
 * ABSTRACTION: High-level method hiding implementation complexity
 */
public List<InventoryItem> searchItemsByName(String searchText) {
    String lowerSearchText = searchText.toLowerCase();
    return inventoryItems.stream()
            .filter(item -> item.getName().toLowerCase().contains(lowerSearchText) ||
                           item.getDescription().toLowerCase().contains(lowerSearchText))
            .toList();
}
```

#### UI Component Management
- **Table Setup**: Complex table configuration is hidden behind simple method calls
- **Form Validation**: Validation logic is abstracted into reusable methods
- **Event Handling**: Complex UI interactions are simplified through event handler methods
- **Role-Based UI Updates**: UI logic for different roles is separated into dedicated controllers

#### Business Logic Abstraction
- **Automatic Calculations**: Low stock status is automatically calculated and updated
- **Data Persistence**: JSON serialization/deserialization is hidden from business logic
- **Statistics Computation**: Complex statistical calculations are abstracted into simple method calls
- **Transaction Logging**: Logging logic hidden behind a simple log() method

```java
/**
 * ABSTRACTION: Business logic methods that hide implementation complexity
 */
public double getTotalValue() {
    return quantity.get() * unitPrice.get();
}
```

---

## Advanced OOP Patterns Used

### 1. Singleton Pattern (DataManager, TransactionLogger)
Ensures only one instance manages inventory data and transaction logs throughout the application.

### 2. Composition
- Controllers use DataManager and ArchiveManager for data access
- Application uses DataManager for lifecycle management
- InventoryItem composes JavaFX properties for data binding

### 3. Observer Pattern
- JavaFX properties automatically notify UI components of changes
- Table views automatically update when data changes
- Role-based UI updates trigger automatic refreshes

### 4. Factory Pattern
- SpinnerValueFactory creates appropriate spinner configurations
- PropertyValueFactory creates cell value factories for table columns
- Transaction factory for creating transaction records

---

## Benefits Achieved Through OOP Implementation

### 1. Maintainability
- **Encapsulation**: Changes to internal implementations don't affect external code
- **Abstraction**: Complex operations are hidden behind simple interfaces

### 2. Extensibility
- **Inheritance**: New features can extend existing functionality
- **Polymorphism**: New item types or behaviors can be added without modifying existing code

### 3. Reusability
- **Abstraction**: Generic methods can be reused across different contexts
- **Encapsulation**: Self-contained classes can be reused in different projects

### 4. Data Integrity
- **Encapsulation**: Private fields with controlled access prevent invalid states
- **Abstraction**: Business rules are enforced automatically

### 5. Code Organization
- **Single Responsibility**: Each class has a clear, focused purpose
- **Separation of Concerns**: UI, business logic, and data persistence are clearly separated

---

## Real-World Application

This inventory management system demonstrates how OOP principles create robust, maintainable software for business operations:

1. **Data Safety**: Encapsulation prevents data corruption
2. **User Experience**: Abstraction provides intuitive interfaces
3. **Business Logic**: Polymorphism enables flexible business rules
4. **System Growth**: Inheritance allows for future enhancements

The system successfully replaces manual paper-based inventory tracking with a digital solution that embodies professional software development practices through proper OOP implementation.
