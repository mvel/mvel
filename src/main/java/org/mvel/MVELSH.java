/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MVELSH {
    public static void main(String[] args) {
        Map map;

        boolean output = true;
        boolean stacktrace = false;
        boolean showExecTime = false;
        boolean benchmarkMode = false;
        boolean template = false;

        String in;

        VariableResolverFactory lvrf = new DefaultLocalVariableResolverFactory(map = new HashMap<String, Object>());

        Object out = null;

        long time;

        System.out.println("Welcome to MVEL!");
        while (true) {
            try {
                System.out.print("mvel$ ");
                
                if ((in = new BufferedReader(new InputStreamReader(System.in)).readLine()).length() == 0) continue;

                if ("quit;".equals(in) || "exit;".equals(in)) return;
                else if ("stacktrace;".equals(in)) {
                    stacktrace = !stacktrace;
                    System.out.println("STACKTRACES: " + (stacktrace ? "ON" : "OFF"));
                    continue;
                }
                else if ("echo;".equals(in)) {
                    output = !output;
                    System.out.println("OUTPUT ECHO: " + (output ? "ON" : "OFF"));
                    continue;
                }
                else if ("template;".equals(in)) {
                    template = !template;
                    System.out.println("TEMPLATE INTERPRETER: " + (template ? "ON" : "OFF"));
                    continue;
                }
                else if ("exectime;".equals(in)) {
                    showExecTime = !showExecTime;
                    System.out.println("SHOW EXEC TIME: " + (showExecTime ? "ON " : "OFF"));
                    continue;
                }
                else if ("benchmark;".equals(in)) {
                    benchmarkMode = !benchmarkMode;
                    showExecTime = true;
                    System.out.println("MVELSH BENCHMARK MODE: " + (benchmarkMode ? "ON" : "OFF"));
                    continue;
                }
                else if ("clear;".equals(in)) {
                    map.clear();
                    System.out.println("CLEARED VARIABLES.");
                    continue;
                }
                else


                if ("help;".equals(in)) {
                    showHelp();
                    continue;
                }

                if (template) {
                    time = System.currentTimeMillis();
                    out = TemplateInterpreter.parse(in, null, map);
                    time = System.currentTimeMillis() - time;
                }
                else if (benchmarkMode) {
                    System.out.println("HOTSPOT WARMUP ...");
                    for (int i = 10000; i != 0; i--) {
                        MVEL.eval(in, lvrf);
                    }
                    System.out.println("RUNNING BENCHMARK (10,000 times) ...");

                    time = System.currentTimeMillis();
                    for (int i = 10000; i != 0; i--) {
                        out = MVEL.eval(in, lvrf);

                    }
                    time = System.currentTimeMillis() - time;
                }
                else {
                    time = System.currentTimeMillis();
                    out = MVEL.eval(in, lvrf);
                    time = System.currentTimeMillis() - time;
                }

                if (showExecTime) System.out.println("DONE in : " + time + "ms.");
                if (output) System.out.println((template ? "TOUT: " : "OUT: ") + out);
            }
            catch (Exception e) {
                if (stacktrace) e.printStackTrace();
                else
                    System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    private static void showHelp() {
        System.out.println("Commands");
        System.out.println("--------");
        System.out.println("echo            -   toggles output echo on/off");
        System.out.println("template        -   use the template parser shell");
        System.out.println("stacktrace      -   toggles stacktraces on/off");
        System.out.println("benchmark       -   toggles benchmark mode on/off");
        System.out.println("exectime        -   toggles execution time display on/off");
        System.out.println("clear           -   clears all variables");
        System.out.println("quit            -   exits the shell");
        System.out.println("\n");
    }
}
