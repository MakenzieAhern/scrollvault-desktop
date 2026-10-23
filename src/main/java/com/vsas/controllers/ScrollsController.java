package com.vsas.controllers;

import com.vsas.App;
import com.vsas.models.Scroll;
import com.vsas.security.SessionManager;
import com.vsas.services.ScrollService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public class ScrollsController {
    @FXML private TableView<Scroll> table;
    @FXML private TableColumn<Scroll, String> colId;
    @FXML private TableColumn<Scroll, String> colTitle;
    @FXML private TableColumn<Scroll, String> colType;
    @FXML private TableColumn<Scroll, String> colDate;
    @FXML private TextArea textPreview;
    @FXML private ImageView imagePreview;
    @FXML private Button btnDownload;
    @FXML private Button btnUpload;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnMyScrolls;
    @FXML private Label messageLabel;
    @FXML private Label previewInfo;
    @FXML private Label dateTimeLabel;
    @FXML private Label titleLabel;
    // filter bar controls
    @FXML private TextField txtKeyword;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbSort;
    @FXML private CheckBox chkAsc;
    @FXML private Label lblFilters;
    @FXML private Button btnClear;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    private final ScrollService svc = new ScrollService();
    private final ObservableList<Scroll> master = FXCollections.observableArrayList();
    private FilteredList<Scroll> filtered;
    private SortedList<Scroll> sorted;

    // testing helpers for alerts/downloads (non-invasive; used only if queried by tests)
    private volatile Alert.AlertType lastAlertType;
    private volatile String lastAlertText;
    private volatile String lastSuggestedName;
    private volatile String lastSavedPath;

    public Alert.AlertType getLastAlertType()    { return lastAlertType; }
    public String          getLastAlertText()    { return lastAlertText; }
    public String          getLastSuggestedName(){ return lastSuggestedName; }
    public String          getLastSavedPath()    { return lastSavedPath; }

    @FXML
    public void initialize() {
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        colType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getType()));
        if (colDate != null) {
            java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUploadDate() == null ? "" : c.getValue().getUploadDate().format(df)
            ));
        }

        loadScrolls();
        filtered = new FilteredList<>(master, s -> true);
        sorted = new SortedList<>(filtered);
        table.setItems(sorted);

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> renderPreview(n));
        // keep clickable so guests see the banner
        if (btnDownload != null) btnDownload.setDisable(false);

        if (!master.isEmpty()) table.getSelectionModel().select(0);

        // ---- filter bar options ----
        if (cmbType != null) {
            cmbType.getItems().setAll("ALL", "PDF", "IMAGE", "TEXT");
            cmbType.setValue("ALL");
        }
        if (cmbSort != null) {
            cmbSort.getItems().setAll("ID", "Title", "Type", "Date");
            cmbSort.setValue("ID");
        }
        if (chkAsc != null) chkAsc.setSelected(true);

        // listeners -> re-apply filters/sort
        if (txtKeyword != null) {
            txtKeyword.textProperty().addListener((o, ov, nv) -> applyFiltersAndSort());
        }
        if (cmbType != null) {
            cmbType.valueProperty().addListener((o, ov, nv) -> applyFiltersAndSort());
        }
        if (cmbSort != null) {
            cmbSort.valueProperty().addListener((o, ov, nv) -> applyFiltersAndSort());
        }
        if (chkAsc != null) {
            chkAsc.selectedProperty().addListener((o, ov, nv) -> applyFiltersAndSort());
        }
        if (dpFrom != null) {
            dpFrom.valueProperty().addListener((o, ov, nv) -> applyFiltersAndSort());
        }
        if (dpTo != null) {
            dpTo.valueProperty().addListener((o, ov, nv) -> applyFiltersAndSort());
        }

        if (btnMyScrolls != null) {
            boolean logged = SessionManager.isLoggedIn();
            btnMyScrolls.setVisible(logged);
            btnMyScrolls.setManaged(logged);
            // ensure event handler is bound even if FXML linkage fails
            btnMyScrolls.setOnAction(this::onMyScrolls);
        }
        if (btnUpload != null) {
            // Keep upload button visible for guests, but show error when clicked
            btnUpload.setVisible(true);
            btnUpload.setManaged(true);
        }

        // initial apply
        applyFiltersAndSort();
    }

    private void loadScrolls() {
        if (btnEdit != null) {
            master.setAll(SessionManager.isLoggedIn()
                    ? svc.listUserScrolls(SessionManager.getCurrentUser().getIdKey())
                    : java.util.List.of());
        } else {
            master.setAll(svc.list());
        }
    }

    private void refreshScrolls() {
        // Reload data without adding duplicate selection listeners or clock timers.
        table.getSelectionModel().clearSelection();
        loadScrolls();
        applyFiltersAndSort();
        if (!table.getItems().isEmpty()) table.getSelectionModel().select(0);
    }

    private void applyFiltersAndSort() {
        final String kw = safeLower(txtKeyword == null ? "" : txtKeyword.getText());
        final String type = (cmbType == null || cmbType.getValue() == null)
                ? "ALL" : cmbType.getValue();

        java.util.function.Predicate<Scroll> p = s -> true;

        if (!kw.isBlank()) {
            p = p.and(s -> safeLower(s.getId()).contains(kw)
                    || safeLower(s.getTitle()).contains(kw));
        }
        if (!"ALL".equalsIgnoreCase(type)) {
            p = p.and(s -> java.util.Objects.equals(s.getType(), type));
        }
        java.time.LocalDate from = dpFrom == null ? null : dpFrom.getValue();
        java.time.LocalDate to = dpTo == null ? null : dpTo.getValue();
        if (from != null) {
            p = p.and(s -> s.getUploadDate() != null && !s.getUploadDate().isBefore(from));
        }
        if (to != null) {
            p = p.and(s -> s.getUploadDate() != null && !s.getUploadDate().isAfter(to));
        }
        if (filtered != null) filtered.setPredicate(p);

        String sortKey = (cmbSort == null || cmbSort.getValue() == null)
                ? "ID" : cmbSort.getValue();
        boolean asc = chkAsc == null || chkAsc.isSelected();

        java.util.Comparator<Scroll> cmp;
        switch (sortKey) {
            case "Title" -> cmp = java.util.Comparator.comparing(Scroll::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "Type"  -> cmp = java.util.Comparator.comparing(Scroll::getType,  String.CASE_INSENSITIVE_ORDER);
            case "Date"  -> cmp = java.util.Comparator.comparing(Scroll::getUploadDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            default      -> cmp = java.util.Comparator.comparing(Scroll::getId,    String.CASE_INSENSITIVE_ORDER);
        }
        if (!asc) cmp = cmp.reversed();
        if (sorted != null) sorted.setComparator(cmp);

        int resultsCount = filtered == null ? 0 : filtered.size();
        // optional: if present, keep unobtrusive; otherwise ignore
        if (lblFilters != null) {
            lblFilters.setManaged(false);
            lblFilters.setVisible(false);
        }

        if (titleLabel != null) {
            titleLabel.setText("Available Scrolls (" + resultsCount + ")");
        }
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT);
    }

    @FXML
    public void onClearFilters(javafx.event.ActionEvent e) {
        if (txtKeyword != null) txtKeyword.clear();
        if (cmbType != null) cmbType.setValue("ALL");
        if (cmbSort != null) cmbSort.setValue("ID");
        if (chkAsc != null) chkAsc.setSelected(true);
        if (dpFrom != null) dpFrom.setValue(null);
        if (dpTo != null) dpTo.setValue(null);
        applyFiltersAndSort();
    }

    private void renderPreview(Scroll s) {
        if (s == null) { textPreview.clear(); imagePreview.setImage(null); if (previewInfo!=null) previewInfo.setText(""); return; }
        if (previewInfo != null) {
            previewInfo.setText("ID: " + s.getId() + "    Title: " + s.getTitle() + "    Type: " + s.getType());
        }
        // Track view count when preview is rendered
        try {
            svc.incrementViewCount(s.getID());
        } catch (Exception ex) {
            System.err.println("Failed to increment view count: " + ex.getMessage());
        }
        switch (s.getType()) {
            case "TEXT" -> {
                textPreview.setVisible(true);
                imagePreview.setVisible(false);
                imagePreview.setManaged(false);
                textPreview.setManaged(true);
                textPreview.setText(svc.readText(s));
            }
            case "IMAGE" -> {
                textPreview.setVisible(false);
                textPreview.setManaged(false);
                imagePreview.setVisible(true);
                imagePreview.setManaged(true);
                imagePreview.setImage(svc.readImage(s));
            }
            default -> {
                textPreview.setVisible(true);
                textPreview.setManaged(true);
                imagePreview.setVisible(false);
                imagePreview.setManaged(false);
                textPreview.setText("Preview not supported for type: " + s.getType());
            }
        }
    }

    @FXML
    public void onDownload(ActionEvent e) {
        Scroll s = table.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert(Alert.AlertType.INFORMATION, "Select a scroll first."); return; }
        if (!SessionManager.isLoggedIn()) {
            setMessageLabel("Please log in or sign up to download scrolls", true);
            return;
        }

        lastSuggestedName = suggestedDownloadName(s);

        File out;
        if ("true".equals(System.getProperty("test.mode"))) {
            File base = new File(System.getProperty("java.io.tmpdir"));
            String unique = System.nanoTime() + "_" + lastSuggestedName;
            out = new File(base, unique);
        } else {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName(lastSuggestedName);
            out = fc.showSaveDialog(null);
        if (out == null) return;
        }

        try (var in = svc.open(s)) {
            if (in == null) { showAlert(Alert.AlertType.ERROR, "Resource not found."); return; }
            try (var fos = new FileOutputStream(out)) {
                in.transferTo(fos);
            }
            lastSavedPath = out.getAbsolutePath();
            // Track download count
            try {
                svc.incrementDownloadCount(s.getID());
            } catch (Exception ex) {
                System.err.println("Failed to increment download count: " + ex.getMessage());
            }
            showAlert(Alert.AlertType.INFORMATION, "Saved to: " + lastSavedPath);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Download failed: " + ex.getMessage());
        }
    }

    static String suggestedDownloadName(Scroll scroll) {
        String resource = scroll.getResourcePath() == null ? "" : scroll.getResourcePath();
        String fileName = resource.substring(Math.max(resource.lastIndexOf('/'), resource.lastIndexOf('\\')) + 1);
        int dot = fileName.lastIndexOf('.');
        String ext = dot > 0 ? fileName.substring(dot).toLowerCase(Locale.ROOT) : "";
        if (!ext.matches("\\.[a-z0-9]{1,10}")) ext = "";
        if (ext.isEmpty()) ext = switch (scroll.getType()) {
            case "TEXT" -> ".txt";
            case "IMAGE" -> ".png";
            case "PDF" -> ".pdf";
            default -> "";
        };
        String title = scroll.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        return title.toLowerCase(Locale.ROOT).endsWith(ext) ? title : title + ext;
    }

    @FXML
    public void onUpload(ActionEvent e) {
        if (!SessionManager.isLoggedIn()) {
            setMessageLabel("Please log in or sign up to upload scrolls", true);
            return;
        }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile == null) return;

        TextInputDialog titleDialog = new TextInputDialog(selectedFile.getName());
        titleDialog.setTitle("Enter Scroll Title");
        titleDialog.setHeaderText("Please enter a title for your scroll");
        titleDialog.setContentText("Title:");
        String scrollTitle = titleDialog.showAndWait().orElse(null);
        if (scrollTitle == null) { return; }
        if (scrollTitle.trim().isEmpty()) {
            setMessageLabel("Upload cancelled: Title is required", true);
            return;
        }

        String fileType = selectedFile.getName().toLowerCase().endsWith(".txt") ? "TEXT" : "IMAGE";
        String newId = svc.generateNextScrollId();

        var currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            setMessageLabel("Upload failed: User session not found", true);
            return;
        }

        String userId = currentUser.getIdKey();
        if (userId == null || userId.isEmpty()) {
            setMessageLabel("Upload failed: User ID not found", true);
            return;
        }

        try {
            com.vsas.models.Scroll newScroll = new com.vsas.models.Scroll(
                newId,
                userId,
                scrollTitle.trim(),
                fileType,
                selectedFile.getAbsolutePath()
            );
            svc.add(newScroll, selectedFile);
            refreshScrolls();
            setMessageLabel("Upload successful!", false);
        } catch (Exception ex) {
            ex.printStackTrace();
            setMessageLabel("Upload failed: " + ex.getMessage(), true);
        }
    }

    @FXML
    public void onEdit(ActionEvent e) {
        com.vsas.models.Scroll selectedScroll = table.getSelectionModel().getSelectedItem();
        if (selectedScroll == null) {
            setMessageLabel("Please select a scroll to edit.", true);
            return;
        }
        if (!SessionManager.isLoggedIn() || !Objects.equals(selectedScroll.getUploaderID(), SessionManager.getCurrentUser().getIdKey())) {
            setMessageLabel("You can only edit your own scrolls.", true);
            return;
        }

        // Show edit options dialog
        Alert editOptionsAlert = new Alert(Alert.AlertType.CONFIRMATION);
        editOptionsAlert.setTitle("Edit Scroll");
        editOptionsAlert.setHeaderText("What would you like to edit?");
        editOptionsAlert.setContentText("Choose an option:");

        ButtonType btnEditTitle = new ButtonType("Edit Title");
        ButtonType btnEditContent = new ButtonType("Edit Content");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        editOptionsAlert.getButtonTypes().setAll(btnEditTitle, btnEditContent, btnCancel);

        editOptionsAlert.showAndWait().ifPresent(choice -> {
            if (choice == btnEditTitle) {
                editScrollTitle(selectedScroll);
            } else if (choice == btnEditContent) {
                editScrollContent(selectedScroll);
            }
        });
    }

    private void editScrollTitle(com.vsas.models.Scroll selectedScroll) {
        TextInputDialog titleDialog = new TextInputDialog(selectedScroll.getTitle());
        titleDialog.setTitle("Edit Scroll Title");
        titleDialog.setHeaderText("Edit title for scroll: " + selectedScroll.getID());
        titleDialog.setContentText("New Title:");

        String newTitle = titleDialog.showAndWait().orElse(null);
        if (newTitle == null) { return; }
        if (newTitle.trim().isEmpty()) {
            setMessageLabel("Edit cancelled: Title cannot be blank", true);
            return;
        }

        try {
            selectedScroll.setTitle(newTitle.trim());
            svc.update(selectedScroll);
            refreshScrolls();
            setMessageLabel("Scroll title updated successfully!", false);
        } catch (Exception ex) {
            setMessageLabel("Failed to update scroll: " + ex.getMessage(), true);
        }
    }

    private void editScrollContent(com.vsas.models.Scroll selectedScroll) {
        // Only allow editing text files
        if (!"TEXT".equalsIgnoreCase(selectedScroll.getType())) {
            setMessageLabel("Only TEXT scrolls can have their content edited.", true);
            return;
        }

        try {
            // Read current content
            String currentContent = svc.readText(selectedScroll);
            if (currentContent.startsWith("Failed to read:") || currentContent.startsWith("Resource not found:")) {
                setMessageLabel("Cannot read scroll content for editing.", true);
                return;
            }

            // Create a dialog with TextArea for editing
            Dialog<String> contentDialog = new Dialog<>();
            contentDialog.setTitle("Edit Scroll Content");
            contentDialog.setHeaderText("Edit content for: " + selectedScroll.getTitle());

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            contentDialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            TextArea contentArea = new TextArea(currentContent);
            contentArea.setWrapText(true);
            contentArea.setPrefRowCount(20);
            contentArea.setPrefColumnCount(60);
            contentArea.setStyle("-fx-font-family: 'Courier New', monospace;");

            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
            vbox.getChildren().addAll(
                new Label("Edit the content below:"),
                contentArea
            );
            vbox.setPrefWidth(700);
            vbox.setPrefHeight(500);

            contentDialog.getDialogPane().setContent(vbox);

            contentDialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return contentArea.getText();
                }
                return null;
            });

            String newContent = contentDialog.showAndWait().orElse(null);
            if (newContent == null) { return; }

            // Save the new content to the file
            java.nio.file.Path scrollPath = svc.getScrollFilePath(selectedScroll);
            if (scrollPath != null && java.nio.file.Files.exists(scrollPath)) {
                java.nio.file.Files.writeString(scrollPath, newContent, java.nio.charset.StandardCharsets.UTF_8);

                // Store the scroll ID before refresh
                String editedScrollId = selectedScroll.getID();

                // Refresh the table to reload data
                refreshScrolls();

                // Re-select the scroll and refresh its preview with the fresh object
                javafx.application.Platform.runLater(() -> {
                    for (int i = 0; i < table.getItems().size(); i++) {
                        if (table.getItems().get(i).getID().equals(editedScrollId)) {
                            table.getSelectionModel().select(i);
                            // Force clear and re-render with fresh scroll object
                            textPreview.clear();
                            renderPreview(table.getItems().get(i));
                            break;
                        }
                    }
                });

                setMessageLabel("Scroll content updated successfully!", false);
            } else {
                setMessageLabel("Failed to locate scroll file for editing.", true);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            setMessageLabel("Failed to update scroll content: " + ex.getMessage(), true);
        }
    }

    @FXML
    public void onDelete(ActionEvent e) {
        com.vsas.models.Scroll selectedScroll = table.getSelectionModel().getSelectedItem();
        if (selectedScroll == null) {
            setMessageLabel("Please select a scroll to delete.", true);
            return;
        }
        if (!SessionManager.isLoggedIn() || !Objects.equals(selectedScroll.getUploaderID(), SessionManager.getCurrentUser().getIdKey())) {
            setMessageLabel("You can only delete your own scrolls.", true);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Scroll");
        confirmation.setContentText("Are you sure you want to delete the scroll '" + selectedScroll.getTitle());

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                svc.delete(selectedScroll);
                refreshScrolls();
                setMessageLabel("Scroll deleted successfully!", false);
            } catch (Exception ex) {
                setMessageLabel("Failed to delete scroll: " + ex.getMessage(), true);
            }
        }
    }

    @FXML
    public void onMyScrolls(ActionEvent e) {
        if (!SessionManager.isLoggedIn()) {
            setMessageLabel("Please log in or sign up to view your scrolls", true);
            return;
        }
        App.setScene("/fxml/my-scrolls.fxml", "VSAS - My Scrolls");
    }

    @FXML
    public void onBackToAllScrolls(ActionEvent e) {
        App.setScene("/fxml/scrolls.fxml", "VSAS - Browse Scrolls");
    }

    @FXML
    public void onGoBack(ActionEvent e) {
        if (SessionManager.isLoggedIn()) {
            App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
        } else {
            App.setScene("/fxml/guest.fxml", "VSAS - Guest");
        }
    }

    private void setMessageLabel(String msg, boolean isError) {
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

    private void showAlert(Alert.AlertType type, String msg) {
        lastAlertType = type;
        lastAlertText = msg;
        if ("true".equals(System.getProperty("test.mode"))) return;
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
