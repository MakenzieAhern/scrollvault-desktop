package com.vsas;

import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;

public class App extends Application {
    private final Random random = new Random();

    private static Stage primaryStage;
    private static final List<Animation> sceneAnimations = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try {
            setScene("/fxml/welcome.fxml", "VSAS - Welcome");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load welcome page", e);
        }
    }

    public static void setScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            stopSceneAnimations();
            primaryStage.setTitle(title.replace("VSAS", "ScrollVault"));
            primaryStage.setScene(scene);
            primaryStage.show();

            Pane blobLayer = (Pane) scene.lookup("#blobLayer");
            Pane particlesLayer = (Pane) scene.lookup("#particlesLayer");
            if (blobLayer != null) new App().spawnBlobs(blobLayer);
            if (particlesLayer != null) new App().spawnParticles(particlesLayer, 100);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set scene: " + fxmlPath, e);
        }
    }

    private void spawnBlobs(Pane layer) {
        layer.getChildren().clear();
        double w = layer.getWidth() <= 0 ? 1200 : layer.getWidth();
        double h = layer.getHeight() <= 0 ? 800 : layer.getHeight();
        for (int i = 0; i < 7; i++) {
            Circle blob = new Circle(240 + random.nextDouble() * 200);
            Color color;
            if (i % 3 == 0) color = Color.web("#00d2ff", 0.38);
            else if (i % 3 == 1) color = Color.web("#3a7bd5", 0.34);
            else color = Color.web("#6a11cb", 0.34);
            blob.setFill(color);
            blob.setEffect(new BoxBlur(35, 35, 3));
            blob.setTranslateX(random.nextDouble() * w);
            blob.setTranslateY(random.nextDouble() * h);
            layer.getChildren().add(blob);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(9 + random.nextInt(7)), blob);
            tt.setByX(-200 + random.nextDouble() * 400);
            tt.setByY(-200 + random.nextDouble() * 400);
            tt.setAutoReverse(true);
            tt.setCycleCount(Timeline.INDEFINITE);
            tt.setInterpolator(Interpolator.SPLINE(0.42, 0.0, 0.58, 1.0));
            sceneAnimations.add(tt);
            tt.play();
        }
    }

    private void spawnParticles(Pane layer, int count) {
        layer.getChildren().clear();
        double w = layer.getWidth() <= 0 ? 1200 : layer.getWidth();
        double h = layer.getHeight() <= 0 ? 800 : layer.getHeight();
        for (int i = 0; i < count; i++) {
            double r = 2.0 + random.nextDouble() * 4.0;
            Circle c = new Circle(r);
            Color tint = (i % 2 == 0) ? Color.web("#66aaff", 0.28) : Color.web("#33ccff", 0.26);
            c.setStroke(tint);
            c.setFill(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.12));
            c.setTranslateX(random.nextDouble() * w);
            c.setTranslateY(random.nextDouble() * h);
            layer.getChildren().add(c);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(9 + random.nextInt(7)), c);
            tt.setByX(-110 + random.nextDouble() * 220);
            tt.setByY(-110 + random.nextDouble() * 220);
            tt.setAutoReverse(true);
            tt.setCycleCount(Timeline.INDEFINITE);
            tt.setInterpolator(Interpolator.SPLINE(0.42, 0.0, 0.58, 1.0));
            sceneAnimations.add(tt);
            tt.play();
        }
    }

    private static void stopSceneAnimations() {
        sceneAnimations.forEach(Animation::stop);
        sceneAnimations.clear();
    }

    @Override
    public void stop() { stopSceneAnimations(); }

    public static void main(String[] args) { launch(args); }
}
