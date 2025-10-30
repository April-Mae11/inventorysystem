package com.nva.printing.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;


/**
 * Authentication Manager - Demonstrates OOP Concepts
 * 
 * SINGLETON PATTERN: Ensures only one instance manages authentication
 * ENCAPSULATION: Private constructor and controlled access to user data
 * ABSTRACTION: Hides authentication complexity behind simple method calls
 * 
 * This class manages user authentication and provides role-based access control
 * for the inventory management system.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Encapsulation: Private fields and controlled access through public methods
 * - Abstraction: Clean API hiding authentication complexity
 * - Singleton Pattern: Single point of authentication management
 * - Security: Secure password handling and session management
 * 
 * @author NVA Printing Services Development Team
 */
public class AuthManager {
    /**
     * Set the current user (for direct user switching)
     * ENCAPSULATION: Allows programmatic session switching
     * @param user The user to set as current
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            LOGGER.info("User switched: " + user.getUsername() + " (" + user.getRole() + ")");
        }
    }
    
    // SINGLETON PATTERN: Static instance for single point of access
    private static AuthManager instance;
    
    // ENCAPSULATION: Private constants for file management
    private static final String USERS_FILE = "users_data.json";
    
    // ENCAPSULATION: Private fields to protect authentication state
    private final ObservableList<User> users;
    private final ObjectMapper objectMapper;
    private User currentUser; // Current logged-in user
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(AuthManager.class.getName());
    
    /**
     * SINGLETON PATTERN: Private constructor prevents direct instantiation
     * ENCAPSULATION: Initializes all private fields with proper defaults
     * ABSTRACTION: Hides complex object mapper configuration
     */
    private AuthManager() {
        this.users = FXCollections.observableArrayList();
        this.objectMapper = new ObjectMapper();
        // Be tolerant when loading user JSON produced by older versions or manual edits
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.currentUser = null;
    }
    
    /**
     * SINGLETON PATTERN: Controlled access to single instance
     * ENCAPSULATION: Lazy initialization of singleton instance
     * @return The single AuthManager instance
     */
    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }
    
    /**
     * ENCAPSULATION: Controlled access to users collection
     * Returns observable list for UI binding while maintaining data integrity
     * @return Observable list of users for UI binding
     */
    public ObservableList<User> getUsers() {
        return users;
    }
    
    /**
     * Get the currently logged-in user
     * ENCAPSULATION: Provides controlled access to current user
     * @return Current user or null if not logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if a user is currently logged in
     * ABSTRACTION: Hides session state checking logic
     * @return true if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Authenticate user with username and password
     * ABSTRACTION: Hides authentication complexity
     * ENCAPSULATION: Manages session state securely
     * 
     * @param username Username to authenticate
     * @param password Password to validate
     * @return true if authentication successful
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty()) {
            return false;
        }

        String trimmedUsername = username.trim();
        String trimmedPassword = password.trim();

    // Find user by username 
    Optional<User> userOpt = users.stream()
        .filter(user -> user.getUsername() != null && user.getUsername().equals(trimmedUsername))
        .findFirst();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) {
                LOGGER.warning("Authentication failed: user is inactive: " + user.getUsername());
                return false;
            }
            boolean passOk = user.validatePassword(trimmedPassword);
            if (passOk) {
                currentUser = user;
                LOGGER.info("User authenticated: " + user.getUsername() + " (" + user.getRole() + ")");
                return true;
            } else {
                LOGGER.warning("Authentication failed: password mismatch for user: " + user.getUsername());
                return false;
            }
        }
        
        LOGGER.warning("Authentication failed: username not found: " + username);
        return false;
    }
    
    /**
     * Logout current user
     * ABSTRACTION: Hides session cleanup complexity
     * ENCAPSULATION: Securely clears session data
     */
    public void logout() {
        if (currentUser != null) {
            LOGGER.info("User logged out: " + currentUser.getUsername());
            currentUser = null;
        }
    }
    
    /**
     * Check if current user has manager privileges
     * ABSTRACTION: Hides role checking logic
     * @return true if current user is a manager
     */
    public boolean hasManagerAccess() {
        return currentUser != null && currentUser.isManager();
    }
    
    /**
     * Check if current user has head production privileges
     * ABSTRACTION: Hides role checking logic
     * @return true if current user is head production
     */
    public boolean hasHeadProductionAccess() {
        return currentUser != null && currentUser.isHeadProduction();
    }
    
    /**
     * Check if current user has cashier privileges
     * ABSTRACTION: Hides role checking logic
     * @return true if current user is cashier
     */
    public boolean hasCashierAccess() {
        return currentUser != null && currentUser.isCashier();
    }
    
    /**
     * Check if current user has specific role
     * ABSTRACTION: Hides role checking logic
     * @param role Role to check for
     * @return true if current user has the specified role
     */
    public boolean hasRole(User.UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }
    
    /**
     * Add a new user to the system
     * ABSTRACTION: High-level method hiding user creation complexity
     * ENCAPSULATION: Manages user data consistency
     * 
     * @param user The user to add
     * @return true if user was added successfully
     */
    public boolean addUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return false;
        }
        
        // Check for duplicate username
        boolean usernameExists = users.stream()
                .anyMatch(u -> u.getUsername().equals(user.getUsername()));
        
        if (usernameExists) {
            LOGGER.warning("Username already exists: " + user.getUsername());
            return false;
        }
        
        users.add(user);
        saveUsers();
        LOGGER.info("User added: " + user.getUsername() + " (" + user.getRole() + ")");
        return true;
    }
    
    /**
     * Update an existing user
     * ABSTRACTION: Simple update interface hiding complexity
     * ENCAPSULATION: Ensures data consistency and automatic saving
     * 
     * @param user The user to update
     * @return true if user was updated successfully
     */
    public boolean updateUser(User user) {
        if (user == null) {
            return false;
        }
        
        // Find and update the user
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(user.getUsername())) {
                users.set(i, user);
                saveUsers();
                LOGGER.info("User updated: " + user.getUsername());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Remove a user from the system
     * ABSTRACTION: Simple removal interface with automatic persistence
     * ENCAPSULATION: Maintains collection integrity
     * 
     * @param username Username of the user to remove
     * @return true if user was removed successfully
     */
    public boolean removeUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        boolean removed = users.removeIf(user -> user.getUsername().equals(username));
        if (removed) {
            saveUsers();
            LOGGER.info("User removed: " + username);
        }
        
        return removed;
    }
    
    /**
     * Delete a user from the system (alias for removeUser)
     * ABSTRACTION: Simple removal interface with automatic persistence
     * ENCAPSULATION: Maintains collection integrity
     * 
     * @param user The user to delete
     * @return true if user was deleted successfully
     */
    public boolean deleteUser(User user) {
        if (user == null) {
            return false;
        }
        
        boolean removed = users.remove(user);
        if (removed) {
            saveUsers();
            LOGGER.info("User deleted: " + user.getUsername());
        }
        
        return removed;
    }
    
    /**
     * Get all users in the system
     * ABSTRACTION: Simple access to user collection
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return new java.util.ArrayList<>(users);
    }
    
    /**
     * Find user by username
     * ABSTRACTION: Hides search logic complexity
     * @param username Username to search for
     * @return User if found, null otherwise
     */
    public User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
    
    // ABSTRACTION: File I/O operations hidden behind simple method calls
    
    /**
     * Load users from JSON file
     * ABSTRACTION: Hides complex JSON deserialization and error handling
     * ENCAPSULATION: Manages internal state during data loading
     */
    public void loadUsers() {
        File usersFile = new File(USERS_FILE);
        
        if (!usersFile.exists()) {
            LOGGER.info("Users file not found. Initializing with default users.");
            initializeDefaultUsers();
            return;
        }
        
        try {
            // ABSTRACTION: Hide JSON parsing complexity
            List<User> userList = objectMapper.readValue(usersFile, 
                    new TypeReference<List<User>>() {});
            
            // ENCAPSULATION: Safely update internal state
            users.clear();
            users.addAll(userList);
            
            LOGGER.info("Loaded " + userList.size() + " users from " + USERS_FILE);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading users from " + USERS_FILE + ": " + e.getMessage(), e);
            initializeDefaultUsers();
        }
    }
    
    /**
     * Save users to JSON file
     * ABSTRACTION: Hides JSON serialization complexity
     * ENCAPSULATION: Protects data through file operations
     */
    public void saveUsers() {
        try {
            // ABSTRACTION: Hide JSON serialization
            objectMapper.writeValue(new File(USERS_FILE), users);
            LOGGER.info("Users saved successfully to " + USERS_FILE);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving users to " + USERS_FILE + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * ENCAPSULATION: Private method to initialize default users
     * ABSTRACTION: Hides the complexity of creating default system users
     */
    private void initializeDefaultUsers() {
    LOGGER.info("Initializing with default users...");
        
        // Create manager account 
        User manager = new User("manager", "manager", User.UserRole.MANAGER, 
                "System Manager", "manager@nva-printing.com");
        users.add(manager);
        
        // Create head production account
        User headProduction = new User("headproduction", "headproduction", User.UserRole.HEAD_PRODUCTION, 
                "Head Production", "headproduction@nva-printing.com");
        users.add(headProduction);
        
        // Create cashier account
        User cashier = new User("cashier", "cashier", User.UserRole.CASHIER, 
                "Cashier", "cashier@nva-printing.com");
        users.add(cashier);
        
        saveUsers();
        LOGGER.info("Default users initialized successfully");
    }
}
