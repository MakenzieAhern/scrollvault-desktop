package com.vsas.controllers;

import com.vsas.App;
import com.vsas.models.User;
import com.vsas.security.SessionManager;
import com.vsas.services.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.nio.file.Path;
import java.util.Locale;

public class AdminPanelController {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colIdKey;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private Button btnSetAdmin;
    @FXML private Button btnSetGeneral;
    @FXML private Label messageLabel;
    @FXML private Label dateTimeLabel;
    @FXML private TextField txtSearch;

    private final AuthService authService = new AuthService(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUserList;

    @FXML
    public void initialize() {
        if (!requireAdmin()) return;

        // Setup date/time label
        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        // Setup table columns
        colIdKey.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().getIdKey() != null ? c.getValue().getIdKey() : "N/A"
        ));
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));
        colFullName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().getFullName() != null ? c.getValue().getFullName() : ""
        ));
        colPhone.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().getPhone() != null ? c.getValue().getPhone() : ""
        ));
        colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().getEmail() != null ? c.getValue().getEmail() : ""
        ));
        colRole.setCellValueFactory(c -> {
            String type = c.getValue().getType();
            String displayRole;
            if (type == null) {
                displayRole = "Registered";
            } else {
                switch (type.toUpperCase()) {
                    case "ADMIN":
                        displayRole = "Admin";
                        break;
                    case "GENERAL":
                        displayRole = "Registered";
                        break;
                    case "GUEST":
                        displayRole = "Guest";
                        break;
                    default:
                        displayRole = type;
                }
            }
            return new javafx.beans.property.SimpleStringProperty(displayRole);
        });

        // Setup filtered list
        filteredUserList = new FilteredList<>(masterUserList, p -> true);
        userTable.setItems(filteredUserList);

        // Setup search listener
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        }

        loadUsers();
    }

    private void loadUsers() {
        try {
            masterUserList.clear();
            masterUserList.addAll(authService.getAllUsers());
            applyFilter();
        } catch (Exception e) {
            showMessage("Failed to load users: " + e.getMessage(), true);
        }
    }

    private void applyFilter() {
        String searchText = (txtSearch != null && txtSearch.getText() != null)
            ? txtSearch.getText().toLowerCase(Locale.ROOT).trim()
            : "";

        if (searchText.isEmpty()) {
            filteredUserList.setPredicate(user -> true);
        } else {
            filteredUserList.setPredicate(user -> {
                // Search by ID Key
                String idKey = user.getIdKey() != null ? user.getIdKey().toLowerCase(Locale.ROOT) : "";
                if (idKey.contains(searchText)) return true;

                // Search by Username
                String username = user.getUsername() != null ? user.getUsername().toLowerCase(Locale.ROOT) : "";
                if (username.contains(searchText)) return true;

                // Search by Full Name
                String fullName = user.getFullName() != null ? user.getFullName().toLowerCase(Locale.ROOT) : "";
                if (fullName.contains(searchText)) return true;

                // Search by Phone Number
                String phone = user.getPhone() != null ? user.getPhone().toLowerCase(Locale.ROOT) : "";
                if (phone.contains(searchText)) return true;

                // Search by Role (display name)
                String type = user.getType() != null ? user.getType().toUpperCase() : "GENERAL";
                String displayRole = "";
                switch (type) {
                    case "ADMIN":
                        displayRole = "admin";
                        break;
                    case "GENERAL":
                        displayRole = "registered";
                        break;
                    case "GUEST":
                        displayRole = "guest";
                        break;
                    default:
                        displayRole = type.toLowerCase(Locale.ROOT);
                }
                if (displayRole.contains(searchText)) return true;

                return false;
            });
        }
    }

    @FXML
    public void onClearSearch(ActionEvent e) {
        if (!requireAdmin()) return;
        if (txtSearch != null) {
            txtSearch.clear();
        }
        applyFilter();
    }

    @FXML
    public void onSetAdmin(ActionEvent e) {
        if (!requireAdmin()) return;
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showMessage("Please select a user first.", true);
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selectedUser.getType())) {
            showMessage("User is already an admin.", true);
            return;
        }

        try {
            authService.updateUserRole(selectedUser.getUsername(), "ADMIN");
            showMessage("User " + selectedUser.getUsername() + " is now an Admin.", false);
            loadUsers();
        } catch (Exception ex) {
            showMessage("Failed to update role: " + ex.getMessage(), true);
        }
    }

    @FXML
    public void onSetGeneral(ActionEvent e) {
        if (!requireAdmin()) return;
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showMessage("Please select a user first.", true);
            return;
        }

        if ("GENERAL".equalsIgnoreCase(selectedUser.getType())) {
            showMessage("User is already a General user.", true);
            return;
        }

        if (selectedUser.getUsername().equalsIgnoreCase(SessionManager.getCurrentUser().getUsername())) {
            showMessage("You cannot remove your own admin privileges.", true);
            return;
        }

        try {
            authService.updateUserRole(selectedUser.getUsername(), "GENERAL");
            showMessage("User " + selectedUser.getUsername() + " is now a General user.", false);
            loadUsers();
        } catch (Exception ex) {
            showMessage("Failed to update role: " + ex.getMessage(), true);
        }
    }

    @FXML
    public void onAddUser(ActionEvent e) {
        if (!requireAdmin()) return;
        // Navigate to the add user page
        App.setScene("/fxml/add-user.fxml", "VSAS - Add User");
    }

    @FXML
    public void onRemoveUser(ActionEvent e) {
        if (!requireAdmin()) return;
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showMessage("Please select a user to remove.", true);
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && selectedUser.getUsername().equalsIgnoreCase(currentUser.getUsername())) {
            showMessage("You cannot delete your own account.", true);
            return;
        }

        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete User: " + selectedUser.getUsername());
        confirmAlert.setContentText("Are you sure you want to delete this user?\n\n" +
                                    "Username: " + selectedUser.getUsername() + "\n" +
                                    "Full Name: " + selectedUser.getFullName() + "\n" +
                                    "Role: " + selectedUser.getType() + "\n\n" +
                                    "This action cannot be undone.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    authService.deleteUser(selectedUser.getUsername(), currentUser.getUsername());
                    showMessage("User " + selectedUser.getUsername() + " deleted successfully.", false);
                    loadUsers();
                } catch (Exception ex) {
                    showMessage("Failed to delete user: " + ex.getMessage(), true);
                }
            }
        });
    }

    @FXML
    public void onRefresh(ActionEvent e) {
        if (!requireAdmin()) return;
        loadUsers();
        showMessage("User list refreshed.", false);
    }

    @FXML
    public void onGoBack(ActionEvent e) {
        App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
    }

    private boolean requireAdmin() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getType())) return true;
        masterUserList.clear();
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
}
