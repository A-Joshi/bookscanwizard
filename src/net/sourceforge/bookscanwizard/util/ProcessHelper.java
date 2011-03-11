/*
 *
 * Copyright (c) 2011 by Steve Devore
 *                       http://bookscanwizard.sourceforge.net
 *
 * This file is part of the Book Scan Wizard.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.sourceforge.bookscanwizard.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

public class ProcessHelper {

    /**
     * Java seems to want to look in the windows directory before the 
     * actual path when running external scripts.  To get around the issue,
     * when running this in windows it will replace the script name with
     * the actual path name.
     * 
     * @param args
     * @throws Exception
     */
    public static void fixScript(String[] args) throws Exception {
        if (isWindows()) {
            String systemPath = System.getenv("Path");
            if (systemPath == null) {
                systemPath = System.getenv("PATH");
            }
            String script = args[0];
            if (!script.toLowerCase().endsWith(".exe")) {
                script = script+".exe";
            }
            StringTokenizer tokens = new StringTokenizer(systemPath, File.pathSeparator);
            while(tokens.hasMoreTokens()) {
                File f = new File(tokens.nextToken(), script);
                if (f.isFile()) {
                    args[0] = f.getPath();
                    break;
                }
            }
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    /* Based on code from
     *http://jdevelopment.nl/java/jdk6-web-start-cache-location/
     */
    public static String getRealClasspath() throws IOException {
        StringBuilder classpath = new StringBuilder();
        String javaHome ="jar:"+new File(System.getProperty("java.home")).toURI().toURL().toString();
        // All jars have an manifest
        Enumeration<URL> e2 = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        while (e2.hasMoreElements()) {
            URL u = e2.nextElement();
            String urlString = u.toExternalForm();
            // skip anything in java.home
            if (urlString.startsWith(javaHome)) {
                continue;
            }
            // index of .jar because the resource is behind it; "foo.jar!META-INF/MANIFEST.MF"
            int jarIndex = urlString.lastIndexOf(".jar");
            // skip non jar code
            if (jarIndex < 1) {
                continue;
            }
            String jarLocation = null;
            if (urlString.startsWith("jar:file:")) {
                // jdk5 webstart cache AND development
                jarLocation = urlString.substring(4, jarIndex + 4);
            } else {
                // jdk6, uses java caching api in urlclassloader which is extended by the jndiclassloader in jdk6
                // we should get the same file from the cache, we copy because of classpath needs .jar suffix
                JarFile cachedFile = ((JarURLConnection) u.openConnection()).getJarFile();
                jarLocation = cachedFile.getName();
            }
            classpath.append(jarLocation).append(File.pathSeparator);
        }
        if (classpath.length() == 0) {
            // only will be the case in a development environment.
            classpath.append(System.getProperty("java.class.path"));
        } else {
            classpath.setLength(classpath.length()-1);
        }
        String retVal = classpath.toString();
        return retVal;
    }

    private static void copyFile(File in, File out) throws IOException {
       FileChannel sourceChannel = new FileInputStream(in).getChannel();
       FileChannel destinationChannel = new FileOutputStream(out).getChannel();
       sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
       sourceChannel.close();
       destinationChannel.close();
    }
}
