package com.example

import com.example.docs.DocumentationContent
import com.example.ui.studio.StudioTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun documentationCoversVideoExtensionAndBattery() {
    assertTrue(DocumentationContent.sections.any { it.id == "video_extension_bridge" })
    assertTrue(DocumentationContent.sections.any { it.id == "battery_optimization" })
    assertTrue(DocumentationContent.sections.any { it.id == "compose_multiplatform_arch" })
    assertEquals(5, StudioTab.entries.size)
  }
}
