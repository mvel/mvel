/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
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
 */

package org.mvel3.parser.antlr4;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import org.junit.Test;
import org.mvel3.parser.printer.PrintUtil;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for the MVEL parser using ANTLR4.
 * Assertions are based on the JavaParser AST which are created by Mvel3ToJavaParserVisitor.
 */
public class Antlr4MvelParserJavaParserASTTest {

    @Test
    public void testParseSimpleExpr() {
        String expr = "name == \"Mark\"";
        Expression expression = Antlr4MvelParser.parseExpressionAsJavaParserAST(expr);

        BinaryExpr binaryExpr = ( (BinaryExpr) expression );
        assertThat(toString(binaryExpr.getLeft())).isEqualTo("name");
        assertThat(toString(binaryExpr.getRight())).isEqualTo("\"Mark\"");
        assertThat(binaryExpr.getOperator()).isEqualTo(BinaryExpr.Operator.EQUALS);
    }

    private String toString(Node n) {
        return PrintUtil.printNode(n);
    }
}