package com.vsas;

import com.vsas.models.Scroll;
import com.vsas.models.User;
import com.vsas.security.SessionManager;
import com.vsas.services.AuthService;
import com.vsas.testutil.FxTestUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Captures production FXML views with synthetic data, rather than a mockup. */
@EnabledIfSystemProperty(named = "scrollvault.captureScreenshots", matches = "true")
class PortfolioScreenshotsTest {
    @TempDir Path temporaryDirectory;

    @Test
    void capturesRealPortfolioViews() throws IOException {
        Path data = temporaryDirectory.resolve("demo");
        DemoData.seed(data);
        Path images = Path.of(System.getProperty("scrollvault.screenshotDir", "docs/images"));
        Files.createDirectories(images);
        String previousDataDirectory = System.getProperty("scrollvault.dataDir");
        User previousUser = SessionManager.getCurrentUser();
        try {
            System.setProperty("scrollvault.dataDir", data.toString());
            SessionManager.logout();
            capture("login", images.resolve("login.png"), scene -> {
                assertEquals("", ((TextField) scene.lookup("#usernameField")).getText());
                assertNotNull(scene.lookup("#loginButton"));
            });

            User admin = new AuthService(data.resolve("users.json"))
                    .login("admin", "admin123").orElseThrow();
            SessionManager.login(admin);
            capture("dashboard", images.resolve("dashboard.png"), scene -> {
                assertTrue(((Label) scene.lookup("#userDisplayLabel")).getText().contains("admin"));
                assertTrue(scene.lookup("#btnManageUsers").isVisible());
            });
            capture("scrolls", images.resolve("library.png"), scene -> {
                @SuppressWarnings("unchecked")
                TableView<Scroll> table = (TableView<Scroll>) scene.lookup("#table");
                assertEquals(3, table.getItems().size());
                table.getSelectionModel().selectFirst();
                assertTrue(((TextArea) scene.lookup("#textPreview")).getText()
                        .startsWith("Welcome to ScrollVault"));
            });
            capture("statistics", images.resolve("statistics.png"), scene -> {
                assertEquals("3", ((Label) scene.lookup("#lblTotalScrolls")).getText());
                assertEquals("0", ((Label) scene.lookup("#lblTotalDownloads")).getText());
                assertEquals(3, ((TableView<?>) scene.lookup("#tblScrollStats")).getItems().size());
            });
        } finally {
            if (previousDataDirectory == null) System.clearProperty("scrollvault.dataDir");
            else System.setProperty("scrollvault.dataDir", previousDataDirectory);
            if (previousUser == null) SessionManager.logout();
            else SessionManager.login(previousUser);
        }
    }

    private static void capture(String view, Path output, Consumer<Scene> verify) {
        FxTestUtils.runAndWait(() -> {
            Stage stage = new Stage();
            try {
                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                        App.class.getResource("/fxml/" + view + ".fxml")));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(Objects.requireNonNull(
                        App.class.getResource("/css/style.css")).toExternalForm());
                stage.setTitle("ScrollVault — " + view);
                stage.setScene(scene);
                stage.show();
                root.applyCss();
                root.layout();
                verify.accept(scene);
                root.applyCss();
                root.layout();

                WritableImage snapshot = scene.snapshot(null);
                int width = (int) snapshot.getWidth();
                int height = (int) snapshot.getHeight();
                // Window managers may constrain the requested scene to the available display.
                assertTrue(width >= 1000, "Screenshot must retain a usable desktop viewport");
                assertTrue(height >= 700, "Screenshot must retain a usable desktop viewport");
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                var pixels = snapshot.getPixelReader();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, pixels.getArgb(x, y));
                    }
                }
                assertTrue(ImageIO.write(image, "png", output.toFile()));
                assertTrue(Files.size(output) > 10_000, "Screenshot should contain the rendered interface");
            } catch (IOException exception) {
                throw new RuntimeException("Unable to capture " + view, exception);
            } finally {
                stage.close();
                stage.setScene(null);
            }
        });
    }
}
