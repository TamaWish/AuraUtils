package com.lozaine.aurautils.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void newerRemoteVersionIsDetected() {
        assertTrue(UpdateChecker.compareVersions("1.3.0", "1.3.1") < 0);
        assertTrue(UpdateChecker.compareVersions("1.3.0", "v1.4.0") < 0);
        assertEquals(0, UpdateChecker.compareVersions("1.3.0", "v1.3.0"));
        assertTrue(UpdateChecker.compareVersions("1.3.1", "1.3.0") > 0);
    }

    @Test
    void parsesGithubLatestJsonFields() {
        String json = "{\"tag_name\":\"v1.4.0\",\"html_url\":\"https://github.com/TamaWish/AuraUtils/releases/tag/v1.4.0\"}";
        assertEquals("v1.4.0", UpdateChecker.firstJsonString(json, Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"")));
        assertEquals(
                "https://github.com/TamaWish/AuraUtils/releases/tag/v1.4.0",
                UpdateChecker.firstJsonString(json, Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"")));
    }
}
