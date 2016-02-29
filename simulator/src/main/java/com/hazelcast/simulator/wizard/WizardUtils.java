/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.wizard;

import com.hazelcast.simulator.utils.CommandLineExitException;
import com.hazelcast.simulator.utils.EmptyStatement;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

final class WizardUtils {

    private static final int FILE_EXTENSION_LENGTH = 4;

    private WizardUtils() {
    }

    static File getProfileFile(String homeDir) {
        File bashrcFile = new File(homeDir, ".bashrc");
        if (bashrcFile.isFile()) {
            return bashrcFile;
        }
        File profileFile = new File(homeDir, ".profile");
        if (profileFile.isFile()) {
            return profileFile;
        }
        throw new CommandLineExitException("Could not find .bashrc or .profile file! Installation not supported on this system!");
    }

    static String getSimulatorPath() {
        return getJarDir(WizardUtils.class).getParentFile().getAbsolutePath();
    }

    /**
     * Compute the absolute file path to the JAR file.
     *
     * Found in http://stackoverflow.com/a/20953376
     * The framework is based on http://stackoverflow.com/a/12733172/1614775
     * But that gets it right for only one of the four cases.
     *
     * @param clazz A class residing in the required JAR.
     * @return A File object for the directory in which the JAR file resides.
     * During testing with NetBeans, the result is ./build/classes/,
     * which is the directory containing what will be in the JAR.
     */
    static File getJarDir(Class clazz) {
        return getFileFromUrl(getUrl(clazz), clazz.getName());
    }

    static File getFileFromUrl(URL url, String className) {
        // convert to external form
        String extURL = url.toExternalForm();

        // prune for various cases
        if (extURL.endsWith(".jar")) {
            // from getCodeSource
            extURL = extURL.substring(0, extURL.lastIndexOf('/'));
        } else {
            // from getResource
            String suffix = "/" + className.replace(".", "/") + ".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!")) {
                extURL = extURL.substring(FILE_EXTENSION_LENGTH, extURL.lastIndexOf('/'));
            }
        }

        // convert back to URL
        try {
            url = new URL(extURL);
        } catch (MalformedURLException e) {
            // leave url unchanged; probably does not happen
            EmptyStatement.ignore(e);
        }

        // convert URL to File
        try {
            return new File(url.toURI());
        } catch (Exception ignored) {
            return new File(url.getPath());
        }
    }

    private static URL getUrl(Class clazz) {
        try {
            return clazz.getProtectionDomain().getCodeSource().getLocation();
            // URL is in one of two forms
            //        ./build/classes/    NetBeans test
            //        jardir/JarName.jar  from a JAR
        } catch (SecurityException e) {
            return clazz.getResource(clazz.getSimpleName() + ".class");
            // URL is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
            //          file:/U:/Fred/java/Tools/UI/build/classes
            //          jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
        }
    }
}
