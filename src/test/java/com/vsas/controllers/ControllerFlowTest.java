package com.vsas.controllers;

import com.vsas.App;
import com.vsas.models.Scroll;
import com.vsas.models.User;
import com.vsas.security.SessionManager;
import com.vsas.services.AuthService;
import com.vsas.services.ScrollService;
import com.vsas.testutil.FxTestUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real FXML/controller wiring with isolated JSON and uploaded files. */
class ControllerFlowTest {
    @TempDir Path directory;
    private String previousDataDir;
    private String previousTestMode;
    private AuthService auth;
    private ScrollService scrolls;
    private User admin;
    private User reader;
    private Stage stage;

    @BeforeEach
    void setUp() throws Exception {
        previousDataDir = System.getProperty("scrollvault.dataDir");
        previousTestMode = System.getProperty("test.mode");
        System.setProperty("scrollvault.dataDir", directory.toString());
        System.setProperty("test.mode", "true");
        auth = new AuthService(directory.resolve("users.json"));
        admin = auth.register("admin1000", "admin", "Demo Admin", "admin@example.com", "0400000000", "admin123", "ADMIN");
        reader = auth.register("read1000", "reader", "Demo Reader", "reader@example.com", "0400000001", "reader123", "GENERAL");
        Files.writeString(directory.resolve("scrolls.json"), "[]");
        scrolls = new ScrollService();
        Path text = directory.resolve("alpha.txt");
        Files.writeString(text, "Alpha document\nUTF-8: café");
        scrolls.add(new Scroll("S-001", reader.getIdKey(), "Alpha Notes", "TEXT", ""), text.toFile());
        scrolls.add(new Scroll("S-002", admin.getIdKey(), "Beta Notes", "TEXT", ""), text.toFile());
        Path png = directory.resolve("photo.png");
        Files.write(png, java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII="));
        scrolls.add(new Scroll("S-003", reader.getIdKey(), "Field Photo", "IMAGE", ""), png.toFile());
        SessionManager.logout();
        FxTestUtils.runAndWait(() -> {
            stage = new Stage();
            try {
                var field = App.class.getDeclaredField("primaryStage");
                field.setAccessible(true);
                field.set(null, stage);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
    }

    @AfterEach
    void tearDown() {
        FxTestUtils.runAndWait(() -> {
            for (Window window : new ArrayList<>(Window.getWindows())) window.hide();
            if (stage != null) stage.close();
        });
        SessionManager.logout();
        restore("scrollvault.dataDir", previousDataDir);
        restore("test.mode", previousTestMode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"welcome", "login", "register", "guest", "dashboard", "profile", "scrolls", "my-scrolls", "admin-panel", "add-user", "statistics", "about", "contact"})
    void everyScreenLoadsWithRealFXML(String screen) {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load(screen);
            assertNotNull(view.controller());
            assertNotNull(view.root());
            Label clock = view.control("dateTimeLabel", Label.class);
            assertFalse(clock.getText().isBlank(), "clock initializes before its first timer tick");
        });
    }

    @Test
    void guestBrowseHasPreviewButCannotDownloadUploadOrOpenOwnLibrary() {
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            ScrollsController controller = (ScrollsController) view.controller();
            assertEquals(3, view.table("table").getItems().size());
            assertTrue(view.control("textPreview", TextArea.class).getText().contains("Alpha document"));
            assertFalse(view.control("btnMyScrolls", Button.class).isVisible());
            controller.onDownload(new ActionEvent());
            assertTrue(view.message().contains("log in"));
            assertNull(controller.getLastSavedPath());
            controller.onUpload(new ActionEvent());
            assertTrue(view.message().contains("log in"));
            controller.onMyScrolls(new ActionEvent());
            assertTrue(view.message().contains("log in"));
        });
        assertEquals(0, scrolls.list().get(0).getDownloadCount());
    }

    @Test
    void guestModeExplicitlyEndsPreviousAuthenticatedSession() {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("login");
            ((LoginController) view.controller()).onContinueAsGuest(new ActionEvent());
            assertNull(SessionManager.getCurrentUser());
            assertEquals("Logged in as: guest", ((Label) stage.getScene().lookup("#userDisplayLabel")).getText());
        });
    }

    @Test
    void loggedInReaderCanDownloadExactBytesAndCounterIncrements() {
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            ScrollsController controller = (ScrollsController) view.controller();
            controller.onDownload(new ActionEvent());
            assertEquals(Alert.AlertType.INFORMATION, controller.getLastAlertType());
            assertEquals("Alpha Notes.txt", controller.getLastSuggestedName());
            try {
                Path downloaded = Path.of(controller.getLastSavedPath());
                assertEquals("Alpha document\nUTF-8: café", Files.readString(downloaded));
                Files.delete(downloaded);
            } catch (Exception e) { fail(e); }
        });
        assertEquals(1, scrolls.list().get(0).getDownloadCount());
    }

    @ParameterizedTest
    @CsvSource({"Photo,/scrolls/photo.jpg,IMAGE,Photo.jpg", "Photo.JPG,/scrolls/photo.jpg,IMAGE,Photo.JPG", "Paper,/scrolls/paper.pdf,PDF,Paper.pdf", "notes.txt,/scrolls/notes.txt,TEXT,notes.txt", "a:b,/scrolls/a.txt,TEXT,a_b.txt"})
    void downloadNamePreservesFileFormatWithoutDuplicateExtension(String title, String path, String type, String expected) {
        assertEquals(expected, ScrollsController.suggestedDownloadName(new Scroll("S-001", title, type, path)));
    }

    @Test
    void downloadWithNoSelectionReportsMessageWithoutCreatingFile() {
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            view.table("table").getSelectionModel().clearSelection();
            ScrollsController controller = (ScrollsController) view.controller();
            controller.onDownload(new ActionEvent());
            assertEquals("Select a scroll first.", controller.getLastAlertText());
            assertNull(controller.getLastSavedPath());
        });
    }

    @Test
    void missingResourceDoesNotReportSuccessfulDownloadOrIncrementCounter() throws Exception {
        Scroll missing = scrolls.list().get(0);
        missing.setResourcePath("/scrolls/missing.txt");
        scrolls.update(missing);
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            ScrollsController controller = (ScrollsController) view.controller();
            controller.onDownload(new ActionEvent());
            assertEquals(Alert.AlertType.ERROR, controller.getLastAlertType());
            assertEquals("Resource not found.", controller.getLastAlertText());
            assertNull(controller.getLastSavedPath());
        });
        assertEquals(0, scrolls.list().get(0).getDownloadCount());
    }

    @Test
    void filtersCombineKeywordTypeAndDateThenClearRestoresCatalogue() {
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            view.control("txtKeyword", TextField.class).setText("notes");
            view.combo("cmbType").setValue("TEXT");
            assertEquals(2, view.table("table").getItems().size());
            view.control("txtKeyword", TextField.class).setText("s-002");
            assertEquals("S-002", view.table("table").getItems().get(0).getID());
            view.control("dpFrom", DatePicker.class).setValue(LocalDate.now().plusDays(1));
            assertTrue(view.table("table").getItems().isEmpty());
            ((ScrollsController) view.controller()).onClearFilters(new ActionEvent());
            assertEquals(3, view.table("table").getItems().size());
            view.combo("cmbType").setValue("IMAGE");
            assertEquals(1, view.table("table").getItems().size());
            assertEquals("IMAGE", view.table("table").getItems().get(0).getType());
        });
    }

    @Test
    void sortDirectionChangesActualTableOrder() {
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            view.combo("cmbSort").setValue("Title");
            assertEquals("Alpha Notes", view.table("table").getItems().get(0).getTitle());
            view.control("chkAsc", CheckBox.class).setSelected(false);
            assertEquals("Field Photo", view.table("table").getItems().get(0).getTitle());
        });
    }

    @Test
    void myScrollsContainsOnlyCurrentOwnersUploads() {
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("my-scrolls");
            assertEquals(2, view.table("table").getItems().size());
            assertTrue(view.table("table").getItems().stream().allMatch(s -> reader.getIdKey().equals(s.getUploaderID())));
        });
    }

    @Test
    void guestCannotExposeOtherUsersLibraryThroughMyScrollsFXML() {
        FxTestUtils.runAndWait(() -> assertTrue(load("my-scrolls").table("table").getItems().isEmpty()));
    }

    @Test
    void editorAndDeleteRejectOtherOwnersWithoutOpeningConfirmationDialogs() {
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("scrolls");
            view.table("table").getSelectionModel().select(1);
            ScrollsController controller = (ScrollsController) view.controller();
            controller.onEdit(new ActionEvent());
            assertTrue(view.message().contains("only edit your own"));
            controller.onDelete(new ActionEvent());
            assertTrue(view.message().contains("only delete your own"));
        });
        assertEquals(3, scrolls.list().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"guest", "reader", "admin"})
    void dashboardAdministrativeControlsMatchRole(String role) {
        if (!role.equals("guest")) SessionManager.login(role.equals("admin") ? admin : reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("dashboard");
            assertEquals(role.equals("admin"), view.control("btnManageUsers", Button.class).isVisible());
            assertEquals(role.equals("admin"), view.control("btnStatistics", Button.class).isVisible());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin-panel", "add-user", "statistics"})
    void restrictedScreensDenyGeneralUsers(String screen) {
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> assertTrue(load(screen).message().contains("Access denied")));
    }

    @Test
    void adminPanelSearchAndRoleMutationPersistToStorage() {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("admin-panel");
            view.control("txtSearch", TextField.class).setText("reader");
            TableView<User> table = view.users();
            assertEquals(1, table.getItems().size());
            table.getSelectionModel().select(0);
            ((AdminPanelController) view.controller()).onSetAdmin(new ActionEvent());
            assertTrue(view.message().contains("is now an Admin"));
            table.getSelectionModel().select(0);
            ((AdminPanelController) view.controller()).onSetGeneral(new ActionEvent());
            assertTrue(view.message().contains("is now a General"));
        });
        try { assertEquals("GENERAL", auth.login("reader", "reader123").orElseThrow().getType()); }
        catch (Exception e) { fail(e); }
    }

    @Test
    void adminPanelRejectsStaleActionsAfterLogout() {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("admin-panel");
            view.users().getSelectionModel().select(1);
            SessionManager.logout();
            AdminPanelController controller = (AdminPanelController) view.controller();
            controller.onSetAdmin(new ActionEvent());
            controller.onSetGeneral(new ActionEvent());
            controller.onRemoveUser(new ActionEvent());
            controller.onAddUser(new ActionEvent());
            controller.onRefresh(new ActionEvent());
            assertTrue(view.message().contains("Access denied"));
            assertTrue(view.users().getItems().isEmpty());
        });
        try { assertEquals("GENERAL", auth.login("reader", "reader123").orElseThrow().getType()); }
        catch (Exception e) { fail(e); }
    }

    @Test
    void adminCannotDemoteOrDeleteOwnAccount() {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("admin-panel");
            view.users().getSelectionModel().select(0);
            AdminPanelController controller = (AdminPanelController) view.controller();
            controller.onSetGeneral(new ActionEvent());
            assertTrue(view.message().contains("own admin privileges"));
            controller.onRemoveUser(new ActionEvent());
            assertTrue(view.message().contains("own account"));
        });
    }

    @Test
    void addUserRejectsStaleSessionBeforeProcessingForm() {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("add-user");
            fillRegistration(view);
            SessionManager.logout();
            ((AddUserController) view.controller()).onAddUser(new ActionEvent());
            assertTrue(view.message().contains("Access denied"));
        });
        try { assertEquals(2, auth.getAllUsers().size()); } catch (Exception e) { fail(e); }
    }

    @Test
    void adminCanCreateUserThroughRealForm() {
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("add-user");
            fillRegistration(view);
            ((AddUserController) view.controller()).onAddUser(new ActionEvent());
            assertTrue(view.message().contains("created successfully"));
            assertTrue(view.control("usernameField", TextField.class).getText().isEmpty());
        });
        try { assertTrue(auth.login("newreader", "secret123").isPresent()); } catch (Exception e) { fail(e); }
    }

    @Test
    void statisticsSummariesFiltersAndRefreshReflectPersistedCounts() throws Exception {
        scrolls.incrementDownloadCount("S-001");
        scrolls.incrementViewCount("S-001");
        SessionManager.login(admin);
        FxTestUtils.runAndWait(() -> {
            View view = load("statistics");
            assertEquals("3", view.control("lblTotalScrolls", Label.class).getText());
            assertEquals("3", view.control("lblTotalUploads", Label.class).getText());
            assertEquals("1", view.control("lblTotalDownloads", Label.class).getText());
            assertEquals("1", view.control("lblTotalViews", Label.class).getText());
            StatisticsController controller = (StatisticsController) view.controller();
            view.control("txtFilterUploader", TextField.class).setText("reader");
            controller.onApplyFilters(new ActionEvent());
            assertEquals(2, view.table("tblScrollStats").getItems().size());
            view.control("txtFilterScrollId", TextField.class).setText("S-003");
            controller.onApplyFilters(new ActionEvent());
            assertEquals(1, view.table("tblScrollStats").getItems().size());
            view.control("dpFilterTo", DatePicker.class).setValue(LocalDate.now().minusDays(1));
            controller.onApplyFilters(new ActionEvent());
            assertEquals(0, view.table("tblScrollStats").getItems().size());
            controller.onClearFilters(new ActionEvent());
            assertEquals(3, view.table("tblScrollStats").getItems().size());
            SessionManager.logout();
            controller.onRefresh(new ActionEvent());
            assertTrue(view.table("tblScrollStats").getItems().isEmpty());
            assertTrue(view.message().contains("Access denied"));
        });
    }

    @ParameterizedTest
    @CsvSource({"usernameField,,fill in all details", "usernameField,overlongusername,more than 10", "usernameField,has space,letters", "phoneField,12ab,Phone number", "confirmField,different,do not match", "passwordField,short,do not match"})
    void registrationValidationUsesRealControls(String field, String value, String expectedMessage) {
        FxTestUtils.runAndWait(() -> {
            View view = load("register");
            fillRegistration(view);
            view.control(field, TextField.class).setText(value == null ? "" : value);
            invoke(view.controller(), "onRegister");
            assertTrue(view.message().contains(expectedMessage), view.message());
        });
    }

    @Test
    void registrationCreatesGeneralUserAndProfileUpdateChangesLogin() {
        FxTestUtils.runAndWait(() -> {
            View view = load("register");
            fillRegistration(view);
            invoke(view.controller(), "onRegister");
            assertTrue(view.message().contains("Registration successful"));
            try {
                User user = auth.login("newreader", "secret123").orElseThrow();
                assertEquals("GENERAL", user.getType());
                SessionManager.login(user);
            } catch (Exception e) { fail(e); }
            View profile = load("profile");
            assertEquals("newreader", profile.control("usernameField", TextField.class).getText());
            profile.control("usernameField", TextField.class).setText("renamed");
            profile.control("newPasswordField", TextField.class).setText("changed123");
            profile.control("confirmPasswordField", TextField.class).setText("changed123");
            invoke(profile.controller(), "onSave");
            assertTrue(profile.message().contains("saved successfully"));
            assertEquals("renamed", SessionManager.getCurrentUser().getUsername());
        });
        try {
            assertTrue(auth.login("renamed", "changed123").isPresent());
            assertTrue(auth.login("newreader", "secret123").isEmpty());
        } catch (Exception e) { fail(e); }
    }

    @ParameterizedTest
    @CsvSource({"usernameField,,fill in all details", "usernameField,has space,letters", "phoneField,invalid,Phone number", "newPasswordField,secret123,do not match"})
    void profileValidationRejectsInvalidUpdates(String field, String value, String expectedMessage) {
        SessionManager.login(reader);
        FxTestUtils.runAndWait(() -> {
            View view = load("profile");
            view.control(field, TextField.class).setText(value == null ? "" : value);
            invoke(view.controller(), "onSave");
            assertTrue(view.message().contains(expectedMessage), view.message());
        });
    }

    @Test
    void loginShowsInvalidBannerThenAuthenticatesAndNavigates() {
        FxTestUtils.runAndWait(() -> {
            View view = load("login");
            view.control("usernameField", TextField.class).setText("reader");
            view.control("passwordField", TextField.class).setText("wrong");
            ((LoginController) view.controller()).onLogin(new ActionEvent());
            assertTrue(view.message().contains("Invalid credentials"));
            assertNull(SessionManager.getCurrentUser());
            view.control("passwordField", TextField.class).setText("reader123");
            ((LoginController) view.controller()).onLogin(new ActionEvent());
            assertEquals("reader", SessionManager.getCurrentUser().getUsername());
            assertNotNull(stage.getScene().lookup("#userDisplayLabel"));
        });
    }

    private View load(String screen) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + screen + ".fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            return new View(root, loader.getController(), loader.getNamespace());
        } catch (Exception e) { throw new RuntimeException("Unable to load " + screen, e); }
    }

    private void fillRegistration(View view) {
        view.control("usernameField", TextField.class).setText("newreader");
        view.control("firstNameField", TextField.class).setText("New");
        view.control("lastNameField", TextField.class).setText("Reader");
        view.control("emailField", TextField.class).setText("new@example.com");
        view.control("phoneField", TextField.class).setText("0400000002");
        view.control("passwordField", TextField.class).setText("secret123");
        view.control("confirmField", TextField.class).setText("secret123");
    }

    private static void invoke(Object controller, String name) {
        try {
            Method method = controller.getClass().getDeclaredMethod(name, ActionEvent.class);
            method.setAccessible(true);
            method.invoke(controller, new ActionEvent());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void restore(String key, String previous) {
        if (previous == null) System.clearProperty(key); else System.setProperty(key, previous);
    }

    private record View(Parent root, Object controller, Map<String, Object> namespace) {
        <T> T control(String name, Class<T> type) { return type.cast(namespace.get(name)); }
        String message() { return control("messageLabel", Label.class).getText(); }
        @SuppressWarnings("unchecked") TableView<Scroll> table(String name) { return (TableView<Scroll>) namespace.get(name); }
        @SuppressWarnings("unchecked") TableView<User> users() { return (TableView<User>) namespace.get("userTable"); }
        @SuppressWarnings("unchecked") ComboBox<String> combo(String name) { return (ComboBox<String>) namespace.get(name); }
    }
}
