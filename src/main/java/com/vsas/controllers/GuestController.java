package com.vsas.controllers;

import com.vsas.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class GuestController {
    @FXML private Label dateTimeLabel;
    @FXML private Label userDisplayLabel;
    @FXML private Label userTypeLabel;

    @FXML
    public void initialize() {
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        if (userDisplayLabel != null) {
            userDisplayLabel.setText("Logged in as: guest");
        }
        if (userTypeLabel != null) {
            userTypeLabel.setText("User Type: Guest");
        }
    }

    @FXML
    public void onGoBack(ActionEvent e) {
        App.setScene("/fxml/login.fxml", "VSAS - Login");
    }

    @FXML
    public void onBrowseScrolls(ActionEvent e) {
        App.setScene("/fxml/scrolls.fxml", "VSAS - Browse Scrolls");
    }
}
