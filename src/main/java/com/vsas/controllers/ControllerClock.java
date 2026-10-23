package com.vsas.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Runs a page clock only while its window is showing, releasing it on navigation. */
final class ControllerClock {
    private ControllerClock() { }

    static void start(Label label) {
        if (label != null) new Binding(label);
    }

    private static final class Binding {
        private final Label label;
        private final Timeline timeline;
        private Window window;
        private final ChangeListener<Boolean> showingListener = (o, oldValue, showing) -> update();
        private final ChangeListener<Window> windowListener = (o, oldValue, newWindow) -> attachWindow(newWindow);

        Binding(Label label) {
            this.label = label;
            DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss");
            Runnable tick = () -> label.setText(LocalDateTime.now().format(format));
            tick.run();
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> tick.run()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            label.sceneProperty().addListener((o, previous, scene) -> {
                if (previous != null) previous.windowProperty().removeListener(windowListener);
                attachScene(scene);
            });
            attachScene(label.getScene());
        }

        private void attachScene(Scene scene) {
            if (scene != null) scene.windowProperty().addListener(windowListener);
            attachWindow(scene == null ? null : scene.getWindow());
        }

        private void attachWindow(Window newWindow) {
            if (window != null) window.showingProperty().removeListener(showingListener);
            window = newWindow;
            if (window != null) window.showingProperty().addListener(showingListener);
            update();
        }

        private void update() {
            if (window != null && window.isShowing() && label.getScene() != null) timeline.play();
            else timeline.stop();
        }
    }
}
