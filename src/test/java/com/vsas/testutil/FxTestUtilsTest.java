package com.vsas.testutil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FxTestUtilsTest {
    @Test void assertionFailureReachesJUnit() {
        AssertionError original = new AssertionError("must fail");
        assertSame(original, assertThrows(AssertionError.class,
            () -> FxTestUtils.runAndWait(() -> { throw original; })));
    }
    @Test void runtimeFailureReachesJUnit() {
        IllegalStateException original = new IllegalStateException("must fail");
        assertSame(original, assertThrows(IllegalStateException.class,
            () -> FxTestUtils.runAndWait(() -> { throw original; })));
    }
}
