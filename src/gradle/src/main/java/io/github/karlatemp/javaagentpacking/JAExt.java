package io.github.karlatemp.javaagentpacking;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class JAExt {
    public final Project project;

    public JAExt(Project project) {
        this.project = project;
    }

    public String packageName;
    public String bootstrap;
    public String urlProtocol = "jpl";
    public SourceSet source;
    public boolean applyTests = false;
    public boolean useAppClassLoader = false;

    public void ofSourceSet(String name) {
        source = project.getExtensions()
                .getByType(SourceSetContainer.class)
                .getByName(name);
    }

    JAExt initMissingFields() {
        if (source == null) {
            ofSourceSet("main");
        }
        if (bootstrap == null) {
            throw new IllegalStateException("Missing bootstrap");
        }
        if (packageName == null) {
            packageName = "io.github.karlatemp.jap." + project.getName();
        }
        return this;
    }
}
