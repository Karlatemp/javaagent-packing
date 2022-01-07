package io.github.karlatemp.jap.launcher;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "ConstantConditions"})
public class Launcher {
    private static String C_protocol = "$${PROTOCOL}";
    private static String C_bootstrap = "$${BOOTSTRAP}";
    private static boolean C_useAppClassLoader = Boolean.parseBoolean("$${USE_APPCLASSLOADER}");

    public static void premain(String opt, Instrumentation instrumentation) throws Throwable {
        Map<String, byte[]> data = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(Objects.requireNonNull(
                        Launcher.class.getResourceAsStream("agent.jar"),
                        "ERROR: agent.jar missing"
                ))
        )) {
            ZipEntry entry;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(20480);
            byte[] buf = new byte[20480];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.isEmpty()) name = "/";
                if (name.charAt(0) != '/') {
                    name = "/" + name;
                }
                baos.reset();
                while (true) {
                    int rd = zipInputStream.read(buf);
                    if (rd == -1) break;
                    baos.write(buf, 0, rd);
                }
                data.put(name, baos.toByteArray());
            }
        }
        URL base = new URL(null, C_protocol + ":///", new JPLUrlStreamHandler(data));
        URLClassLoader classLoader = new URLClassLoader(new URL[]{
                base
        }, C_useAppClassLoader
                ? ClassLoader.getSystemClassLoader()
                : ClassLoader.getSystemClassLoader().getParent()
        );
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        lookup.findStatic(
                classLoader.loadClass(C_bootstrap),
                "premain",
                MethodType.methodType(void.class, String.class, Instrumentation.class)
        ).invoke(opt, instrumentation);
    }
}
