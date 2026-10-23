package com.vsas;

import com.vsas.testutil.FxTestUtils;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppLifecycleTest {
    @Test
    void startsWelcomePageAndNavigatesToLogin() {
        FxTestUtils.runAndWait(() -> {
            Stage stage = new Stage();
            App app = new App();
            try {
                app.start(stage);
                assertTrue(stage.isShowing());
                assertEquals("ScrollVault - Welcome", stage.getTitle());
                assertNotNull(stage.getScene().lookup("#getStartedBtn"));
                App.setScene("/fxml/login.fxml", "ScrollVault - Login");
                assertEquals("ScrollVault - Login", stage.getTitle());
            } finally { app.stop(); stage.close(); }
        });
    }
}
