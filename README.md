# Javaagent Packing

A [gradle plugin](https://plugins.gradle.org/plugin/io.github.karlatemp.javaagent-packing)
for developing javaagent applications.

-----

## Why this plugin

### Fast packing all dependencies

Used many dependencies? JavaagentPacking will pack all dependencies you used automatic.

### Avoid classpath pollution

Using this plugin can avoid classpath pollution. Applications on AppClassLoader can't access classes of javaagent.

### Configure simply

Just adding a bit code into `build.groovy`

```groovy
javaagent {
    bootstrap = "org.example.myjavaagent.Launcher"
    ofSourceSet("main")
    applyTests = true
}
```

----

## Using this plugin

Adding `id("io.github.karlatemp.javaagent-packing") version "0.0.4"` into your `plugins`

### Configure

```groovy
javaagent {
    // required.
    //      The launcher of javaagent
    //      Require `public static void premain(String, Instrumentation)` exists
    bootstrap = "org.example.myjavaagent.Launcher"
    // optional: Default: the main source set of project
    //      SourceSet of javaagent application
    //      See gradle document for more details.
    ofSourceSet("main")
    // optional: Default: "io.github.karlatemp.jap." + project.name
    //      The package of wrapper jar (the final built jar)
    packageName = "org.example.myjavaagent.wrapper"
    // optional: Default: jpl
    //      The custom protocol scheme of `Launcher.class.getProtectionDomain().getLocation()`
    urlProtocol = "jpl"
    // optional: Default false
    //      Add `-javaagent` into tests
    applyTests = false
}

// Configuration of directly bootstrap
// In build.gradle.kts, use
//      tasks.named<JavaExec>("launchJavaagent") {}
launchJavaagent {
    mainClass.set('org.example.myjavaagent.test.Main')
    // If `classpath` missing, use test source set default.
    // If `classpath` modified, javaagent-packing will not change it again.
}

```
