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

package org.mvel3;

import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.parser.printer.CoerceRewriter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.github.javaparser.Providers.provider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MVELTranspilerTest implements TranspilerTest {

    @Test
    public void testAssignmentIncrement() {
        test(ctx -> ctx.addDeclaration("i", Integer.class),
             "i += 10;",
             "context.put(\"i\", i += 10);");
    }

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
    public void testAssignmentIncrementInFieldWithPrimitive() {
        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.age += 10;",
             "p.setAge(p.getAge() + 10);");
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
    public void testConvertPropertyToAccessorForEach() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<String>");},
             "for(var a: $p.addresses){\n" +
             "  results.add(a.city);\n" +
             "}\n",
             "for (var a : $p.getAddresses()) {\n" +
             "  results.add(a.getCity());\n" +
             "}\n");
    }

    @Test
    public void testGenericsOnListAccess() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var x = $p.addresses[0].city + $p.addresses[1].city;",
             "var x = $p.getAddresses().get(0).getCity() + $p.getAddresses().get(1).getCity();");
    }

    @Test
    public void testConvertIfConditionAndStatements() {
        String expectedJavaCode =  "if ($p.getAddresses() != null) {\n" +
                "  results.add($p.getName());\n" +
                "} else {\n" +
                "results.add($p.getAge());\n" +
                "}\n";

        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<Object>");},
             "if($p.addresses != null){\n" +
                     "  results.add($p.name);\n" +
                     "} else {\n " +
                     "  results.add($p.age);" +
                     "}",
             expectedJavaCode);
    }

    @Test
    public void testPrimitiveWithBigDecimal() {
        for(Primitive p : CoerceRewriter.FLOAT_PRIMITIVES) {
            test("var x = 10B * " + p.toBoxedType() + ".MAX_VALUE;",
                 "var x = new BigDecimal(\"10\")." +
                 "multiply( BigDecimal.valueOf(" + p.toBoxedType() + ".MAX_VALUE), java.math.MathContext.DECIMAL128);");
        }
    }

    @Test
    public void testBoxTypeWithBigDecimal() {
        for(Primitive p : CoerceRewriter.FLOAT_PRIMITIVES) {
            test("var x = 10B * " + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE);",
                 "var x = new BigDecimal(\"10\").multiply( BigDecimal.valueOf(" + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE)), java.math.MathContext.DECIMAL128);");
        }
    }

    @Test
    public void testBoxTypeWithBigInteger() {
        for(Primitive p : CoerceRewriter.INTEGER_PRIMITIVES) {
            test(ctx -> {},
                 "var x = 10I * " + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE);",
                 "var x = new BigInteger(\"10\").multiply( BigInteger.valueOf(" + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE)));");
        }
    }

    @Test
    public void testPrimitiveWithBigInteger() {
        for(Primitive p : CoerceRewriter.INTEGER_PRIMITIVES) {
            test(ctx -> {},
                 "var x = 10I * " + p.toBoxedType() + ".MAX_VALUE;",
                 "var x = new BigInteger(\"10\").multiply( BigInteger.valueOf(" + p.toBoxedType() + ".MAX_VALUE));");
        }
    }

    @Test // I changed this, to avoid implicit narrowing (mdp)
    public void testPromoteBigDecimalToIntValueInsideIf() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("$m", BigDecimal.class);
             },
             "if($p.isEven($p.salary.intValue()) && $p.isEven($m.intValue())){}\n",
             "    if ($p.isEven($p.getSalary().intValue()) && $p.isEven($m.intValue())) {}\n");
    }

    @Test
    // I changed this, to avoid implicit narrowing (mdp)
    // Currently broken because maybeCoerceArguments doesn't understand static import methods
    public void testPromoteBigDecimalToIntValueInsideIfWithStaticMethod() {
        test(ctx -> {
                 ctx.addDeclaration("$m", BigDecimal.class);
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addStaticImport(Person.class.getCanonicalName() + ".isEven");
             },
             "if(isEven($p.salary.intValue()) && isEven($m.intValue())){} ",
             "    if (isEven($p.getSalary().intValue()) && isEven($m.intValue())) {}\n");
    }


    @Test
    public void testConvertPropertyToAccessorWhile() {
        String expectedJavaCode =  "while ($p.getAddresses() != null) {\n" +
                "  results.add($p.getName());\n" +
                "}\n";

        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                     ctx.addDeclaration("results", List.class, "<String>");},
             "while($p.addresses != null){" +
                     "  results.add($p.name);\n" +
                     "}",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorDoWhile() {
        String expectedJavaCode =  "do {\n" +
                "  results.add($p.getName());\n" +
                "} while ($p.getAddresses() != null);\n";

        test(ctx -> {
            ctx.addDeclaration("$p", Person.class);
            ctx.addDeclaration("results", List.class, "<String>");
             },
             "do {\n" +
                     "  results.add($p.name);\n" +
                     "} while($p.addresses != null);",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorFor() {
        String expectedJavaCode =  "for (int i = 0; i < $p.getAddresses(); i++) {\n" +
                "  results.add($p.getName());\n" +
                "}";

        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<String>");},
             "for(int i = 0; i < $p.addresses; i++) {\n" +
                     "  results.add($p.name);\n" +
                     "} ",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorSwitch() {
        String expectedJavaCode =
                "        switch($p.getName()) {\n" +
                "            case \"Luca\":\n" +
                "                results.add($p.getName());\n" +
                "}";

        test(ctx -> {ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("results", List.class, "<String>");},
                     "        switch($p.name) {\n" +
                     "            case \"Luca\":\n" +
                     "                results.add($p.name);\n" +
                     "}",
             expectedJavaCode);
    }

    @Test
    public void testAccessorInArguments() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "insert(\"Modified person age to 1 for: \" + $p.name);",
             "insert(\"Modified person age to 1 for: \" + $p.getName());");
    }

    @Test
    public void testEnumField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "int key = $p.gender.getKey();",
             "int key = $p.getGender().getKey();");
    }

    @Test
    public void testEnumConstant() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "int key = Gender.FEMALE.getKey();",
             "int key = Gender.FEMALE.getKey();");
    }

    @Test
    public void testPublicField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.parentPublic.getParent().name;",
             "$p.parentPublic.getParent().getName();");

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.getParent().parentPublic.name;",
             "$p.getParent().parentPublic.getName();");
    }

    @Test
    public void testStaticdMethod() {
        System.out.println("string");
        test("System.out.println(\"Hello World\");",
             "System.out.println(\"Hello World\");");
    }

    @Test
    public void testStringLength() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name.length;",
             "$p.getName().length();");
    }

    @Test
    public void testAssignment() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "Person np = $p; np = $p;",
             "Person np = $p; np = $p;");
    }

    @Test
    public void testAssignmentUndeclared() {
        // This use to test that it wuold add in a type declaration.
        // However this functionality has been dropped. Users must declare type of use 'var', for the first time a var is used.
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var np = $p;",
             "var np = $p;");
    }

    @Test
    public void testSetter() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = \"Luca\";",
             "$p.setName(\"Luca\");");
    }

    @Test
    public void testBoxingSetter() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.ageAsInteger = 20;",
             "$p.setAgeAsInteger(20);");
    }

    @Test
    public void testSetterBigDecimal() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = $p.salary + 50000;",
             "$p.setSalary($p.getSalary().add(new BigDecimal(\"50000\"), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testSetterBigDecimalConstant() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = 50000;",
             "$p.setSalary(new BigDecimal(\"50000\"));");
    }

    @Test
    public void testSetterBigDecimalConstantFromLong() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = 50000L;", // notice it trims the trailing L
             "$p.setSalary(new BigDecimal(\"50000\"));");
    }

    @Test
    public void testSetterCoerceToStringWithBigDecimal() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = BigDecimal.valueOf(1);",
             "$p.setName(java.util.Objects.toString(BigDecimal.valueOf(1), null));");
    }

    @Test
    public void testSetterCoerceToStringWithBigDecimal2() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = \"10.50\";",
             "$p.setSalary(new BigDecimal(\"10.50\"));");
    }

    @Test
    public void testSetterStringWithBigDecimalFromField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = $p.salary;",
             "$p.setName(java.util.Objects.toString($p.getSalary(), null));");
    }

    @Test
    public void testSetterStringWithBigDecimalFromVariable() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("$m", BigDecimal.class);
             },
             "$p.name = $m;",
             "$p.setName(java.util.Objects.toString($m, null));");
    }

    @Test
    public void testSetterWithBigDecimalFromBigDecimalLiteral() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = 10000B;",
             "$p.setName(java.util.Objects.toString(new BigDecimal(\"10000\"), null));");
    }

    @Test
    public void testSetterStringWithBigIntegerFromBigDecimalLiteral() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = 10000I;",
             "$p.setName(java.util.Objects.toString(new BigInteger(\"10000\"), null));");
    }

    @Test
    public void testSetterStringWithBigDecimalFromBigDecimalConstant() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = BigDecimal.ZERO;",
             "$p.setName(java.util.Objects.toString(BigDecimal.ZERO, null));");
    }

    @Test
    public void testSetterStringWithNull() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = null;",
             "$p.setName(null);");
    }

    @Test
    public void testSetterBigDecimalConstantModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { salary = 50000 };",
             "$p.setSalary(new java.math.BigDecimal(50000)); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testSetterBigDecimalLiteralModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { salary = 50000B };",
             "$p.setSalary(new java.math.BigDecimal(\"50000\")); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testSetterBigDecimalLiteralModifyNegative() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { salary = -50000B };",
             "$p.setSalary(new java.math.BigDecimal(\"-50000\")); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testBigDecimalModulo() {
        test(ctx -> ctx.addDeclaration("$b1", BigDecimal.class),
             "java.math.BigDecimal result = $b1 % 2;",
             "java.math.BigDecimal result = $b1.remainder(new BigDecimal(\"2\"), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalModuloPromotion() {
        test("BigDecimal result = 12 % 10;",
             "BigDecimal result = BigDecimal.valueOf(12 % 10);");
    }

    @Test
    public void testBigDecimalModuloWithOtherBigDecimal() {
        test(ctx -> {
                 ctx.addDeclaration("$b1", BigDecimal.class);
                 ctx.addDeclaration("$b2", BigDecimal.class);
             },
             "java.math.BigDecimal result = $b1 % $b2;",
             "java.math.BigDecimal result = $b1.remainder($b2, java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalModuloOperationSumMultiply() {
        test(ctx -> {
                 ctx.addDeclaration("bd1", BigDecimal.class);
                 ctx.addDeclaration("bd2", BigDecimal.class);
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.salary = $p.salary + (bd1.multiply(bd2));",
             "$p.setSalary($p.getSalary().add(bd1.multiply(bd2), java.math.MathContext.DECIMAL128));\n");
    }

    @Test
    public void testDoNotConvertAdditionInStringConcatenation() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                          "     list.add(\"before \" + $p + \", money = \" + $p.salary); " +
                          "     modify ( $p )  { salary = 50000 };  " +
                          "     list.add(\"after \" + $p + \", money = \" + $p.salary); ",
                         "      list.add(\"before \" + $p + \", money = \" + $p.getSalary()); " +
                         "      $p.setSalary(new java.math.BigDecimal(50000));" +
                         "      list.add(\"after \" + $p + \", money = \" + $p.getSalary()); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testBigIntegerOperatorsWithDeclareRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> ctx.addDeclaration("p", Person.class),
                 "var x = 10" + op + "p.ageAsBigInteger;",
                 "var x = new BigInteger(\"10\")." + method + "(p.getAgeAsBigInteger());"
                );

        }
    }

    @Test
    public void testBigIntegerAssignOperatorsWithFieldAccessRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("p", Person.class);
                 },
                 "p.ageAsBigInteger " + op + "= 10;",
                 "p.setAgeAsBigInteger( p.getAgeAsBigInteger()." + method + "(new BigInteger(\"10\")));"
                );

        }
    }

    @Test
    public void testBigIntegerAssignOperatorsWithMapRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("p", Person.class);
                 },
                 "p.bigIntegerMap[\"k1\"] " + op + "= 10;",
                 "p.getBigIntegerMap().put( \"k1\", p.getBigIntegerMap().get(\"k1\")." +
                 method + "(new BigInteger(\"10\")));"
                );

        }
    }

    @Test
    public void testBigIntegerAssignOperatorsWithVarRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("b", BigDecimal.class);
                 },
                 "b " + op + "= 10;",
                 "context.put(\"b\", b = b." + method + "(new BigDecimal(\"10\"), java.math.MathContext.DECIMAL128));"
                );

        }
    }

    @Test
    public void testBigDecimalOperatorsWithRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("p", Person.class);
                 },
                 "var x = 10" + op + "p.salary;",
                 "var x = new BigDecimal(\"10\")." + method + "(p.getSalary(), java.math.MathContext.DECIMAL128);"
                );
        }
    }

    @Test
    public void testDeclationExpressionBigIntegerLiteral() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.ageAsBigInteger = 10000I;",
             "$p.setAgeAsBigInteger(new BigInteger(\"10000\"));");
    }

    @Test
    public void testDeclationExpressionWithBigIntegerLiteral1() {
        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = 10*p.parentPublic.ageAsBigInteger;",
             "var x = new BigInteger(\"10\").multiply(p.parentPublic.getAgeAsBigInteger());"
             );

        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = p.parentPublic.ageAsBigInteger*10;",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(new BigInteger(\"10\"));"
            );
    }

    @Test
    public void testDeclationExpressionWithBigIntegerLiteral2() {
        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = (10+10)*p.parentPublic.ageAsBigInteger;",
             "var x = BigInteger.valueOf(10 + 10).multiply(p.parentPublic.getAgeAsBigInteger());"
            );

        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = p.parentPublic.ageAsBigInteger*(10+10);",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(BigInteger.valueOf(10+10));"
            );
    }

    @Test
    public void testDeclationExpressionWithBigIntegerLiteral3() {
        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = 10I*p.parentPublic.ageAsBigInteger;",
             "var x = new BigInteger(\"10\").multiply(p.parentPublic.getAgeAsBigInteger());"
            );

        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = p.parentPublic.ageAsBigInteger*10I;",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(new BigInteger(\"10\"));"
            );
    }

    @Test
    public void testSetterPublicField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.nickName = \"Luca\";",
             "$p.nickName = \"Luca\";");
    }

    @Test
    public void testInitializerArrayAccess() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var l = new ArrayList(); " +
                     "l.add(\"first\"); " +
                     "System.out.println(l[0]);",
             "var l = new ArrayList(); " +
                     "l.add(\"first\"); " +
                     "System.out.println(l.get(0));");
    }


    @Test
    public void testMapGet() {
        test(ctx -> ctx.addDeclaration("m", Map.class),
             "m[\"key\"];",
             "" +
                     "m.get(\"key\");\n" +
                     "");
    }

    @Test
    public void testMapGetAsField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key3\"];",
             "$p.getItems().get(\"key3\");");
    }

    @Test
    public void testMapGetInMethodCall() {
        test(ctx -> ctx.addDeclaration("m", Map.class),
             "System.out.println(m[\"key\"]);",
             "System.out.println(m.get(\"key\"));");
    }




    @Test
    public void testMapSet() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key3\"] = \"value3\";",
             "$p.getItems().put(\"key3\", \"value3\");");
    }

    @Test
    public void testListSet() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.streets[2] = \"value3\";",
             "$p.getStreets().set(2, \"value3\");");
    }

    @Test
    public void testArraySet() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.roads[2] = \"value3\";",
             "$p.getRoads()[2] = \"value3\";");
    }

    @Test
    public void testMapSetWithVariable() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "String key3 = \"key3\";\n" +
                     "$p.items[key3] = \"value3\";",
             "String key3 = \"key3\";\n" +
                     "$p.getItems().put(key3, \"value3\");");
    }

    @Test
    public void testMapSetWithConstant() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key3\"] = \"value3\";",
             "$p.getItems().put(\"key3\", \"value3\");");
    }

    @Test
    public void testMapSetWithVariableCoercionString() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key\"] = 2;",
             "$p.getItems().put(\"key\", String.valueOf(2));");
    }

    @Test
    public void testMapPutWithVariableCoercionString() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key\"] = 2;",
             "$p.getItems().put(\"key\", String.valueOf(2));");
    }

    @Test
    public void testMapSetWithMapGetAsValueAndTypeParameterCoercion() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("s", String.class);
                 ctx.addDeclaration("map", Map.class, "<String, Integer>");
             },
             "$p.items[\"key4\"] = map[s];",
             "$p.getItems().put(\"key4\", java.util.Objects.toString(map.get(s), null));");
    }

    @Test
    public void testMapSetWithMapGetAsValueAndTypeParameterCoercionAndCompoundOperator() {

        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("s", String.class);
                 ctx.addDeclaration("map", Map.class, "<String, Integer>");
             },
             "$p.items[\"key4\"] += map[s];",
             "$p.getItems().put(\"key4\", $p.getItems().get(\"key4\") + map.get(s));");
    }

    @Test
    public void testVariableAssignmentWithCoercion() {
        test(ctx -> {
                 ctx.addDeclaration("s", String.class);
                 ctx.addDeclaration("map", Map.class, "<String, Integer>");
             },
             "s = map[s];",
             "context.put(\"s\", s = java.util.Objects.toString(map.get(s), null));");
    }

    @Test
    public void testMapSetToNewMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "Map<String, String> newhashmap = new HashMap<>();\n" +
                     "$p.items = newhashmap;\n",
                     "Map<String, String> newhashmap = new HashMap<>(); \n" +
                     "$p.setItems(newhashmap);");
    }

    @Test
    public void testInitializerMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "var m = new HashMap();\n" +
                     "m.put(\"key\", 2);\n" +
                     "System.out.println(m[\"key\"]);",
                     "var  m = new HashMap();\n" +
                     "m.put(\"key\", 2);\n" +
                     "System.out.println(m.get(\"key\"));");
    }

    @Test
    public void testMixArrayMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),

                     "    var m = new HashMap<String, List>();\n" +
                     "    var l = new ArrayList<String>();\n" +
                     "    l.add(\"first\");\n" +
                     "    m.put(\"content\", l);\n" +
                     "    System.out.println(m[\"content\"][0]);\n" +
                     "    l.add(m[\"content\"][0]);",

                     "    var m = new HashMap<String, List>();\n" +
                     "    var l = new ArrayList<String>();\n" +
                     "    l.add(\"first\");\n" +
                     "    m.put(\"content\", l);\n" +
                     "    System.out.println(m.get(\"content\").get(0));\n" +
                     "    l.add(m.get(\"content\").get(0));");
    }

    @Test
    public void testBigDecimal() {
        test(
                     "    BigDecimal sum = 0;\n" +
                     "    BigDecimal money = 10;\n" +
                     "    sum += money;\n" +
                     "    sum -= money;",

                     "   BigDecimal sum = new BigDecimal(\"0\");\n" +
                     "   BigDecimal money =  new BigDecimal(\"10\");\n" +
                     "   sum = sum.add(money, java.math.MathContext.DECIMAL128);\n" +
                     "   sum = sum.subtract(money, java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void bigDecimalLessThan() {
        test("    BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero < ten) {}",
             "BigDecimal zero = new BigDecimal(\"0\");\n" +
                     "    BigDecimal ten = new BigDecimal(\"10\");\n" +
                     "    if (zero.compareTo(ten) < 0) {}");
    }

    @Test
    public void bigDecimalLessThanOrEqual() {
        test("BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero <= ten) {}",
             "BigDecimal zero = new BigDecimal(\"0\");\n" +
                     "    BigDecimal ten = new BigDecimal(\"10\");\n" +
                     "    if (zero.compareTo(ten) <= 0) {}");
    }

    @Test
    public void bigDecimalGreaterThan() {
        test("BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero > ten) {}",
             "BigDecimal zero = new BigDecimal(\"0\");\n" +
                     "    BigDecimal ten = new BigDecimal(\"10\");\n" +
                     "    if (zero.compareTo(ten) > 0) {}\n");
    }

    @Test
    public void bigDecimalGreaterThanOrEqual() {
        test("BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero >= ten) {}",
             "BigDecimal zero = new BigDecimal(\"0\");\n" +
                     "    BigDecimal ten = new BigDecimal(\"10\");\n" +
                     "    if (zero.compareTo(ten) >= 0) {}");
    }

    @Test
    public void bigDecimalEquals() {
        test("    BigDecimal zero = 0;\n" +
                     "    if(zero == 23) {}\n",
             "BigDecimal zero = new BigDecimal(\"0\");\n" +
                     "    if (zero.compareTo(new BigDecimal(\"23\")) == 0) {}");
    }

    @Test
    public void bigDecimalNotEquals() {
        test("BigDecimal zero = 0;\n" +
                     "    if(zero != 23) {}",
             "BigDecimal zero = new BigDecimal(\"0\");\n" +
                     "    if (zero.compareTo(new BigDecimal(\"23\")) != 0) {}");
    }

    @Test
    public void testBigDecimalCompoundOperatorOnField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary += 50000B;",
             "$p.setSalary($p.getSalary().add(new BigDecimal(\"50000\"), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testBigDecimalCompoundOperatorWithOnField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary += $p.salary;",
             "$p.setSalary($p.getSalary().add($p.getSalary(), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testBigDecimalArithmetic() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "java.math.BigDecimal operation = $p.salary + $p.salary;",
             "java.math.BigDecimal operation = $p.getSalary().add($p.getSalary(), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalArithmeticWithConversionLiteral() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "java.math.BigDecimal operation = $p.salary + 10B;",
             "java.math.BigDecimal operation = $p.getSalary().add(new BigDecimal(\"10\"), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalArithmeticWithConversionFromInteger() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "java.math.BigDecimal operation = $p.salary + 10;",
             "java.math.BigDecimal operation = $p.getSalary().add(new BigDecimal(\"10\"), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalPromotionAllFourOperations() {

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "    BigDecimal result = 0B;" +
                     "    result += 50000;\n" +
                     "    result -= 10000;\n" +
                     "    result /= 10;\n" +
                     "    result *= 10;\n" +
                     "    result *= $p.salary;\n" +
                     "    $p.salary = result;",
             "BigDecimal result = new BigDecimal(\"0\");\n" +
                     "        result = result.add(new BigDecimal(\"50000\"), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.subtract(new BigDecimal(\"10000\"), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.divide(new BigDecimal(\"10\"), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.multiply(new BigDecimal(\"10\"), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.multiply($p.getSalary(), java.math.MathContext.DECIMAL128);\n" +
                     "        $p.setSalary(result);");
    }

    @Test
    public void testPromotionOfIntToBigDecimal() {
        test( "    BigDecimal result = 0B;" +
                     "    int anotherVariable = 20;" +
                     "    result += anotherVariable;",
             "BigDecimal result = new BigDecimal(\"0\");\n" +
                     "     int anotherVariable = 20;\n" +
                     "     result = result.add(BigDecimal.valueOf(anotherVariable), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testPromotionOfIntToBigDecimalOnField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "    int anotherVariable = 20;" +
                     "    $p.salary += anotherVariable;",
             "" +
                     "        int anotherVariable = 20;\n" +
                     "        $p.setSalary($p.getSalary().add(BigDecimal.valueOf(anotherVariable), java.math.MathContext.DECIMAL128));\n" +
                     "");
    }

    @Test
    public void testModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { name = \"Luca\", age = 35 };",
             "$p.setName(\"Luca\");\n $p.setAge(35);\n",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { items = $p2.items };",
             "$p.setItems($p2.getItems());\n",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p", "$p2"));
    }

    @Test
    public void testModifySemiColon() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify($p) { setAge(1); };",
             "{ $p.setAge(1); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyWithAssignment() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify($p) { age = $p.age+1 };",
             "{ $p.setAge($p.getAge() + 1); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyWithMethodCall() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify($p) { addresses.clear() };",
             "{ $p.getAddresses().clear(); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testAddCastToMapGet() {
        test(ctx -> ctx.addDeclaration("map", Map.class, "<String, java.util.Map>"),
             "Map pMap = map.get(\"whatever\");",
             "Map pMap = map.get(\"whatever\");");
    }

    @Test
    public void testAddCastToMapGetOfDeclaration() {
        test(ctx -> {
                 ctx.addDeclaration("map", Map.class, "<String, java.util.Map>");
                 ctx.addDeclaration("$p", Person.class);
             },
             "Map pMap = map.get( $p.name );",
             "Map pMap = map.get($p.getName() );");
    }

    @Test
    public void testSimpleVariableDeclaration() {
        test("int i;",
             "int i;");
    }

    @Test
    public void testModifyInsideIfBlock() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "         if ($p.getParent() != null) {\n" +
                     "              $p.setName(\"with_parent\");\n" +
                     "         } else {\n" +
                         "         modify ($p) {\n" +
                         "            name = \"without_parent\"" +
                         "         }\n" +
                     "         " +
                     "      ",
                     "  if ($p.getParent() != null) { " +
                     "      $p.setName(\"with_parent\"); " +
                     "  } else {\n " +
                     "      {\n" +
                     "          $p.setName(\"without_parent\");\n" +
                     "      }\n",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyOrdering() {
        test(ctx -> ctx.addDeclaration("$person", Person.class),
                     "        Address $newAddress = new Address();\n" +
                     "        $newAddress.setCity( \"Brno\" );\n" +
                     "        insert( $newAddress );\n" +
                     "        modify( $person ) {\n" +
                     "          setAddress( $newAddress )\n" +
                     "        }",
             "Addresss $newAddress = new Address(); " +
             "$newAddress.setCity(\"Brno\"); " +
             "insert($newAddress);\n" +
             "  $person.setAddress($newAddress);\n");
    }

    @Test
    public void testSetterOnVarRewrite() {
        test(
             "    Person p = new Person(\"yoda\");\n" +
             "    p.age = 100; \n",
             "    Person p = new Person(\"yoda\");\n" +
             "    p.setAge(100); \n"
            );
    }

    @Test
    public void testSetterOnInferredVarRewrite() {
        test("    var p = new Person(\"yoda\");\n" +
                "    p.age = 100; \n",
                "    var p = new Person(\"yoda\");\n" +
                "    p.setAge(100); \n"
            );
    }

    @Test
    public void testSetterOnMethodReturnRewrite() {
        test(
                MVELTranspilerTest.class.getCanonicalName() + ".createPerson(\"yoda\").age = 100;\n",
                 "    " + MVELTranspilerTest.class.getCanonicalName() + ".createPerson(\"yoda\").setAge(100);\n"
            );
    }

    @Test
    public void testGetterRewriteOnAssign() {
        test(
                "    var p = new Person(\"yoda\");\n" +
                "    p.age = 100; \n" +
                "    int a = p.age; \n",
                "    var p = new Person(\"yoda\");\n" +
                "    p.setAge(100); \n" +
                "    int a = p.getAge();\n"
            );
    }

    @Test
    public void testGetterRewriteInArgument() {
        test( "    var p = new Person(\"yoda\");\n" +
                "    p.age = 100; \n" +
                "    System.out.println(p.age); \n",
                "    var p = new Person(\"yoda\");\n" +
                "    p.setAge(100); \n" +
                "    System.out.println(p.getAge()); \n"
            );
    }

    public static Person createPerson(String name) {
        return new Person(name);
    }

    @Test
    public void forIterationWithSubtype() {
        // this wouldn't rewrite the property accessor if it didn't know the generics.
        test(ctx -> ctx.addDeclaration("$people", List.class, "<Person>"),
             "    for (var p : $people ) {\n" +
             "        System.out.println(\"Person's salary: \" + p.salary );\n" +
             "}",
             "    for (var p : $people) {\n" +
             "        System.out.println(\"Person's salary: \" + p.getSalary() );\n" +
             "    }\n"
            );
    }

    @Test
    public void forIterationWithSubtype2() {
        // this wouldn't rewrite the property accessor if it didn't know the generics.
        test(ctx -> ctx.addDeclaration("$people", List.class, "<Person>"),
             "    for (var p : $people ) {\n" +
             "        System.out.println(\"Person's salary: \" + p.salary );\n" +
             "    }\n",
             "    for (var p : $people) {\n" +
             "        System.out.println(\"Person's salary: \" + p.getSalary() );\n" +
             "    }\n"
            );
    }

    @Test
    public void forIterationWithSubtypeNested() {
        // this wouldn't rewrite the property accessor if it didn't know the generics.
        test(ctx -> {
                 ctx.addDeclaration("$people", List.class, "<Person>");
                 ctx.addDeclaration("$addresses", List.class, "<Address>");
             },
                     "for (var p : $people ) {\n" +
                     "       System.out.println(\"Simple statement\");\n" +
                     "       for (var a : $addresses ) {\n" +
                     "           System.out.println(\"Person's salary: \" + p.salary + \" address's City: \" + a.city);\n" +
                     "       }\n" +
                     "}",
                     "for (var p : $people) {\n" +
                     "           System.out.println(\"Simple statement\");\n" +
                     "           for (var a : $addresses) {\n" +
             "           System.out.println(\"Person's salary: \" + p.getSalary() + \" address's City: \" + a.getCity());\n" +
                     "            }\n" +
                     "}"
        );
    }

    @Test
    public void testMultiLineStringLiteral() {
        test(" { String s = \"\"\"\n string content\n \"\"\";",
             " { String s = \"\"\"\n string content\n \"\"\";");
    }

    @Test
    public void testCreateVariableCoerceRewrite() {
        test( "int a = \"5\";",
              "int a =  Integer.parseInt(\"5\");");
    }

    @Test
    public void testCreateArrayWithArgCoercionRewrite() {
        test( "int[] a = new int[] {\"3\"};",
              "int[] a = new int[] {Integer.parseInt(\"3\")};");
    }

    @Test
    public void testCreateMultiDimensionalArrayWithArgCoercionRewrite() {
        test( "int[][] x = new int[][] {{\"3\"}, {\"3\"}};",
              "int[][] x = new int[][] { { Integer.parseInt(\"3\") }, { Integer.parseInt(\"3\") } };");
    }
    @Test
    public void testNameExprWithStringConcatenationNoRewrite() {
        test( "String a = \"Jonny\" + 5 + \"Is Alive\";",
              "String a = \"Jonny\" + 5 + \"Is Alive\";");
    }

    @Test
    public void testNameExprWithStringConcatenationNoRewrite2() {
        test( "String a = \"5\" + 5;",
              "String a = \"5\" + 5;");
    }

    @Test
    public void testNameExprWithStringConcatenationNoRewrite3() {
        test( "int a = \"5\" + 5;",
              "int a = Integer.parseInt(\"5\" + 5);");
    }

    @Test
    public void testNameExprAssignOperatorNoRewrite() {
        test( "int a = 5;" +
              "a = 6;",
              "int a = 5;" +
              "a = 6;");
    }

    @Test
    public void testNameExprAssignOperatorCoerceRewrite() {
        test( "int a = 5;" +
              "a = \"6\";",
              "int a = 5;" +
              "a = Integer.parseInt(\"6\");");
    }


    @Test
    public void testNameExprAssignOperatorCoerceRewriteKeepLong() {
        test( "Long a = 5;" +
              "a = \"6\";",
              "Long a = 5;" +
              "a = Long.valueOf(\"6\");");
    }


    @Test
    public void testNameExprBigDecimalAssignOperatorCoerceLiteralRewrite() {
        test( "BigDecimal bd = 10B;" +
              "bd = 20;",
              "BigDecimal bd = new BigDecimal(\"10\");" +
              "bd = new BigDecimal(\"20\");");
    }

    @Test
    public void testNameExprBigDecimalAssignOperatorCoerceIntVarRewrite() {
        test( "int x = 20; BigDecimal bd = 10B;" +
              "bd = x;",
              "int x = 20; BigDecimal bd = new BigDecimal(\"10\");" +
              "bd = BigDecimal.valueOf(x);");
    }

    @Test
    public void testNameExprBigIntegerAssignOperatorCoerceOverloadRewrite() {
        test( "BigDecimal bd = 10B;" +
              "bd += 20;",
              "BigDecimal bd = new BigDecimal(\"10\");bd = bd.add(new BigDecimal(\"20\"), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testNameExprBigDecimalAssignOperatorCoerceTwiceOverloadRewrite() {
        test( "BigDecimal bd = 10B;" +
              "bd += 20 + 20 + 40B;",
              "BigDecimal bd = new BigDecimal(\"10\");" +
              "bd = bd.add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), " +
              "                                            java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testNameExprMapBigDecimalAssignOperatorCoerceOverloadRewrite() {
        test( "Map<String, BigDecimal> map = new HashMap<>();map[\"bd\"] = 10B;"  +
              "map[\"bd\"] += 20 + 20 + 40B;",
              "Map<String, BigDecimal> map = new HashMap<>();map.put(\"bd\", new BigDecimal(\"10\"));"  +
              "map.put(\"bd\", map.get(\"bd\").add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testNameExprMapBigDecimalArrayAssignOperatorCoerceOverloadRewrite() {
        test( "BigDecimal[] map = new BigDecimal[] {10B};"  +
              "map[2] += 20 + 20 + 40B;",
              "BigDecimal[] map = new BigDecimal[] {new BigDecimal(\"10\")};"  +
              "map[2] = map[2].add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testNameExprArrayAssignOperatorNoRewrite() {
        test( "int[] a = new int[] {3};" +
              "a[0] = 5;",
              "int[] a = new int[] {3};" +
              "a[0] = 5;");
    }

    @Test
    public void testNameExprArrayAssignOperatorCoerceRewrite() {
        test( "int[] a = new int[] {3};" +
              "a[0] = \"5\";",
              "int[] a = new int[] {3};" +
              "a[0] = Integer.parseInt(\"5\");");
    }

    @Test
    public void testNameExprMapAssignOperatorNoRewrite() {
        test( ctx -> ctx.addDeclaration("map", Map.class, "<String, Integer>"),
              "map[\"key1\"] = 5;",
              "map.put(\"key1\", 5);");
    }

    @Test
    public void testNameExprMapAssignOperatorCoerceRewrite() {
        test( ctx -> ctx.addDeclaration("map", Map.class, "<String, Integer>"),
              "map[\"key1\"] = \"5\";",
              "map.put(\"key1\", Integer.valueOf(\"5\"));");
    }

    @Test
    public void testNameExprCompoundOperatorNoRewrite() {
        test( "var a = 5;" +
              "a += 5;",
              "var a = 5;" +
              "a += 5;");
    }

    @Test
    public void testNameExprCompoundOperatorCoerceRewrite() {
        test( "var a = 5;" +
              "a += \"5\";",
              "var a = 5;" +
              "a += Integer.parseInt(\"5\");");
    }

    @Test
    public void testNameExprArrayCompoundOperatorNoRewrite() {
        test( "int[] a = new int[] {3};" +
              "a[0] += 5;",
              "int[] a = new int[] {3};" +
              "a[0] += 5;");
    }

    @Test
    public void testNameExprArrayCompoundOperatorCoerceRewrite() {
        test( "int[] a = new int[] {3};" +
              "a[0] += \"5\";",
              "int[] a = new int[] {3};" +
              "a[0] += Integer.parseInt(\"5\");");
    }

    @Test
    public void testNameExprMultiDimArrayAssignNoRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "int[][] x = new int[3][3];x[2][2] = 5;",
              "int[][] x = new int[3][3];x[2][2] = 5;");
    }

    @Test
    public void testNameExprMultiDimArrayAssignCompoundNorewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "int[][] x = new int[3][3];x[2][2] += 5;",
              "int[][] x = new int[3][3];x[2][2] += 5;");
    }

    @Test
    public void testNameExprMultiDimArrayAssignCoerceRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "int[][] x = new int[3][3];x[2][2] = \"5\";",
              "int[][] x = new int[3][3];x[2][2] = Integer.parseInt(\"5\");");
    }

    @Test
    public void testNameExprMultiDimArrayAssignCompoundCoercerewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "int[][] x = new int[3][3];x[2][2] += \"5\";",
              "int[][] x = new int[3][3];x[2][2] += Integer.parseInt(\"5\");");
    }

    @Test
    public void testFieldAccessorAssignOperatorNoRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicAge = 5;",
              "p.publicAge = 5;");
    }

    @Test
    public void testFieldAccessorAssignOperatorCoerce() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicAge = \"5\";",
              "p.publicAge = Integer.parseInt(\"5\");");
    }

    @Test
    public void testFieldAccessorCompoundOperatorNoRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicAge += 5;",
              "p.publicAge += 5;");
    }

    @Test
    public void testFieldAccessorCompoundOperatorCoerce() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.age += \"5\";",
              "p.setAge(p.getAge() + Integer.parseInt(\"5\"));");
    }

    @Test
    public void tesFieldAccessorBigDecimalAssignOperatorCoerceLiteralRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicBigDec = 10B;" +
              "p.publicBigDec = 20;",
              "p.publicBigDec = new BigDecimal(\"10\");" +
              "p.publicBigDec = new BigDecimal(\"20\");");
    }

    @Test
    public void testFieldAccessorBigDecimalAssignOperatorCoerceIntVarRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "int x = 20; p.publicBigDec = 10B;" +
              "p.publicBigDec = x;",
              "int x = 20; p.publicBigDec = new BigDecimal(\"10\");" +
              "p.publicBigDec = BigDecimal.valueOf(x);");
    }

    @Test
    public void testFieldAccessorBigIntegerAssignOperatorCoerceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicBigDec = 10B;" +
              "p.publicBigDec += 20;",
              "p.publicBigDec = new BigDecimal(\"10\");p.publicBigDec = p.publicBigDec.add(new BigDecimal(\"20\"), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testFieldAccessorBigDecimalAssignOperatorCoerceTwiceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicBigDec = 10B;" +
              "p.publicBigDec += 20 + 20 + 40B;",
              "p.publicBigDec = new BigDecimal(\"10\");" +
              "p.publicBigDec = p.publicBigDec.add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), " +
              "                                            java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testFieldAccessorMapBigDecimalAssignOperatorCoerceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicMapBigDec[\"bd\"] = 10B;"  +
              "p.publicMapBigDec[\"bd\"] += 20 + 20 + 40B;",
              "p.publicMapBigDec.put(\"bd\", new BigDecimal(\"10\"));"  +
              "p.publicMapBigDec.put(\"bd\", p.publicMapBigDec.get(\"bd\").add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testFieldAccessorMapBigDecimalArrayAssignOperatorCoerceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicArrayBigDec = new BigDecimal[] {10B};"  +
              "p.publicArrayBigDec[2] += 20 + 20 + 40B;",
              "p.publicArrayBigDec = new BigDecimal[] {new BigDecimal(\"10\")};"  +
              "p.publicArrayBigDec[2] = p.publicArrayBigDec[2].add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testFieldAccessExprArrayAssignOperatorNoRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicIntArray[0] = 5;",
              "p.publicIntArray[0] = 5;");
    }

    @Test
    public void testFieldAccessExprArrayAssignOperatorCoerceRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicIntArray[0] = \"5\";",
              "p.publicIntArray[0] = Integer.parseInt(\"5\");");
    }
    @Test
    public void testFieldAccessExprArrayCompoundOperatorNoRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicIntArray[0] += 5;",
              "p.publicIntArray[0] += 5;");
    }

    @Test
    public void testFieldAccessArrayCompoundOperatorCoercionRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicIntArray[0] += \"5\";",
              "p.publicIntArray[0] += Integer.parseInt(\"5\");");
    }


    @Test
    public void testPropertyAccessorMapAssignOperatorPutRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.prices[\"key1\"] = 5;",
              "p.getPrices().put(\"key1\", 5);");
    }

    @Test
    public void testPropertyAccessorMapAssignOperatorPutCoerceRewrite() {
        // coerces with valueOf instead of partInt, as it's coercing to Integer instead of int.
        // this is due to generics.
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.prices[\"key1\"] = \"5\";",
              "p.getPrices().put(\"key1\", Integer.valueOf(\"5\"));");
    }

    @Test
    public void testPropertyAccessorMapCompoundOperatorRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.prices[\"key1\"] += 5;",
              "p.getPrices().put(\"key1\", p.getPrices().get(\"key1\") + 5);");
    }

    @Test
    public void testPropertyAccessorMapCompoundOperatorCoerceRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.prices[\"key1\"] += \"5\";",
              "p.getPrices().put(\"key1\", p.getPrices().get(\"key1\") + Integer.valueOf(\"5\"));");
    }

    @Test
    public void testPropertyAccessorAssigndOperatorWithSetterRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.age = 5;",
              "p.setAge(5);");
    }


    @Test
    public void testPropertyAccessorAssigndOperatorWithSetterRewriteCoerce() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.age = \"5\";",
              "p.setAge(Integer.parseInt(\"5\"));");
    }

    @Test
    public void testPropertyAccessorCompoundOperatorSetterRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.age += 5;",
              "p.setAge( p.getAge() + 5);");
    }


    @Test
    public void tesPropertyAccessorBigDecimalAssignOperatorCoerceLiteralRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.salary = 10B;" +
              "p.salary = 20;",
              "p.setSalary( new BigDecimal(\"10\"));" +
              "p.setSalary(new BigDecimal(\"20\"));");
    }

    @Test
    public void testPropertyAccessorBigDecimalAssignOperatorCoerceIntVarRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "int x = 20; p.salary = 10B;" +
              "p.salary = x;",
              "int x = 20; p.setSalary(new BigDecimal(\"10\"));" +
              "p.setSalary(BigDecimal.valueOf(x));");
    }

    @Test
    public void testPropertyAccessorBigIntegerAssignOperatorCoerceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.salary = 10B;" +
              "p.salary += 20;",
              "p.setSalary(new BigDecimal(\"10\"));p.setSalary(p.getSalary().add(new BigDecimal(\"20\"), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testPropertyAccessorBigDecimalAssignOperatorCoerceTwiceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.salary = 10B;" +
              "p.salary += 20 + 20 + 40B;",
              "p.setSalary(new BigDecimal(\"10\"));" +
              "p.setSalary(p.getSalary().add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), " +
              "                                            java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testPropertyAccessorMapBigDecimalAssignOperatorCoerceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.bigDecimalMap[\"bd\"] = 10B;"  +
              "p.bigDecimalMap[\"bd\"] += 20 + 20 + 40B;",
              "p.getBigDecimalMap().put(\"bd\", new BigDecimal(\"10\"));"  +
              "p.getBigDecimalMap().put(\"bd\", p.getBigDecimalMap().get(\"bd\").add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), " +
              "java.math.MathContext.DECIMAL128), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testPropertyAccessorMapBigDecimalArrayAssignOperatorCoerceOverloadRewrite() {
        test( ctx -> ctx.addDeclaration("p", Person.class),
              "p.publicArrayBigDec = new BigDecimal[] {10B};"  +
              "p.publicArrayBigDec[2] += 20 + 20 + 40B;",
              "p.publicArrayBigDec = new BigDecimal[] {new BigDecimal(\"10\")};"  +
              "p.publicArrayBigDec[2] = p.publicArrayBigDec[2].add(BigDecimal.valueOf(20 + 20).add(new BigDecimal(\"40\"), java.math.MathContext.DECIMAL128), " +
              "java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testStringToPrimitiveCoercion() {
        for (Primitive p : CoerceRewriter.FLOAT_PRIMITIVES) {
            String v = "";
            if (p == Primitive.CHAR) {
                continue;
            }
            switch (p) {
                //case CHAR: v += Character.MAX_VALUE; break;
                case SHORT: v += Short.MAX_VALUE; break;
                case INT: v += Integer.MAX_VALUE; break;
                case LONG: v += Long.MAX_VALUE; break;
                case FLOAT: v += Float.MAX_VALUE; break;
                case DOUBLE: v += Double.MAX_VALUE; break;
            }

            // Test coerces to Number object wrapper
            test( p.toBoxedType() + " x = 0; x += \"" + v +"\";",
                  p.toBoxedType() + " x = 0; x += " + p.toBoxedType() + ".valueOf(\"" + v + "\");");

            // Test coerces to primitive
            String primName = p.name().toLowerCase();
            String parseName = "parse" +primName.substring(0, 1).toUpperCase() +  primName.substring(1);

            test( primName + " x = 0; x += \"" + v +"\";",
                  primName + " x = 0; x += " + p.toBoxedType() + "." + parseName + "(\"" + v + "\");");
        }

        // Now test Char. Note this always only cerces to char, not Character. The following will not compile:
        // Character x = Character.valueOf("a".charAt(0)); x += Character.valueOf("a".charAt(0));
        test( "char x = 0; x += \"" + Character.MAX_VALUE + "\";",
              "char x = 0; x += \"" + Character.MAX_VALUE + "\".charAt(0);");

        test( "Character x = 0; x += \"" + Character.MAX_VALUE + "\";",
              "Character x = 0; x += \"" + Character.MAX_VALUE + "\".charAt(0);");
    }

    @Test
    public void testStringNumberPlusBinaryExpressionNoRewrite() {
        test("int x = 5; String y = \"6\"; var z = x + y;",
             "int x = 5; String y = \"6\"; var z = x + y;");
    }

    @Test
    public void testStringNumberPlusBinaryExpressionRewrite() {
        test("int x = 5; String y = \"6\"; int z = x + y;",
             "int x = 5; String y = \"6\"; int z = Integer.parseInt(x + y);");
    }

    @Test
    public void testStringNumberTimesBinaryExpressionRewrite() {
        test("int x = 5; String y = \"6\"; var z = x * y;",
             "int x = 5; String y = \"6\"; var z = x * Integer.parseInt(y);");
    }

    @Test
    public void testStringNumberTimesBinaryExpressionRewriteAsVarVersion() {
        test("int x = 5; String y = \"6\"; int z = x * y;",
             "int x = 5; String y = \"6\"; int z = x * Integer.parseInt(y);");
    }

    @Test
    public void testBinaryExpressionAsArgs() {
        test(ctx -> ctx.addDeclaration("p", Person.class),
             "int x = 5; String y = \"6\"; p.setAge(x * y);",
             "int x = 5; String y = \"6\"; p.setAge(x * Integer.parseInt(y));");
    }

    @Test
    public void testVarArgCoercionNumbersThenVarArgStrings() {
        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.process2(\"1\", \"2\", \"3\", \"4\", \"5\");",
             "p.process2(Integer.parseInt(\"1\"), Integer.parseInt(\"2\"), Integer.parseInt(\"3\"), \"4\", \"5\");");

        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.process2(\"1\", \"2\", \"3\", 4, 5);",
             "p.process2(Integer.parseInt(\"1\"), Integer.parseInt(\"2\"), Integer.parseInt(\"3\"), String.valueOf(4), String.valueOf(5));");

        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.process2(\"1\", \"2\", \"3\");",
             "p.process2(Integer.parseInt(\"1\"), Integer.parseInt(\"2\"), Integer.parseInt(\"3\"));");

        try {
            test(ctx -> ctx.addDeclaration("p", Person.class),
                 "p.process2(\"1\", \"2\");",
                 "p.process2(Integer.parseInt(\"1\"), Integer.parseInt(\"2\"));");
            fail("There is no such method, so it should not attempt coercion and the test should fail");
        } catch (AssertionError e) {
            // swallow
        }
    }

    @Test
    public void testVarArgCoercionStringsThenVarArgNumbers() {
        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.process1(1, 2, 3, 4, 5);",
             "p.process1(String.valueOf(1), String.valueOf(2), String.valueOf(3), 4, 5);");

        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.process1(1, 2, 3, \"4\", \"5\");",
             "p.process1(String.valueOf(1), String.valueOf(2), String.valueOf(3), Integer.parseInt(\"4\"), Integer.parseInt(\"5\"));");

        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.process1(1, 2, 3);",
             "p.process1(String.valueOf(1), String.valueOf(2), String.valueOf(3));");

        try {
            test(ctx -> ctx.addDeclaration("p", Person.class),
                 "p.process1(1, 2);",
                 "p.process1(String.valueOf(1), String.valueOf(2));");
            fail("There is no such method, so it should not attempt coercion and the test should fail");
        } catch (AssertionError e) {
            // swallow
        }
    }

}