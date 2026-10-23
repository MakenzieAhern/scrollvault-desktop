package com.vsas.controllers;

import com.vsas.App;
import com.vsas.services.AuthService;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;

public class RegisterController {
    @FXML private TextField usernameField, firstNameField, lastNameField, emailField, phoneField;
    @FXML private PasswordField passwordField, confirmField;
    @FXML private Label messageLabel;
    @FXML private Label dateTimeLabel;

    private final AuthService auth = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));

    @FXML
    public void initialize() {
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }
    }

    @FXML
    private void onRegister(ActionEvent e) {
        String username = safe(usernameField);
        String firstName = safe(firstNameField);
        String lastName = safe(lastNameField);
        String fullName = buildFullName(firstName, lastName);
        String email = safe(emailField);
        String phone = safe(phoneField);
        String p1 = passwordField == null ? null : passwordField.getText();
        String p2 = confirmField == null ? null : confirmField.getText();

        // Required checks for all except email
        if (isBlank(username) || isBlank(firstName) || isBlank(lastName) || isBlank(phone) || isBlank(p1) || isBlank(p2)) {
            showBanner("Please fill in all details");
            return;
        }
        // Username format: up to 10 chars, letters numbers underscore only
        if (!username.matches("^[A-Za-z0-9_]{1,10}$")) {
            if (username.length() > 10) showBanner("Username cannot be more than 10 characters");
            else showBanner("Username can only contain letters, numbers, and _");
            return;
        }
        // Phone must be exactly 10 digits
        if (!phone.matches("\\d{10}")) {
            showBanner("Phone number is wrong");
            return;
        }
        // Password match
        if (!p1.equals(p2)) {
            showBanner("Passwords do not match");
            return;
        }
        try {
            // auto-generates ID, enforces unique username/id
            auth.register(null, username, fullName, email, phone, p1, "GENERAL");
            showSuccess("Registration successful. Please log in.");
            PauseTransition pause = new PauseTransition(Duration.seconds(3.2));
            pause.setOnFinished(ev -> {
                if (isActiveView()) App.setScene("/fxml/login.fxml", "VSAS - Login");
            });
            pause.play();
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("username")) {
                showBanner("Username already exists, please choose a different username");
            } else {
                showBanner(ex.getMessage());
            }
        } catch (IOException ex) {
            showBanner("Failed to register. Try again");
        }
    }

    private String buildFullName(String first, String last) {
        if (first == null) first = "";
        if (last == null) last = "";
        return (first + " " + last).trim();
    }

    private String safe(TextField tf) { return tf == null ? null : tf.getText(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

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

    @FXML
    private void onBackToLogin(ActionEvent e) {
        App.setScene("/fxml/login.fxml", "VSAS - Login");
    }
    private boolean isActiveView() {
        return messageLabel != null && messageLabel.getScene() != null
                && messageLabel.getScene().getWindow() != null
                && messageLabel.getScene().getWindow().isShowing();
    }

}
