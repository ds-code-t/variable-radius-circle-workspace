plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.example"
version = "2.0.0"

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application { mainClass.set("com.example.circleworkspace.CircleWorkspaceApp") }

tasks.test { useJUnitPlatform() }

// OpenJFX 0.1.0 configures JavaFX runtime details for the application run task
// during task execution. Gradle 9.x configuration-cache rules reject that.
// Declaring the limitation here prevents IDE or command-line cache flags from
// turning a normal desktop launch into a build failure.
tasks.named<JavaExec>("run") {
    notCompatibleWithConfigurationCache(
        "The OpenJFX Gradle plugin configures JavaFX runtime arguments at execution time."
    )
}
