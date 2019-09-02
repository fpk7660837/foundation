package net.ntworld.foundation.processor

import net.ntworld.foundation.EventHandler
import net.ntworld.foundation.Handler
import net.ntworld.foundation.generator.GeneratorSettings
import net.ntworld.foundation.generator.setting.EventHandlerSetting
import net.ntworld.foundation.generator.type.ClassInfo
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class EventHandlerProcessor() : Processor {
    override val annotations: List<Class<out Annotation>> = listOf(
        Handler::class.java
    )

    internal data class CollectedEventHandler(
        val eventPackageName: String,
        val eventClassName: String,
        val handlerPackageName: String,
        val handlerClassName: String,
        val makeByFactory: Boolean
    )

    private val data = mutableMapOf<String, CollectedEventHandler>()

    override fun startProcess(settings: GeneratorSettings) {
        data.clear()
        settings.eventHandlers.forEach { item ->
            data[item.name] = CollectedEventHandler(
                eventPackageName = item.event.packageName,
                eventClassName = item.event.className,
                handlerPackageName = item.handler.packageName,
                handlerClassName = item.handler.className,
                makeByFactory = item.makeByFactory
            )
        }
    }

    override fun applySettings(settings: GeneratorSettings): GeneratorSettings {
        val eventHandlers = data.values.map {
            EventHandlerSetting(
                event = ClassInfo(
                    packageName = it.eventPackageName,
                    className = it.eventClassName
                ),
                handler = ClassInfo(
                    packageName = it.handlerPackageName,
                    className = it.handlerClassName
                ),
                makeByFactory = it.makeByFactory
            )
        }
        return settings.copy(eventHandlers = eventHandlers)
    }

    override fun shouldProcess(
        annotation: Class<out Annotation>,
        element: Element,
        processingEnv: ProcessingEnvironment,
        roundEnv: RoundEnvironment
    ): Boolean {
//        val debug = mutableListOf<String>()
//        val packageE = processingEnv.elementUtils.getPackageElement("com.example.contract")
//        packageE.enclosedElements.forEach {
//            if (it.simpleName.toString() == "CreateTodoCommand") {
//                debug.add(it.kind.isInterface.toString())
//                debug.add("---------")
//                it.enclosedElements.forEach {
//                    debug.add(it.simpleName.toString())
//                }
//                debug.add("---------")
//                debug.add("---------")
//                val metadataAnnotation = it.getAnnotation(Metadata::class.java)
//                if (null !== metadataAnnotation) {
//                    val header = KotlinClassHeader(
//                        data1 = metadataAnnotation.data1,
//                        data2 = metadataAnnotation.data2,
//                        bytecodeVersion = metadataAnnotation.bytecodeVersion,
//                        extraInt = metadataAnnotation.extraInt,
//                        extraString = metadataAnnotation.extraString,
//                        kind = metadataAnnotation.kind,
//                        metadataVersion = metadataAnnotation.metadataVersion,
//                        packageName = metadataAnnotation.packageName
//                    )
//                    val metadata = KotlinClassMetadata.read(header)
//                    if (metadata is KotlinClassMetadata.Class) {
//                        val kmClass = metadata.toKmClass()
//
//                        kmClass.supertypes.forEach {
//                            if (it.classifier is KmClassifier.Class) {
//                                debug.add((it.classifier as KmClassifier.Class).name)
//                            }
//                        }
//
//                        // kmClass.s
//                        kmClass.properties.forEach {
//                            debug.add(it.name)
//                            if (null !== it.syntheticMethodForAnnotations) {
//                                debug.add(it.syntheticMethodForAnnotations!!.name)
//                            }
//                            if (it.returnType.classifier is KmClassifier.Class) {
//                                debug.add((it.returnType.classifier as KmClassifier.Class).name)
//                            }
//                            it.returnType.arguments.forEach {
//                                val type = it.type
//                                val variance = it.variance
//                                if (variance !== null) {
//                                    debug.add(variance.name)
//                                }
//                                if (null !== type) {
//                                    if (type.classifier is KmClassifier.Class) {
//                                        debug.add((type.classifier as KmClassifier.Class).name)
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        throw Exception(debug.joinToString(" & "))


        return when (annotation) {
            Handler::class.java -> {
                CodeUtility.isImplementInterface(
                    processingEnv, element.asType(), EventHandler::class.java.canonicalName, false
                )
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
        elements.forEach { element ->
            val packageName = this.getPackageNameOfClass(element)
            val className = element.simpleName.toString()
            val key = "$packageName.$className"
            initCollectedEventHandlerIfNeeded(packageName, className)

            // If the Handler is provided enough information, then no need to find data
            if (processAnnotationProperties(processingEnv, key, element, element.getAnnotation(Handler::class.java))) {
                return@forEach
            }

            val implementedInterface = (element as TypeElement).interfaces
                .firstOrNull {
                    val e = processingEnv.typeUtils.asElement(it) as? TypeElement ?: return@firstOrNull false
                    e.qualifiedName.toString() == EventHandler::class.java.canonicalName
                }

            if (null !== implementedInterface) {
                findImplementedEvent(processingEnv, key, implementedInterface as DeclaredType)
            }

            data[key] = data[key]!!.copy(
                makeByFactory = !CodeUtility.canConstructedByInfrastructure(processingEnv, element)
            )
        }
    }

    private fun processAnnotationProperties(
        processingEnv: ProcessingEnvironment,
        key: String,
        element: Element,
        annotation: Handler
    ): Boolean {
        if (annotation.type !== Handler.Type.Event) {
            return false
        }

        var inputTypeName = ""
        val mirrors = element.annotationMirrors
        mirrors.forEach {
            if (it.annotationType.toString() == FrameworkProcessor.Handler) {
                it.elementValues.forEach {
                    if (it.key.simpleName.toString() == "input" &&
                        it.value.value.toString() !== java.lang.Object::class.java.canonicalName
                    ) {
                        inputTypeName = it.value.value.toString()
                    }
                }
            }
        }

        if (inputTypeName.isEmpty()) {
            return false
        }

        val inputElement = processingEnv.elementUtils.getTypeElement(inputTypeName)
        ContractCollector.collect(processingEnv, element)
        data[key] = data[key]!!.copy(
            eventPackageName = getPackageNameOfClass(inputElement),
            eventClassName = inputElement.simpleName.toString(),
            makeByFactory = annotation.factory
        )
        return true
    }

    private fun findImplementedEvent(processingEnv: ProcessingEnvironment, key: String, type: DeclaredType) {
        if (type.typeArguments.size != 1) {
            return
        }
        val eventType = type.typeArguments.first()
        val element = processingEnv.typeUtils.asElement(eventType)
        ContractCollector.collect(processingEnv, element)
        data[key] = data[key]!!.copy(
            eventPackageName = getPackageNameOfClass(element),
            eventClassName = element.simpleName.toString()
        )
    }

    private fun getPackageNameOfClass(element: Element): String {
        val upperElement = element.enclosingElement as? PackageElement ?: throw FoundationProcessorException(
            "@Handler do not support nested class."
        )

        return upperElement.qualifiedName.toString()
    }

    private fun initCollectedEventHandlerIfNeeded(packageName: String, className: String) {
        val key = "$packageName.$className"
        if (!data.containsKey(key)) {
            data[key] = CollectedEventHandler(
                eventPackageName = "",
                eventClassName = "",
                handlerPackageName = packageName,
                handlerClassName = className,
                makeByFactory = false
            )
        }
    }
}