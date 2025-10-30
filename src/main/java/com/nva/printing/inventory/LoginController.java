package com.nva.printing.inventory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Login Controller - Demonstrates OOP Concepts
 * 
 * INHERITANCE: Implements Initializable interface from JavaFX framework
 * ENCAPSULATION: Private fields with controlled access and validation
 * ABSTRACTION: Hides authentication complexity behind simple event handlers
 * POLYMORPHISM: Method overriding and interface implementations
 * 
 * This controller manages the login interface and demonstrates how OOP
 * principles create secure and maintainable authentication systems.
 * 
 * OOP PRINCIPLES IMPLEMENTED:
 * - Inheritance: Extends JavaFX capabilities through interface implementation
 * - Encapsulation: Private validation and authentication methods
 * - Abstraction: Clean interface hiding authentication complexity
 * - Polymorphism: Method overriding and interface implementations
 * 
 * @author NVA Printing Services Development Team
 */
public class LoginController implements Initializable {
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    // Toggle password visibility (show/hide password)
    @FXML
    private void handleTogglePasswordVisibility() {
        // If you have passwordVisibleField and togglePasswordButton, implement toggle logic here
        // Example:
        if (passwordVisibleField != null && passwordField != null && togglePasswordButton != null) {
            boolean show = !passwordVisibleField.isVisible();
            if (show) {
                passwordVisibleField.setText(passwordField.getText());
                passwordVisibleField.setVisible(true);
                passwordVisibleField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                togglePasswordButton.setText("Hide");
            } else {
                passwordField.setText(passwordVisibleField.getText());
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordVisibleField.setVisible(false);
                passwordVisibleField.setManaged(false);
                togglePasswordButton.setText("Show");
            }
        }
    }

    // Forgot password handler for FXML
    @FXML
    private void handleForgotPassword() {
        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setTitle("Forgot Password");
        userDialog.setHeaderText("Enter your username to reset password");
        userDialog.setContentText("Username:");
        userDialog.showAndWait().ifPresent(username -> {
            // Simulate sending code
            String code = String.valueOf((int)(Math.random()*900000)+100000);
            Alert codeAlert = new Alert(Alert.AlertType.INFORMATION);
            codeAlert.setTitle("Authentication Code");
            codeAlert.setHeaderText("A code has been sent to your email (simulated)");
            codeAlert.setContentText("Your code: " + code);
            codeAlert.showAndWait();
            TextInputDialog codeDialog = new TextInputDialog();
            codeDialog.setTitle("Enter Code");
            codeDialog.setHeaderText("Enter the code sent to your email");
            codeDialog.setContentText("Code:");
            codeDialog.showAndWait().ifPresent(enteredCode -> {
                if (enteredCode.equals(code)) {
                    showChangePasswordDialog(username);
                } else {
                    Alert fail = new Alert(Alert.AlertType.ERROR);
                    fail.setTitle("Invalid Code");
                    fail.setHeaderText(null);
                    fail.setContentText("Incorrect code. Please try again.");
                    fail.showAndWait();
                }
            });
        });
    }
    private void setupValidation() {
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }
    
    // ENCAPSULATION: FXML-injected UI components with controlled access
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    
    // ENCAPSULATION: Private fields for internal state management
    private AuthManager authManager; // COMPOSITION: Uses AuthManager for authentication
    private Stage loginStage;
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    
    /**
     * POLYMORPHISM: Override Initializable.initialize()
     * ABSTRACTION: Hides complex initialization behind simple method call
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // COMPOSITION: Get AuthManager singleton instance
        authManager = AuthManager.getInstance();
        
        // ABSTRACTION: Break initialization into manageable parts
    setupForm();
    setupValidation();
        // Note: setupEventHandlers() will be called after setLoginStage()
        // Set focus to username field
        Platform.runLater(() -> usernameField.requestFocus());
    }

    // Forgot Password: prompt for new password after code verification
    private void showChangePasswordDialog(String username) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter your new password");

        Label newPassLabel = new Label("New Password:");
        PasswordField newPassField = new PasswordField();
        Label confirmPassLabel = new Label("Confirm Password:");
        PasswordField confirmPassField = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(newPassLabel, 0, 0);
        grid.add(newPassField, 1, 0);
        grid.add(confirmPassLabel, 0, 1);
        grid.add(confirmPassField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType okButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return newPassField.getText();
            }
            return null;
        });

        // Validate passwords match before closing
        dialog.getDialogPane().lookupButton(okButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!newPassField.getText().equals(confirmPassField.getText()) || newPassField.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Password Error");
                alert.setHeaderText(null);
                alert.setContentText("Passwords do not match or are empty.");
                alert.showAndWait();
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(newPassword -> {
            if (newPassword != null && !newPassword.isEmpty()) {
                User user = authManager.findUserByUsername(username);
                if (user != null) {
                    user.setPassword(newPassword);
                    authManager.saveUsers();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Password Changed");
                    alert.setHeaderText(null);
                    alert.setContentText("Your password has been changed successfully.");
                    alert.showAndWait();
                }
            }
        });
    }
    
    /**
     * Set the login stage reference
     * ENCAPSULATION: Controlled access to stage management
     * @param stage The login stage
     */
    public void setLoginStage(Stage stage) {
        this.loginStage = stage;
        // Now that we have the stage, we can set up event handlers
        setupEventHandlers();
    }
    
    /**
     * ENCAPSULATION: Private method for form component configuration
     * ABSTRACTION: Hides form setup complexity from public interface
     */
    private void setupForm() {
        // Set application title and subtitle
        titleLabel.setText("NVA Printing Services");
        subtitleLabel.setText("Inventory Management System");
        
        // Clear status label
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: #666666;");
        
        // Set button text
        loginButton.setText("Login");
        cancelButton.setText("Cancel");
    }
    
    /**
     * ENCAPSULATION: Private method for validation setup
     * ABSTRACTION: Hides real-time validation complexity
     */
    // (removed duplicate setupValidation method)
    
    /**
     * ENCAPSULATION: Private validation method
     * ABSTRACTION: Hides validation logic complexity
     */
    private void validateForm() {
        boolean isValid = !usernameField.getText().trim().isEmpty() &&
                         !passwordField.getText().trim().isEmpty();
        
        loginButton.setDisable(!isValid);
    }
    
    /**
     * ENCAPSULATION: Private method for event handler setup
     * ABSTRACTION: Hides event handling complexity
     */
    private void setupEventHandlers() {
        // Handle Enter key press for login
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        
        // Handle Escape key press for cancel - only if loginStage is available
        if (loginStage != null && loginStage.getScene() != null) {
            loginStage.getScene().setOnKeyPressed(e -> {
                if (e.getCode().toString().equals("ESCAPE")) {
                    handleCancel();
                }
            });
        }
    }
    
    /**
     * Handle login button click
     * ABSTRACTION: Hides authentication complexity behind simple method call
     * ENCAPSULATION: Manages authentication state securely
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password;
        // Use the visible password field value
        if (passwordVisibleField != null && passwordVisibleField.isVisible()) {
            password = passwordVisibleField.getText();
        } else {
            password = passwordField.getText();
        }
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter both username and password.", false);
            return;
        }
        
        // Debug: print attempted username and loaded users for troubleshooting
        try {
            String available = authManager.getUsers().stream()
                    .map(u -> u.getUsername())
                    .collect(Collectors.joining(","));
            LOGGER.info("Login attempt for username='" + username + "'. Loaded users count=" + authManager.getUsers().size() + " -> [" + available + "]");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Login debug: could not list users: " + e.getMessage(), e);
        }

        // If users weren't loaded for some reason, try loading now
        if (authManager.getUsers().isEmpty()) {
            LOGGER.info("User list empty at login time — loading users now.");
            authManager.loadUsers();
        }

        // Disable login button to prevent multiple attempts
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        
        // ABSTRACTION: Hide authentication complexity
        boolean authenticated = authManager.authenticate(username, password);
        
        if (authenticated) {
            showStatus("Login successful! Opening application...", true);
            // Use PauseTransition for non-blocking delay
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
            pause.setOnFinished(event -> openMainApplication());
            pause.play();
        } else {
            showStatus("Invalid username or password. Please try again.", false);
            loginButton.setDisable(false);
            loginButton.setText("Login");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
    
    /**
     * Handle cancel button click
     * ABSTRACTION: Hides application exit complexity
     */
    @FXML
    private void handleCancel() {
        LOGGER.info("Login cancelled by user");
        Platform.exit();
        System.exit(0);
    }
    
    /**
     * Open the main application after successful login
     * ABSTRACTION: Hides application startup complexity
     * ENCAPSULATION: Manages stage transitions securely
     */
    private void openMainApplication() {
        try {
            User currentUser = authManager.getCurrentUser();
            String fxmlFile;
            String title;
            
            // Route to appropriate view based on user role
            if (currentUser.isManager()) {
                fxmlFile = "/manager-view.fxml";
                title = "NVA Printing Services - Manager Dashboard";
            } else if (currentUser.isHeadProduction()) {
                fxmlFile = "/headproduction-view.fxml";
                title = "NVA Printing Services - Head Production Dashboard";
            } else if (currentUser.isCashier()) {
                fxmlFile = "/cashier-view.fxml";
                title = "NVA Printing Services - Cashier Dashboard";
            } else {
                // Fallback to main view
                fxmlFile = "/main-view.fxml";
                title = "NVA Printing Services - Inventory Management System";
            }
            
            // Load appropriate application view
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            // Note: Removed verbose FXML debug dump for production
            Parent root = loader.load();
            
            // Create main application stage (open maximized)
            Stage mainStage = new Stage();
            mainStage.setTitle(title);
            mainStage.setScene(new Scene(root));
            mainStage.setResizable(true);
            // Apply styles
            mainStage.getScene().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            // Start maximized so user gets full workspace immediately
            mainStage.setMaximized(true);
            // Show main application and close login window
            mainStage.show();
            if (loginStage != null) {
                loginStage.close();
            }
            
            LOGGER.info("Main application opened for user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
            
        } catch (Exception e) {
            // Log full stacktrace for debugging FXML loading errors
            LOGGER.log(Level.SEVERE, "Error opening main application: " + e.toString(), e);

            // Show a detailed error dialog with expandable stack trace so the user can copy it
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error opening application");
            alert.setHeaderText("Failed to open the selected dashboard view");
            alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage());

            // Create expandable Exception stack trace area
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(new Label("The exception stacktrace was:"), 0, 0);
            expContent.add(textArea, 0, 1);

            // Set expandable Exception into the dialog pane.
            alert.getDialogPane().setExpandableContent(expContent);
            // showAndWait() can throw IllegalStateException if called during
            // animation or layout processing. Defer to the FX thread after
            // current pulse using Platform.runLater to be safe.
            Platform.runLater(() -> alert.showAndWait());

            showStatus("Error opening application: " + e.getClass().getSimpleName() + ": " + e.getMessage(), false);
            loginButton.setDisable(false);
            loginButton.setText("Login");
        }
    }
    
    /**
     * Show status message to user
     * ENCAPSULATION: Private method for status display management
     * ABSTRACTION: Hides status styling complexity
     * 
     * @param message Status message to display
     * @param isSuccess Whether this is a success or error message
     */
    private void showStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);
        if (isSuccess) {
            statusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
        }
    }
    

}
