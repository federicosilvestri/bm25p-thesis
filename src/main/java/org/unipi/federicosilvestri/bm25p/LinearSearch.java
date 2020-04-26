package org.unipi.federicosilvestri.bm25p;

import org.terrier.applications.CLITool;
import org.terrier.utility.ApplicationSetup;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class LinearSearch {

    public static final File TERRIER_FULL_JAR = new File("/home/federicosilvestri/IdeaProjects/bm25p/terrier/modules/assemblies/target/terrier-project-5.2-jar-with-dependencies.jar");
    public static final File LOGBACK_FILE = new File("/home/federicosilvestri/IdeaProjects/bm25p/terrier/etc/logback.xml");

    public static void main(String args[]) throws Exception {
        // loading other classes for terrier
        addToClasspath(TERRIER_FULL_JAR);
        // adding logback
        addToClasspath(LOGBACK_FILE);

        // First we need to setup terrier environment
        setupTerrierEnv();

        CLITool.main(new String[]{"help"});
    }

    public static void addToClasspath(File file) {
        try {
            URL url = file.toURI().toURL();

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    private static void setupTerrierEnv() {
        System.setProperty("terrier.home", "/home/federicosilvestri/IdeaProjects/bm25p/terrier");
        System.setProperty("terrier.etc", "/home/federicosilvestri/IdeaProjects/bm25p/terrier/etc");
    }
}

