package com.nva.printing.inventory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main Application Class - Demonstrates OOP Concepts
 * 
 * INHERITANCE: Extends JavaFX Application class to inherit framework capabilities
 * ENCAPSULATION: Private static field with controlled access through getter
 * ABSTRACTION: Hides JavaFX application lifecycle complexity
 * POLYMORPHISM: Overrides Application.start() method for custom behavior
 * 
 * This class serves as the entry point for the NVA Printing Services
 * Inventory Management System and demonstrates how OOP principles
 * integrate with framework-based applications.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Inheritance: Extends JavaFX Application for GUI framework integration
 * - Encapsulation: Private fields with controlled access
 * - Abstraction: Hides application startup complexity
 * - Polymorphism: Override framework methods for custom behavior
 * - Composition: Uses DataManager for application-level data management
 * 
 * @author Allen Kirk A. Cailing, April Mae O. Cape, Loreen Angel Culanculan, 
 *         Vanessa V. Embornas, Bernadine E. Suson, Gerome L. Velasco
 */

 
public class InventoryApplication extends Application {
    
    // ENCAPSULATION: Private static field for controlled access to primary stage
    private static Stage primaryStage;
    
    /**
     * POLYMORPHISM: Override Application.start() method
     * ABSTRACTION: Hides complex JavaFX application initialization
     * This method is called by the JavaFX framework during application startup
     * 
     * @param stage Primary stage provided by JavaFX framework
     * @throws IOException If FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        // ENCAPSULATION: Store reference to primary stage for controlled access
        primaryStage = stage;
        
        // ABSTRACTION: Hide login screen loading complexity
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login-view.fxml"));
        Parent root = fxmlLoader.load();
        
        // Get the login controller and set the stage reference
        LoginController loginController = fxmlLoader.getController();
        loginController.setLoginStage(stage);
        
        // ABSTRACTION: Hide scene configuration complexity
        Scene scene = new Scene(root, 500, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // ABSTRACTION: Hide stage configuration complexity
        stage.setTitle("NVA Printing Services - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        
        // ENCAPSULATION: Safe handling of optional icon loading
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            // Icon file not found, continue without it
            Logger.getLogger(InventoryApplication.class.getName()).info("Icon file not found, using default window icon");
        }
        
        // Display the login screen
        String repro = System.getProperty("repro.headproduction");
        if ("true".equalsIgnoreCase(repro)) {
            // Directly load headproduction view for repro
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/headproduction-view.fxml"));
                Parent reproRoot = loader.load();
                Scene reproScene = new Scene(reproRoot);
                reproScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                stage.setTitle("NVA Printing Services - Head Production (Repro)");
                stage.setScene(reproScene);
                stage.setResizable(true);
                stage.setMaximized(true);
                stage.show();
                return;
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(InventoryApplication.class.getName()).log(java.util.logging.Level.SEVERE, "Failed to load headproduction for repro: " + e.getMessage(), e);
                // Fall back to login UI on error
            }
        }

        stage.show();
        
        Logger.getLogger(InventoryApplication.class.getName()).info("NVA Printing Services Login Screen Started");

        // Ensure data is saved when the UI window is closed (extra safeguard to shutdown hook)
        stage.setOnCloseRequest(event -> {
            DataManager.getInstance().saveData();
            AuthManager.getInstance().saveUsers();
            ArchiveManager.getInstance().saveArchiveData();
            // Ensure transaction logs are flushed to disk/DB on window close
            TransactionLogger.getInstance().shutdown();
            Logger.getLogger(InventoryApplication.class.getName()).info("Data, users, and archive saved on window close");
        });
    }
    
    /**
     * ENCAPSULATION: Controlled access to primary stage
     * Provides read-only access to the primary stage for other components
     * 
     * @return The primary application stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Application entry point
     * ABSTRACTION: Hides application lifecycle management
     * COMPOSITION: Integrates with DataManager and AuthManager for data persistence and authentication
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // COMPOSITION: Initialize authentication system on startup using AuthManager
        AuthManager.getInstance().loadUsers();
        
        // COMPOSITION: Initialize data storage on startup using DataManager
        DataManager.getInstance().loadData();
        
        // COMPOSITION: Initialize archive system on startup using ArchiveManager
        ArchiveManager.getInstance().loadArchiveData();
        
        // ABSTRACTION: Hide shutdown hook complexity
        // Add shutdown hook to save data automatically on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DataManager.getInstance().saveData();
            AuthManager.getInstance().saveUsers();
            ArchiveManager.getInstance().saveArchiveData();
            // Ensure transaction logs are flushed when JVM shuts down
            TransactionLogger.getInstance().shutdown();
            Logger.getLogger(InventoryApplication.class.getName()).info("Data, users, and archive saved successfully on application exit");
        }));
        
        // If repro property set, launch JavaFX and directly show Head Production for debugging
        if ("true".equalsIgnoreCase(System.getProperty("repro.headproduction"))) {
            // Initialize users and data first
            AuthManager.getInstance().loadUsers();
            DataManager.getInstance().loadData();
            ArchiveManager.getInstance().loadArchiveData();
            // Launch JavaFX and then load headproduction view on FX thread
            launch(args);
            return;
        }

        // INHERITANCE: Call inherited launch method to start JavaFX application
        launch(args);
    }
}
