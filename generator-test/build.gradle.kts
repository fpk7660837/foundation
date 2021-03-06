val artifactGroup: String by project
val foundationVersion: String by project
val processorVersion: String by project
val kotlinxCoroutinesVersion: String by project
val kotlinxSerializationRuntimeVersion: String by project
val javaFakerVersion: String by project

plugins {
    idea
}

group = "$artifactGroup.integration-test"
version = processorVersion

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":generator-test-contract"))
    implementation("com.github.nhat-phan.foundation:foundation-jvm:$foundationVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationRuntimeVersion")
    compile("com.github.javafaker:javafaker:$javaFakerVersion")

    kapt(project(":foundation-processor"))
    kaptTest(project(":foundation-processor"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

idea {
    module {
        sourceDirs = sourceDirs + files(
            "build/generated/source/kapt/main",
            "build/generated/source/kaptKotlin/main"
        )

        testSourceDirs = testSourceDirs + files(
            "build/generated/source/kapt/test",
            "build/generated/source/kaptKotlin/test"
        )

        generatedSourceDirs = generatedSourceDirs + files(
            "build/generated/source/kapt/main",
            "build/generated/source/kaptKotlin/main",
            "build/generated/source/kapt/test",
            "build/generated/source/kaptKotlin/test"
        )
    }
}

kapt {
    arguments {
        arg("foundation.processor.dev", "true")
    }
}
