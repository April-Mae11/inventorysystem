package com.nva.printing.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User Model - Demonstrates OOP Concepts
 * 
 * ENCAPSULATION: Private fields with controlled access through getters/setters
 * ABSTRACTION: Hides user data management complexity
 * 
 * This class represents a user in the system with role-based access control.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Encapsulation: Private fields with controlled access
 * - Abstraction: Clean interface for user management
 * - Data Integrity: Role validation and secure password handling
 * 
 * @author NVA Printing Services Development Team
 */
public class User {
    
    // ENCAPSULATION: Private fields to protect user data
    private String username;
    private String password;
    private UserRole role;
    private String fullName;
    private String email;
    private boolean isActive;
    
    /**
     * User Role Enum - Demonstrates OOP Concepts
     * 
     * ENCAPSULATION: Type-safe role representation
     * ABSTRACTION: Hides role complexity behind simple enum values
     */
    public enum UserRole {
        MANAGER("Manager", "Full system access"),
        HEAD_PRODUCTION("Head Production", "Production management access"),
        CASHIER("Cashier", "Cashier operations and inventory usage tracking");
        
        private final String displayName;
        private final String description;
        
        UserRole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Default constructor for Jackson deserialization
     */
    public User() {
        this.isActive = true;
    }
    
    /**
     * Parameterized constructor for creating new users
     * 
     * @param username Unique username
     * @param password User password
     * @param role User role
     * @param fullName User's full name
     * @param email User's email address
     */
    public User(String username, String password, UserRole role, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.isActive = true;
    }
    
    // ENCAPSULATION: Controlled access to user data
    
    @JsonProperty("username")
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    @JsonProperty("password")
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    @JsonProperty("role")
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    @JsonProperty("fullName")
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    @JsonProperty("email")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    @JsonProperty("isActive")
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    /**
     * Check if user has manager privileges
     * ABSTRACTION: Hides role checking logic
     * @return true if user is a manager
     */
    public boolean isManager() {
        return role == UserRole.MANAGER;
    }
    
    /**
     * Check if user has head production privileges
     * ABSTRACTION: Hides role checking logic
     * @return true if user is head production
     */
    public boolean isHeadProduction() {
        return role == UserRole.HEAD_PRODUCTION;
    }
    
    /**
     * Check if user has cashier privileges
     * ABSTRACTION: Hides role checking logic
     * @return true if user is cashier
     */
    public boolean isCashier() {
        return role == UserRole.CASHIER;
    }
    
    /**
     * Validate user credentials
     * ABSTRACTION: Hides authentication logic
     * @param inputPassword Password to validate
     * @return true if password matches
     */
    public boolean validatePassword(String inputPassword) {
        return this.password != null && this.password.equals(inputPassword);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username != null ? username.equals(user.username) : user.username == null;
    }
    
    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return String.format("User{username='%s', role=%s, fullName='%s', isActive=%s}",
                username, role, fullName, isActive);
    }
}
