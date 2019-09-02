package net.ntworld.foundation.generator.setting

import kotlinx.serialization.Serializable
import net.ntworld.foundation.generator.type.ClassInfo

@Serializable
data class ImplementationSetting(
    val implementation: ClassInfo,
    val contract: ClassInfo,
    val type: Type
) {
    enum class Type {
        Aggregate,
        State,
        ReceivedData,
        Event,
        Command,
        Query
    }
}
