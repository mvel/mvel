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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.Person;
import org.mvel3.TranspilerTest;
import org.mvel3.parser.MvelParser;

public class Antlr4AdditionalMVELTranspilerTest implements TranspilerTest {

    // --- additional tests for Antlr based transpiler. Basically, smaller and more focused tests

    @Test
    public void testSimpleExpression() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.age == 20;",
             "$p.getAge() == 20;");
    }

    @Test
    public void testListAccess() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var x = $p.addresses[0];",
             "var x = $p.getAddresses().get(0);");
    }

    @Test
    public void testStaticMethod() {
        test("System.out.println(\"Hello World\");",
             "System.out.println(\"Hello World\");");
    }

    @Test
    public void testBigDecimalLiteral() {
        test("var x = 10B;",
             "var x = BigDecimal.valueOf(10);");
    }

    @Test
    public void testBigIntegerLiteral() {
        test("var x = 10I;",
             "var x = BigInteger.valueOf(10);");
    }

    @Test
    public void testBigDecimalDecimalLiteral() {
        test("var x = 10.5B;",
             "var x = new BigDecimal(\"10.5\");");
    }

//    Use this when you want to test the legacy JavaCC parser
//    @BeforeClass
//    public static void enableAntlrParser() {
//        MvelParser.Factory.USE_ANTLR = false;
//    }

    @Ignore("Rewrite is not yet implemented")
    @Test
    public void testListCreationLiteral() {
        test("var list = [1, 2, 3];",
             "var list = java.util.Arrays.asList(1, 2, 3);");
    }

    @Ignore("Rewrite is not yet implemented")
    @Test
    public void testListCreationLiteralEmpty() {
        test("var list = [];",
             "var list = java.util.Collections.emptyList();");
    }

    @Ignore("Rewrite is not yet implemented")
    @Test
    public void testMapCreationLiteral() {
        test("var map = [\"a\": 1, \"b\": 2];",
             "var map = java.util.Map.of(\"a\", 1, \"b\", 2);");
    }

    @Ignore("Rewrite is not yet implemented")
    @Test
    public void testMapCreationLiteralEmpty() {
        test("var map = [:];",
             "var map = java.util.Collections.emptyMap();");
    }
}