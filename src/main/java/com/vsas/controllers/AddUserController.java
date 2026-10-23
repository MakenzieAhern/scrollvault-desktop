package com.vsas.controllers;

import com.vsas.App;
import com.vsas.models.User;
import com.vsas.security.SessionManager;
import com.vsas.services.AuthService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.nio.file.Path;

public class AddUserController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label messageLabel;
    @FXML private Label dateTimeLabel;

    private final AuthService authService = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));

    @FXML
    public void initialize() {
        if (!requireAdmin()) return;

        // Setup date/time label
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        // Setup role combo box
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("GENERAL", "ADMIN");
            roleComboBox.setValue("GENERAL");
        }
    }

    @FXML
    public void onAddUser(ActionEvent e) {
        if (!requireAdmin()) return;
        // Clear previous messages
        if (messageLabel != null) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }

        // Get values
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleComboBox.getValue();

        // Validate required fields
        if (username.isEmpty()) {
            showMessage("Username is required.", true);
            return;
        }

        if (password.isEmpty()) {
            showMessage("Password is required.", true);
            return;
        }

        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters.", true);
            return;
        }

        if (!password.equals(confirm)) {
            showMessage("Passwords do not match.", true);
            return;
        }

        if (firstName.isEmpty()) {
            showMessage("First name is required.", true);
            return;
        }

        if (lastName.isEmpty()) {
            showMessage("Last name is required.", true);
            return;
        }

        if (phone.isEmpty()) {
            showMessage("Phone number is required.", true);
            return;
        }

        if (role == null || role.isEmpty()) {
            showMessage("Role is required.", true);
            return;
        }

        // Combine first and last name
        String fullName = firstName + " " + lastName;

        // Try to create the user
        try {
            User newUser = authService.createUserByAdmin(
                username,
                fullName,
                email.isEmpty() ? null : email,
                phone,
                password,
                role
            );

            showMessage("User " + newUser.getUsername() + " created successfully!", false);

            // Clear form fields
            usernameField.clear();
            passwordField.clear();
            confirmField.clear();
            firstNameField.clear();
            lastNameField.clear();
            emailField.clear();
            phoneField.clear();
            roleComboBox.setValue("GENERAL");

            // Navigate back to admin panel after a short delay
            Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                if (isActiveView()) App.setScene("/fxml/admin-panel.fxml", "VSAS - Admin Panel");
            }));
            delay.play();

        } catch (Exception ex) {
            showMessage("Failed to create user: " + ex.getMessage(), true);
        }
    }

    @FXML
    public void onBackToAdminPanel(ActionEvent e) {
        if (!requireAdmin()) return;
        App.setScene("/fxml/admin-panel.fxml", "VSAS - Admin Panel");
    }

    private boolean requireAdmin() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getType())) return true;
        showMessage("Access denied. Admin privileges required.", true);
        return false;
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            if (isError) {
                messageLabel.getStyleClass().remove("banner-success");
                messageLabel.getStyleClass().add("banner-error");
            } else {
                messageLabel.getStyleClass().remove("banner-error");
                messageLabel.getStyleClass().add("banner-success");
            }
            messageLabel.setManaged(true);
            messageLabel.setVisible(true);
        }
    }
    private boolean isActiveView() {
        return messageLabel != null && messageLabel.getScene() != null
                && messageLabel.getScene().getWindow() != null
                && messageLabel.getScene().getWindow().isShowing();
    }

}
