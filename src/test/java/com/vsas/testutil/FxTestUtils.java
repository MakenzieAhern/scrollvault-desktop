package com.vsas.testutil;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class FxTestUtils {
    private static volatile boolean started = false;

    private FxTestUtils() {}

    public static void initFx() {
        if (started) return;
        synchronized (FxTestUtils.class) {
            if (started) return;
            try {
                // Start JavaFX toolkit
                Platform.startup(() -> Platform.setImplicitExit(false));
            } catch (IllegalStateException ignored) {
                // already started
            }
            started = true;
        }
    }

    public static void runOnFxThreadAndWait(Runnable r) {
        initFx();
        if (Platform.isFxApplicationThread()) {
            r.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { r.run(); }
            catch (Throwable t) { failure.set(t); }
            finally { latch.countDown(); }
        });
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out waiting for FX task");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        Throwable problem = failure.get();
        if (problem instanceof Error error) throw error;
        if (problem instanceof RuntimeException exception) throw exception;
        if (problem != null) throw new RuntimeException(problem);
    }

    public static void runAndWait(Runnable r) {
        runOnFxThreadAndWait(r);
    }

    public static Object getField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field " + fieldName, e);
        }
    }
}
