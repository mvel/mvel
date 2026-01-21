/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.parser.antlr4;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mvel3.Person;
import org.mvel3.TranspilerTest;
import org.mvel3.parser.MvelParser;

class Antlr4AdditionalMVELTranspilerTest implements TranspilerTest {

    // Use 'false' when you want to test the legacy JavaCC parser
    @BeforeAll
    static void enableAntlrParser() {
        MvelParser.Factory.USE_ANTLR = true;
    }

    // --- additional tests for Antlr based transpiler. Basically, smaller and more focused tests

    @Test
    void testSimpleExpression() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "return $p.age == 20;",
             "return $p.getAge() == 20;");
    }

    @Test
    void testListAccess() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var x = $p.addresses[0];",
             "var x = $p.getAddresses().get(0);");
    }

    @Test
    void testStaticMethod() {
        test("System.out.println(\"Hello World\");",
             "System.out.println(\"Hello World\");");
    }

    @Test
    void testBigDecimalLiteral() {
        test("var x = 10B;",
             "var x = BigDecimal.valueOf(10);");
    }

    @Test
    void testBigIntegerLiteral() {
        test("var x = 10I;",
             "var x = BigInteger.valueOf(10);");
    }

    @Test
    void testBigDecimalDecimalLiteral() {
        test("var x = 10.5B;",
             "var x = new BigDecimal(\"10.5\");");
    }

    @Test
    void testListCreationLiteral() {
        test("var list = [1, 2, 3];",
             "var list = java.util.Arrays.asList(1, 2, 3);");
    }

    @Test
    void testListCreationLiteralEmpty() {
        test("var list = [];",
             "var list = java.util.Collections.emptyList();");
    }

    @Test
    void testMapCreationLiteral() {
        test("var map = [\"a\": 1, \"b\": 2];",
             "var map = java.util.Map.of(\"a\", 1, \"b\", 2);");
    }

    @Test
    void testMapCreationLiteralEmpty() {
        test("var map = [:];",
             "var map = java.util.Collections.emptyMap();");
    }

    // TODO: Revisit null-safe operator transpilation.
    //  Should we return null? Do we use this only for expression (= returns boolean), so returning false in case of null?

    @Test
    void testNullSafeFieldAccess() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "return $p!.name;",
             "return $p != null ? $p.getName() : null;");
    }

    @Test
    void testNullSafeMethodCall() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "return $p!.getName();",
             "return $p != null ? $p.getName() : null;");
    }

    @Test
    void testNullSafeChainedMethodCall() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "return $p!.getAddresses()!.get(0);",
             "return $p != null ? ($p.getAddresses() != null ? $p.getAddresses().get(0) : null) : null;");
    }

    @Test
    void testNullSafeChainedMix() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "return $p!.addresses!.get(0);",
             "return $p != null ? ($p.getAddresses() != null ? $p.getAddresses().get(0) : null) : null;");
    }

    @Test
    void testTemporalLiteral() {
        test("var duration = 1m5s;",
             "var duration = java.time.Duration.ofMinutes(1).plusSeconds(5);");
    }

    @Test
    void testTemporalLiteralDay() {
        test("var duration = 1day10min30sec;",
             "var duration = java.time.Duration.ofDays(1).plusMinutes(10).plusSeconds(30);");
    }
}