package com.vsas.controllers;

import com.vsas.App;
import com.vsas.utils.PrefsService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;

import java.util.Arrays;
import java.util.List;

public class WelcomeController {
    @FXML private Label dateTimeLabel;
    @FXML private Button getStartedBtn;
    @FXML private Button aboutBtn;
    @FXML private ChoiceBox<String> languageChoice;
    @FXML private ToggleButton lightToggle;
    @FXML private ToggleButton darkToggle;



    @FXML
    public void initialize() {
        // Theme init and apply after scene is ready
        String theme = PrefsService.getTheme();
        if ("light".equalsIgnoreCase(theme)) {
            if (lightToggle != null) lightToggle.setSelected(true);
        } else {
            if (darkToggle != null) darkToggle.setSelected(true);
        }
        Platform.runLater(() -> applyTheme(theme));

        // Language options (header removed; guard for nulls)
        if (languageChoice != null) {
            List<String> langs = Arrays.asList("en", "es", "fr");
            languageChoice.getItems().setAll(langs);
            languageChoice.setValue(PrefsService.getLanguage());
            languageChoice.setTooltip(new Tooltip("Choose language"));
            languageChoice.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> PrefsService.setLanguage(n));
        }

        // Tooltips on actions
        if (lightToggle != null) Tooltip.install(lightToggle, new Tooltip("Switch to Light theme"));
        if (darkToggle != null) Tooltip.install(darkToggle, new Tooltip("Switch to Dark theme"));

        // Live date/time
        ControllerClock.start(dateTimeLabel);

        // Ensure About width matches Get Started width
        if (getStartedBtn != null && aboutBtn != null) {
            Platform.runLater(() -> aboutBtn.setPrefWidth(getStartedBtn.getWidth()));
        }

        // Remember-me removed from welcome page

        // No auto-redirect from remembered username
    }

    private void applyTheme(String theme) {
        var root = dateTimeLabel.getScene() == null ? null : dateTimeLabel.getScene().getRoot();
        if (root == null) return;
        root.getStyleClass().removeAll("light", "dark");
        if ("light".equalsIgnoreCase(theme)) root.getStyleClass().add("light"); else root.getStyleClass().add("dark");
    }

    @FXML
    public void onThemeLight() {
        PrefsService.setTheme("light");
        applyTheme("light");
        if (lightToggle != null) lightToggle.setSelected(true);
        if (darkToggle != null) darkToggle.setSelected(false);
    }

    @FXML
    public void onThemeDark() {
        PrefsService.setTheme("dark");
        applyTheme("dark");
        if (darkToggle != null) darkToggle.setSelected(true);
        if (lightToggle != null) lightToggle.setSelected(false);
    }

    @FXML
    public void onGetStarted() {
        App.setScene("/fxml/login.fxml", "VSAS - Login");
    }

    @FXML
    public void onAboutAction() {
        App.setScene("/fxml/about.fxml", "VSAS - About");
    }

    @FXML
    public void onContactAction() {
        App.setScene("/fxml/contact.fxml", "VSAS - Contact");
    }

    @FXML public void onHelp(MouseEvent e) { showInfo("Help", "Step 1: Register\nStep 2: Login\nStep 3: Browse Scrolls"); }
    @FXML public void onAbout(MouseEvent e) { showInfo("About", "VSAS — Virtual Scrolls Access System\nTeam: SOFT2412"); }
    @FXML public void onContact(MouseEvent e) { showInfo("Contact", "Email: support@example.com"); }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
