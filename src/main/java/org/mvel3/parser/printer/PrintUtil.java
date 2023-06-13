/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.mvel3.parser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.TypeSolver;

public class PrintUtil {

    public static String printNode(Node node) {
        return printNode(node, null);
    }

    public static String printNode(Node node, TypeSolver typeSolver) {
        //return LexicalPreservingPrinter.print(node);

        PrinterConfiguration prettyPrinterConfiguration = new DefaultPrinterConfiguration();
        DefaultPrettyPrinterVisitor printVisitor = new DefaultPrettyPrinterVisitor(prettyPrinterConfiguration);

        node.accept(printVisitor, null);
        return printVisitor.toString();
    }

}
