### 🆕 Recent Function Additions & OOP Concepts

The following recently added functions and features further demonstrate OOP principles:

- **Stock In/Stock Out Functions** (MainController, CashierController):
  - Encapsulation: Private methods for stock management, only accessible within controllers.
  - Abstraction: Hide complex validation and transaction logic behind simple method calls.
  - Polymorphism: Use of method overriding and event handler callbacks.

- **Archive Viewing and Usage Tracking** (CashierController):
  - Encapsulation: Private fields and methods for archive data and UI state.
  - Abstraction: Archive management and statistics are hidden behind controller methods.
  - Composition: Uses ArchiveManager for data access.

- **Transaction Logging** (TransactionLogger):
  - Singleton Pattern: Ensures only one logger instance manages all transactions.
  - Encapsulation: Private fields for transaction records.
  - Abstraction: Logging logic hidden behind a simple log() method.

- **Role-Based UI Updates** (All Controllers):
  - Polymorphism: Event handlers and UI callbacks adapt to user role.
  - Abstraction: UI logic for different roles is separated into dedicated controllers.

These additions continue to follow the project's OOP architecture, ensuring maintainability, extensibility, and clarity.
# NVA Printing Services - Inventory Management System
## Implementation Summary

### ✅ Completed Features

#### 1. **Login System with Authentication**
- **Login Screen**: Professional login interface with green diamond theme
- **Authentication Manager**: Singleton pattern for secure user management
- **User Model**: Complete user data structure with role-based access
- **Default Accounts**:
  - Manager: `username: manager`, `password: manager`
  - Head Production: `username: headproduction`, `password: headproduction`

#### 2. **Role-Based Access Control**
- **Manager Role**: Full system access including user management
- **Head Production Role**: Production-focused access (no delete functionality)
- **Role-Specific Controllers**: Separate controllers for each role
- **Role-Specific Views**: Customized interfaces for each user type

#### 3. **Stack Level System**
- **LOW**: Quantity ≤ minimum stock level
- **NORMAL**: Quantity ≤ 2 × minimum stock level  
- **FULL**: Quantity > 2 × minimum stock level
- **Stack Level Filter**: Filter inventory by stack levels
- **Visual Indicators**: Color-coded stack level display

#### 4. **Alphabetical Product Sorting**
- **Default Sorting**: Products sorted alphabetically by name (A-Z)
- **Sortable Columns**: All columns support sorting
- **Persistent Sorting**: Maintains sort order during operations

#### 5. **Green Diamond Professional Theme**
- **Color Scheme**: Professional green (#2e7d2e) with diamond-like precision
- **No Glow Effects**: Clean, professional appearance without distracting effects
- **Consistent Styling**: All UI components follow the theme
- **Professional Borders**: Subtle borders and shadows for depth

#### 6. **Enhanced User Interface**
- **Role-Specific Dashboards**: Customized interfaces for Manager and Head Production
- **User Information Display**: Shows current user and role in status bar
- **Enhanced Toolbar**: Role-appropriate buttons and controls
- **Professional Menu System**: Organized menu structure with role-based access

#### 7. **Stack Level Management**
- View stack levels in inventory table
- Filter by stack levels (OUT OF STOCK, LOW, NORMAL, FULL)
- Visual indicators for stock status
- Automatic stack level calculation
- When quantity hits 0, status is set to **Out of Stock** and color is **red (#c62828)**

### 🏗️ System Architecture

#### **OOP Principles Implementation**
- **Encapsulation**: Private fields with controlled access through getters/setters
- **Inheritance**: JavaFX framework integration through interface implementation
- **Polymorphism**: Method overriding and callback interfaces
- **Abstraction**: Complex operations hidden behind simple method calls

#### **Design Patterns**
- **Singleton Pattern**: AuthManager and DataManager for centralized access
- **MVC Pattern**: Clear separation of concerns with controllers and models
- **Observer Pattern**: JavaFX properties for automatic UI updates

#### **File Structure**
```
src/main/java/com/nva/printing/inventory/
├── InventoryApplication.java          # Main application entry point
├── User.java                         # User model with role enum
├── AuthManager.java                  # Authentication management
├── DataManager.java                  # Data persistence
├── InventoryItem.java                # Inventory item model
├── ItemFormController.java           # Add/edit form controller
├── LoginController.java              # Login screen controller
├── MainController.java               # Original main controller
└── roles/
    ├── ManagerController.java        # Manager role controller
    └── HeadProductionController.java # Head Production role controller

src/main/resources/
├── login-view.fxml                   # Login screen layout
├── main-view.fxml                    # Main application layout
├── manager-view.fxml                 # Manager dashboard layout
├── headproduction-view.fxml          # Head Production dashboard layout
├── item-form.fxml                    # Add/edit form layout
└── styles.css                        # Green diamond theme styles
```

### 🔐 Security Features

#### **Authentication**
- Secure password validation
- Session management
- Automatic logout functionality
- Role-based access control

#### **Data Protection**
- JSON-based data persistence
- Automatic backup creation
- Data validation and integrity checks
- Secure file operations

### 📊 Enhanced Features

#### **Inventory Management**
- **Advanced Filtering**: Search by name, category, stack level, and stock status
- **Real-time Statistics**: Live updates of inventory metrics
- **Batch Operations**: Multiple item selection and operations
- **Data Validation**: Comprehensive input validation and error handling

#### **User Experience**
- **Responsive Design**: Adaptive layouts for different screen sizes
- **Keyboard Shortcuts**: Enter key navigation and Escape key handling
- **Status Feedback**: Clear success/error messages
- **Professional UI**: Clean, modern interface design

### 🚀 How to Use

#### **Starting the Application**
1. Run `setup.bat` or use Maven: `mvn javafx:run`
2. Login with default credentials:
   - Manager: `manager` / `manager`
   - Head Production: `headproduction` / `headproduction`
   - Cashier: `cashier` / `cashier`


#### **Manager Features**
- Full inventory management (add, edit, delete)
- User management (create, edit, and assign roles to users)
- Export functionality (generate and download inventory reports)
- Complete system access
- Stock In/Stock Out operations (track additions and removals)
- Transaction logging for all inventory changes
- Archive viewing and usage tracking (view all item usage history)
- End of Day processing (resets Stock In and Stock Out totals to zero for the next business day)
- Undo End of Day (reopen previous day for corrections or adjustments)
- Role-based UI updates for Manager

#### **Head Production Features**
- Inventory viewing and editing
- Add new items
- Production-focused interface
- Limited access for security
- Stock In/Stock Out operations (track additions and removals)
- Transaction logging for production-related inventory changes
- Archive viewing and usage tracking (view production item usage history)
- Role-based UI updates for Head Production
- View stack levels in inventory table
- Filter by stack levels (OUT OF STOCK, LOW, NORMAL, FULL)
- Visual indicators for stock status
- Automatic stack level calculation
- When quantity hits 0, status is set to **Out of Stock** and color is **red (#c62828)**

#### **Cashier Features**
- Inventory usage tracking
- Archive viewing
- Stock In/Stock Out operations
- Transaction logging
- When quantity hits 0, status is set to **Out of Stock** and color is **red (#c62828)**

#### **Switching Users (Manager, Cashier, Head Production)**
- To switch between user roles, use the logout button or menu option in the application.
- After logging out, you will be returned to the login screen.
- Enter the credentials for the desired role:
  - Manager: `manager` / `manager`
  - Head Production: `headproduction` / `headproduction`
  - Cashier: `cashier` / `cashier`
- The interface and available features will update automatically based on the selected role.

### 📈 Technical Improvements

#### **Performance**
- Efficient data filtering and sorting
- Lazy loading of UI components
- Optimized table rendering
- Memory-efficient data structures

#### **Maintainability**
- Clean code architecture
- Comprehensive documentation
- Modular design
- Easy to extend and modify

#### **Reliability**
- Error handling and validation
- Data backup and recovery
- Graceful failure handling
- Consistent state management

### 🎯 Future Enhancements

#### **Planned Features**
- Advanced reporting
- Multi-language support

#### **Technical Improvements**
- Database integration
- Cloud synchronization
- Mobile application
- API development
- Advanced analytics

---

**Version**: 2.0.0  
**Last Updated**: September 4, 2025  
**Status**: ✅ Complete and Ready for Use

The NVA Printing Services Inventory Management System now features a complete role-based authentication system, professional green diamond theme, stack level management, and alphabetical sorting - all implemented with proper OOP principles and design patterns.
