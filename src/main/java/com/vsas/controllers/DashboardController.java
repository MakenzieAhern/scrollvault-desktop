package com.vsas.controllers;

import com.vsas.security.SessionManager;
import com.vsas.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import com.vsas.models.User;

public class DashboardController {
    @FXML private Label userDisplayLabel;
    @FXML private Label dateTimeLabel;
    @FXML private Label userInfoLabel;
    @FXML private Label userTypeLabel;
    @FXML private javafx.scene.control.Button btnManageUsers;
    @FXML private javafx.scene.control.Button btnStatistics;

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        String username = (u != null && u.getUsername() != null) ? u.getUsername() : "guest";
        if (userDisplayLabel != null) userDisplayLabel.setText("Logged in as: " + username);
        if (userTypeLabel != null) {
            String typeRaw = (u != null && u.getType() != null) ? u.getType() : "GUEST";
            String displayType;
            switch (typeRaw.toUpperCase()) {
                case "ADMIN":
                    displayType = "Admin";
                    break;
                case "GENERAL":
                    displayType = "Registered";
                    break;
                default:
                    displayType = "Guest";
            }
            userTypeLabel.setText("User Type: " + displayType);
        }

        if (userInfoLabel != null) {
            String typeRaw = (u != null && u.getType() != null) ? u.getType().toUpperCase() : "GUEST";
            boolean isAdmin = "ADMIN".equals(typeRaw);
            boolean isGeneral = "GENERAL".equals(typeRaw);
            if (u != null && (isAdmin || isGeneral)) {
                String display = buildDisplayIdKey(u, isAdmin, isGeneral);
                if (display != null) {
                    userInfoLabel.setText("ID Key: " + display);
                    userInfoLabel.setManaged(true);
                    userInfoLabel.setVisible(true);
                } else {
                    userInfoLabel.setManaged(false);
                    userInfoLabel.setVisible(false);
                }
            } else {
                userInfoLabel.setManaged(false);
                userInfoLabel.setVisible(false);
            }
        }

        if (dateTimeLabel != null) {
            ControllerClock.start(dateTimeLabel);
        }

        // Show "Manage Users" and "View Statistics" buttons only for admins
        {
            boolean isAdmin = u != null && "ADMIN".equalsIgnoreCase(u.getType());
            if (btnManageUsers != null) {
                btnManageUsers.setVisible(isAdmin);
                btnManageUsers.setManaged(isAdmin);
            }
            if (btnStatistics != null) {
                btnStatistics.setVisible(isAdmin);
                btnStatistics.setManaged(isAdmin);
            }
        }
    }

    private boolean isAdmin() {
        User user = SessionManager.getCurrentUser();
        return user != null && "ADMIN".equalsIgnoreCase(user.getType());
    }

    private String buildDisplayIdKey(User u, boolean isAdmin, boolean isGeneral) {
        String idKey = u.getIdKey();
        if (idKey != null) {
            String lower = idKey.toLowerCase();
            if (lower.matches("admin\\d{4}") || lower.matches("[a-z]{4}\\d{4}")) {
                return idKey;
            }
        }
        String digits = extractTrailingDigits(idKey);
        if (digits == null) digits = "1000";
        if (digits.length() > 4) digits = digits.substring(digits.length() - 4);
        if (digits.length() < 4) {
            try {
                digits = String.format("%04d", Integer.parseInt(digits));
            } catch (NumberFormatException ex) {
                digits = "1000";
            }
        }
        if (isAdmin) {
            return "admin" + digits;
        }
        if (isGeneral) {
            String prefix = buildNamePrefix(u);
            return prefix + digits;
        }
        return null;
    }

    private String extractTrailingDigits(String s) {
        if (s == null || s.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }
        if (sb.length() == 0) return null;
        return sb.reverse().toString();
    }

    private String buildNamePrefix(User u) {
        String fullName = u.getFullName();
        String username = u.getUsername();
        String first = null, last = null;
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 1) first = parts[0];
            if (parts.length >= 2) last = parts[parts.length - 1];
        }
        String a = first == null ? null : first.toLowerCase();
        String b = last == null ? null : last.toLowerCase();
        String ab;
        if (a != null && b != null) {
            ab = take(a, 2) + take(b, 2);
        } else if (a != null) {
            ab = take(a, 4);
        } else if (b != null) {
            ab = take(b, 4);
        } else {
            String un = username == null ? "user" : username.toLowerCase();
            ab = take(un, 4);
        }
        if (ab.length() < 4) {
            StringBuilder pad = new StringBuilder(ab);
            while (pad.length() < 4) pad.append('x');
            ab = pad.toString();
        } else if (ab.length() > 4) {
            ab = ab.substring(0, 4);
        }
        return ab;
    }

    private String take(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }
    @FXML
    private void onUpdateProfile(ActionEvent e) {
        try {
            App.setScene("/fxml/profile.fxml", "VSAS - Update Profile");
        } catch (RuntimeException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Failed to open Profile page");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onGoBack(ActionEvent e) {
        App.setScene("/fxml/dashboard.fxml", "VSAS - Dashboard");
    }

    @FXML
    private void onLogout(ActionEvent e) {
        SessionManager.logout();
        App.setScene("/fxml/login.fxml", "VSAS - Login");
    }

    @FXML
    private void onBrowseScrolls(ActionEvent e) {
        App.setScene("/fxml/scrolls.fxml", "VSAS - Browse Scrolls");
    }

    @FXML
    private void onManageUsers(ActionEvent e) {
        if (!isAdmin()) return;
        App.setScene("/fxml/admin-panel.fxml", "VSAS - Manage Users");
    }

    @FXML
    private void onViewStatistics(ActionEvent e) {
        if (!isAdmin()) return;
        App.setScene("/fxml/statistics.fxml", "VSAS - Statistics");
    }
}
