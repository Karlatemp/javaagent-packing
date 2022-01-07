package testm;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestM {

    @Test
    void run() {
        GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(new File("tmp"))
                .forwardOutput()
                .withArguments("launchJavaagent", "--info", "--stacktrace")
                .build();

//        project.getPluginManager().apply(JavaagentPacking.class);
    }
}
