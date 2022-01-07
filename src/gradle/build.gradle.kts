plugins {
    id("java")
    id("java-library")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("maven-publish")
}

val crtProject = project

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(gradleApi())

    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("javaagentPacking") {
            id = "io.github.karlatemp.javaagent-packing"
            displayName = "Javaagent Packing"
            implementationClass = "io.github.karlatemp.javaagentpacking.JavaagentPacking"
            description = "A gradle plugin for developing javaagent applications."
        }
    }
}

pluginBundle {
    website = "https://github.com/Karlatemp/javaagent-packing"
    vcsUrl = "https://github.com/Karlatemp/javaagent-packing"
    description = "A gradle plugin for developing javaagent applications."
    tags = listOf("javaagent")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("tmtst") {
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("testm.EvExec")
}

project(":launcher").afterEvaluate {
    crtProject.tasks.named<Copy>("processResources") {
        from(project(":launcher").tasks.getByName<Jar>("jar").outputs.files) spec@{
            val spec = this@spec
            spec.rename { "launcher.jar" }
            spec.into("io/github/karlatemp/javaagentpacking")
        }
    }
}
