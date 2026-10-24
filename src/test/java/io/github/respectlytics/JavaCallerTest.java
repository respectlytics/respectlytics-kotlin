package io.github.respectlytics;

import static org.junit.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Java callers: track(eventName) still compiles and runs as a static call, and
 * track(eventName, properties) takes a plain Java map, or null.
 */
public class JavaCallerTest {

    @Test
    public void trackIsCallableFromJavaWithAndWithoutProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("paywall", "b");
        properties.put("price", 7.5);

        // Not configured: each call reaches the SDK's own check, so each overload exists
        assertThrows(IllegalStateException.class, () -> Respectlytics.track("app_opened"));
        assertThrows(IllegalStateException.class, () -> Respectlytics.track("paywall_viewed", properties));
        assertThrows(IllegalStateException.class, () -> Respectlytics.track("paywall_viewed", null));
    }
}
