plugins {
    id("java")
    id("java-library")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.19.0" apply false
}

allprojects {
    group = "io.github.karlatemp"
    version = "0.0.4"

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}
