package com.callvault.app.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoSavePatternMatcherTest {

    @BeforeEach fun setup() = AutoSavePatternMatcher.clearCache()

    @Test fun matchesPlainPrefix() {
        assertTrue(AutoSavePatternMatcher.matches("callVault +919876543210", "callVault", false, ""))
    }

    @Test fun matchesPrefixWithS1Tag() {
        assertTrue(AutoSavePatternMatcher.matches("callVault-s1 +919876543210", "callVault", true, ""))
    }

    @Test fun matchesPrefixWithS2Tag() {
        assertTrue(AutoSavePatternMatcher.matches("callVault-s2 +919876543210", "callVault", true, ""))
    }

    @Test fun matchesWithSuffix() {
        assertTrue(AutoSavePatternMatcher.matches("callVault +919876543210-lead", "callVault", false, "-lead"))
    }

    @Test fun rejectsHumanRename() {
        assertFalse(AutoSavePatternMatcher.matches("Mom", "callVault", false, ""))
    }

    @Test fun rejectsNullOrBlank() {
        assertFalse(AutoSavePatternMatcher.matches(null, "callVault", false, ""))
        assertFalse(AutoSavePatternMatcher.matches("", "callVault", false, ""))
    }

    @Test fun toleratesExtraWhitespace() {
        assertTrue(AutoSavePatternMatcher.matches("  callVault +919876543210  ", "callVault", false, ""))
    }
}
