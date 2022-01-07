package io.github.karlatemp.javaagentpacking;

import kotlin.io.FilesKt;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("NullableProblems")
public class JavaagentPacking implements Plugin<Project> {
    public static final String GROUP_ID = "javaagent";
    private static final Map<String, byte[]> launcherBytecode = new HashMap<>();
    private static final String launcherBytecodeSha1;

    static {
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(Objects.requireNonNull(
                        JavaagentPacking.class.getResourceAsStream("launcher.jar"),
                        "launcher.jar not found"))
        )) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            ZipEntry entry;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(20480);
            byte[] buf = new byte[20480];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.isEmpty()) continue;
                if (!name.endsWith(".class")) continue;
                if (name.charAt(0) == '/') {
                    name = name.substring(1);
                }
                baos.reset();
                while (true) {
                    int rd = zipInputStream.read(buf);
                    if (rd == -1) break;
                    baos.write(buf, 0, rd);
                }
                byte[] data = baos.toByteArray();
                launcherBytecode.put(name, data);
                messageDigest.update(data);
            }
            launcherBytecodeSha1 = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (Exception ioException) {
            throw new ExceptionInInitializerError(ioException);
        }
    }

    @Override
    public void apply(Project target) {
        target.getExtensions().create("javaagent", JAExt.class, target);

        final TaskProvider<JavaExec> task_launchJavaagent = target.getTasks().register("launchJavaagent", JavaExec.class, launchJavaagent -> {
            launchJavaagent.setGroup(GROUP_ID);
        });
        final TaskProvider<Jar> task_jarJavaagent = target.getTasks().register("jarJavaagent", Jar.class, jarJavaagent -> {
            jarJavaagent.setGroup(GROUP_ID);
            jarJavaagent.getArchiveClassifier().set("core");
        });
        TaskProvider<Jar> task_packJavaagent = target.getTasks().register("packJavaagent", Jar.class, packJavaagent -> {
            packJavaagent.setGroup(GROUP_ID);
            packJavaagent.getArchiveClassifier().set("javaagent");

        });

        target.afterEvaluate($$ -> {
            task_jarJavaagent.configure(jarJavaagent -> {

                JAExt jaExt = target.getExtensions().getByType(JAExt.class);
                jarJavaagent.dependsOn(jaExt.source.getClassesTaskName());
                jaExt.source.getRuntimeClasspath().forEach(it -> {
                    if (it.exists()) {
                        jarJavaagent.from(it.isDirectory() ? it : target.zipTree(it));
                    }
                });
                jarJavaagent.dependsOn(jaExt.source.getRuntimeClasspath().getBuildDependencies());
                jarJavaagent.exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class");
            });

            TaskProvider<Task> task_buildJavaagentWrapper = target.getTasks().register("buildJavaagentWrapper", buildJavaagentWrapper -> {
                buildJavaagentWrapper.setGroup(GROUP_ID);
                File temporaryDir = buildJavaagentWrapper.getTemporaryDir();
                buildJavaagentWrapper.getOutputs().dir(temporaryDir);

                JAExt ext = target.getExtensions().getByType(JAExt.class).initMissingFields();

                buildJavaagentWrapper.getInputs().property("b", ext.bootstrap);
                buildJavaagentWrapper.getInputs().property("p", ext.urlProtocol);
                buildJavaagentWrapper.getInputs().property("pkg", ext.packageName);
                buildJavaagentWrapper.getInputs().property("c", launcherBytecodeSha1);

                buildJavaagentWrapper.doLast(new Action<Task>() {
                    @Override
                    public void execute(Task $$$) {
                        FilesKt.deleteRecursively(temporaryDir);
                        temporaryDir.mkdirs();

                        String pkg = ext.packageName.replace('.', '/');
                        Remapper remapper = new Remapper() {
                            @Override
                            public String map(String internalName) {
                                if (internalName == null) return null;
                                if (internalName.startsWith("io/github/karlatemp/jap/launcher/")) {
                                    return internalName.replace("io/github/karlatemp/jap/launcher/", pkg + '/');
                                }
                                return internalName;
                            }
                        };

                        for (Map.Entry<String, byte[]> entry : launcherBytecode.entrySet()) {
                            ClassWriter cw = new ClassWriter(0);
                            class CmCC extends ClassRemapper {
                                String iin;

                                public CmCC(ClassVisitor classVisitor, Remapper remapper) {
                                    super(classVisitor, remapper);
                                    this.cv = new ClassVisitor(api, cv) {
                                        @Override
                                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                            iin = name;
                                            super.visit(version, access, name, signature, superName, interfaces);
                                        }
                                    };
                                }

                                private Object ldcV(Object value) {
                                    if (value instanceof String) {
                                        Object v = value;
                                        switch ((String) value) {
                                            case "$${PROTOCOL}": {
                                                value = ext.urlProtocol;
                                                break;
                                            }
                                            case "$${BOOTSTRAP}": {
                                                value = ext.bootstrap;
                                                break;
                                            }
                                            case "$${USE_APPCLASSLOADER}": {
                                                value = String.valueOf(ext.useAppClassLoader);
                                                break;
                                            }
                                        }
                                    }
                                    return value;
                                }

                                @Override
                                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                    return super.visitField(access, name, descriptor, signature, ldcV(value));
                                }

                                @Override
                                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                    return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {

                                        @Override
                                        public void visitLdcInsn(Object value) {
                                            super.visitLdcInsn(ldcV(value));
                                        }
                                    };
                                }
                            }
                            CmCC cmcc = new CmCC(cw, remapper);
                            new ClassReader(entry.getValue()).accept(
                                    cmcc, 0
                            );
                            File out = new File(temporaryDir, cmcc.iin + ".class");
                            out.getParentFile().mkdirs();
                            FilesKt.writeBytes(out, cw.toByteArray());
                        }
                    }
                });
            });

            task_packJavaagent.configure(packJavaagent -> {

                packJavaagent.dependsOn(task_jarJavaagent);
                packJavaagent.dependsOn(task_buildJavaagentWrapper);

                packJavaagent.from(task_buildJavaagentWrapper.get().getOutputs().getFiles().getFiles());
                JAExt ext = target.getExtensions().getByType(JAExt.class).initMissingFields();
                packJavaagent.from(task_jarJavaagent.get().getOutputs().getFiles().getSingleFile(), spec -> {
                    spec.into(ext.packageName.replace('.', '/'));
                    spec.rename($$$ -> "agent.jar");
                });

                Map<String, String> attributes = new HashMap<>();
                attributes.put("Premain-Class", ext.packageName.replace('/', '.') + ".Launcher");
                attributes.put("Can-Redefine-Classes", "true");
                attributes.put("Can-Retransform-Classes", "true");
                packJavaagent.getManifest().attributes(attributes);
            });
            task_launchJavaagent.configure(launchJavaagent -> {
                launchJavaagent.dependsOn(task_packJavaagent);
                launchJavaagent.doFirst($$$$ -> {
                    launchJavaagent.jvmArgs("-javaagent:" + task_packJavaagent.get().getOutputs().getFiles().getSingleFile().getAbsolutePath());
                });
                if (launchJavaagent.getClasspath().isEmpty()) {
                    launchJavaagent.classpath(target.getExtensions().getByType(SourceSetContainer.class).getByName("test").getRuntimeClasspath());
                }
            });

            JAExt jaExt = target.getExtensions().getByType(JAExt.class);
            if (jaExt.applyTests) {
                target.getTasks().withType(Test.class, testTask -> {
                    testTask.dependsOn(task_packJavaagent);
                    testTask.doFirst($$$$ -> {
                        testTask.jvmArgs("-javaagent:" + task_packJavaagent.get().getOutputs().getFiles().getSingleFile().getAbsolutePath());
                    });
                });
            }
        });
    }
}
