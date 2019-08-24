package net.ntworld.foundation.generator

import net.ntworld.foundation.generator.setting.EventSettings
import net.ntworld.foundation.generator.type.ClassInfo
import org.junit.Test

class EventDataMessageConverterGeneratorTest {
    @Test
    fun testGenerate() {
        val settings = EventSettings(
            name = "test.event.CreatedEvent",
            event = ClassInfo(
                packageName = "test.event",
                className = "CreatedEvent"
            ),
            implementation = ClassInfo(
                packageName = "test.event",
                className = "CreatedEvent"
            ),
            fields = emptyList(),
            type = "created",
            variant = 0
        )

        val result = EventDataMessageConverterGenerator.generate(settings)
        println(result.content)
    }
}