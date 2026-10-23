package com.vsas.controllers;

import com.vsas.App;
import com.vsas.models.Scroll;
import com.vsas.models.User;
import com.vsas.security.SessionManager;
import com.vsas.services.AuthService;
import com.vsas.services.ScrollService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class StatisticsController {

    // Summary labels
    @FXML private Label lblTotalScrolls;
    @FXML private Label lblTotalDownloads;
    @FXML private Label lblTotalUploads;
    @FXML private Label lblTotalViews;
    @FXML private Label lblResultCount;
    @FXML private Label messageLabel;
    @FXML private Label dateTimeLabel;

    // Filter controls
    @FXML private TextField txtFilterScrollId;
    @FXML private TextField txtFilterUploader;
    @FXML private DatePicker dpFilterFrom;
    @FXML private DatePicker dpFilterTo;

    // Statistics table
    @FXML private TableView<Scroll> tblScrollStats;
    @FXML private TableColumn<Scroll, String> colScrollId;
    @FXML private TableColumn<Scroll, String> colScrollName;
    @FXML private TableColumn<Scroll, String> colScrollType;
    @FXML private TableColumn<Scroll, String> colUploader;
    @FXML private TableColumn<Scroll, String> colUploadDate;
    @FXML private TableColumn<Scroll, String> colDownloads;
    @FXML private TableColumn<Scroll, String> colViews;

    private final ScrollService scrollService = new ScrollService();
    private final AuthService authService = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));
    private final ObservableList<Scroll> masterScrollList = FXCollections.observableArrayList();
    private FilteredList<Scroll> filteredScrollList;

    @FXML
    public void initialize() {
        if (!requireAdmin()) return;

        // Setup date/time label
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        // Setup scroll statistics table columns
        colScrollId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getID()));
        colScrollName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        colScrollType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getType()));
        colUploader.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(getUploaderUsername(c.getValue())));
        colUploadDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUploadDateString()));
        colDownloads.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getDownloadCount())));
        colViews.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getViewCount())));

        // Setup filtered list
        filteredScrollList = new FilteredList<>(masterScrollList, p -> true);
        tblScrollStats.setItems(filteredScrollList);

        loadStatistics();
    }

    private String getUploaderUsername(Scroll scroll) {
        if (scroll.getUploaderID() == null || scroll.getUploaderID().isEmpty()) {
            return "System";
        }
        try {
            for (User user : authService.getAllUsers()) {
                if (scroll.getUploaderID().equals(user.getIdKey())) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown";
    }

    private void loadStatistics() {
        try {
            // Load all scrolls
            masterScrollList.clear();
            List<Scroll> allScrolls = scrollService.list();
            masterScrollList.addAll(allScrolls);

            // Calculate summary statistics
            int totalScrolls = allScrolls.size();
            int totalDownloads = allScrolls.stream().mapToInt(Scroll::getDownloadCount).sum();
            int totalUploads = (int) allScrolls.stream().filter(s -> s.getUploaderID() != null && !s.getUploaderID().isEmpty()).count();
            int totalViews = allScrolls.stream().mapToInt(Scroll::getViewCount).sum();

            // Update summary cards
            if (lblTotalScrolls != null) lblTotalScrolls.setText(String.valueOf(totalScrolls));
            if (lblTotalDownloads != null) lblTotalDownloads.setText(String.valueOf(totalDownloads));
            if (lblTotalUploads != null) lblTotalUploads.setText(String.valueOf(totalUploads));
            if (lblTotalViews != null) lblTotalViews.setText(String.valueOf(totalViews));

            // Update result count
            updateResultCount();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load statistics: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void onApplyFilters(ActionEvent e) {
        if (!requireAdmin()) return;
        applyFilters();
    }

    @FXML
    public void onClearFilters(ActionEvent e) {
        if (!requireAdmin()) return;
        if (txtFilterScrollId != null) txtFilterScrollId.clear();
        if (txtFilterUploader != null) txtFilterUploader.clear();
        if (dpFilterFrom != null) dpFilterFrom.setValue(null);
        if (dpFilterTo != null) dpFilterTo.setValue(null);
        applyFilters();
    }

    private void applyFilters() {
        String scrollIdFilter = txtFilterScrollId != null && txtFilterScrollId.getText() != null
            ? txtFilterScrollId.getText().toLowerCase(Locale.ROOT).trim()
            : "";
        String uploaderFilter = txtFilterUploader != null && txtFilterUploader.getText() != null
            ? txtFilterUploader.getText().toLowerCase(Locale.ROOT).trim()
            : "";
        LocalDate fromDate = dpFilterFrom != null ? dpFilterFrom.getValue() : null;
        LocalDate toDate = dpFilterTo != null ? dpFilterTo.getValue() : null;

        filteredScrollList.setPredicate(scroll -> {
            // Filter by Scroll ID
            if (!scrollIdFilter.isEmpty()) {
                if (scroll.getID() == null || !scroll.getID().toLowerCase(Locale.ROOT).contains(scrollIdFilter)) {
                    return false;
                }
            }

            // Filter by Uploader
            if (!uploaderFilter.isEmpty()) {
                String uploaderUsername = getUploaderUsername(scroll).toLowerCase(Locale.ROOT);
                if (!uploaderUsername.contains(uploaderFilter)) {
                    return false;
                }
            }

            // Filter by date range
            if (fromDate != null || toDate != null) {
                LocalDate uploadDate = scroll.getUploadDate();
                if (uploadDate == null) return false;

                if (fromDate != null && uploadDate.isBefore(fromDate)) {
                    return false;
                }
                if (toDate != null && uploadDate.isAfter(toDate)) {
                    return false;
                }
            }

            return true;
        });

        updateResultCount();
    }

    private void updateResultCount() {
        if (lblResultCount != null) {
            int count = filteredScrollList.size();
            lblResultCount.setText("Showing " + count + " scroll" + (count != 1 ? "s" : ""));
        }
    }

    @FXML
    public void onRefresh(ActionEvent e) {
        if (!requireAdmin()) return;
        loadStatistics();
        applyFilters();
        showMessage("Statistics refreshed successfully.", false);
    }

    private boolean requireAdmin() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getType())) return true;
        masterScrollList.clear();
        if (lblResultCount != null) lblResultCount.setText("Showing 0 scrolls");
        if (lblTotalScrolls != null) lblTotalScrolls.setText("0");
        if (lblTotalDownloads != null) lblTotalDownloads.setText("0");
        if (lblTotalUploads != null) lblTotalUploads.setText("0");
        if (lblTotalViews != null) lblTotalViews.setText("0");
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

    @FXML
    public void onGoBack(ActionEvent e) {
        App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
    }
}
