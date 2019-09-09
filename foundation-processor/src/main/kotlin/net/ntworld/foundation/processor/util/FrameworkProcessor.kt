package net.ntworld.foundation.processor.util

internal object FrameworkProcessor {
    const val SETTINGS_PATH = "resources"
    const val SETTINGS_FILENAME = "foundation-settings.json"

    const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

    const val Contract = "net.ntworld.foundation.Contract"
    const val Aggregate = "net.ntworld.foundation.Aggregate"
    const val AbstractEventSourced = "net.ntworld.foundation.eventSourcing.AbstractEventSourced"

    const val Handler = "net.ntworld.foundation.Handler"
    const val Faked = "net.ntworld.foundation.Faked"
    const val Implementation = "net.ntworld.foundation.Implementation"

    const val EventSourced = "net.ntworld.foundation.eventSourcing.EventSourced"
    const val EventSourcing = "net.ntworld.foundation.eventSourcing.EventSourcing"
    const val EventSourcingMetadata = "net.ntworld.foundation.eventSourcing.EventSourcing.Metadata"
    const val EventSourcingEncrypted = "net.ntworld.foundation.eventSourcing.EventSourcing.Encrypted"
}