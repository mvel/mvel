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

//    @Test
//    public void testInlineCast3() {
//        test(ctx -> ctx.addDeclaration("l", List.class),
//             "l#ArrayList#[0];",
//             "((ArrayList)l).get(0);");
//    }
}