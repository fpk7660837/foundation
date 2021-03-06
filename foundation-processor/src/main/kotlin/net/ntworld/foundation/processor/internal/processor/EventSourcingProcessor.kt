package net.ntworld.foundation.processor.internal.processor

import net.ntworld.foundation.eventSourcing.EventSourcing
import net.ntworld.foundation.generator.*
import net.ntworld.foundation.generator.setting.EventSourcingSetting
import net.ntworld.foundation.generator.type.ClassInfo
import net.ntworld.foundation.generator.type.EventField
import net.ntworld.foundation.processor.internal.FoundationProcessorException
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement

internal class EventSourcingProcessor : Processor {
    override val annotations: List<Class<out Annotation>> = listOf(
        EventSourcing::class.java,
        EventSourcing.Encrypted::class.java,
        EventSourcing.Metadata::class.java
    )

    private data class CollectedEventField(
        val name: String,
        val metadata: Boolean,
        val encrypted: Boolean,
        val faked: String
    )

    private data class CollectedEvent(
        val packageName: String,
        val className: String,
        val fields: MutableMap<String, CollectedEventField>,
        val type: String,
        val variant: Int
    )

    private val data: MutableMap<String, CollectedEvent> = mutableMapOf()

    override fun startProcess(settings: GeneratorSettings) {
        data.clear()
        settings.eventSourcings.forEach { item ->
            val fields = mutableMapOf<String, CollectedEventField>()

            item.fields.forEach {
                fields[it.name] = CollectedEventField(
                    name = it.name,
                    metadata = it.metadata,
                    encrypted = it.encrypted,
                    faked = it.faked
                )
            }
            data[item.name] = CollectedEvent(
                packageName = item.event.packageName,
                className = item.event.className,
                type = item.type,
                variant = item.variant,
                fields = fields
            )
        }
    }

    override fun applySettings(settings: GeneratorSettings): GeneratorSettings {
        val events = data.values.map {
            val fields = it.fields.values.map {
                EventField(name = it.name, metadata = it.metadata, encrypted = it.encrypted, faked = it.faked)
            }
            EventSourcingSetting(
                event = ClassInfo(packageName = it.packageName, className = it.className),
                // TODO: find implementation name
                implementation = ClassInfo(packageName = it.packageName, className = it.className),
                fields = fields,
                type = it.type,
                variant = it.variant,
                // TODO: find hasSecondConstructor if implementation != event
                hasSecondConstructor = false
            )
        }

        return settings.copy(eventSourcings = events.toList())
    }

    override fun shouldProcess(
        annotation: Class<out Annotation>,
        element: Element,
        processingEnv: ProcessingEnvironment,
        roundEnv: RoundEnvironment
    ): Boolean {
        return when (annotation) {
            EventSourcing::class.java -> {
                element.kind.isClass
            }

            EventSourcing.Encrypted::class.java, EventSourcing.Metadata::class.java -> {
                element.kind == ElementKind.FIELD && element.enclosingElement.kind == ElementKind.CLASS
            }

            else -> false
        }
    }

    override fun process(
        annotation: Class<out Annotation>,
        elements: List<Element>,
        processingEnv: ProcessingEnvironment,
        roundEnv: RoundEnvironment
    ) {
        when (annotation) {
            EventSourcing::class.java -> {
                processElementsAnnotatedByEventSourcing(elements)
            }
            EventSourcing.Encrypted::class.java -> {
                processElementsAnnotatedByEncrypted(elements)
            }
            EventSourcing.Metadata::class.java -> {
                processElementsAnnotatedByMetadata(elements)
            }
        }
    }

    private fun processElementsAnnotatedByEventSourcing(elements: List<Element>) {
        elements.forEach {
            val packageName = this.getPackageNameOfEvent(it)
            val className = it.simpleName.toString()
            val key = "$packageName.$className"
            initEventSettingsIfNeeded(packageName, className)

            val annotation = it.getAnnotation(EventSourcing::class.java)
            it.enclosedElements
                .filter {
                    it.kind == ElementKind.FIELD && !BLACKLIST.contains(it.simpleName.toString())
                }
                .forEach {
                    initEventFieldIfNeeded(packageName, className, it.simpleName.toString())
                }

            data[key] = data[key]!!.copy(
                type = annotation.type,
                variant = annotation.variant
            )
        }
    }

    private fun processElementsAnnotatedByEncrypted(elements: List<Element>) {
        elements.forEach {
            this.collectEventField(it) { item ->
                item.copy(
                    encrypted = true,
                    faked = it.getAnnotation(EventSourcing.Encrypted::class.java).faked
                )
            }
        }
    }

    private fun processElementsAnnotatedByMetadata(elements: List<Element>) {
        elements.forEach {
            this.collectEventField(it) { item -> item.copy(metadata = true) }
        }
    }

    private fun getPackageNameOfEvent(element: Element): String {
        val upperElement = element.enclosingElement as? PackageElement ?: throw FoundationProcessorException(
            "@EventSourcing do not support nested class."
        )

        return upperElement.qualifiedName.toString()
    }

    private fun initEventSettingsIfNeeded(packageName: String, className: String) {
        val key = "$packageName.$className"
        if (!data.containsKey(key)) {
            data[key] = CollectedEvent(
                packageName = packageName,
                className = className,
                fields = mutableMapOf(),
                type = "",
                variant = 0
            )
        }
    }

    private fun collectEventField(element: Element, block: (item: CollectedEventField) -> CollectedEventField) {
        val packageName = this.getPackageNameOfEvent(element.enclosingElement)
        val className = element.enclosingElement.simpleName.toString()
        val fieldName = element.simpleName.toString()
        val key = "$packageName.$className"
        initEventSettingsIfNeeded(packageName, className)
        initEventFieldIfNeeded(packageName, className, fieldName)

        data[key]!!.fields[fieldName] = block.invoke(data[key]!!.fields[fieldName]!!)
    }


    private fun initEventFieldIfNeeded(packageName: String, className: String, field: String) {
        val key = "$packageName.$className"
        if (!data[key]!!.fields.containsKey(field)) {
            data[key]!!.fields[field] =
                CollectedEventField(
                    name = field,
                    metadata = false,
                    encrypted = false,
                    faked = ""
                )
        }
    }

    companion object {
        private val BLACKLIST = arrayOf("Companion")
    }
}
