/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.javacompiler;

import java.util.Arrays;

/**
 * 
 * There are options to use various flavours of runtime compilers.
 * Apache JCI is used as the interface to all the runtime compilers.
 * 
 * You can also use the system property "drools.dialect.java.compiler" to set the desired compiler.
 * The valid values are "ECLIPSE" and "NATIVE" only.
 * 
 * drools.dialect.java.compiler = <ECLIPSE|NATIVE>
 * drools.dialect.java.compiler.lnglevel = <1.5|1.6>
 * 
 * The default compiler is Eclipse and the default lngLevel is 1.5.
 * The lngLevel will attempt to autodiscover your system using the 
 * system property "java.version"
 */
public class JavaConfiguration {

    // This should be in alphabetic order to search with BinarySearch
    protected static final String[]  LANGUAGE_LEVELS = new String[]{"1.5", "1.6", "1.7", "1.8", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "9"};

    public static final String JAVA_COMPILER_PROPERTY = "org.mvel.compiler";
    public static final String JAVA_LANG_LEVEL_PROPERTY = "org.mvel3.lnglevel";

    private String                      languageLevel;

    public static String findJavaVersion() {
        return findJavaVersion( System.getProperty( JAVA_LANG_LEVEL_PROPERTY, System.getProperty("java.version") ) );
    }

    public static String findJavaVersion(String level) {
        if (level.startsWith("1.5")) {
            return "1.5";
        } else if (level.startsWith("1.6")) {
            return "1.6";
        } else if (level.startsWith("1.7")) {
            return "1.7";
        } else if (level.startsWith("1.8")) {
            return "1.8";
        } else if (level.startsWith("9")) {
            return "9";
        } else if (level.startsWith("10")) {
            return "10";
        } else if (level.startsWith("15")) {
            return "15";
        } else if (level.startsWith("16")) {
            return "16";
        } else if (level.startsWith("17")) {
            return "17";
        } else if (level.startsWith("18")) {
            return "18";
        } else if (level.startsWith("19")) {
            return "19";
        }

        return "11";
    }

    public String getJavaLanguageLevel() {
        return this.languageLevel;
    }

    /**
     * You cannot set language level below 1.5, as we need static imports, 1.5 is now the default.
     * @param languageLevel
     */
    public void setJavaLanguageLevel(final String languageLevel) {
        if ( Arrays.binarySearch( LANGUAGE_LEVELS, languageLevel ) < 0 ) {
            throw new RuntimeException( "value '" + languageLevel + "' is not a valid language level" );
        }
        this.languageLevel = languageLevel;
    }

}
