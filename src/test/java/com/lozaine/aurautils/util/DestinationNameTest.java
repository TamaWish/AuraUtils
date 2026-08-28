package com.lozaine.aurautils.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DestinationNameTest {

    @Test
    void acceptsTrimmedSafeNames() {
        assertEquals("Spawn_2", DestinationName.parse(" Spawn_2 ").orElseThrow());
    }

    @Test
    void rejectsYamlAndFormattingCharacters() {
        assertFalse(DestinationName.parse("a.b").isPresent());
        assertFalse(DestinationName.parse("&cname").isPresent());
        assertFalse(DestinationName.parse("name with spaces").isPresent());
    }

    @Test
    void rejectsNamesLongerThanThirtyTwoCharacters() {
        assertFalse(DestinationName.parse("a".repeat(33)).isPresent());
        assertTrue(DestinationName.parse("a".repeat(32)).isPresent());
    }
}
