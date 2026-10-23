package com.vsas;

import com.vsas.testutil.FxTestUtils;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @Test
    void setScene_with_missing_fxml_throws_runtime() {
        FxTestUtils.runOnFxThreadAndWait(() -> {
            assertThrows(RuntimeException.class, () ->
                App.setScene("/fxml/__missing__.fxml", "Missing")
            );
        });
    }

    @Test
    void spawn_methods_populate_layers_without_stage() {
        FxTestUtils.runOnFxThreadAndWait(() -> {
            Pane blobLayer = new Pane();
            Pane particlesLayer = new Pane();
            App app = new App();
            invokePrivate(app, "spawnBlobs", blobLayer);
            invokePrivate(app, "spawnParticles", particlesLayer, 10);
            assertTrue(blobLayer.getChildren().size() > 0);
            assertEquals(10, particlesLayer.getChildren().size());
            app.stop();
        });
    }

    @Test
    void setScene_with_valid_but_no_layers_does_not_throw() {
        FxTestUtils.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            App app = new App();
            try {
                app.start(stage);
                assertDoesNotThrow(() -> App.setScene("/fxml/test_no_layers.fxml", "ScrollVault - No Layers"));
                assertEquals("ScrollVault - No Layers", stage.getTitle());
            } finally { app.stop(); stage.close(); }
        });
    }

    private static void invokePrivate(Object target, String method, Object... args) {
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) types[i] = args[i] instanceof Integer ? int.class : args[i].getClass();
            var m = App.class.getDeclaredMethod(method, types);
            m.setAccessible(true);
            m.invoke(target, args);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
