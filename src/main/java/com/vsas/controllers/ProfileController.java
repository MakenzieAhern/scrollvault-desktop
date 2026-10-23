package com.vsas.controllers;

import com.vsas.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import java.nio.file.Path;
import com.vsas.services.AuthService;
import com.vsas.security.SessionManager;
import com.vsas.models.User;

public class ProfileController {
    @FXML private Label dateTimeLabel;
    @FXML private Label messageLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField usernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private final AuthService auth = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));

    @FXML
    public void initialize() {
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        // Pre-fill current user details
        User u = SessionManager.getCurrentUser();
        if (u != null) {
            if (usernameField != null) usernameField.setText(u.getUsername());
            if (emailField != null) emailField.setText(u.getEmail());
            if (phoneField != null) phoneField.setText(u.getPhone());
            String full = u.getFullName();
            if (full != null && !full.isBlank()) {
                String[] parts = full.split("\\s+", 2);
                if (parts.length >= 1 && firstNameField != null) firstNameField.setText(parts[0]);
                if (parts.length == 2 && lastNameField != null) lastNameField.setText(parts[1]);
            }
        }
    }

    @FXML
    private void onGoBack(ActionEvent e) {
        App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
    }

    @FXML
    private void onSave(ActionEvent e) {
        User current = SessionManager.getCurrentUser();
        if (current == null) return;

        String username = text(usernameField);
        String firstName = text(firstNameField);
        String lastName = text(lastNameField);
        String email = text(emailField);
        String phone = text(phoneField);
        String fullName = buildFullName(firstName, lastName);
        String newPass = newPasswordField == null ? null : newPasswordField.getText();
        String confirm = confirmPasswordField == null ? null : confirmPasswordField.getText();

        // Validation
        if (isBlank(username) || isBlank(firstName) || isBlank(lastName) || isBlank(phone)) {
            showBanner("Please fill in all details");
            return;
        }
        if (!username.matches("^[A-Za-z0-9_]{1,10}$")) {
            showBanner("Username can only contain letters, numbers, and _ (max 10)");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            showBanner("Phone number is wrong");
            return;
        }
        if (!isBlank(newPass) || !isBlank(confirm)) {
            if (!safeEquals(newPass, confirm)) {
                showBanner("Passwords do not match");
                return;
            }
            if (newPass.length() < 6) {
                showBanner("Password must be at least 6 characters");
                return;
            }
        }

        try {
            User updated = auth.updateProfile(current, username, fullName, email, phone, isBlank(newPass) ? null : newPass);
            SessionManager.login(updated); // refresh session with updated user
            showSuccess("Changes saved successfully");
            // Optionally wait briefly, then go back
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.2));
            pause.setOnFinished(ev -> {
                if (isActiveView()) App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
            });
            pause.play();
        } catch (IllegalArgumentException ex) {
            showBanner(ex.getMessage() == null ? "Failed to save changes" : ex.getMessage());
        } catch (Exception ex) {
            showBanner("Failed to save changes");
        }
    }

    private String text(TextField t) { return t == null ? null : t.getText(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean safeEquals(String a, String b) { return a == null ? b == null : a.equals(b); }
    private String buildFullName(String first, String last) { return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim(); }

    private void showBanner(String text) {
        if (messageLabel == null) return;
        messageLabel.getStyleClass().remove("banner-success");
        if (!messageLabel.getStyleClass().contains("banner-error")) messageLabel.getStyleClass().add("banner-error");
        messageLabel.setText(text);
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void showSuccess(String text) {
        if (messageLabel == null) return;
        messageLabel.getStyleClass().remove("banner-error");
        if (!messageLabel.getStyleClass().contains("banner-success")) messageLabel.getStyleClass().add("banner-success");
        messageLabel.setText(text);
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }
    private boolean isActiveView() {
        return messageLabel != null && messageLabel.getScene() != null
                && messageLabel.getScene().getWindow() != null
                && messageLabel.getScene().getWindow().isShowing();
    }

}
