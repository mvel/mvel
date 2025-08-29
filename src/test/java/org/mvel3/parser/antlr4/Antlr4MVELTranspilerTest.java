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

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.Person;
import org.mvel3.TranspilerTest;

public class Antlr4MVELTranspilerTest implements TranspilerTest {

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

    // In DRL, it's l#ArrayList.removeRange(0, 10);")
    @Test
    public void testInlineCast1() {
        test(ctx -> ctx.addDeclaration("l", List.class),
             "l#ArrayList#removeRange(0, 10);",
             "((ArrayList)l).removeRange(0, 10);");
    }

    @Test
    public void testInlineCast2() {
        test(ctx -> ctx.addDeclaration("l", List.class),
             "l#java.util.ArrayList#removeRange(0, 10);",
             "((java.util.ArrayList)l).removeRange(0, 10);");
    }

    @Test
    public void testInlineCast3() {
        test(ctx -> ctx.addDeclaration("l", List.class),
             "l#ArrayList#[0];",
             "((ArrayList)l).get(0);");
    }

    @Test
    public void testInlineCoercion4() {
        test(ctx -> {
                 ctx.addDeclaration("l", Long.class);
                 ctx.addImport(java.util.Date.class.getCanonicalName());},
             "var x = l#Date#;",
             "var x = new java.util.Date(l);");
    }

    @Test
    public void testInlineCoercion5() {
        test(ctx -> {
                 ctx.addDeclaration("l", Long.class);
                 ctx.addImport(java.util.Date.class.getCanonicalName());},
             "var x = l#Date#getTime();",
             "var x = new java.util.Date(l).getTime();");
    }

    @Test
    public void testConvertPropertyToAccessor() {
        String expectedJavaCode = "$p.getParent().getParent().getName();";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.parent.getParent().name;",
             expectedJavaCode);

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.getParent().parent.name;",
             expectedJavaCode);

        test(ctx ->
                     ctx.addDeclaration("$p", Person.class),
             "$p.parent.parent.name;",
             expectedJavaCode);

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.getParent().getParent().getName();",
             expectedJavaCode);
    }

    @Test
    public void testGenericsOnListAccess() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var x = $p.addresses[0].city + $p.addresses[1].city;",
             "var x = $p.getAddresses().get(0).getCity() + $p.getAddresses().get(1).getCity();");
    }

    @Test
    public void testSetter() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = \"Luca\";",
             "$p.setName(\"Luca\");");
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

    @Test
    public void testMapGet() {
        test(ctx -> ctx.addDeclaration("m", Map.class),
             "m[\"key\"];",
             "m.get(\"key\");");
    }

    @Test
    public void testAssignment() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "Person np = $p; np = $p;",
             "Person np = $p; np = $p;");
    }

    @Test
    public void testModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { name = \"Luca\"; age = 35; };",
             "{$p.setName(\"Luca\");\n $p.setAge(35);update($p);}");
    }

    @Test
    public void testConvertIfConditionAndStatements() {
        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<Object>");},
             "if($p.addresses != null){\n" +
                     "  results.add($p.name);\n" +
                     "} else {\n " +
                     "  results.add($p.age);" +
                     "}",
             "if ($p.getAddresses() != null) {\n" +
                     "  results.add($p.getName());\n" +
                     "} else {\n" +
                     "results.add($p.getAge());\n" +
                     "}");
    }

    @Test
    public void testConvertPropertyToAccessorWhile() {
        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                     ctx.addDeclaration("results", List.class, "<String>");},
             "while($p.addresses != null){" +
                     "  results.add($p.name);\n" +
                     "}",
             "while ($p.getAddresses() != null) {\n" +
                     "  results.add($p.getName());\n" +
                     "}");
    }

    @Test
    public void testConvertPropertyToAccessorFor() {
        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<String>");},
             "for(int i = 0; i < $p.addresses; i++) {\n" +
                     "  results.add($p.name);\n" +
                     "} ",
             "for (int i = 0; i < $p.getAddresses(); i++) {\n" +
                     "  results.add($p.getName());\n" +
                     "}");
    }

    @Test
    public void testConvertPropertyToAccessorSwitch() {
        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<String>");},
                     "        switch($p.name) {\n" +
                     "            case \"Luca\":\n" +
                     "                results.add($p.name);\n" +
                     "}",
                     "        switch($p.getName()) {\n" +
                     "            case \"Luca\":\n" +
                     "                results.add($p.getName());\n" +
                     "}");
    }

}