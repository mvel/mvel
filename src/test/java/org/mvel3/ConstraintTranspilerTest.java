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

import org.junit.Ignore;
import org.mvel3.transpiler.TranspiledResult;
import org.mvel3.transpiler.context.Declaration;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;

public class ConstraintTranspilerTest implements TranspilerTest {

    @Test
    public void testBigDecimalPromotion() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = salary + salary;}",
                       "{var x = _this.getSalary().add(_this.getSalary(), java.math.MathContext.DECIMAL128);}");
    }

    @Test @Ignore // we are not coercing Strings yet (mdp);
    public void testBigDecimalStringEquality() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = salary == \"90\";}",
                       "{var x = _this.getSalary().compareTo(new java.math.BigDecimal(\"90\")) == 0;}");
    }

    @Test
    public void testBigDecimalPromotionToIntMethod() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = isEven(salary.intValue());}",
                       "{var x = _this.isEven(_this.getSalary().intValue());}");
    }

    @Test
    public void testConversionConstructorArgument() {
        testExpression(c -> c.addDeclaration("$p", Person.class), "{var x = new Person($p.name, $p);}",
                       "{var x = new Person($p.getName(), $p);}");
    }

    @Test
    public void testBigDecimalMultiplyInt() {
        testExpression(c -> c.addDeclaration("$bd1", BigDecimal.class), "{var x = $bd1 * 10;}",
                       "{var x = $bd1.multiply(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);}");
    }

    @Test
    public void testBigDecimalMultiplyNegativeInt() {
        testExpression(c -> c.addDeclaration("$bd1", BigDecimal.class), "{var x = $bd1 * -1;}",
                       "{var x = $bd1.multiply(BigDecimal.valueOf(-1), java.math.MathContext.DECIMAL128);}");
    }

    @Test
    public void testBigDecimalAddInt() {
        testExpression(c -> c.addDeclaration("$bd1", BigDecimal.class), "{var x = $bd1 + 10;}",
                       "{var x = $bd1.add(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);}");
    }

    @Test
    public void testBigDecimalSubtractInt() {
        testExpression(c -> c.addDeclaration("$bd1", BigDecimal.class), "{var x = $bd1 - 10;}",
                       "{var x = $bd1.subtract(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);}");
    }

    @Test
    public void testBigDecimalDivideInt() {
        testExpression(c -> c.addDeclaration("$bd1", BigDecimal.class), "{var x = $bd1 / 10;}",
                       "{var x = $bd1.divide(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);}");
    }

    @Test
    public void testBigDecimalModInt() {
        testExpression(c -> c.addDeclaration("$bd1", BigDecimal.class), "{var x = $bd1 % 10;}",
                       "{var x = $bd1.remainder(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);}");
    }

    @Test @Ignore // no coercion of Strings yet (mdp)
    public void testBigDecimalStringNonEquality() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = salary != \"90\";}",
                       "{var x = _this.getSalary().compareTo(new java.math.BigDecimal(\"90\")) != 0;}");
    }

    @Test
    public void testRootObjectWithPropertyAndBigRewrite() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = salary != 90;}",
                       "{var x = _this.getSalary().compareTo(BigDecimal.valueOf(90)) != 0;}");
    }

    @Test
    public void testRootObjectWithNestedPropertiesAndBigRewrite() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = parent.salary != 90;}",
                       "{var x = _this.getParent().getSalary().compareTo(BigDecimal.valueOf(90)) != 0;}");
    }

    @Test
    public void testRootObjectWithPropertyAndNestedMethdAndBigRewrite() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = parent.getSalary() != 90;}",
                       "{var x = _this.getParent().getSalary().compareTo(BigDecimal.valueOf(90)) != 0;}");
    }

    @Test
    public void testRootObjectWithMethodAndBigRewrite() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = getSalary() != 90;}",
                       "{var x = _this.getSalary().compareTo(BigDecimal.valueOf(90)) != 0;}");
    }

    @Test
    public void testRootObjectWithMethodAndNestedPropertyAndBigRewrite() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = getParent().salary != 90;}",
                       "{var x = _this.getParent().getSalary().compareTo(BigDecimal.valueOf(90)) != 0;}");
    }

    @Test
    public void testRootObjectWithMethodAndNestedMethodAndBigRewrite() {
        testExpression(c -> c.setRootDeclaration(Declaration.of("_this", Person.class)), "{var x = getParent().getSalary() != 90;}",
                       "{var x = _this.getParent().getSalary().compareTo(BigDecimal.valueOf(90)) != 0;}");
    }

    public <K, R> void testExpression(Consumer<EvaluatorBuilder<Map, Void, Object>> testFunction,
                               String inputExpression,
                               String expectedResult,
                               Consumer<TranspiledResult> resultAssert) {
        test(testFunction,
             inputExpression,
             expectedResult,
             resultAssert);
    }

    <K, R> void testExpression(Consumer<EvaluatorBuilder<Map, Void, Object>> testFunction,
                                  String inputExpression,
                                  String expectedResult) {
        testExpression(testFunction, inputExpression, expectedResult, t -> {
        });
    }
}