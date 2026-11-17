plugins {
    java
    application
}

group = "dev.aether.client"
version = "0.1.0-alpha"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.3"
val lwjglNatives = providers.gradleProperty("lwjglNatives").orElse("natives-windows").get()

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("dev.aether.client.launcher.AetherLauncher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Aether Client"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
    }
}
