package com.vsas.controllers;

import com.vsas.App;
import com.vsas.security.SessionManager;
import com.vsas.services.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.nio.file.Path;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerBtn;
    @FXML private Button guestBtn;
    @FXML private Label messageLabel;
    @FXML private Label dateTimeLabel;

    private final AuthService auth = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));

    @FXML
    public void initialize() {
        // Do not prefill username on startup

        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }
    }

    @FXML
    public void onLogin(ActionEvent e) {
        try {
            var userOpt = auth.login(usernameField.getText(), passwordField.getText());
            if (userOpt.isPresent()) {
                if (messageLabel != null) { messageLabel.setManaged(false); messageLabel.setVisible(false); }
                SessionManager.login(userOpt.get());
                App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
            } else {
                if (messageLabel != null) {
                    messageLabel.setText("Oops! Invalid credentials");
                    messageLabel.setManaged(true);
                    messageLabel.setVisible(true);
                }
            }
        } catch (Exception ex) {
            if (messageLabel != null) {
                messageLabel.setText("Oops! Invalid credentials");
                messageLabel.setManaged(true);
                messageLabel.setVisible(true);
            }
        }
    }

    @FXML
    public void onRegister(ActionEvent e) {
        App.setScene("/fxml/register.fxml", "VSAS - Register");
    }

    @FXML
    public void onContinueAsGuest(ActionEvent e) {
        SessionManager.logout();
        App.setScene("/fxml/guest.fxml", "VSAS - Guest");
    }

    @FXML
    public void onGoBack(ActionEvent e) {
        App.setScene("/fxml/welcome.fxml", "VSAS - Welcome");
    }
}
