package com.callNest.app.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoSavePatternMatcherTest {

    @BeforeEach fun setup() = AutoSavePatternMatcher.clearCache()

    @Test fun matchesPlainPrefix() {
        assertTrue(AutoSavePatternMatcher.matches("callNest +919876543210", "callNest", false, ""))
    }

    @Test fun matchesPrefixWithS1Tag() {
        assertTrue(AutoSavePatternMatcher.matches("callNest-s1 +919876543210", "callNest", true, ""))
    }

    @Test fun matchesPrefixWithS2Tag() {
        assertTrue(AutoSavePatternMatcher.matches("callNest-s2 +919876543210", "callNest", true, ""))
    }

    @Test fun matchesWithSuffix() {
        assertTrue(AutoSavePatternMatcher.matches("callNest +919876543210-lead", "callNest", false, "-lead"))
    }

    @Test fun rejectsHumanRename() {
        assertFalse(AutoSavePatternMatcher.matches("Mom", "callNest", false, ""))
    }

    @Test fun rejectsNullOrBlank() {
        assertFalse(AutoSavePatternMatcher.matches(null, "callNest", false, ""))
        assertFalse(AutoSavePatternMatcher.matches("", "callNest", false, ""))
    }

    @Test fun toleratesExtraWhitespace() {
        assertTrue(AutoSavePatternMatcher.matches("  callNest +919876543210  ", "callNest", false, ""))
    }
}
