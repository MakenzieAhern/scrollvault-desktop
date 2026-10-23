package com.vsas.controllers;

import com.vsas.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class ContactController {
    @FXML private Label dateTimeLabel;

    @FXML
    public void initialize() {
        ControllerClock.start(dateTimeLabel);
    }

    @FXML
    public void onGoBack() {
        App.setScene("/fxml/welcome.fxml", "VSAS - Welcome");
    }
}
