package io.k.tnts;

import java.lang.instrument.Instrumentation;

public class Launch {
    public static void premain(String s, Instrumentation instrumentation) {
        System.out.println("Loaded.... " + instrumentation + ", " + Launch.class.getClassLoader());
        System.out.println(Launch.class.getProtectionDomain().getCodeSource().getLocation());
        System.out.println(Launch.class.getResource("Launch.class"));
        new Throwable("Stack trace").printStackTrace(System.out);
    }

    public static void main(String[] args) {
        System.out.println(Launch.class.getClassLoader());
    }
}