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
package com.hazelcast.simulator.utils;

public final class VersionUtils {

    private VersionUtils() {
    }

    public static boolean isMinVersion(String minVersion, String actualVersion) {
        return (versionCompare(minVersion, actualVersion) <= 0);
    }

    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical comparison that works for version strings,
     * e.g. versionCompare("1.10", "1.6").
     *
     * Warning:
     * - It does not work if "1.10" is supposed to be equal to "1.10.0".
     * - Everything after a "-" is ignored.
     *
     * @param firstVersionString  a string of ordinal numbers separated by decimal points.
     * @param secondVersionString a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     * The result is a positive integer if str1 is _numerically_ greater than str2.
     * The result is zero if the strings are _numerically_ equal.
     */
    public static int versionCompare(String firstVersionString, String secondVersionString) {
        if (firstVersionString.indexOf('-') != -1) {
            firstVersionString = firstVersionString.substring(0, firstVersionString.indexOf('-'));
        }
        if (secondVersionString.indexOf('-') != -1) {
            secondVersionString = secondVersionString.substring(0, secondVersionString.indexOf('-'));
        }
        String[] firstVersion = firstVersionString.split("\\.");
        String[] secondVersion = secondVersionString.split("\\.");

        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < firstVersion.length && i < secondVersion.length && firstVersion[i].equals(secondVersion[i])) {
            i++;
        }
        if (i < firstVersion.length && i < secondVersion.length) {
            // compare first non-equal ordinal number
            int diff = Integer.valueOf(firstVersion[i]).compareTo(Integer.valueOf(secondVersion[i]));
            return Integer.signum(diff);
        } else {
            // the strings are equal or one string is a substring of the other
            // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
            return Integer.signum(firstVersion.length - secondVersion.length);
        }
    }
}
