package com.callNest.app.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutoSaveNameBuilderTest {

    @Test fun prefixOnly_noSim_noSuffix() {
        val out = AutoSaveNameBuilder.build("callNest", false, null, "", "+919876543210")
        assertEquals("callNest +919876543210", out)
    }

    @Test fun prefix_simSlot0_emitsS1() {
        val out = AutoSaveNameBuilder.build("callNest", true, 0, "", "+919876543210")
        assertEquals("callNest-s1 +919876543210", out)
    }

    @Test fun prefix_simSlot1_emitsS2() {
        val out = AutoSaveNameBuilder.build("callNest", true, 1, "", "+919876543210")
        assertEquals("callNest-s2 +919876543210", out)
    }

    @Test fun prefix_withSuffix() {
        val out = AutoSaveNameBuilder.build("callNest", false, null, "-lead", "+919876543210")
        assertEquals("callNest +919876543210-lead", out)
    }

    @Test fun prefix_simAndSuffix() {
        val out = AutoSaveNameBuilder.build("callNest", true, 0, "-lead", "+919876543210")
        assertEquals("callNest-s1 +919876543210-lead", out)
    }

    @Test fun blankPrefix_fallsBackTocallNest() {
        val out = AutoSaveNameBuilder.build("   ", false, null, "", "+919876543210")
        assertEquals("callNest +919876543210", out)
    }

    @Test fun simTagOmittedWhenIncludeSimTagFalse() {
        val out = AutoSaveNameBuilder.build("CV", false, 0, "", "+447700900000")
        assertEquals("CV +447700900000", out)
    }

    @Test fun simTagOmittedWhenSlotNull() {
        val out = AutoSaveNameBuilder.build("CV", true, null, "", "+447700900000")
        assertEquals("CV +447700900000", out)
    }

    @Test fun longSuffix() {
        val long = "-".repeat(40) + "tag"
        val out = AutoSaveNameBuilder.build("CV", false, null, long, "+15551234")
        assertEquals("CV +15551234$long", out)
    }

    @Test fun specialCharsPreserved() {
        val out = AutoSaveNameBuilder.build("Çall✓", true, 1, " #1", "+919876543210")
        assertEquals("Çall✓-s2 +919876543210#1", out)
    }

    @Test fun formatSnapshot_encodesSettings() {
        val snap = AutoSaveNameBuilder.formatSnapshot("CV", true, "x")
        assertEquals("CV|sim=true|x", snap)
    }
}
